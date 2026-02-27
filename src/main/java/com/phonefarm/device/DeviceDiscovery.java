package com.phonefarm.device;

import org.bytedeco.javacpp.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通过 JavaCV 内置的 ffmpeg 可执行文件枚举 DirectShow 视频采集设备。
 * 仅适用于 Windows；Linux/macOS 需切换为 v4l2 / avfoundation。
 */
public final class DeviceDiscovery {

    private static final Logger log = LoggerFactory.getLogger(DeviceDiscovery.class);
    private static final Pattern DEVICE_NAME = Pattern.compile("\"(.+?)\"");

    private DeviceDiscovery() {}

    /**
     * 列出当前系统所有 DirectShow 视频设备名称。
     * 返回空列表而非 null 表示未检测到设备。
     */
    public static List<String> listVideoDevices() {
        List<String> devices = new ArrayList<>();
        try {
            String ffmpegPath = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
            log.debug("ffmpeg executable: {}", ffmpegPath);

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-list_devices", "true",
                    "-f", "dshow",
                    "-i", "dummy"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            List<String> output = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }
            process.waitFor();

            boolean inVideoSection = false;
            for (String line : output) {
                if (line.contains("DirectShow video devices")) {
                    inVideoSection = true;
                    continue;
                }
                if (line.contains("DirectShow audio devices")) {
                    break;
                }
                if (inVideoSection && !line.contains("Alternative name")) {
                    Matcher m = DEVICE_NAME.matcher(line);
                    if (m.find()) {
                        devices.add(m.group(1));
                    }
                }
            }

            log.info("发现 {} 个视频设备: {}", devices.size(), devices);
        } catch (Exception e) {
            log.error("枚举视频设备失败", e);
        }
        return devices;
    }
}
