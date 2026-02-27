package com.phonefarm.ui;

import com.phonefarm.config.DeviceConfig;
import com.phonefarm.device.DeviceDiscovery;
import com.phonefarm.driver.VideoDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * 主预览窗口：设备选择 → 打开采集卡 → 实时显示画面。
 */
public class PreviewFrame extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(PreviewFrame.class);

    private final PreviewPanel previewPanel;
    private VideoDriver videoDriver;

    public PreviewFrame() {
        super("Auto-MHXY · 手机自动化管理平台");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(640, 400));

        previewPanel = new PreviewPanel();
        add(previewPanel, BorderLayout.CENTER);

        JPanel bottomBar = createBottomBar();
        add(bottomBar, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
                dispose();
                System.exit(0);
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * 发现设备 → 用户选择 → 启动采集。
     * 返回 true 表示成功启动。
     */
    public boolean discoverAndStart() {
        List<String> devices = DeviceDiscovery.listVideoDevices();
        if (devices.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "未检测到视频采集设备，请检查采集卡连接。",
                    "设备未找到", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        String selected;
        if (devices.size() == 1) {
            selected = devices.get(0);
            log.info("仅检测到一个设备，自动选择: {}", selected);
        } else {
            selected = (String) JOptionPane.showInputDialog(this,
                    "检测到多个视频设备，请选择：",
                    "选择采集设备",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    devices.toArray(),
                    devices.get(0));
            if (selected == null) return false;
        }

        startCapture(new DeviceConfig(selected));
        return true;
    }

    public void startCapture(DeviceConfig config) {
        shutdown();
        videoDriver = new VideoDriver(config);
        videoDriver.addListener(previewPanel);
        videoDriver.start();
        setTitle("Auto-MHXY · " + config.getDeviceName());
    }

    public void shutdown() {
        if (videoDriver != null && videoDriver.isRunning()) {
            videoDriver.removeListener(previewPanel);
            videoDriver.stop();
            videoDriver = null;
        }
    }

    public PreviewPanel getPreviewPanel() {
        return previewPanel;
    }

    public VideoDriver getVideoDriver() {
        return videoDriver;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(new Color(45, 45, 45));

        JButton screenshotBtn = new JButton("截图");
        screenshotBtn.setToolTipText("截取当前虚拟画布");
        screenshotBtn.addActionListener(e -> takeScreenshot());
        bar.add(screenshotBtn);

        JButton refreshBtn = new JButton("刷新设备");
        refreshBtn.addActionListener(e -> discoverAndStart());
        bar.add(refreshBtn);

        return bar;
    }

    private void takeScreenshot() {
        var img = previewPanel.getCurrentImage();
        if (img == null) {
            JOptionPane.showMessageDialog(this, "当前无画面", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            java.io.File dir = new java.io.File("screenshots");
            dir.mkdirs();
            String filename = "screenshots/screenshot_" +
                    System.currentTimeMillis() + ".png";
            javax.imageio.ImageIO.write(img, "png", new java.io.File(filename));
            log.info("截图已保存: {}", filename);
            JOptionPane.showMessageDialog(this, "截图已保存: " + filename);
        } catch (Exception ex) {
            log.error("截图失败", ex);
        }
    }
}
