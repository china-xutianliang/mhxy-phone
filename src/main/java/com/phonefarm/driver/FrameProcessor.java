package com.phonefarm.driver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * 帧后处理器：ROI 自动检测 + 裁剪。
 * 不做旋转和缩放，保留原始像素，缩放交给显示端。
 */
public final class FrameProcessor {

    private static final Logger log = LoggerFactory.getLogger(FrameProcessor.class);
    private static final int DEFAULT_BRIGHTNESS_THRESHOLD = 20;

    private FrameProcessor() {}

    // ── 自动 ROI 检测 ──

    public static Rectangle detectRoi(BufferedImage image) {
        return detectRoi(image, DEFAULT_BRIGHTNESS_THRESHOLD);
    }

    public static Rectangle detectRoi(BufferedImage image, int threshold) {
        int w = image.getWidth();
        int h = image.getHeight();
        int step = 2;

        int left   = scanLeft(image, w, h, step, threshold);
        int right  = scanRight(image, w, h, step, threshold, left);
        int top    = scanTop(image, w, h, step, threshold, left, right);
        int bottom = scanBottom(image, w, h, step, threshold, left, right, top);

        int margin = 2;
        left   = Math.max(0, left - margin);
        top    = Math.max(0, top - margin);
        right  = Math.min(w - 1, right + margin);
        bottom = Math.min(h - 1, bottom + margin);

        int roiW = right - left + 1;
        int roiH = bottom - top + 1;

        if (roiW < w / 10 || roiH < h / 10) {
            log.warn("检测到的 ROI 过小 ({}x{})，跳过裁剪", roiW, roiH);
            return null;
        }

        float areaPct = (float) roiW * roiH / (w * h) * 100;
        if (areaPct > 98f) {
            log.info("ROI 接近全帧 ({}%)，无需裁剪", String.format("%.1f", areaPct));
            return null;
        }

        return new Rectangle(left, top, roiW, roiH);
    }

    /**
     * 裁剪 ROI 区域，输出原始像素（不旋转、不缩放）。
     */
    public static BufferedImage crop(BufferedImage raw, Rectangle roi) {
        int sx, sy, sw, sh;
        if (roi != null) {
            sx = clamp(roi.x, 0, raw.getWidth());
            sy = clamp(roi.y, 0, raw.getHeight());
            sw = Math.min(roi.width, raw.getWidth() - sx);
            sh = Math.min(roi.height, raw.getHeight() - sy);
        } else {
            sx = 0; sy = 0;
            sw = raw.getWidth();
            sh = raw.getHeight();
        }
        if (sw <= 0 || sh <= 0) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
        }

        BufferedImage out = new BufferedImage(sw, sh, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(raw, 0, 0, sw, sh, sx, sy, sx + sw, sy + sh, null);
        g2.dispose();
        return out;
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(val, max));
    }

    // ── 边界扫描 ──

    private static int scanLeft(BufferedImage img, int w, int h, int step, int thr) {
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y += step)
                if (bright(img.getRGB(x, y)) > thr) return x;
        return 0;
    }

    private static int scanRight(BufferedImage img, int w, int h, int step, int thr, int left) {
        for (int x = w - 1; x >= left; x--)
            for (int y = 0; y < h; y += step)
                if (bright(img.getRGB(x, y)) > thr) return x;
        return w - 1;
    }

    private static int scanTop(BufferedImage img, int w, int h, int step, int thr,
                                int left, int right) {
        for (int y = 0; y < h; y++)
            for (int x = left; x <= right; x += step)
                if (bright(img.getRGB(x, y)) > thr) return y;
        return 0;
    }

    private static int scanBottom(BufferedImage img, int w, int h, int step, int thr,
                                   int left, int right, int top) {
        for (int y = h - 1; y >= top; y--)
            for (int x = left; x <= right; x += step)
                if (bright(img.getRGB(x, y)) > thr) return y;
        return h - 1;
    }

    private static int bright(int rgb) {
        return (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3;
    }
}
