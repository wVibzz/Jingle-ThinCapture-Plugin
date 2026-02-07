package xyz.vibzz.jingle.thincapture;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ThinCapturePluginPanel {
    public JPanel mainPanel;

    private JTextField thinWField, thinHField;
    private JTextField fpsField;

    private JCheckBox entityEnableBox;
    private JTextField entityOverlayXField, entityOverlayYField, entityOverlayWField, entityOverlayHField;
    private JTextField entityRegionXField, entityRegionYField, entityRegionWField, entityRegionHField;

    private JCheckBox pieEnableBox;
    private JTextField pieOverlayXField, pieOverlayYField, pieOverlayWField, pieOverlayHField;
    private JTextField pieRegionXField, pieRegionYField, pieRegionWField, pieRegionHField;

    public ThinCapturePluginPanel() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        ThinCaptureOptions o = ThinCapture.getOptions();

        // ===== General Settings =====
        JPanel generalPanel = new JPanel();
        generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
        generalPanel.setBorder(BorderFactory.createTitledBorder("General"));
        generalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel thinRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        thinRow.add(new JLabel("Thin BT size:"));
        thinWField = new JTextField(String.valueOf(o.thinBTWidth), 4);
        thinHField = new JTextField(String.valueOf(o.thinBTHeight), 4);
        thinRow.add(thinWField);
        thinRow.add(new JLabel("\u00d7"));
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
        fpsRow.add(new JLabel("FPS limit:"));
        fpsField = new JTextField(String.valueOf(o.fpsLimit), 4);
        fpsField.getDocument().addDocumentListener(docListener(() -> {
            o.fpsLimit = clamp(intFrom(fpsField, 30), 5, 240);
            ThinCapture.updateFpsLimit();
        }));
        fpsRow.add(fpsField);
        generalPanel.add(fpsRow);

        mainPanel.add(generalPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        // ===== Entity Counter =====
        entityOverlayXField = field(o.entityScreenX);
        entityOverlayYField = field(o.entityScreenY);
        entityOverlayWField = field(o.entityScreenW);
        entityOverlayHField = field(o.entityScreenH);
        entityRegionXField = field(o.entityCaptureX);
        entityRegionYField = field(o.entityCaptureY);
        entityRegionWField = field(o.entityCaptureW);
        entityRegionHField = field(o.entityCaptureH);

        entityEnableBox = new JCheckBox("Enabled");
        entityEnableBox.setSelected(o.entityEnabled);
        entityEnableBox.addActionListener(a -> { o.entityEnabled = entityEnableBox.isSelected(); reloadEnabled(); });

        JCheckBox entityTextOnlyBox = new JCheckBox("Text only (keep bright pixels)");
        entityTextOnlyBox.setSelected(o.entityTextOnly);
        entityTextOnlyBox.addActionListener(a -> o.entityTextOnly = entityTextOnlyBox.isSelected());

        JTextField entityThreshField = new JTextField(String.valueOf(o.entityTextThreshold), 3);
        entityThreshField.getDocument().addDocumentListener(docListener(() ->
                o.entityTextThreshold = clamp(intFrom(entityThreshField, 200), 0, 255)
        ));

        JCheckBox entityTransBgBox = new JCheckBox("Transparent background");
        entityTransBgBox.setSelected(o.entityTransparentBg);
        JTextField entityBgField = new JTextField(o.entityBgColor, 7);
        entityBgField.setEnabled(!o.entityTransparentBg);
        entityTransBgBox.addActionListener(a -> {
            o.entityTransparentBg = entityTransBgBox.isSelected();
            entityBgField.setEnabled(!o.entityTransparentBg);
        });
        entityBgField.getDocument().addDocumentListener(docListener(() -> o.entityBgColor = entityBgField.getText().trim()));

        JButton entityApply = new JButton("Apply");
        entityApply.addActionListener(a -> {
            o.entityScreenX = intFrom(entityOverlayXField, 0);
            o.entityScreenY = intFrom(entityOverlayYField, 0);
            o.entityScreenW = intFrom(entityOverlayWField, 300);
            o.entityScreenH = intFrom(entityOverlayHField, 20);
            o.entityCaptureX = clamp(intFrom(entityRegionXField, 0), 0, o.thinBTWidth - 1);
            o.entityCaptureY = clamp(intFrom(entityRegionYField, 0), 0, o.thinBTHeight - 1);
            o.entityCaptureW = clamp(intFrom(entityRegionWField, 300), 1, o.thinBTWidth - o.entityCaptureX);
            o.entityCaptureH = clamp(intFrom(entityRegionHField, 20), 1, o.thinBTHeight - o.entityCaptureY);
            o.entityTextThreshold = clamp(intFrom(entityThreshField, 200), 0, 255);
            refreshFields(o, entityOverlayXField, entityOverlayYField, entityOverlayWField, entityOverlayHField,
                    entityRegionXField, entityRegionYField, entityRegionWField, entityRegionHField, entityThreshField, true);
        });

        mainPanel.add(buildCaptureSection("Entity Counter", entityEnableBox,
                entityOverlayXField, entityOverlayYField, entityOverlayWField, entityOverlayHField,
                entityRegionXField, entityRegionYField, entityRegionWField, entityRegionHField,
                entityTextOnlyBox, entityThreshField, entityTransBgBox, entityBgField, entityApply));
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        // ===== Pie Chart =====
        pieOverlayXField = field(o.pieScreenX);
        pieOverlayYField = field(o.pieScreenY);
        pieOverlayWField = field(o.pieScreenW);
        pieOverlayHField = field(o.pieScreenH);
        pieRegionXField = field(o.pieCaptureX);
        pieRegionYField = field(o.pieCaptureY);
        pieRegionWField = field(o.pieCaptureW);
        pieRegionHField = field(o.pieCaptureH);

        pieEnableBox = new JCheckBox("Enabled");
        pieEnableBox.setSelected(o.pieEnabled);
        pieEnableBox.addActionListener(a -> { o.pieEnabled = pieEnableBox.isSelected(); reloadEnabled(); });

        JCheckBox pieTextOnlyBox = new JCheckBox("Text only (keep bright pixels)");
        pieTextOnlyBox.setSelected(o.pieTextOnly);
        pieTextOnlyBox.addActionListener(a -> o.pieTextOnly = pieTextOnlyBox.isSelected());

        JTextField pieThreshField = new JTextField(String.valueOf(o.pieTextThreshold), 3);
        pieThreshField.getDocument().addDocumentListener(docListener(() ->
                o.pieTextThreshold = clamp(intFrom(pieThreshField, 200), 0, 255)
        ));

        JCheckBox pieTransBgBox = new JCheckBox("Transparent background");
        pieTransBgBox.setSelected(o.pieTransparentBg);
        JTextField pieBgField = new JTextField(o.pieBgColor, 7);
        pieBgField.setEnabled(!o.pieTransparentBg);
        pieTransBgBox.addActionListener(a -> {
            o.pieTransparentBg = pieTransBgBox.isSelected();
            pieBgField.setEnabled(!o.pieTransparentBg);
        });
        pieBgField.getDocument().addDocumentListener(docListener(() -> o.pieBgColor = pieBgField.getText().trim()));

        JButton pieApply = new JButton("Apply");
        pieApply.addActionListener(a -> {
            o.pieScreenX = intFrom(pieOverlayXField, 0);
            o.pieScreenY = intFrom(pieOverlayYField, 0);
            o.pieScreenW = intFrom(pieOverlayWField, 200);
            o.pieScreenH = intFrom(pieOverlayHField, 200);
            o.pieCaptureX = clamp(intFrom(pieRegionXField, 0), 0, o.thinBTWidth - 1);
            o.pieCaptureY = clamp(intFrom(pieRegionYField, 0), 0, o.thinBTHeight - 1);
            o.pieCaptureW = clamp(intFrom(pieRegionWField, 200), 1, o.thinBTWidth - o.pieCaptureX);
            o.pieCaptureH = clamp(intFrom(pieRegionHField, 200), 1, o.thinBTHeight - o.pieCaptureY);
            o.pieTextThreshold = clamp(intFrom(pieThreshField, 200), 0, 255);
            refreshFields(o, pieOverlayXField, pieOverlayYField, pieOverlayWField, pieOverlayHField,
                    pieRegionXField, pieRegionYField, pieRegionWField, pieRegionHField, pieThreshField, false);
        });

        mainPanel.add(buildCaptureSection("Pie Chart", pieEnableBox,
                pieOverlayXField, pieOverlayYField, pieOverlayWField, pieOverlayHField,
                pieRegionXField, pieRegionYField, pieRegionWField, pieRegionHField,
                pieTextOnlyBox, pieThreshField, pieTransBgBox, pieBgField, pieApply));

        mainPanel.add(Box.createVerticalGlue());
        reloadEnabled();
    }

    private JPanel buildCaptureSection(String title, JCheckBox enableBox,
                                       JTextField ox, JTextField oy, JTextField ow, JTextField oh,
                                       JTextField rx, JTextField ry, JTextField rw, JTextField rh,
                                       JCheckBox textOnlyBox, JTextField threshField,
                                       JCheckBox transBgBox, JTextField bgField, JButton applyBtn) {

        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(title));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(enableBox, BorderLayout.WEST);
        topRow.add(applyBtn, BorderLayout.EAST);
        section.add(topRow);

        // Overlay position (on your monitor)
        JPanel overlayRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
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
        }));
        overlayRow.add(selectMonitor);
        section.add(overlayRow);

        // MC region (inside Minecraft window)
        JPanel regionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
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
        }));
        regionRow.add(selectMC);
        section.add(regionRow);

        // Filtering
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filterRow.add(textOnlyBox);
        filterRow.add(new JLabel("Threshold:"));
        filterRow.add(threshField);
        section.add(filterRow);

        JPanel bgRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        bgRow.add(transBgBox);
        bgRow.add(new JLabel("Background Color:"));
        bgRow.add(bgField);
        section.add(bgRow);

        return section;
    }

    private void refreshFields(ThinCaptureOptions o,
                               JTextField ox, JTextField oy, JTextField ow, JTextField oh,
                               JTextField rx, JTextField ry, JTextField rw, JTextField rh,
                               JTextField thresh, boolean isEntity) {
        if (isEntity) {
            ox.setText(String.valueOf(o.entityScreenX));
            oy.setText(String.valueOf(o.entityScreenY));
            ow.setText(String.valueOf(o.entityScreenW));
            oh.setText(String.valueOf(o.entityScreenH));
            rx.setText(String.valueOf(o.entityCaptureX));
            ry.setText(String.valueOf(o.entityCaptureY));
            rw.setText(String.valueOf(o.entityCaptureW));
            rh.setText(String.valueOf(o.entityCaptureH));
            thresh.setText(String.valueOf(o.entityTextThreshold));
        } else {
            ox.setText(String.valueOf(o.pieScreenX));
            oy.setText(String.valueOf(o.pieScreenY));
            ow.setText(String.valueOf(o.pieScreenW));
            oh.setText(String.valueOf(o.pieScreenH));
            rx.setText(String.valueOf(o.pieCaptureX));
            ry.setText(String.valueOf(o.pieCaptureY));
            rw.setText(String.valueOf(o.pieCaptureW));
            rh.setText(String.valueOf(o.pieCaptureH));
            thresh.setText(String.valueOf(o.pieTextThreshold));
        }
    }

    private void reloadEnabled() {
        ThinCaptureOptions o = ThinCapture.getOptions();
        setFieldsEnabled(o.entityEnabled, entityOverlayXField, entityOverlayYField, entityOverlayWField, entityOverlayHField,
                entityRegionXField, entityRegionYField, entityRegionWField, entityRegionHField);
        setFieldsEnabled(o.pieEnabled, pieOverlayXField, pieOverlayYField, pieOverlayWField, pieOverlayHField,
                pieRegionXField, pieRegionYField, pieRegionWField, pieRegionHField);
    }

    private void setFieldsEnabled(boolean enabled, JTextField... fields) {
        for (JTextField f : fields) f.setEnabled(enabled);
    }

    public void onSwitchTo() {
        ThinCaptureOptions o = ThinCapture.getOptions();
        entityOverlayXField.setText(String.valueOf(o.entityScreenX));
        entityOverlayYField.setText(String.valueOf(o.entityScreenY));
        entityOverlayWField.setText(String.valueOf(o.entityScreenW));
        entityOverlayHField.setText(String.valueOf(o.entityScreenH));
        entityRegionXField.setText(String.valueOf(o.entityCaptureX));
        entityRegionYField.setText(String.valueOf(o.entityCaptureY));
        entityRegionWField.setText(String.valueOf(o.entityCaptureW));
        entityRegionHField.setText(String.valueOf(o.entityCaptureH));
        pieOverlayXField.setText(String.valueOf(o.pieScreenX));
        pieOverlayYField.setText(String.valueOf(o.pieScreenY));
        pieOverlayWField.setText(String.valueOf(o.pieScreenW));
        pieOverlayHField.setText(String.valueOf(o.pieScreenH));
        pieRegionXField.setText(String.valueOf(o.pieCaptureX));
        pieRegionYField.setText(String.valueOf(o.pieCaptureY));
        pieRegionWField.setText(String.valueOf(o.pieCaptureW));
        pieRegionHField.setText(String.valueOf(o.pieCaptureH));
    }

    // --- Utility ---

    private static JTextField field(int val) { return new JTextField(String.valueOf(val), 4); }

    private static int intFrom(JTextField f, int fallback) {
        String t = f.getText().trim();
        boolean neg = t.startsWith("-");
        String nums = IntStream.range(0, t.length()).mapToObj(i -> t.charAt(i))
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