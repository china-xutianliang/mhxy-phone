package com.phonefarm.ui;

import com.phonefarm.driver.FrameListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * 实时视频预览面板。
 * <ul>
 *   <li>等比缩放画面，黑底填充</li>
 *   <li>右上角叠加实时 FPS</li>
 *   <li>鼠标点击输出归一化坐标 (0~10000)，为 Step-4 做准备</li>
 * </ul>
 */
public class PreviewPanel extends JPanel implements FrameListener {

    private static final Logger log = LoggerFactory.getLogger(PreviewPanel.class);

    private volatile BufferedImage currentImage;
    private volatile double fps;

    // 缓存绘制参数，供坐标转换复用
    private int drawX, drawY, drawW, drawH;
    private double drawScale;

    public PreviewPanel() {
        setDoubleBuffered(true);
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(960, 540));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e);
            }
        });
    }

    @Override
    public void onFrame(BufferedImage image, double fps) {
        this.currentImage = image;
        this.fps = fps;
        repaint();
    }

    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        BufferedImage img = currentImage;
        if (img == null) {
            drawNoSignal(g2);
            return;
        }

        double scaleX = (double) getWidth() / img.getWidth();
        double scaleY = (double) getHeight() / img.getHeight();
        drawScale = Math.min(scaleX, scaleY);
        drawW = (int) (img.getWidth() * drawScale);
        drawH = (int) (img.getHeight() * drawScale);
        drawX = (getWidth() - drawW) / 2;
        drawY = (getHeight() - drawH) / 2;

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, drawX, drawY, drawW, drawH, null);

        drawFpsOverlay(g2);
    }

    private void drawNoSignal(Graphics2D g2) {
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        String text = "No Signal";
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(text)) / 2;
        int y = (getHeight() + fm.getAscent()) / 2;
        g2.drawString(text, x, y);
    }

    private void drawFpsOverlay(Graphics2D g2) {
        if (fps <= 0) return;
        String text = String.format("FPS: %.1f", fps);
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(text);
        int px = getWidth() - textW - 12;
        int py = 6;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(px - 4, py, textW + 8, fm.getHeight() + 4, 6, 6);
        g2.setColor(Color.GREEN);
        g2.drawString(text, px, py + fm.getAscent() + 2);
    }

    private void handleClick(MouseEvent e) {
        BufferedImage img = currentImage;
        if (img == null || drawScale <= 0) return;

        int imgX = (int) ((e.getX() - drawX) / drawScale);
        int imgY = (int) ((e.getY() - drawY) / drawScale);

        if (imgX < 0 || imgX >= img.getWidth() || imgY < 0 || imgY >= img.getHeight()) {
            return;
        }

        int normX = imgX * 10000 / img.getWidth();
        int normY = imgY * 10000 / img.getHeight();
        log.info("点击: pixel({}, {}) → normalized({}, {})", imgX, imgY, normX, normY);
    }
}
