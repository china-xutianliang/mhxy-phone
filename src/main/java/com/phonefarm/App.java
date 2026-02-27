package com.phonefarm;

import com.phonefarm.ui.PreviewFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        log.info("===== Auto-MHXY 启动 =====");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            PreviewFrame frame = new PreviewFrame();
            frame.setVisible(true);
            frame.discoverAndStart();
        });
    }
}
