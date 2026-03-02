package com.phonefarm.driver;

import com.phonefarm.config.DeviceConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VideoDriverTest {

    @Test
    void newDriver_shouldNotBeRunning() {
        DeviceConfig config = new DeviceConfig("FakeDevice");
        VideoDriver driver = new VideoDriver(config);
        assertFalse(driver.isRunning());
        assertFalse(driver.hasStartedSuccessfully());
        assertNull(driver.getLastError());
    }

    @Test
    void config_shouldBeAccessible() {
        DeviceConfig config = new DeviceConfig("TestDevice");
        VideoDriver driver = new VideoDriver(config);
        assertSame(config, driver.getConfig());
    }
}
