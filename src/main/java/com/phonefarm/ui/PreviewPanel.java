package com.phonefarm.ui;

import com.phonefarm.driver.FrameListener;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 纯净视频渲染面板。
 * 不绘制任何覆盖层（FPS、设备名等），确保大漠截图/找图无干扰。
 */
public class PreviewPanel extends JPanel implements FrameListener {

    private volatile BufferedImage currentImage;

    public PreviewPanel() {
        setDoubleBuffered(true);
        setBackground(Color.BLACK);
    }

    @Override
    public void onFrame(BufferedImage image, double fps) {
        this.currentImage = image;
        repaint();
    }

    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage img = currentImage;
        if (img == null) {
            drawWaiting(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int pw = getWidth();
        int ph = getHeight();
        int iw = img.getWidth();
        int ih = img.getHeight();

        double scale = Math.min((double) pw / iw, (double) ph / ih);
        int dw = (int) (iw * scale);
        int dh = (int) (ih * scale);
        int dx = (pw - dw) / 2;
        int dy = (ph - dh) / 2;

        g2.drawImage(img, dx, dy, dw, dh, null);
    }

    private void drawWaiting(Graphics g) {
        g.setColor(new Color(60, 60, 60));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        String text = "waiting...";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text,
                (getWidth() - fm.stringWidth(text)) / 2,
                (getHeight() + fm.getAscent()) / 2);
    }
}
