package com.phonefarm.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceConfigTest {

    @Test
    void defaultConfig_shouldHave1080pResolution() {
        DeviceConfig config = new DeviceConfig("TestDevice");
        assertEquals("TestDevice", config.getDeviceName());
        assertEquals(1920, config.getCaptureWidth());
        assertEquals(1080, config.getCaptureHeight());
        assertFalse(config.hasRoi());
        assertEquals(0, config.getRotation());
    }

    @Test
    void customResolution_shouldBePreserved() {
        DeviceConfig config = new DeviceConfig("TestDevice", 1280, 720);
        assertEquals(1280, config.getCaptureWidth());
        assertEquals(720, config.getCaptureHeight());
    }

    @Test
    void roi_whenSet_shouldReportHasRoi() {
        DeviceConfig config = new DeviceConfig("TestDevice");
        config.setRoiX(100);
        config.setRoiY(50);
        config.setRoiWidth(800);
        config.setRoiHeight(1200);
        assertTrue(config.hasRoi());
    }

    @Test
    void toString_shouldContainDeviceName() {
        DeviceConfig config = new DeviceConfig("USB Video");
        assertTrue(config.toString().contains("USB Video"));
    }
}
