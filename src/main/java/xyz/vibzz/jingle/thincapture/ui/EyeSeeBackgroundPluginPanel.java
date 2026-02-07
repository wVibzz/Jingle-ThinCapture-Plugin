package xyz.vibzz.jingle.thincapture.ui;

import xyz.vibzz.jingle.thincapture.ThinCapture;
import xyz.vibzz.jingle.thincapture.ThinCaptureOptions;
import xyz.vibzz.jingle.thincapture.config.BackgroundConfig;
import xyz.vibzz.jingle.thincapture.frame.BackgroundFrame;

import javax.swing.*;
import java.awt.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EyeSeeBackgroundPluginPanel {
    public final JPanel mainPanel;
    private final JPanel backgroundsContainer;

    public EyeSeeBackgroundPluginPanel() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        ThinCaptureOptions o = ThinCapture.getOptions();

        // General Settings
        JPanel generalPanel = new JPanel();
        generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
        generalPanel.setBorder(BorderFactory.createTitledBorder("EyeSee Background Settings"));
        generalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel enableRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        enableRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JCheckBox enableBox = new JCheckBox("Enable EyeSee Backgrounds");
        enableBox.setSelected(o.eyeSeeEnabled);
        enableBox.addActionListener(a -> o.eyeSeeEnabled = enableBox.isSelected());
        enableRow.add(enableBox);
        JLabel desc = new JLabel("Shows/hides with EyeSee projector toggle");
        desc.setFont(desc.getFont().deriveFont(Font.ITALIC, 11f));
        enableRow.add(desc);
        generalPanel.add(enableRow);

        mainPanel.add(generalPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        backgroundsContainer = new JPanel();
        backgroundsContainer.setLayout(new BoxLayout(backgroundsContainer, BoxLayout.Y_AXIS));
        backgroundsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(backgroundsContainer);

        // Add button
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        addRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addBgBtn = new JButton("+ Add Background");
        addBgBtn.addActionListener(a -> {
            String name = JOptionPane.showInputDialog(mainPanel, "Background name:", "Background");
            if (name != null && !name.trim().isEmpty()) {
                ThinCapture.addEyeSeeBackground(name.trim());
                rebuildBackgrounds();
            }
        });
        addRow.add(addBgBtn);

        mainPanel.add(addRow);
        mainPanel.add(Box.createVerticalGlue());

        rebuildBackgrounds();
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
        BackgroundConfig bg = o.eyeSeeBackgrounds.get(index);

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
                ThinCapture.renameEyeSeeBackground(index, newName.trim());
                rebuildBackgrounds();
            }
        });

        JButton removeBtn = createRemoveButton("background \"" + bg.name + "\"", () -> {
            ThinCapture.removeEyeSeeBackground(index);
            rebuildBackgrounds();
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
                BackgroundFrame frame = ThinCapture.getEyeSeeBgFrame(index);
                if (frame != null) frame.loadImage(path);
            }
        });
        clearBtn.addActionListener(a -> {
            bgPathField.setText("");
            bg.imagePath = "";
            BackgroundFrame frame = ThinCapture.getEyeSeeBgFrame(index);
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

            BackgroundFrame frame = ThinCapture.getEyeSeeBgFrame(index);
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

    private void rebuildBackgrounds() {
        backgroundsContainer.removeAll();

        ThinCaptureOptions o = ThinCapture.getOptions();

        for (int i = 0; i < o.eyeSeeBackgrounds.size(); i++) {
            backgroundsContainer.add(buildBackgroundPanel(i));
            backgroundsContainer.add(Box.createRigidArea(new Dimension(0, 4)));
        }

        backgroundsContainer.revalidate();
        backgroundsContainer.repaint();
    }

    public void onSwitchTo() {
        rebuildBackgrounds();
    }

    private static int intFrom(JTextField f, int fallback) {
        String t = f.getText().trim();
        boolean neg = t.startsWith("-");
        String nums = IntStream.range(0, t.length()).mapToObj(t::charAt)
                .filter(Character::isDigit).map(String::valueOf).collect(Collectors.joining());
        return nums.isEmpty() ? fallback : (neg ? -1 : 1) * Integer.parseInt(nums);
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}