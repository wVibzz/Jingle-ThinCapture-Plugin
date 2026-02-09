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

public class PlanarAbusePluginPanel {
    public final JPanel mainPanel;
    private final JPanel capturesContainer;
    private JLabel sizeLabel;

    public PlanarAbusePluginPanel() {
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
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("General"));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(buildSizeRow());
        p.add(buildFpsRow(o));
        return p;
    }

    private JPanel buildSizeRow() {
        sizeLabel = new JLabel();
        refreshSizeLabel();
        JLabel desc = new JLabel("(synced from Resizing script)");
        desc.setFont(desc.getFont().deriveFont(Font.ITALIC, 11f));
        return createRow(new JLabel("Planar Abuse size:"), sizeLabel, desc);
    }

    private void refreshSizeLabel() {
        if (sizeLabel == null) return;
        sizeLabel.setText(ThinCapture.getEffectivePlanarWidth() + " \u00d7 " + ThinCapture.getEffectivePlanarHeight());
    }

    private JPanel buildFpsRow(ThinCaptureOptions o) {
        JTextField f = new JTextField(String.valueOf(o.planarAbuseFpsLimit), 4);
        f.getDocument().addDocumentListener(docListener(() -> {
            o.planarAbuseFpsLimit = clamp(intFrom(f, 30), 5, 240);
            ThinCapture.updateFpsLimit();
        }));
        return createRow(new JLabel("FPS limit:"), f);
    }

    private JPanel buildAddButtonRow() {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton b = new JButton("+ Add Capture");
        b.addActionListener(a -> {
            String n = JOptionPane.showInputDialog(mainPanel, "Capture name:", "New Capture");
            if (n != null && !n.trim().isEmpty()) { ThinCapture.addPlanarCapture(n.trim()); rebuildCaptures(); }
        });
        r.add(b);
        return r;
    }

    private JPanel buildCapturePanel(int index) {
        CaptureConfig c = ThinCapture.getOptions().planarAbuseCaptures.get(index);
        JPanel s = new JPanel();
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setBorder(BorderFactory.createTitledBorder(c.name));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.add(buildCaptureTopRow(index, c));
        s.add(buildMonitorRow(c));
        s.add(buildMCRegionRow(c));
        s.add(buildTransparencySection(c));
        return s;
    }

    private JPanel buildCaptureTopRow(int index, CaptureConfig c) {
        JCheckBox en = new JCheckBox("Enabled");
        en.setSelected(c.enabled);
        en.addActionListener(a -> c.enabled = en.isSelected());
        JButton ren = createSmallButton("Rename", a -> {
            String n = JOptionPane.showInputDialog(mainPanel, "New name:", c.name);
            if (n != null && !n.trim().isEmpty()) { ThinCapture.renamePlanarCapture(index, n.trim()); rebuildCaptures(); }
        });
        JButton rem = createRemoveButton("capture \"" + c.name + "\"", () -> { ThinCapture.removePlanarCapture(index); rebuildCaptures(); });
        return createRow(en, ren, rem);
    }

    private JPanel buildMonitorRow(CaptureConfig c) {
        JTextField ox = field(c.screenX), oy = field(c.screenY), ow = field(c.screenW), oh = field(c.screenH);
        Consumer<Rectangle> cb = r -> { ox.setText(""+r.x); oy.setText(""+r.y); ow.setText(""+r.width); oh.setText(""+r.height); c.screenX=r.x; c.screenY=r.y; c.screenW=r.width; c.screenH=r.height; };
        JButton sel = createSmallButton("Select", a -> RegionSelector.selectOnScreen(cb));
        JButton edt = createSmallButton("Edit", a -> RegionSelector.editOnScreen(new Rectangle(intFrom(ox,0),intFrom(oy,0),intFrom(ow,200),intFrom(oh,200)), cb));
        JButton app = createSmallButton("Apply", a -> { c.screenX=intFrom(ox,0); c.screenY=intFrom(oy,0); c.screenW=Math.max(1,intFrom(ow,200)); c.screenH=Math.max(1,intFrom(oh,200)); ox.setText(""+c.screenX); oy.setText(""+c.screenY); ow.setText(""+c.screenW); oh.setText(""+c.screenH); });
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        row.add(new JLabel("Monitor  X:")); row.add(ox); row.add(new JLabel("Y:")); row.add(oy);
        row.add(new JLabel("W:")); row.add(ow); row.add(new JLabel("H:")); row.add(oh);
        row.add(sel); row.add(edt); row.add(app);
        return row;
    }

    private JPanel buildMCRegionRow(CaptureConfig c) {
        JTextField rx = field(c.captureX), ry = field(c.captureY), rw = field(c.captureW), rh = field(c.captureH);
        Consumer<Rectangle> cb = r -> { rx.setText(""+r.x); ry.setText(""+r.y); rw.setText(""+r.width); rh.setText(""+r.height); c.captureX=r.x; c.captureY=r.y; c.captureW=r.width; c.captureH=r.height; };
        JButton sel = createSmallButton("Select", a -> RegionSelector.selectOnMCWindow(cb));
        JButton edt = createSmallButton("Edit", a -> RegionSelector.editOnMCWindow(new Rectangle(intFrom(rx,0),intFrom(ry,0),intFrom(rw,200),intFrom(rh,200)), cb));
        JButton app = createSmallButton("Apply", a -> {
            int ew=ThinCapture.getEffectivePlanarWidth(), eh=ThinCapture.getEffectivePlanarHeight();
            c.captureX=clamp(intFrom(rx,0),0,ew-1); c.captureY=clamp(intFrom(ry,0),0,eh-1);
            c.captureW=clamp(Math.max(1,intFrom(rw,200)),1,ew-c.captureX); c.captureH=clamp(Math.max(1,intFrom(rh,200)),1,eh-c.captureY);
            rx.setText(""+c.captureX); ry.setText(""+c.captureY); rw.setText(""+c.captureW); rh.setText(""+c.captureH);
        });
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        row.add(new JLabel("MC Region X:")); row.add(rx); row.add(new JLabel("Y:")); row.add(ry);
        row.add(new JLabel("W:")); row.add(rw); row.add(new JLabel("H:")); row.add(rh);
        row.add(sel); row.add(edt); row.add(app);
        return row;
    }

    private JPanel buildTransparencySection(CaptureConfig c) {
        JPanel sec = new JPanel();
        sec.setLayout(new BoxLayout(sec, BoxLayout.Y_AXIS));
        sec.setBorder(BorderFactory.createTitledBorder("Transparency"));
        sec.setAlignmentX(Component.LEFT_ALIGNMENT);
        sec.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JCheckBox tBox = new JCheckBox("Enable (filter white text)");
        tBox.setSelected(c.textOnly);
        JLabel tLbl = new JLabel("Threshold:");
        JTextField tFld = new JTextField(String.valueOf(c.textThreshold), 3);
        JLabel tNote = new JLabel("[0-255]");
        tNote.setFont(tNote.getFont().deriveFont(Font.ITALIC, 10f));
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        r1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        r1.add(tBox); r1.add(Box.createHorizontalStrut(8)); r1.add(tLbl); r1.add(tFld); r1.add(tNote);
        sec.add(r1);

        JRadioButton bgT = new JRadioButton("Transparent");
        JRadioButton bgC = new JRadioButton("Solid color");
        JRadioButton bgI = new JRadioButton("Image");
        ButtonGroup bg = new ButtonGroup(); bg.add(bgT); bg.add(bgC); bg.add(bgI);
        if (c.transparentBg) bgT.setSelected(true);
        else if (c.bgImagePath != null && !c.bgImagePath.trim().isEmpty()) bgI.setSelected(true);
        else bgC.setSelected(true);
        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        r2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        r2.add(new JLabel("Background:")); r2.add(bgT); r2.add(bgC); r2.add(bgI);
        sec.add(r2);

        JLabel cLbl = new JLabel("Hex:");
        JTextField cFld = new JTextField(c.bgColor, 7);
        JTextField iFld = new JTextField(c.bgImagePath, 14);
        JButton brw = createSmallButton("Browse...", a -> {
            JFileChooser ch = new JFileChooser();
            ch.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images (png, jpg, bmp, gif)", "png", "jpg", "jpeg", "bmp", "gif"));
            if (ch.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) { String p = ch.getSelectedFile().getAbsolutePath(); iFld.setText(p); c.bgImagePath = p; }
        });
        JButton clr = createSmallButton("Clear", a -> { iFld.setText(""); c.bgImagePath = ""; });
        JButton app = createSmallButton("Apply", a -> {
            int ew=ThinCapture.getEffectivePlanarWidth(), eh=ThinCapture.getEffectivePlanarHeight();
            c.captureX=clamp(c.captureX,0,ew-1); c.captureY=clamp(c.captureY,0,eh-1);
            c.captureW=clamp(c.captureW,1,ew-c.captureX); c.captureH=clamp(c.captureH,1,eh-c.captureY);
            c.textThreshold=clamp(intFrom(tFld,200),0,255); tFld.setText(""+c.textThreshold);
        });

        JPanel r3 = new JPanel(new BorderLayout());
        r3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JPanel r3L = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        r3L.add(Box.createHorizontalStrut(16)); r3L.add(cLbl); r3L.add(cFld);
        r3L.add(Box.createHorizontalStrut(12)); r3L.add(iFld); r3L.add(brw); r3L.add(clr);
        JPanel r3R = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        r3R.add(app);
        r3.add(r3L, BorderLayout.WEST); r3.add(r3R, BorderLayout.EAST);
        sec.add(r3);

        Runnable upd = () -> {
            boolean on = tBox.isSelected();
            tLbl.setEnabled(on); tFld.setEnabled(on); tNote.setEnabled(on);
            bgT.setEnabled(on); bgC.setEnabled(on); bgI.setEnabled(on);
            cLbl.setEnabled(on && bgC.isSelected()); cFld.setEnabled(on && bgC.isSelected());
            iFld.setEnabled(on && bgI.isSelected()); brw.setEnabled(on && bgI.isSelected()); clr.setEnabled(on && bgI.isSelected());
        };
        Runnable sync = () -> { c.textOnly = tBox.isSelected(); c.transparentBg = bgT.isSelected(); if (bgC.isSelected()) c.bgImagePath = ""; };

        tBox.addActionListener(a -> { sync.run(); upd.run(); });
        bgT.addActionListener(a -> { sync.run(); upd.run(); });
        bgC.addActionListener(a -> { sync.run(); upd.run(); });
        bgI.addActionListener(a -> { sync.run(); upd.run(); });
        tFld.getDocument().addDocumentListener(docListener(() -> c.textThreshold = clamp(intFrom(tFld, 200), 0, 255)));
        cFld.getDocument().addDocumentListener(docListener(() -> c.bgColor = cFld.getText().trim()));
        iFld.getDocument().addDocumentListener(docListener(() -> c.bgImagePath = iFld.getText().trim()));
        upd.run();

        return sec;
    }

    private void rebuildCaptures() {
        capturesContainer.removeAll();
        ThinCaptureOptions o = ThinCapture.getOptions();
        for (int i = 0; i < o.planarAbuseCaptures.size(); i++) {
            capturesContainer.add(buildCapturePanel(i));
            capturesContainer.add(Box.createRigidArea(new Dimension(0, 4)));
        }
        capturesContainer.revalidate();
        capturesContainer.repaint();
    }

    public void onSwitchTo() { refreshSizeLabel(); rebuildCaptures(); }

    private JPanel createRow(Component... components) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : components) r.add(c);
        return r;
    }

    private JButton createSmallButton(String text, java.awt.event.ActionListener action) {
        JButton b = new JButton(text); b.setMargin(new Insets(1,6,1,6)); b.addActionListener(action); return b;
    }

    private JButton createRemoveButton(String label, Runnable onConfirm) {
        JButton b = new JButton("Remove"); b.setMargin(new Insets(1,6,1,6)); b.setForeground(Color.RED);
        b.addActionListener(a -> { if (JOptionPane.showConfirmDialog(mainPanel, "Remove "+label+"?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) onConfirm.run(); });
        return b;
    }

    private static JTextField field(int v) { return new JTextField(String.valueOf(v), 4); }

    private static int intFrom(JTextField f, int fb) {
        String t = f.getText().trim(); boolean neg = t.startsWith("-");
        String n = IntStream.range(0,t.length()).mapToObj(t::charAt).filter(Character::isDigit).map(String::valueOf).collect(Collectors.joining());
        return n.isEmpty() ? fb : (neg?-1:1)*Integer.parseInt(n);
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