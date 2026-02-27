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
 * 视频采集驱动：一个实例绑定一块采集卡，在专属线程中持续抓帧。
 * <p>
 * 帧处理流水线（Step-1 仅实现 1→5）：
 * <ol>
 *   <li>FFmpegFrameGrabber 抓取原始帧</li>
 *   <li>ROI 裁剪（Step-2）</li>
 *   <li>旋转矫正（Step-3）</li>
 *   <li>输出虚拟画布（Mat + BufferedImage）</li>
 *   <li>通知所有 FrameListener</li>
 * </ol>
 */
public class VideoDriver implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(VideoDriver.class);

    private final DeviceConfig config;
    private final List<FrameListener> listeners = new CopyOnWriteArrayList<>();
    private final Java2DFrameConverter converter = new Java2DFrameConverter();

    private volatile boolean running;
    private volatile double currentFps;
    private Thread captureThread;

    public VideoDriver(DeviceConfig config) {
        this.config = config;
    }

    public void addListener(FrameListener listener) {
        listeners.add(listener);
    }

    public void removeListener(FrameListener listener) {
        listeners.remove(listener);
    }

    public double getCurrentFps() {
        return currentFps;
    }

    public boolean isRunning() {
        return running;
    }

    public DeviceConfig getConfig() {
        return config;
    }

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
            try {
                captureThread.join(3000);
            } catch (InterruptedException ignored) {
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
            grabber.start();
            log.info("采集卡已打开: {} ({}x{})",
                    config.getDeviceName(), grabber.getImageWidth(), grabber.getImageHeight());

            AtomicInteger frameCount = new AtomicInteger();
            long fpsTimer = System.nanoTime();

            while (running && !Thread.currentThread().isInterrupted()) {
                Frame frame = grabber.grab();
                if (frame == null || frame.image == null) continue;

                BufferedImage image = converter.convert(frame);
                if (image == null) continue;

                BufferedImage copy = deepCopy(image);
                frameCount.incrementAndGet();

                long now = System.nanoTime();
                if (now - fpsTimer >= 1_000_000_000L) {
                    currentFps = frameCount.getAndSet(0) / ((now - fpsTimer) / 1.0e9);
                    fpsTimer = now;
                }

                for (FrameListener listener : listeners) {
                    try {
                        listener.onFrame(copy, currentFps);
                    } catch (Exception e) {
                        log.warn("FrameListener 异常", e);
                    }
                }
            }
        } catch (Exception e) {
            if (running) {
                log.error("采集线程异常: {}", config.getDeviceName(), e);
            }
        } finally {
            if (grabber != null) {
                try { grabber.stop(); } catch (Exception ignored) {}
                try { grabber.release(); } catch (Exception ignored) {}
            }
            running = false;
            log.info("采集线程退出: {}", config.getDeviceName());
        }
    }

    private FFmpegFrameGrabber createGrabber() {
        FFmpegFrameGrabber g = new FFmpegFrameGrabber("video=" + config.getDeviceName());
        g.setFormat("dshow");
        g.setImageWidth(config.getCaptureWidth());
        g.setImageHeight(config.getCaptureHeight());
        g.setOption("rtbufsize", "702000k");
        g.setOption("fflags", "nobuffer");
        g.setOption("flags", "low_delay");
        g.setOption("probesize", "5000000");
        g.setOption("analyzeduration", "1000000");
        return g;
    }

    /**
     * Frame 内部缓冲区会被下一帧覆盖，必须深拷贝后才能跨线程传递。
     */
    private static BufferedImage deepCopy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }
}
