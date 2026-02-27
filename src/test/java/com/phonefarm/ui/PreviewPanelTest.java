package com.phonefarm.ui;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class PreviewPanelTest {

    @Test
    void onFrame_shouldUpdateCurrentImage() {
        PreviewPanel panel = new PreviewPanel();
        assertNull(panel.getCurrentImage(), "初始应无画面");

        BufferedImage testImg = new BufferedImage(1920, 1080, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = testImg.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 1920, 1080);
        g.dispose();

        panel.onFrame(testImg, 30.0);

        assertNotNull(panel.getCurrentImage(), "应已接收画面");
        assertEquals(1920, panel.getCurrentImage().getWidth());
        assertEquals(1080, panel.getCurrentImage().getHeight());
    }

    @Test
    void paintComponent_withNoImage_shouldNotThrow() {
        PreviewPanel panel = new PreviewPanel();
        panel.setSize(800, 600);

        BufferedImage canvas = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        assertDoesNotThrow(() -> panel.paintComponent(g2));
        g2.dispose();
    }

    @Test
    void paintComponent_withImage_shouldNotThrow() {
        PreviewPanel panel = new PreviewPanel();
        panel.setSize(800, 600);

        BufferedImage testImg = new BufferedImage(1920, 1080, BufferedImage.TYPE_3BYTE_BGR);
        panel.onFrame(testImg, 60.0);

        BufferedImage canvas = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        assertDoesNotThrow(() -> panel.paintComponent(g2));
        g2.dispose();
    }
}
