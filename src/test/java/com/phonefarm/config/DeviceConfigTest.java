package com.phonefarm.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceConfigTest {

    @Test
    void defaultConfig_shouldHaveDeviceName() {
        DeviceConfig config = new DeviceConfig("TestDevice");
        assertEquals("TestDevice", config.getDeviceName());
        assertTrue(config.isAutoRoi());
        assertFalse(config.hasManualRoi());
    }

    @Test
    void manualRoi_whenSet_shouldReportHasManualRoi() {
        DeviceConfig config = new DeviceConfig("TestDevice");
        config.setRoiX(100);
        config.setRoiY(50);
        config.setRoiWidth(800);
        config.setRoiHeight(1200);
        assertTrue(config.hasManualRoi());
    }

    @Test
    void toString_shouldContainDeviceName() {
        DeviceConfig config = new DeviceConfig("USB Video");
        assertTrue(config.toString().contains("USB Video"));
    }
}
