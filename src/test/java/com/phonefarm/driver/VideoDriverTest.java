package com.phonefarm.driver;

import com.phonefarm.config.DeviceConfig;
import com.phonefarm.device.DeviceDiscovery;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class VideoDriverTest {

    @Test
    void constructor_shouldAcceptConfig() {
        DeviceConfig config = new DeviceConfig("FakeDevice");
        VideoDriver driver = new VideoDriver(config);
        assertFalse(driver.isRunning());
        assertSame(config, driver.getConfig());
    }

    @Test
    void startStop_withRealDevice_shouldGrabFrames() throws Exception {
        List<String> devices = DeviceDiscovery.listVideoDevices();
        assumeFalse(devices.isEmpty(), "跳过：未检测到采集设备");

        DeviceConfig config = new DeviceConfig(devices.get(0));
        VideoDriver driver = new VideoDriver(config);

        CountDownLatch latch = new CountDownLatch(5);
        AtomicReference<BufferedImage> lastFrame = new AtomicReference<>();

        driver.addListener((image, fps) -> {
            lastFrame.set(image);
            latch.countDown();
        });

        driver.start();
        assertTrue(driver.isRunning(), "驱动应处于运行状态");

        boolean received = latch.await(15, TimeUnit.SECONDS);
        assertTrue(received, "应在 15 秒内收到至少 5 帧");

        BufferedImage img = lastFrame.get();
        assertNotNull(img, "帧图像不应为 null");
        assertTrue(img.getWidth() > 0 && img.getHeight() > 0, "帧尺寸应为正值");

        System.out.printf("帧尺寸: %dx%d, FPS: %.1f%n",
                img.getWidth(), img.getHeight(), driver.getCurrentFps());

        driver.stop();
        assertFalse(driver.isRunning(), "停止后应为非运行状态");
    }

    @Test
    void listener_addAndRemove_shouldWork() {
        DeviceConfig config = new DeviceConfig("FakeDevice");
        VideoDriver driver = new VideoDriver(config);

        FrameListener listener = (img, fps) -> {};
        driver.addListener(listener);
        driver.removeListener(listener);
        // 无异常即通过
    }
}
