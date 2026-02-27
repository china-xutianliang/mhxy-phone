package com.phonefarm.device;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeviceDiscoveryTest {

    @Test
    void listVideoDevices_shouldReturnNonNullList() {
        List<String> devices = DeviceDiscovery.listVideoDevices();
        assertNotNull(devices, "设备列表不应为 null");
        System.out.println("检测到设备数: " + devices.size());
        devices.forEach(d -> System.out.println("  → " + d));
    }

    @Test
    void listVideoDevices_calledTwice_shouldBeConsistent() {
        List<String> first = DeviceDiscovery.listVideoDevices();
        List<String> second = DeviceDiscovery.listVideoDevices();
        assertEquals(first, second, "两次枚举结果应一致");
    }
}
