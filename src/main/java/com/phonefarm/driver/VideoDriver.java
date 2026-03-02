package com.phonefarm.driver;

import com.phonefarm.config.DeviceConfig;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 视频采集驱动：绑定一块采集卡，持续抓帧并完成 ROI 裁剪。
 * 不强制设置采集分辨率，使用采集卡原生输出；
 * 不做旋转和缩放，保留原始像素交给显示端处理。
 */
public class VideoDriver implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(VideoDriver.class);
    private static final int ROI_WARMUP_FRAMES = 15;

    private final DeviceConfig config;
    private final List<FrameListener> listeners = new CopyOnWriteArrayList<>();
    private final Java2DFrameConverter converter = new Java2DFrameConverter();

    private volatile boolean running;
    private volatile boolean startedSuccessfully;
    private volatile String lastError;
    private volatile double currentFps;
    private Thread captureThread;

    public VideoDriver(DeviceConfig config) {
        this.config = config;
    }

    public void addListener(FrameListener listener)    { listeners.add(listener); }
    public void removeListener(FrameListener listener) { listeners.remove(listener); }
    public double getCurrentFps()                      { return currentFps; }
    public boolean isRunning()                         { return running; }
    public DeviceConfig getConfig()                    { return config; }
    public boolean hasStartedSuccessfully()            { return startedSuccessfully; }
    public String getLastError()                       { return lastError; }

    public void start() {
        if (running) return;
        running = true;
        captureThread = new Thread(this, "VideoDriver-" + config.getDeviceName());
        captureThread.setDaemon(true);
        captureThread.start();
        log.info("VideoDriver 启动: {}", config);
    }

    public void stop() {
        running = false;
        if (captureThread != null) {
            captureThread.interrupt();
            try { captureThread.join(3000); } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("VideoDriver 停止: {}", config.getDeviceName());
    }

    @Override
    public void run() {
        FFmpegFrameGrabber grabber = null;
        try {
            grabber = createGrabber();
            log.info("正在打开采集设备: {} ...", config.getDeviceName());
            grabber.start();

            int rawW = grabber.getImageWidth();
            int rawH = grabber.getImageHeight();
            log.info("采集卡已打开: {} (原始 {}x{})", config.getDeviceName(), rawW, rawH);
            startedSuccessfully = true;

            Rectangle roi = resolveRoi();
            boolean needCrop = (roi != null);
            boolean roiResolved = (roi != null || !config.isAutoRoi());
            int warmup = 0;

            AtomicInteger frameCount = new AtomicInteger();
            long fpsTimer = System.nanoTime();

            while (running && !Thread.currentThread().isInterrupted()) {
                Frame frame = grabber.grab();
                if (frame == null || frame.image == null) continue;

                BufferedImage raw = converter.convert(frame);
                if (raw == null) continue;

                if (!roiResolved) {
                    warmup++;
                    if (warmup >= ROI_WARMUP_FRAMES) {
                        roi = FrameProcessor.detectRoi(raw);
                        roiResolved = true;
                        needCrop = (roi != null);
                        if (roi != null) {
                            log.info("[{}] 自动 ROI: x={} y={} {}x{}",
                                    config.getDeviceName(), roi.x, roi.y, roi.width, roi.height);
                        } else {
                            log.info("[{}] 无需裁剪（手机内容填满采集画面）", config.getDeviceName());
                        }
                    }
                }

                BufferedImage processed = needCrop
                        ? FrameProcessor.crop(raw, roi)
                        : deepCopy(raw);

                frameCount.incrementAndGet();
                long now = System.nanoTime();
                if (now - fpsTimer >= 1_000_000_000L) {
                    currentFps = frameCount.getAndSet(0) / ((now - fpsTimer) / 1.0e9);
                    fpsTimer = now;
                }

                for (FrameListener listener : listeners) {
                    try {
                        listener.onFrame(processed, currentFps);
                    } catch (Exception e) {
                        log.warn("FrameListener 异常", e);
                    }
                }
            }
        } catch (Exception e) {
            lastError = e.getMessage();
            log.error("采集线程异常: {}", config.getDeviceName(), e);
        } finally {
            if (grabber != null) {
                try { grabber.stop(); } catch (Exception ignored) {}
                try { grabber.release(); } catch (Exception ignored) {}
            }
            running = false;
            log.info("采集线程退出: {}", config.getDeviceName());
        }
    }

    private Rectangle resolveRoi() {
        if (config.hasManualRoi()) {
            Rectangle r = new Rectangle(
                    config.getRoiX(), config.getRoiY(),
                    config.getRoiWidth(), config.getRoiHeight());
            log.info("[{}] 使用手动 ROI: {}", config.getDeviceName(), r);
            return r;
        }
        return null;
    }

    private FFmpegFrameGrabber createGrabber() {
        FFmpegFrameGrabber g = new FFmpegFrameGrabber("video=" + config.getDeviceName());
        g.setFormat("dshow");
        g.setImageWidth(1920);
        g.setImageHeight(1080);
        g.setOption("rtbufsize", "50000k");
        g.setOption("probesize", "3000000");
        g.setOption("analyzeduration", "1000000");
        return g;
    }

    private static BufferedImage deepCopy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }
}
