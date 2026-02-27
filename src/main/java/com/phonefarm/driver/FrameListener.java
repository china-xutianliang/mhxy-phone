package com.phonefarm.driver;

import java.awt.image.BufferedImage;

/**
 * 每帧回调。VideoDriver 在采集线程中同步调用，
 * 实现方应尽快返回以免阻塞采集。
 */
@FunctionalInterface
public interface FrameListener {
    void onFrame(BufferedImage image, double fps);
}
