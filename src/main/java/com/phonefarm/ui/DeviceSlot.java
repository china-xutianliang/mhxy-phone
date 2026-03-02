package com.phonefarm.ui;

import com.phonefarm.config.DeviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 单台设备的运行时状态管理。
 * 状态机: IDLE -> PREVIEW
 * 投屏时自动检测横竖屏方向，自动缩放到 iPhone 13 分辨率。
 */
public class DeviceSlot {

    private static final Logger log = LoggerFactory.getLogger(DeviceSlot.class);

    public enum Status { IDLE, PREVIEW }

    private final int index;
    private final String deviceName;
    private final Consumer<String> uiLogger;

    private DeviceWindow window;
    private Status status = Status.IDLE;

    public DeviceSlot(int index, String deviceName, Consumer<String> uiLogger) {
        this.index = index;
        this.deviceName = deviceName;
        this.uiLogger = uiLogger;
    }

    public boolean openPreview() {
        if (status != Status.IDLE) return false;

        DeviceConfig config = new DeviceConfig(deviceName);
        config.setAutoRoi(true);

        window = new DeviceWindow(index, deviceName, config);
        window.setOnClose(() -> {
            shutdownAll();
            uiLog("投屏已关闭");
        });
        window.setVisible(true);
        window.startCapture();

        status = Status.PREVIEW;
        uiLog("投屏已开启 (自动检测方向, 缩放到 iPhone 13)");
        return true;
    }

    public void closePreview() {
        if (window != null) {
            window.stopCapture();
            window.dispose();
            window = null;
        }
        if (status == Status.PREVIEW) status = Status.IDLE;
    }

    public void shutdownAll() {
        closePreview();
        status = Status.IDLE;
    }

    public int getIndex()              { return index; }
    public String getDeviceName()      { return deviceName; }
    public Status getStatus()          { return status; }
    public DeviceWindow getWindow()    { return window; }

    public String getStatusText() {
        return switch (status) {
            case IDLE    -> "就绪";
            case PREVIEW -> "投屏中";
        };
    }

    private void uiLog(String msg) {
        String full = "MHXY_" + index + " " + msg;
        log.info(full);
        if (uiLogger != null) uiLogger.accept(full);
    }
}
