package xyz.vibzz.jingle.thincapture.ui;

import xyz.vibzz.jingle.thincapture.ThinCapture;
import xyz.vibzz.jingle.thincapture.ThinCaptureOptions;
import xyz.vibzz.jingle.thincapture.config.BackgroundConfig;
import xyz.vibzz.jingle.thincapture.frame.BackgroundFrame;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BackgroundsPluginPanel {
    public final JPanel mainPanel;
    private final JPanel thinBTContainer;
    private final JPanel planarContainer;
    private final JPanel eyeSeeContainer;

    public BackgroundsPluginPanel() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Preload toggle
        ThinCaptureOptions o = ThinCapture.getOptions();

        JPanel preloadRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        preloadRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        preloadRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JCheckBox preloadBox = new JCheckBox("Preload Backgrounds");
        preloadBox.setSelected(o.preloadBackgrounds);
        preloadBox.addActionListener(a -> o.preloadBackgrounds = preloadBox.isSelected());
        preloadRow.add(preloadBox);
        JLabel preloadDesc = new JLabel("Keep backgrounds always behind MC when focused (reduces delay on toggle)");
        preloadDesc.setFont(preloadDesc.getFont().deriveFont(Font.ITALIC, 11f));
        preloadRow.add(preloadDesc);
        mainPanel.add(preloadRow);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Thin BT Backgrounds Section
        JPanel thinBTSection = new JPanel();
        thinBTSection.setLayout(new BoxLayout(thinBTSection, BoxLayout.Y_AXIS));
        thinBTSection.setBorder(BorderFactory.createTitledBorder("Thin BT Backgrounds"));
        thinBTSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel thinBTDesc = new JLabel("Shown when Minecraft matches Thin BT dimensions");
        thinBTDesc.setFont(thinBTDesc.getFont().deriveFont(Font.ITALIC, 11f));
        thinBTDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        thinBTSection.add(thinBTDesc);
        thinBTSection.add(Box.createRigidArea(new Dimension(0, 4)));

        thinBTContainer = new JPanel();
        thinBTContainer.setLayout(new BoxLayout(thinBTContainer, BoxLayout.Y_AXIS));
        thinBTContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        thinBTSection.add(thinBTContainer);

        JButton addThinBTBtn = new JButton("+ Add Thin BT Background");
        addThinBTBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addThinBTBtn.addActionListener(a -> {
            String name = JOptionPane.showInputDialog(mainPanel, "Background name:", "Background");
            if (name != null && !name.trim().isEmpty()) {
                ThinCapture.addBackground(name.trim());
                rebuildBackgrounds();
            }
        });
        thinBTSection.add(addThinBTBtn);

        mainPanel.add(thinBTSection);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Planar Abuse Backgrounds Section
        JPanel planarSection = new JPanel();
        planarSection.setLayout(new BoxLayout(planarSection, BoxLayout.Y_AXIS));
        planarSection.setBorder(BorderFactory.createTitledBorder("Planar Abuse Backgrounds"));
        planarSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel planarDesc = new JLabel("Shown when Minecraft matches Planar Abuse dimensions");
        planarDesc.setFont(planarDesc.getFont().deriveFont(Font.ITALIC, 11f));
        planarDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        planarSection.add(planarDesc);
        planarSection.add(Box.createRigidArea(new Dimension(0, 4)));

        planarContainer = new JPanel();
        planarContainer.setLayout(new BoxLayout(planarContainer, BoxLayout.Y_AXIS));
        planarContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        planarSection.add(planarContainer);

        JButton addPlanarBtn = new JButton("+ Add Planar Abuse Background");
        addPlanarBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addPlanarBtn.addActionListener(a -> {
            String name = JOptionPane.showInputDialog(mainPanel, "Background name:", "Background");
            if (name != null && !name.trim().isEmpty()) {
                ThinCapture.addPlanarBackground(name.trim());
                rebuildBackgrounds();
            }
        });
        planarSection.add(addPlanarBtn);

        mainPanel.add(planarSection);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // EyeSee Backgrounds Section
        JPanel eyeSeeSection = new JPanel();
        eyeSeeSection.setLayout(new BoxLayout(eyeSeeSection, BoxLayout.Y_AXIS));
        eyeSeeSection.setBorder(BorderFactory.createTitledBorder("EyeSee Backgrounds"));
        eyeSeeSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel enableRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        enableRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        enableRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JCheckBox enableBox = new JCheckBox("Enable EyeSee Backgrounds");
        enableBox.setSelected(o.eyeSeeEnabled);
        enableBox.addActionListener(a -> o.eyeSeeEnabled = enableBox.isSelected());
        enableRow.add(enableBox);
        JLabel eyeSeeDesc = new JLabel("Shown with EyeSee projector toggle");
        eyeSeeDesc.setFont(eyeSeeDesc.getFont().deriveFont(Font.ITALIC, 11f));
        enableRow.add(eyeSeeDesc);
        eyeSeeSection.add(enableRow);
        eyeSeeSection.add(Box.createRigidArea(new Dimension(0, 4)));

        eyeSeeContainer = new JPanel();
        eyeSeeContainer.setLayout(new BoxLayout(eyeSeeContainer, BoxLayout.Y_AXIS));
        eyeSeeContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        eyeSeeSection.add(eyeSeeContainer);

        JButton addEyeSeeBtn = new JButton("+ Add EyeSee Background");
        addEyeSeeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        addEyeSeeBtn.addActionListener(a -> {
            String name = JOptionPane.showInputDialog(mainPanel, "Background name:", "Background");
            if (name != null && !name.trim().isEmpty()) {
                ThinCapture.addEyeSeeBackground(name.trim());
                rebuildBackgrounds();
            }
        });
        eyeSeeSection.add(addEyeSeeBtn);

        mainPanel.add(eyeSeeSection);
        mainPanel.add(Box.createVerticalGlue());

        rebuildBackgrounds();
    }

    private enum BgType { THIN_BT, PLANAR, EYESEE }

    private void rebuildBackgrounds() {
        ThinCaptureOptions o = ThinCapture.getOptions();

        // Rebuild Thin BT backgrounds
        thinBTContainer.removeAll();
        for (int i = 0; i < o.backgrounds.size(); i++) {
            thinBTContainer.add(buildBackgroundPanel(i, o.backgrounds.get(i), BgType.THIN_BT));
            thinBTContainer.add(Box.createRigidArea(new Dimension(0, 4)));
        }
        thinBTContainer.revalidate();
        thinBTContainer.repaint();

        // Rebuild Planar Abuse backgrounds
        planarContainer.removeAll();
        for (int i = 0; i < o.planarAbuseBackgrounds.size(); i++) {
            planarContainer.add(buildBackgroundPanel(i, o.planarAbuseBackgrounds.get(i), BgType.PLANAR));
            planarContainer.add(Box.createRigidArea(new Dimension(0, 4)));
        }
        planarContainer.revalidate();
        planarContainer.repaint();

        // Rebuild EyeSee backgrounds
        eyeSeeContainer.removeAll();
        for (int i = 0; i < o.eyeSeeBackgrounds.size(); i++) {
            eyeSeeContainer.add(buildBackgroundPanel(i, o.eyeSeeBackgrounds.get(i), BgType.EYESEE));
            eyeSeeContainer.add(Box.createRigidArea(new Dimension(0, 4)));
        }
        eyeSeeContainer.revalidate();
        eyeSeeContainer.repaint();
    }

    private JPanel buildBackgroundPanel(int index, BackgroundConfig bg, BgType type) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(bg.name));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(buildTopRow(index, bg, type));
        section.add(buildImageRow(index, bg, type));
        section.add(buildPositionRow(index, bg, type));

        return section;
    }

    private JPanel buildTopRow(int index, BackgroundConfig bg, BgType type) {
        JCheckBox enableBox = new JCheckBox("Enabled");
        enableBox.setSelected(bg.enabled);
        enableBox.addActionListener(a -> bg.enabled = enableBox.isSelected());

        JButton renameBtn = createSmallButton("Rename", a -> {
            String newName = JOptionPane.showInputDialog(mainPanel, "New name:", bg.name);
            if (newName != null && !newName.trim().isEmpty()) {
                switch (type) {
                    case THIN_BT: ThinCapture.renameBackground(index, newName.trim()); break;
                    case PLANAR:  ThinCapture.renamePlanarBackground(index, newName.trim()); break;
                    case EYESEE:  ThinCapture.renameEyeSeeBackground(index, newName.trim()); break;
                }
                rebuildBackgrounds();
            }
        });

        JButton removeBtn = createRemoveButton("background \"" + bg.name + "\"", () -> {
            switch (type) {
                case THIN_BT: ThinCapture.removeBackground(index); break;
                case PLANAR:  ThinCapture.removePlanarBackground(index); break;
                case EYESEE:  ThinCapture.removeEyeSeeBackground(index); break;
            }
            rebuildBackgrounds();
        });

        return createRow(enableBox, renameBtn, removeBtn);
    }

    private JPanel buildImageRow(int index, BackgroundConfig bg, BgType type) {
        JTextField bgPathField = new JTextField(bg.imagePath, 18);

        JButton browseBtn = createBrowseButton(path -> {
            bgPathField.setText(path);
            bg.imagePath = path;
            BackgroundFrame frame = getFrameForType(index, type);
            if (frame != null) frame.loadImage(path);
        });

        JButton clearBtn = createSmallButton("Clear", a -> {
            bgPathField.setText("");
            bg.imagePath = "";
            BackgroundFrame frame = getFrameForType(index, type);
            if (frame != null) frame.loadImage("");
        });

        return createRow(new JLabel("Image:"), bgPathField, browseBtn, clearBtn);
    }

    private JPanel buildPositionRow(int index, BackgroundConfig bg, BgType type) {
        JTextField bgXField = new JTextField(String.valueOf(bg.x), 4);
        JTextField bgYField = new JTextField(String.valueOf(bg.y), 4);
        JTextField bgWField = new JTextField(String.valueOf(bg.width), 5);
        JTextField bgHField = new JTextField(String.valueOf(bg.height), 5);

        Consumer<Rectangle> onRegionSelected = r -> {
            bgXField.setText(String.valueOf(r.x));
            bgYField.setText(String.valueOf(r.y));
            bgWField.setText(String.valueOf(r.width));
            bgHField.setText(String.valueOf(r.height));
            bg.x = r.x;
            bg.y = r.y;
            bg.width = r.width;
            bg.height = r.height;
        };

        JButton selectBtn = createSmallButton("Select", a -> RegionSelector.selectOnScreen(onRegionSelected));

        JButton editBtn = createSmallButton("Edit", a -> {
            Rectangle current = new Rectangle(
                    intFrom(bgXField, 0), intFrom(bgYField, 0), intFrom(bgWField, 1920), intFrom(bgHField, 1080)
            );
            RegionSelector.editOnScreen(current, onRegionSelected);
        });

        JButton applyBtn = createSmallButton("Apply", a -> {
            bg.x = intFrom(bgXField, 0);
            bg.y = intFrom(bgYField, 0);
            bg.width = Math.max(1, intFrom(bgWField, 1920));
            bg.height = Math.max(1, intFrom(bgHField, 1080));

            bgXField.setText(String.valueOf(bg.x));
            bgYField.setText(String.valueOf(bg.y));
            bgWField.setText(String.valueOf(bg.width));
            bgHField.setText(String.valueOf(bg.height));

            BackgroundFrame frame = getFrameForType(index, type);
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

        return posRow;
    }

    private BackgroundFrame getFrameForType(int index, BgType type) {
        switch (type) {
            case THIN_BT: return ThinCapture.getBgFrame(index);
            case PLANAR:  return ThinCapture.getPlanarBgFrame(index);
            case EYESEE:  return ThinCapture.getEyeSeeBgFrame(index);
            default:      return null;
        }
    }

    public void onSwitchTo() {
        rebuildBackgrounds();
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

    private JButton createBrowseButton(Consumer<String> onFileSelected) {
        JButton browseBtn = new JButton("Browse...");
        browseBtn.setMargin(new Insets(1, 6, 1, 6));
        browseBtn.addActionListener(a -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Images (png, jpg, bmp, gif)", "png", "jpg", "jpeg", "bmp", "gif"
            ));
            if (chooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                onFileSelected.accept(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        return browseBtn;
    }

    private static int intFrom(JTextField f, int fallback) {
        String t = f.getText().trim();
        boolean neg = t.startsWith("-");
        String nums = IntStream.range(0, t.length()).mapToObj(t::charAt)
                .filter(Character::isDigit).map(String::valueOf).collect(Collectors.joining());
        return nums.isEmpty() ? fallback : (neg ? -1 : 1) * Integer.parseInt(nums);
    }
}