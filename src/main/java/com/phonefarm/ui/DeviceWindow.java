package com.phonefarm.ui;

import com.phonefarm.config.DeviceConfig;
import com.phonefarm.driver.FrameListener;
import com.phonefarm.driver.VideoDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

/**
 * 每台手机的独立预览窗口。
 * 窗口固定为 iPhone 13 逻辑分辨率（390×844 竖屏 / 844×390 横屏），
 * 根据首帧画面的宽高比自动判断横竖屏。
 */
public class DeviceWindow extends JFrame implements FrameListener {

    private static final Logger log = LoggerFactory.getLogger(DeviceWindow.class);

    /** iPhone 13 逻辑点分辨率 */
    public static final int IPHONE13_W = 390;
    public static final int IPHONE13_H = 844;

    private final int index;
    private final String deviceName;
    private final DeviceConfig config;
    private final PreviewPanel panel;
    private VideoDriver driver;
    private Runnable onClose;

    /** 当前窗口是否处于横屏布局, null=未初始化 */
    private volatile Boolean currentLandscape;

    public DeviceWindow(int index, String deviceName, DeviceConfig config) {
        super("MHXY_" + index);
        this.index = index;
        this.deviceName = deviceName;
        this.config = config;

        panel = new PreviewPanel();
        setContentPane(panel);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        applyWindowSize(IPHONE13_W, IPHONE13_H);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopCapture();
                if (onClose != null) onClose.run();
            }
        });
    }

    @Override
    public void onFrame(BufferedImage image, double fps) {
        if (image != null) {
            boolean landscape = image.getWidth() > image.getHeight();
            if (currentLandscape == null || currentLandscape != landscape) {
                currentLandscape = landscape;
                int winW = landscape ? IPHONE13_H : IPHONE13_W;
                int winH = landscape ? IPHONE13_W : IPHONE13_H;
                log.info("MHXY_{} 方向切换: {}x{} → {} → 窗口 {}x{}",
                        index, image.getWidth(), image.getHeight(),
                        landscape ? "横屏" : "竖屏", winW, winH);
                SwingUtilities.invokeLater(() -> applyWindowSize(winW, winH));
            }
        }
        panel.onFrame(image, fps);
    }

    private void applyWindowSize(int contentW, int contentH) {
        panel.setPreferredSize(new Dimension(contentW, contentH));
        pack();
        log.info("MHXY_{} 窗口内容区: {}x{}", index, contentW, contentH);
    }

    public void startCapture() {
        driver = new VideoDriver(config);
        driver.addListener(this);
        driver.start();
        log.info("MHXY_{} 开始采集: {}", index, deviceName);
    }

    public void stopCapture() {
        if (driver != null) {
            driver.removeListener(this);
            driver.stop();
            driver = null;
        }
    }

    public void setOnClose(Runnable onClose) { this.onClose = onClose; }
    public int getIndex()                    { return index; }
    public String getDeviceName()            { return deviceName; }
    public DeviceConfig getConfig()          { return config; }
    public VideoDriver getDriver()           { return driver; }
    public PreviewPanel getPanel()           { return panel; }
}
