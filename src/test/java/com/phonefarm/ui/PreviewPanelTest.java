package com.phonefarm.ui;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class PreviewPanelTest {

    @Test
    void initialState_shouldHaveNullImage() {
        PreviewPanel panel = new PreviewPanel();
        assertNull(panel.getCurrentImage());
    }

    @Test
    void onFrame_shouldUpdateCurrentImage() {
        PreviewPanel panel = new PreviewPanel();
        BufferedImage img = new BufferedImage(100, 200, BufferedImage.TYPE_3BYTE_BGR);
        panel.onFrame(img, 30.0);
        assertSame(img, panel.getCurrentImage());
    }
}
