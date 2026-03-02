package com.phonefarm.config;

/**
 * 单台设备的采集配置。
 * 帧处理流水线: Raw → ROI裁剪 → 输出原始像素 → 显示时缩放到窗口
 */
public class DeviceConfig {

    private String deviceName;

    // —— ROI 裁剪 ——
    private boolean autoRoi = true;
    private int roiX;
    private int roiY;
    private int roiWidth;
    private int roiHeight;

    public DeviceConfig() {}

    public DeviceConfig(String deviceName) {
        this.deviceName = deviceName;
    }

    public String  getDeviceName()                    { return deviceName; }
    public void    setDeviceName(String deviceName)    { this.deviceName = deviceName; }

    public boolean isAutoRoi()                         { return autoRoi; }
    public void    setAutoRoi(boolean autoRoi)         { this.autoRoi = autoRoi; }
    public int     getRoiX()                           { return roiX; }
    public void    setRoiX(int roiX)                   { this.roiX = roiX; }
    public int     getRoiY()                           { return roiY; }
    public void    setRoiY(int roiY)                   { this.roiY = roiY; }
    public int     getRoiWidth()                       { return roiWidth; }
    public void    setRoiWidth(int roiWidth)           { this.roiWidth = roiWidth; }
    public int     getRoiHeight()                      { return roiHeight; }
    public void    setRoiHeight(int roiHeight)         { this.roiHeight = roiHeight; }

    public boolean hasManualRoi() {
        return roiWidth > 0 && roiHeight > 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DeviceConfig{").append(deviceName);
        if (hasManualRoi()) sb.append(", roi=[").append(roiX).append(",").append(roiY)
                .append(" ").append(roiWidth).append("x").append(roiHeight).append("]");
        else if (autoRoi) sb.append(", autoRoi");
        return sb.append('}').toString();
    }
}
