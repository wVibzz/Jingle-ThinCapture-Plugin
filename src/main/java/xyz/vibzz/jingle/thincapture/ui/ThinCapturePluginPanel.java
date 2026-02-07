package xyz.vibzz.jingle.thincapture.ui;

import xyz.vibzz.jingle.thincapture.ThinCapture;
import xyz.vibzz.jingle.thincapture.ThinCaptureOptions;
import xyz.vibzz.jingle.thincapture.config.BackgroundConfig;
import xyz.vibzz.jingle.thincapture.config.CaptureConfig;
import xyz.vibzz.jingle.thincapture.frame.BackgroundFrame;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ThinCapturePluginPanel {
    public final JPanel mainPanel;
    private final JPanel capturesContainer;

    public ThinCapturePluginPanel() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        ThinCaptureOptions o = ThinCapture.getOptions();

        // General Settings
        JPanel generalPanel = new JPanel();
        generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
        generalPanel.setBorder(BorderFactory.createTitledBorder("General"));
        generalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel thinRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        thinRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        thinRow.add(new JLabel("Thin BT size:"));
        final JTextField thinWField = new JTextField(String.valueOf(o.thinBTWidth), 4);
        final JTextField thinHField = new JTextField(String.valueOf(o.thinBTHeight), 4);
        thinRow.add(thinWField);
        thinRow.add(new JLabel("×"));
        thinRow.add(thinHField);
        JButton thinApply = new JButton("Apply");
        thinApply.addActionListener(a -> {
            o.thinBTWidth = intFrom(thinWField, 280);
            o.thinBTHeight = intFrom(thinHField, 1000);
            thinWField.setText(String.valueOf(o.thinBTWidth));
            thinHField.setText(String.valueOf(o.thinBTHeight));
        });
        thinRow.add(thinApply);
        JLabel desc = new JLabel("Must match your Thin BT size in the Resizing script.");
        desc.setFont(desc.getFont().deriveFont(Font.ITALIC, 11f));
        thinRow.add(desc);
        generalPanel.add(thinRow);

        JPanel fpsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        fpsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        fpsRow.add(new JLabel("FPS limit:"));
        final JTextField fpsField = new JTextField(String.valueOf(o.fpsLimit), 4);
        fpsField.getDocument().addDocumentListener(docListener(() -> {
            o.fpsLimit = clamp(intFrom(fpsField, 30), 5, 240);
            ThinCapture.updateFpsLimit();
        }));
        fpsRow.add(fpsField);
        generalPanel.add(fpsRow);

        mainPanel.add(generalPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        capturesContainer = new JPanel();
        capturesContainer.setLayout(new BoxLayout(capturesContainer, BoxLayout.Y_AXIS));
        capturesContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(capturesContainer);

        // Add buttons
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

        JButton addBgBtn = new JButton("+ Add Background");
        addBgBtn.addActionListener(a -> {
            String name = JOptionPane.showInputDialog(mainPanel, "Background name:", "Background");
            if (name != null && !name.trim().isEmpty()) {
                ThinCapture.addBackground(name.trim());
                rebuildCaptures();
            }
        });
        addRow.add(addBgBtn);

        mainPanel.add(addRow);
        mainPanel.add(Box.createVerticalGlue());

        rebuildCaptures();
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

    private JPanel buildBackgroundPanel(int index) {
        ThinCaptureOptions o = ThinCapture.getOptions();
        BackgroundConfig bg = o.backgrounds.get(index);

        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(bg.name));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox enableBox = new JCheckBox("Enabled");
        enableBox.setSelected(bg.enabled);
        enableBox.addActionListener(a -> bg.enabled = enableBox.isSelected());

        JButton renameBtn = new JButton("Rename");
        renameBtn.setMargin(new Insets(1, 6, 1, 6));
        renameBtn.addActionListener(a -> {
            String newName = JOptionPane.showInputDialog(mainPanel, "New name:", bg.name);
            if (newName != null && !newName.trim().isEmpty()) {
                ThinCapture.renameBackground(index, newName.trim());
                rebuildCaptures();
            }
        });

        JButton removeBtn = createRemoveButton("background \"" + bg.name + "\"", () -> {
            ThinCapture.removeBackground(index);
            rebuildCaptures();
        });

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.add(enableBox);
        topRow.add(renameBtn);
        topRow.add(removeBtn);
        section.add(topRow);

        JTextField bgPathField = new JTextField(bg.imagePath, 18);
        JButton browseBtn = new JButton("Browse...");
        browseBtn.setMargin(new Insets(1, 6, 1, 6));
        JButton clearBtn = new JButton("Clear");
        clearBtn.setMargin(new Insets(1, 6, 1, 6));

        browseBtn.addActionListener(a -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Images (png, jpg, bmp, gif)", "png", "jpg", "jpeg", "bmp", "gif"
            ));
            if (chooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                bgPathField.setText(path);
                bg.imagePath = path;
                BackgroundFrame frame = ThinCapture.getBgFrame(index);
                if (frame != null) frame.loadImage(path);
            }
        });
        clearBtn.addActionListener(a -> {
            bgPathField.setText("");
            bg.imagePath = "";
            BackgroundFrame frame = ThinCapture.getBgFrame(index);
            if (frame != null) frame.loadImage("");
        });

        JPanel imageRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        imageRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        imageRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        imageRow.add(new JLabel("Image:"));
        imageRow.add(bgPathField);
        imageRow.add(browseBtn);
        imageRow.add(clearBtn);
        section.add(imageRow);

        JTextField bgXField = new JTextField(String.valueOf(bg.x), 4);
        JTextField bgYField = new JTextField(String.valueOf(bg.y), 4);
        JTextField bgWField = new JTextField(String.valueOf(bg.width), 5);
        JTextField bgHField = new JTextField(String.valueOf(bg.height), 5);

        JButton selectBtn = new JButton("Select");
        selectBtn.setMargin(new Insets(1, 6, 1, 6));
        selectBtn.addActionListener(a -> RegionSelector.selectOnScreen(r -> {
            bgXField.setText(String.valueOf(r.x));
            bgYField.setText(String.valueOf(r.y));
            bgWField.setText(String.valueOf(r.width));
            bgHField.setText(String.valueOf(r.height));
            bg.x = r.x;
            bg.y = r.y;
            bg.width = r.width;
            bg.height = r.height;
        }));

        JButton editBtn = new JButton("Edit");
        editBtn.setMargin(new Insets(1, 6, 1, 6));
        editBtn.addActionListener(a -> {
            Rectangle current = new Rectangle(
                    intFrom(bgXField, 0), intFrom(bgYField, 0), intFrom(bgWField, 1920), intFrom(bgHField, 1080)
            );
            RegionSelector.editOnScreen(current, r -> {
                bgXField.setText(String.valueOf(r.x));
                bgYField.setText(String.valueOf(r.y));
                bgWField.setText(String.valueOf(r.width));
                bgHField.setText(String.valueOf(r.height));
                bg.x = r.x;
                bg.y = r.y;
                bg.width = r.width;
                bg.height = r.height;
            });
        });

        JButton applyBtn = new JButton("Apply");
        applyBtn.setMargin(new Insets(1, 6, 1, 6));
        applyBtn.addActionListener(a -> {
            bg.x = intFrom(bgXField, 0);
            bg.y = intFrom(bgYField, 0);
            bg.width = clamp(intFrom(bgWField, 1920), 1, 7680);
            bg.height = clamp(intFrom(bgHField, 1080), 1, 4320);
            bg.imagePath = bgPathField.getText().trim();

            bgXField.setText(String.valueOf(bg.x));
            bgYField.setText(String.valueOf(bg.y));
            bgWField.setText(String.valueOf(bg.width));
            bgHField.setText(String.valueOf(bg.height));

            BackgroundFrame frame = ThinCapture.getBgFrame(index);
            if (frame != null) frame.loadImage(bg.imagePath);
        });

        JPanel posRow = new JPanel(new BorderLayout());
        posRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        posRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel posRowLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        posRowLeft.add(new JLabel("X:"));
        posRowLeft.add(bgXField);
        posRowLeft.add(new JLabel("Y:"));
        posRowLeft.add(bgYField);
        posRowLeft.add(new JLabel("Width:"));
        posRowLeft.add(bgWField);
        posRowLeft.add(new JLabel("Height:"));
        posRowLeft.add(bgHField);
        posRowLeft.add(selectBtn);
        posRowLeft.add(editBtn);

        JPanel posRowRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        posRowRight.add(applyBtn);

        posRow.add(posRowLeft, BorderLayout.WEST);
        posRow.add(posRowRight, BorderLayout.EAST);
        section.add(posRow);

        return section;
    }

    private JPanel buildCapturePanel(int index) {
        ThinCaptureOptions o = ThinCapture.getOptions();
        CaptureConfig c = o.captures.get(index);

        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(c.name));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox enableBox = new JCheckBox("Enabled");
        enableBox.setSelected(c.enabled);
        enableBox.addActionListener(a -> c.enabled = enableBox.isSelected());

        JButton renameBtn = new JButton("Rename");
        renameBtn.setMargin(new Insets(1, 6, 1, 6));
        renameBtn.addActionListener(a -> {
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

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.add(enableBox);
        topRow.add(renameBtn);
        topRow.add(removeBtn);
        section.add(topRow);

        JTextField ox = field(c.screenX), oy = field(c.screenY), ow = field(c.screenW), oh = field(c.screenH);
        JPanel overlayRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        overlayRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        overlayRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        overlayRow.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        overlayRow.add(new JLabel("Monitor  Starting X:"));overlayRow.add(ox);
        overlayRow.add(new JLabel("Starting Y:"));overlayRow.add(oy);
        overlayRow.add(new JLabel("Window Width:"));overlayRow.add(ow);
        overlayRow.add(new JLabel("Window Height:"));overlayRow.add(oh);

        JButton selectMonitor = new JButton("Select");
        selectMonitor.setMargin(new Insets(1, 6, 1, 6));
        selectMonitor.addActionListener(a -> RegionSelector.selectOnScreen(r -> {
            ox.setText(String.valueOf(r.x));
            oy.setText(String.valueOf(r.y));
            ow.setText(String.valueOf(r.width));
            oh.setText(String.valueOf(r.height));
            c.screenX = r.x;
            c.screenY = r.y;
            c.screenW = r.width;
            c.screenH = r.height;
        }));
        overlayRow.add(selectMonitor);

        JButton editMonitor = new JButton("Edit");
        editMonitor.setMargin(new Insets(1, 6, 1, 6));
        editMonitor.addActionListener(a -> {
            Rectangle current = new Rectangle(
                    intFrom(ox, 0), intFrom(oy, 0), intFrom(ow, 200), intFrom(oh, 200)
            );
            RegionSelector.editOnScreen(current, r -> {
                ox.setText(String.valueOf(r.x));
                oy.setText(String.valueOf(r.y));
                ow.setText(String.valueOf(r.width));
                oh.setText(String.valueOf(r.height));
                c.screenX = r.x;
                c.screenY = r.y;
                c.screenW = r.width;
                c.screenH = r.height;
            });
        });
        overlayRow.add(editMonitor);
        section.add(overlayRow);

        JTextField rx = field(c.captureX), ry = field(c.captureY), rw = field(c.captureW), rh = field(c.captureH);
        JPanel regionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        regionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        regionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        regionRow.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        regionRow.add(new JLabel("MC Region Starting X:"));regionRow.add(rx);
        regionRow.add(new JLabel("Starting Y:"));regionRow.add(ry);
        regionRow.add(new JLabel("Capture Width:"));regionRow.add(rw);
        regionRow.add(new JLabel("Capture Height:"));regionRow.add(rh);

        JButton selectMC = new JButton("Select");
        selectMC.setMargin(new Insets(1, 6, 1, 6));
        selectMC.addActionListener(a -> RegionSelector.selectOnMCWindow(r -> {
            rx.setText(String.valueOf(r.x));
            ry.setText(String.valueOf(r.y));
            rw.setText(String.valueOf(r.width));
            rh.setText(String.valueOf(r.height));
            c.captureX = r.x;
            c.captureY = r.y;
            c.captureW = r.width;
            c.captureH = r.height;
        }));
        regionRow.add(selectMC);

        JButton editMC = new JButton("Edit");
        editMC.setMargin(new Insets(1, 6, 1, 6));
        editMC.addActionListener(a -> {
            Rectangle current = new Rectangle(
                    intFrom(rx, 0), intFrom(ry, 0), intFrom(rw, 200), intFrom(rh, 200)
            );
            RegionSelector.editOnMCWindow(current, r -> {
                rx.setText(String.valueOf(r.x));
                ry.setText(String.valueOf(r.y));
                rw.setText(String.valueOf(r.width));
                rh.setText(String.valueOf(r.height));
                c.captureX = r.x;
                c.captureY = r.y;
                c.captureW = r.width;
                c.captureH = r.height;
            });
        });
        regionRow.add(editMC);
        section.add(regionRow);

        // Transparency
        JPanel transpSection = new JPanel();
        transpSection.setLayout(new BoxLayout(transpSection, BoxLayout.Y_AXIS));
        transpSection.setBorder(BorderFactory.createTitledBorder("Transparency"));
        transpSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        transpSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JCheckBox transparencyBox = new JCheckBox("Enable (filter white text)");
        transparencyBox.setSelected(c.textOnly);

        JLabel threshLabel = new JLabel("Threshold:");
        JTextField threshField = new JTextField(String.valueOf(c.textThreshold), 3);
        JLabel threshNote = new JLabel("[0-255]");
        threshNote.setFont(threshNote.getFont().deriveFont(Font.ITALIC, 10f));

        JPanel transpRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        transpRow1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        transpRow1.add(transparencyBox);
        transpRow1.add(Box.createHorizontalStrut(8));
        transpRow1.add(threshLabel);
        transpRow1.add(threshField);
        transpRow1.add(threshNote);
        transpSection.add(transpRow1);

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

        JPanel transpRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        transpRow2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        transpRow2.add(new JLabel("Background:"));
        transpRow2.add(bgTransparentRadio);
        transpRow2.add(bgColorRadio);
        transpRow2.add(bgImageRadio);
        transpSection.add(transpRow2);

        JButton applyBtn = new JButton("Apply");
        applyBtn.setMargin(new Insets(1, 6, 1, 6));

        JLabel colorLabel = new JLabel("Hex:");
        JTextField bgField = new JTextField(c.bgColor, 7);

        JTextField bgImageField = new JTextField(c.bgImagePath, 14);
        JButton browseBtn = new JButton("Browse...");
        browseBtn.setMargin(new Insets(1, 6, 1, 6));
        JButton clearImgBtn = new JButton("Clear");
        clearImgBtn.setMargin(new Insets(1, 6, 1, 6));

        JPanel transpRow3 = new JPanel(new BorderLayout());
        transpRow3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JPanel transpRow3Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        transpRow3Left.add(Box.createHorizontalStrut(16));
        transpRow3Left.add(colorLabel);
        transpRow3Left.add(bgField);
        transpRow3Left.add(Box.createHorizontalStrut(12));
        transpRow3Left.add(bgImageField);
        transpRow3Left.add(browseBtn);
        transpRow3Left.add(clearImgBtn);

        JPanel transpRow3Right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        transpRow3Right.add(applyBtn);

        transpRow3.add(transpRow3Left, BorderLayout.WEST);
        transpRow3.add(transpRow3Right, BorderLayout.EAST);
        transpSection.add(transpRow3);

        Runnable updateTranspState = () -> {
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

        transparencyBox.addActionListener(a -> { syncConfig.run(); updateTranspState.run(); });
        bgTransparentRadio.addActionListener(a -> { syncConfig.run(); updateTranspState.run(); });
        bgColorRadio.addActionListener(a -> { syncConfig.run(); updateTranspState.run(); });
        bgImageRadio.addActionListener(a -> { syncConfig.run(); updateTranspState.run(); });

        threshField.getDocument().addDocumentListener(docListener(() ->
                c.textThreshold = clamp(intFrom(threshField, 200), 0, 255)
        ));
        bgField.getDocument().addDocumentListener(docListener(() -> c.bgColor = bgField.getText().trim()));
        bgImageField.getDocument().addDocumentListener(docListener(() -> c.bgImagePath = bgImageField.getText().trim()));

        browseBtn.addActionListener(a -> {
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
        clearImgBtn.addActionListener(a -> {
            bgImageField.setText("");
            c.bgImagePath = "";
        });

        updateTranspState.run();
        section.add(transpSection);

        applyBtn.addActionListener(a -> {
            c.screenX = intFrom(ox, 0);
            c.screenY = intFrom(oy, 0);
            c.screenW = intFrom(ow, 200);
            c.screenH = intFrom(oh, 200);
            c.captureX = clamp(intFrom(rx, 0), 0, o.thinBTWidth - 1);
            c.captureY = clamp(intFrom(ry, 0), 0, o.thinBTHeight - 1);
            c.captureW = clamp(intFrom(rw, 200), 1, o.thinBTWidth - c.captureX);
            c.captureH = clamp(intFrom(rh, 200), 1, o.thinBTHeight - c.captureY);
            c.textThreshold = clamp(intFrom(threshField, 200), 0, 255);

            ox.setText(String.valueOf(c.screenX));
            oy.setText(String.valueOf(c.screenY));
            ow.setText(String.valueOf(c.screenW));
            oh.setText(String.valueOf(c.screenH));
            rx.setText(String.valueOf(c.captureX));
            ry.setText(String.valueOf(c.captureY));
            rw.setText(String.valueOf(c.captureW));
            rh.setText(String.valueOf(c.captureH));
            threshField.setText(String.valueOf(c.textThreshold));
        });

        return section;
    }

    private void rebuildCaptures() {
        capturesContainer.removeAll();

        ThinCaptureOptions o = ThinCapture.getOptions();

        for (int i = 0; i < o.backgrounds.size(); i++) {
            capturesContainer.add(buildBackgroundPanel(i));
            capturesContainer.add(Box.createRigidArea(new Dimension(0, 4)));
        }

        for (int i = 0; i < o.captures.size(); i++) {
            capturesContainer.add(buildCapturePanel(i));
            capturesContainer.add(Box.createRigidArea(new Dimension(0, 4)));
        }

        capturesContainer.revalidate();
        capturesContainer.repaint();
    }

    public void onSwitchTo() {
        rebuildCaptures();
    }

    private static JTextField field(int val) { return new JTextField(String.valueOf(val), 4); }

    private static int intFrom(JTextField f, int fallback) {
        String t = f.getText().trim();
        boolean neg = t.startsWith("-");
        String nums = IntStream.range(0, t.length()).mapToObj(t::charAt)
                .filter(Character::isDigit).map(String::valueOf).collect(Collectors.joining());
        return nums.isEmpty() ? fallback : (neg ? -1 : 1) * Integer.parseInt(nums);
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    private static DocumentListener docListener(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) {}
        };
    }
}