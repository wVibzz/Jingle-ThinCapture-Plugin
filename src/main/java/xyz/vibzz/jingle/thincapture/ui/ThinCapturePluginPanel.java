package xyz.vibzz.jingle.thincapture.ui;

import xyz.vibzz.jingle.thincapture.ThinCapture;
import xyz.vibzz.jingle.thincapture.ThinCaptureOptions;
import xyz.vibzz.jingle.thincapture.config.CaptureConfig;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ThinCapturePluginPanel {
    public final JPanel mainPanel;
    private final JPanel capturesContainer;
    private JLabel sizeLabel;

    public ThinCapturePluginPanel() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        mainPanel.add(buildGeneralPanel());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        capturesContainer = new JPanel();
        capturesContainer.setLayout(new BoxLayout(capturesContainer, BoxLayout.Y_AXIS));
        capturesContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(capturesContainer);

        mainPanel.add(buildAddButtonRow());
        mainPanel.add(Box.createVerticalGlue());

        rebuildCaptures();
    }

    private JPanel buildGeneralPanel() {
        ThinCaptureOptions o = ThinCapture.getOptions();

        JPanel generalPanel = new JPanel();
        generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
        generalPanel.setBorder(BorderFactory.createTitledBorder("General"));
        generalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        generalPanel.add(buildAmdRow(o));
        generalPanel.add(buildSizeRow());
        generalPanel.add(buildFpsRow(o));

        return generalPanel;
    }

    private JPanel buildAmdRow(ThinCaptureOptions o) {
        JCheckBox amdBox = new JCheckBox("AMD Compatibility Mode");
        amdBox.setSelected(o.amdCompatMode);
        amdBox.addActionListener(a -> o.amdCompatMode = amdBox.isSelected());

        JLabel desc = new JLabel("Enable if captures show black on AMD. [GLOBAL TOGGLE]");
        desc.setFont(desc.getFont().deriveFont(Font.ITALIC, 11f));

        return createRow(amdBox, desc);
    }

    private JPanel buildSizeRow() {
        sizeLabel = new JLabel();
        refreshSizeLabel();

        JLabel desc = new JLabel("(synced from Resizing script)");
        desc.setFont(desc.getFont().deriveFont(Font.ITALIC, 11f));

        return createRow(new JLabel("Thin BT size:"), sizeLabel, desc);
    }

    private void refreshSizeLabel() {
        if (sizeLabel != null) {
            int w = ThinCapture.getEffectiveThinBTWidth();
            int h = ThinCapture.getEffectiveThinBTHeight();
            sizeLabel.setText(w + " \u00d7 " + h);
        }
    }

    private JPanel buildFpsRow(ThinCaptureOptions o) {
        JTextField fpsField = new JTextField(String.valueOf(o.fpsLimit), 4);
        fpsField.getDocument().addDocumentListener(docListener(() -> {
            o.fpsLimit = clamp(intFrom(fpsField, 30), 5, 240);
            ThinCapture.updateFpsLimit();
        }));

        return createRow(new JLabel("FPS limit:"), fpsField);
    }

    private JPanel buildAddButtonRow() {
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        addRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addBtn = new JButton("+ Add Capture");
        addBtn.addActionListener(a -> {
            String name = JOptionPane.showInputDialog(mainPanel, "Capture name:", "New Capture");
            if (name != null && !name.trim().isEmpty()) {
                ThinCapture.addCapture(name.trim());
                rebuildCaptures();
            }
        });
        addRow.add(addBtn);

        return addRow;
    }

    // ===== Capture Panel =====

    private JPanel buildCapturePanel(int index) {
        ThinCaptureOptions o = ThinCapture.getOptions();
        CaptureConfig c = o.captures.get(index);

        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(c.name));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(buildCaptureTopRow(index, c));
        section.add(buildMonitorRow(c));
        section.add(buildMCRegionRow(c));
        section.add(buildTransparencySection(c));

        return section;
    }

    private JPanel buildCaptureTopRow(int index, CaptureConfig c) {
        JCheckBox enableBox = new JCheckBox("Enabled");
        enableBox.setSelected(c.enabled);
        enableBox.addActionListener(a -> c.enabled = enableBox.isSelected());

        JButton renameBtn = createSmallButton("Rename", a -> {
            String newName = JOptionPane.showInputDialog(mainPanel, "New name:", c.name);
            if (newName != null && !newName.trim().isEmpty()) {
                ThinCapture.renameCapture(index, newName.trim());
                rebuildCaptures();
            }
        });

        JButton removeBtn = createRemoveButton("capture \"" + c.name + "\"", () -> {
            ThinCapture.removeCapture(index);
            rebuildCaptures();
        });

        return createRow(enableBox, renameBtn, removeBtn);
    }

    private JPanel buildMonitorRow(CaptureConfig c) {
        JTextField ox = field(c.screenX), oy = field(c.screenY), ow = field(c.screenW), oh = field(c.screenH);

        Consumer<Rectangle> onRegionSelected = r -> {
            ox.setText(String.valueOf(r.x));
            oy.setText(String.valueOf(r.y));
            ow.setText(String.valueOf(r.width));
            oh.setText(String.valueOf(r.height));
            c.screenX = r.x;
            c.screenY = r.y;
            c.screenW = r.width;
            c.screenH = r.height;
        };

        JButton selectBtn = createSmallButton("Select", a -> RegionSelector.selectOnScreen(onRegionSelected));

        JButton editBtn = createSmallButton("Edit", a -> {
            Rectangle current = new Rectangle(intFrom(ox, 0), intFrom(oy, 0), intFrom(ow, 200), intFrom(oh, 200));
            RegionSelector.editOnScreen(current, onRegionSelected);
        });

        JButton applyBtn = createSmallButton("Apply", a -> {
            c.screenX = intFrom(ox, 0);
            c.screenY = intFrom(oy, 0);
            c.screenW = Math.max(1, intFrom(ow, 200));
            c.screenH = Math.max(1, intFrom(oh, 200));
            ox.setText(String.valueOf(c.screenX));
            oy.setText(String.valueOf(c.screenY));
            ow.setText(String.valueOf(c.screenW));
            oh.setText(String.valueOf(c.screenH));
        });

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        row.add(new JLabel("Monitor  X:"));
        row.add(ox);
        row.add(new JLabel("Y:"));
        row.add(oy);
        row.add(new JLabel("W:"));
        row.add(ow);
        row.add(new JLabel("H:"));
        row.add(oh);
        row.add(selectBtn);
        row.add(editBtn);
        row.add(applyBtn);

        return row;
    }

    private JPanel buildMCRegionRow(CaptureConfig c) {
        JTextField rx = field(c.captureX), ry = field(c.captureY), rw = field(c.captureW), rh = field(c.captureH);

        Consumer<Rectangle> onRegionSelected = r -> {
            rx.setText(String.valueOf(r.x));
            ry.setText(String.valueOf(r.y));
            rw.setText(String.valueOf(r.width));
            rh.setText(String.valueOf(r.height));
            c.captureX = r.x;
            c.captureY = r.y;
            c.captureW = r.width;
            c.captureH = r.height;
        };

        JButton selectBtn = createSmallButton("Select", a -> RegionSelector.selectOnMCWindow(onRegionSelected));

        JButton editBtn = createSmallButton("Edit", a -> {
            Rectangle current = new Rectangle(intFrom(rx, 0), intFrom(ry, 0), intFrom(rw, 200), intFrom(rh, 200));
            RegionSelector.editOnMCWindow(current, onRegionSelected);
        });

        JButton applyBtn = createSmallButton("Apply", a -> {
            int effW = ThinCapture.getEffectiveThinBTWidth();
            int effH = ThinCapture.getEffectiveThinBTHeight();
            c.captureX = clamp(intFrom(rx, 0), 0, effW - 1);
            c.captureY = clamp(intFrom(ry, 0), 0, effH - 1);
            c.captureW = clamp(Math.max(1, intFrom(rw, 200)), 1, effW - c.captureX);
            c.captureH = clamp(Math.max(1, intFrom(rh, 200)), 1, effH - c.captureY);
            rx.setText(String.valueOf(c.captureX));
            ry.setText(String.valueOf(c.captureY));
            rw.setText(String.valueOf(c.captureW));
            rh.setText(String.valueOf(c.captureH));
        });

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        row.add(new JLabel("MC Region X:"));
        row.add(rx);
        row.add(new JLabel("Y:"));
        row.add(ry);
        row.add(new JLabel("W:"));
        row.add(rw);
        row.add(new JLabel("H:"));
        row.add(rh);
        row.add(selectBtn);
        row.add(editBtn);
        row.add(applyBtn);

        return row;
    }

    private JPanel buildTransparencySection(CaptureConfig c) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder("Transparency"));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JCheckBox transparencyBox = new JCheckBox("Enable (filter white text)");
        transparencyBox.setSelected(c.textOnly);

        JLabel threshLabel = new JLabel("Threshold:");
        JTextField threshField = new JTextField(String.valueOf(c.textThreshold), 3);
        JLabel threshNote = new JLabel("[0-255]");
        threshNote.setFont(threshNote.getFont().deriveFont(Font.ITALIC, 10f));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row1.add(transparencyBox);
        row1.add(Box.createHorizontalStrut(8));
        row1.add(threshLabel);
        row1.add(threshField);
        row1.add(threshNote);
        section.add(row1);

        JRadioButton bgTransparentRadio = new JRadioButton("Transparent");
        JRadioButton bgColorRadio = new JRadioButton("Solid color");
        JRadioButton bgImageRadio = new JRadioButton("Image");
        ButtonGroup bgGroup = new ButtonGroup();
        bgGroup.add(bgTransparentRadio);
        bgGroup.add(bgColorRadio);
        bgGroup.add(bgImageRadio);

        if (c.transparentBg) {
            bgTransparentRadio.setSelected(true);
        } else if (c.bgImagePath != null && !c.bgImagePath.trim().isEmpty()) {
            bgImageRadio.setSelected(true);
        } else {
            bgColorRadio.setSelected(true);
        }

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row2.add(new JLabel("Background:"));
        row2.add(bgTransparentRadio);
        row2.add(bgColorRadio);
        row2.add(bgImageRadio);
        section.add(row2);

        JLabel colorLabel = new JLabel("Hex:");
        JTextField bgField = new JTextField(c.bgColor, 7);
        JTextField bgImageField = new JTextField(c.bgImagePath, 14);

        JButton browseBtn = createSmallButton("Browse...", a -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Images (png, jpg, bmp, gif)", "png", "jpg", "jpeg", "bmp", "gif"
            ));
            if (chooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                bgImageField.setText(path);
                c.bgImagePath = path;
            }
        });

        JButton clearImgBtn = createSmallButton("Clear", a -> {
            bgImageField.setText("");
            c.bgImagePath = "";
        });

        JButton applyBtn = createSmallButton("Apply", a -> {
            int effW = ThinCapture.getEffectiveThinBTWidth();
            int effH = ThinCapture.getEffectiveThinBTHeight();
            c.captureX = clamp(c.captureX, 0, effW - 1);
            c.captureY = clamp(c.captureY, 0, effH - 1);
            c.captureW = clamp(c.captureW, 1, effW - c.captureX);
            c.captureH = clamp(c.captureH, 1, effH - c.captureY);
            c.textThreshold = clamp(intFrom(threshField, 200), 0, 255);
            threshField.setText(String.valueOf(c.textThreshold));
        });

        JPanel row3 = new JPanel(new BorderLayout());
        row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JPanel row3Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        row3Left.add(Box.createHorizontalStrut(16));
        row3Left.add(colorLabel);
        row3Left.add(bgField);
        row3Left.add(Box.createHorizontalStrut(12));
        row3Left.add(bgImageField);
        row3Left.add(browseBtn);
        row3Left.add(clearImgBtn);

        JPanel row3Right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        row3Right.add(applyBtn);

        row3.add(row3Left, BorderLayout.WEST);
        row3.add(row3Right, BorderLayout.EAST);
        section.add(row3);

        Runnable updateState = () -> {
            boolean on = transparencyBox.isSelected();
            threshLabel.setEnabled(on);
            threshField.setEnabled(on);
            threshNote.setEnabled(on);
            bgTransparentRadio.setEnabled(on);
            bgColorRadio.setEnabled(on);
            bgImageRadio.setEnabled(on);

            boolean colorOn = on && bgColorRadio.isSelected();
            boolean imageOn = on && bgImageRadio.isSelected();
            colorLabel.setEnabled(colorOn);
            bgField.setEnabled(colorOn);
            bgImageField.setEnabled(imageOn);
            browseBtn.setEnabled(imageOn);
            clearImgBtn.setEnabled(imageOn);
        };

        Runnable syncConfig = () -> {
            c.textOnly = transparencyBox.isSelected();
            c.transparentBg = bgTransparentRadio.isSelected();
            if (bgColorRadio.isSelected()) {
                c.bgImagePath = "";
            }
        };

        transparencyBox.addActionListener(a -> { syncConfig.run(); updateState.run(); });
        bgTransparentRadio.addActionListener(a -> { syncConfig.run(); updateState.run(); });
        bgColorRadio.addActionListener(a -> { syncConfig.run(); updateState.run(); });
        bgImageRadio.addActionListener(a -> { syncConfig.run(); updateState.run(); });

        threshField.getDocument().addDocumentListener(docListener(() ->
                c.textThreshold = clamp(intFrom(threshField, 200), 0, 255)
        ));
        bgField.getDocument().addDocumentListener(docListener(() -> c.bgColor = bgField.getText().trim()));
        bgImageField.getDocument().addDocumentListener(docListener(() -> c.bgImagePath = bgImageField.getText().trim()));

        updateState.run();

        return section;
    }

    private void rebuildCaptures() {
        capturesContainer.removeAll();

        ThinCaptureOptions o = ThinCapture.getOptions();

        for (int i = 0; i < o.captures.size(); i++) {
            capturesContainer.add(buildCapturePanel(i));
            capturesContainer.add(Box.createRigidArea(new Dimension(0, 4)));
        }

        capturesContainer.revalidate();
        capturesContainer.repaint();
    }

    public void onSwitchTo() {
        refreshSizeLabel();
        rebuildCaptures();
    }

    // ===== UI Helpers =====

    private JPanel createRow(Component... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : components) {
            row.add(c);
        }
        return row;
    }

    private JButton createSmallButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text);
        btn.setMargin(new Insets(1, 6, 1, 6));
        btn.addActionListener(action);
        return btn;
    }

    private JButton createRemoveButton(String label, Runnable onConfirm) {
        JButton removeBtn = new JButton("Remove");
        removeBtn.setMargin(new Insets(1, 6, 1, 6));
        removeBtn.setForeground(Color.RED);
        removeBtn.addActionListener(a -> {
            int confirm = JOptionPane.showConfirmDialog(mainPanel,
                    "Remove " + label + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                onConfirm.run();
            }
        });
        return removeBtn;
    }

    private static JTextField field(int val) {
        return new JTextField(String.valueOf(val), 4);
    }

    private static int intFrom(JTextField f, int fallback) {
        String t = f.getText().trim();
        boolean neg = t.startsWith("-");
        String nums = IntStream.range(0, t.length()).mapToObj(t::charAt)
                .filter(Character::isDigit).map(String::valueOf).collect(Collectors.joining());
        return nums.isEmpty() ? fallback : (neg ? -1 : 1) * Integer.parseInt(nums);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static DocumentListener docListener(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) {}
        };
    }
}