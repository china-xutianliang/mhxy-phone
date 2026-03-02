package com.phonefarm.device;

import org.bytedeco.javacpp.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通过 JavaCV 内置的 ffmpeg 可执行文件枚举 DirectShow 视频采集设备。
 * 仅适用于 Windows；Linux/macOS 需切换为 v4l2 / avfoundation。
 */
public final class DeviceDiscovery {

    private static final Logger log = LoggerFactory.getLogger(DeviceDiscovery.class);

    /**
     * ffmpeg 6.x 格式: "DeviceName" (video) / (audio) / (none)
     * 捕获组1=设备名, 捕获组2=类型
     */
    private static final Pattern DEVICE_LINE =
            Pattern.compile("\"(.+?)\"\\s+\\((video|audio|none)\\)");

    /** ffmpeg 5.x 及更早版本的设备名匹配 */
    private static final Pattern DEVICE_NAME_LEGACY = Pattern.compile("\"(.+?)\"");

    /**
     * Windows 控制台进程输出使用系统代码页（中文系统为 GBK），
     * 而 Java 18+ 默认字符集为 UTF-8，需要显式指定。
     */
    private static final Charset PROCESS_CHARSET = detectProcessCharset();

    private static final Set<String> EXCLUDED_KEYWORDS = Set.of(
            "webcam", "camera", "facetime", "obs virtual", "virtual camera",
            "integrated", "ir ");

    private DeviceDiscovery() {}

    private static Charset detectProcessCharset() {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                return Charset.forName("GBK");
            } catch (Exception e) {
                return Charset.defaultCharset();
            }
        }
        return Charset.defaultCharset();
    }

    /**
     * 只返回采集卡/投屏设备（排除摄像头、虚拟摄像头）。
     * 过滤规则：只保留 (video) 类型，且设备名不含摄像头关键词。
     */
    public static List<String> listCaptureDevices() {
        List<String> all = listVideoDevices();
        List<String> filtered = new ArrayList<>();
        for (String name : all) {
            String lower = name.toLowerCase();
            boolean excluded = EXCLUDED_KEYWORDS.stream().anyMatch(lower::contains);
            if (excluded) {
                log.info("已排除非采集卡设备: \"{}\"", name);
            } else {
                filtered.add(name);
            }
        }
        log.info("采集卡设备 {} 个: {}", filtered.size(), filtered);
        return filtered;
    }

    /**
     * 列出当前系统所有 DirectShow 视频设备名称（不做过滤）。
     */
    public static List<String> listVideoDevices() {
        List<String> devices = new ArrayList<>();
        try {
            String ffmpegPath = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
            log.info("ffmpeg executable: {}", ffmpegPath);

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
                    new InputStreamReader(process.getInputStream(), PROCESS_CHARSET))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                    log.debug("[ffmpeg] {}", line);
                }
            }

            int exitCode = process.waitFor();
            log.debug("ffmpeg 退出码: {}", exitCode);

            if (output.isEmpty()) {
                log.warn("ffmpeg 无任何输出，可能路径或权限有问题: {}", ffmpegPath);
                return devices;
            }

            boolean hasNewFormat = output.stream()
                    .anyMatch(l -> DEVICE_LINE.matcher(l).find());

            if (hasNewFormat) {
                for (String line : output) {
                    if (line.contains("Alternative name")) continue;
                    Matcher m = DEVICE_LINE.matcher(line);
                    if (m.find()) {
                        String name = m.group(1);
                        String type = m.group(2);
                        if ("video".equals(type)) {
                            devices.add(name);
                        }
                    }
                }
            } else {
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
                        Matcher m = DEVICE_NAME_LEGACY.matcher(line);
                        if (m.find()) {
                            devices.add(m.group(1));
                        }
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
