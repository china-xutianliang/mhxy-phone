package com.phonefarm.config;

/**
 * 单台设备的完整配置，贯穿 Raw → VirtualCanvas → Script 三层。
 * 当前 Step-1 只使用 deviceName / captureWidth / captureHeight，
 * 其余字段为后续 ROI 标定、旋转、串口控制预留。
 */
public class DeviceConfig {

    private String deviceName;
    private int captureWidth = 1920;
    private int captureHeight = 1080;

    // —— Step-2/3 预留 ——
    private int roiX;
    private int roiY;
    private int roiWidth;
    private int roiHeight;
    private int rotation; // 0, 90, 180, 270

    public DeviceConfig() {}

    public DeviceConfig(String deviceName) {
        this.deviceName = deviceName;
    }

    public DeviceConfig(String deviceName, int captureWidth, int captureHeight) {
        this.deviceName = deviceName;
        this.captureWidth = captureWidth;
        this.captureHeight = captureHeight;
    }

    public String getDeviceName()                   { return deviceName; }
    public void   setDeviceName(String deviceName)   { this.deviceName = deviceName; }
    public int    getCaptureWidth()                  { return captureWidth; }
    public void   setCaptureWidth(int captureWidth)  { this.captureWidth = captureWidth; }
    public int    getCaptureHeight()                 { return captureHeight; }
    public void   setCaptureHeight(int captureHeight){ this.captureHeight = captureHeight; }
    public int    getRoiX()                          { return roiX; }
    public void   setRoiX(int roiX)                  { this.roiX = roiX; }
    public int    getRoiY()                          { return roiY; }
    public void   setRoiY(int roiY)                  { this.roiY = roiY; }
    public int    getRoiWidth()                      { return roiWidth; }
    public void   setRoiWidth(int roiWidth)          { this.roiWidth = roiWidth; }
    public int    getRoiHeight()                     { return roiHeight; }
    public void   setRoiHeight(int roiHeight)        { this.roiHeight = roiHeight; }
    public int    getRotation()                      { return rotation; }
    public void   setRotation(int rotation)          { this.rotation = rotation; }

    public boolean hasRoi() {
        return roiWidth > 0 && roiHeight > 0;
    }

    @Override
    public String toString() {
        return "DeviceConfig{" + deviceName +
                ", capture=" + captureWidth + "x" + captureHeight +
                (hasRoi() ? ", roi=[" + roiX + "," + roiY + " " + roiWidth + "x" + roiHeight + "]" : "") +
                ", rotation=" + rotation + '}';
    }
}
