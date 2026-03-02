package com.phonefarm.ui;

import com.phonefarm.device.DeviceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Auto-MHXY 管理主窗口。
 * 列: 设备名 | 投屏 | 状态
 * 投屏自动检测横竖屏方向，固定缩放到 iPhone 13 分辨率。
 */
public class MainFrame extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(MainFrame.class);

    private static final int COL_DEVICE  = 0;
    private static final int COL_PREVIEW = 1;
    private static final int COL_STATUS  = 2;

    private final List<DeviceSlot> slots = new ArrayList<>();
    private final DeviceTableModel tableModel = new DeviceTableModel();
    private final JTable table;
    private final JTextArea logArea;
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss");

    public MainFrame() {
        super("Auto-MHXY \u00b7 \u624b\u673a\u81ea\u52a8\u5316\u7ba1\u7406\u5e73\u53f0");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("stdout.encoding", "UTF-8");

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(620, 480));

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(40, 40, 40));

        // ── 顶部工具栏 ──
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        toolbar.setBackground(new Color(50, 50, 50));
        JButton refreshBtn = createButton("刷新设备");
        refreshBtn.addActionListener(e -> refreshDevices());
        toolbar.add(refreshBtn);
        JLabel titleLabel = new JLabel("  Auto-MHXY · iPhone 13");
        titleLabel.setForeground(new Color(180, 180, 180));
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        toolbar.add(titleLabel);
        root.add(toolbar, BorderLayout.NORTH);

        // ── 设备表格 ──
        table = new JTable(tableModel);
        table.setRowHeight(36);
        table.setBackground(new Color(45, 45, 45));
        table.setForeground(new Color(210, 210, 210));
        table.setGridColor(new Color(60, 60, 60));
        table.setSelectionBackground(new Color(70, 70, 70));
        table.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        table.getTableHeader().setBackground(new Color(55, 55, 55));
        table.getTableHeader().setForeground(new Color(200, 200, 200));
        table.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        setupColumns();

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(new Color(45, 45, 45));

        // ── 日志面板 ──
        logArea = new JTextArea(8, 60);
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(170, 210, 170));
        logArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        logArea.setLineWrap(true);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                "日志", 0, 0,
                new Font("Microsoft YaHei", Font.PLAIN, 12),
                new Color(140, 140, 140)));
        logScroll.setPreferredSize(new Dimension(0, 160));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, logScroll);
        splitPane.setDividerLocation(280);
        splitPane.setResizeWeight(0.7);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        root.add(splitPane, BorderLayout.CENTER);

        setContentPane(root);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdownAll();
                dispose();
                System.exit(0);
            }
        });

        pack();
        setLocationRelativeTo(null);
        appendLog("系统", "应用已启动，请点击 [刷新设备] 开始");
    }

    // ── 设备刷新 ──

    public void refreshDevices() {
        shutdownAll();
        appendLog("系统", "正在扫描设备...");
        List<String> devices = DeviceDiscovery.listCaptureDevices();
        slots.clear();
        for (int i = 0; i < devices.size(); i++) {
            slots.add(new DeviceSlot(i + 1, devices.get(i), this::appendLogRaw));
        }
        tableModel.fireTableDataChanged();
        if (devices.isEmpty()) {
            appendLog("系统", "未检测到采集卡设备");
        } else {
            appendLog("系统", "发现 " + devices.size() + " 个采集卡设备");
        }
    }

    private void shutdownAll() {
        for (DeviceSlot slot : slots) slot.shutdownAll();
    }

    // ── 表格列配置 ──

    private void setupColumns() {
        table.getColumnModel().getColumn(COL_DEVICE).setPreferredWidth(220);
        table.getColumnModel().getColumn(COL_PREVIEW).setPreferredWidth(80);
        table.getColumnModel().getColumn(COL_STATUS).setPreferredWidth(100);

        table.getColumnModel().getColumn(COL_PREVIEW).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(COL_PREVIEW).setCellEditor(new ButtonEditor());

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(centerRenderer);
    }

    // ── 日志 ──

    private void appendLog(String source, String msg) {
        appendLogRaw(source + " " + msg);
    }

    private void appendLogRaw(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(timeFmt.format(new Date()) + " " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // ── 投屏按钮逻辑 ──

    private void onPreviewClick(int row) {
        if (row < 0 || row >= slots.size()) return;
        DeviceSlot slot = slots.get(row);

        if (slot.getStatus() == DeviceSlot.Status.IDLE) {
            slot.openPreview();
        } else {
            slot.shutdownAll();
        }
        tableModel.fireTableRowsUpdated(row, row);
    }

    private String getPreviewBtnText(int row) {
        if (row >= slots.size()) return "";
        return slots.get(row).getStatus() == DeviceSlot.Status.IDLE ? "打开" : "关闭";
    }

    // ══════════════════════════════════════
    //  TableModel
    // ══════════════════════════════════════

    private class DeviceTableModel extends AbstractTableModel {
        private final String[] COLS = {"设备名", "投屏", "状态"};

        @Override public int getRowCount()    { return slots.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int col) { return COLS[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= slots.size()) return "";
            DeviceSlot slot = slots.get(row);
            return switch (col) {
                case COL_DEVICE  -> slot.getDeviceName();
                case COL_PREVIEW -> getPreviewBtnText(row);
                case COL_STATUS  -> slot.getStatusText();
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object val, int row, int col) {
            // no editable data columns
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == COL_PREVIEW;
        }
    }

    // ══════════════════════════════════════
    //  Button Renderer / Editor
    // ══════════════════════════════════════

    private class ButtonRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                                                       boolean sel, boolean focus, int row, int col) {
            JButton btn = new JButton(val != null ? val.toString() : "");
            btn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            btn.setFocusPainted(false);
            btn.setBackground(new Color(60, 60, 60));
            btn.setForeground(new Color(220, 220, 220));
            return btn;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private int currentRow;

        public ButtonEditor() {
            super(new JCheckBox());
            button = new JButton();
            button.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            button.setFocusPainted(false);
            button.setBackground(new Color(60, 60, 60));
            button.setForeground(new Color(220, 220, 220));
            button.addActionListener(e -> {
                fireEditingStopped();
                onPreviewClick(currentRow);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val,
                                                     boolean sel, int row, int col) {
            currentRow = row;
            button.setText(val != null ? val.toString() : "");
            return button;
        }

        @Override
        public Object getCellEditorValue() { return button.getText(); }
    }

    // ── 工具方法 ──

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        btn.setBackground(new Color(70, 70, 70));
        btn.setForeground(new Color(220, 220, 220));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(90, 90, 90)),
                BorderFactory.createEmptyBorder(4, 16, 4, 16)));
        return btn;
    }
}
