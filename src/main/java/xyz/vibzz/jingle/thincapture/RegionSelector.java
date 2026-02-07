package xyz.vibzz.jingle.thincapture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

/**
 * A fullscreen transparent overlay that lets the user drag-select a rectangle.
 * Returns the selected rectangle via a callback.
 */
public class RegionSelector extends JFrame {
    private Point startPoint = null;
    private Point endPoint = null;
    private final Consumer<Rectangle> onSelected;

    public RegionSelector(String title, Rectangle bounds, Consumer<Rectangle> onSelected) {
        super();
        this.onSelected = onSelected;

        this.setUndecorated(true);
        this.setAlwaysOnTop(true);
        this.setBackground(new Color(0, 0, 0, 80));
        this.setBounds(bounds);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        this.setTitle(title);

        JPanel overlay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;

                // Dim background
                g2.setColor(new Color(0, 0, 0, 80));
                g2.fillRect(0, 0, getWidth(), getHeight());

                if (startPoint != null && endPoint != null) {
                    Rectangle r = getSelectionRect();

                    // Clear selected area (make it brighter)
                    g2.setComposite(AlphaComposite.Clear);
                    g2.fillRect(r.x, r.y, r.width, r.height);
                    g2.setComposite(AlphaComposite.SrcOver);

                    // Draw selection border
                    g2.setColor(Color.RED);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRect(r.x, r.y, r.width, r.height);

                    // Draw size label
                    String label = r.width + " x " + r.height;
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                    FontMetrics fm = g2.getFontMetrics();
                    int labelX = r.x + (r.width - fm.stringWidth(label)) / 2;
                    int labelY = r.y - 6;
                    if (labelY < 16) labelY = r.y + r.height + fm.getHeight() + 4;
                    g2.drawString(label, labelX, labelY);
                }

                // Instructions
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
                String msg = "Click and drag to select. Press ESC to cancel.";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, 30);
            }
        };
        overlay.setOpaque(false);
        this.setContentPane(overlay);

        overlay.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                endPoint = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                endPoint = e.getPoint();
                Rectangle r = getSelectionRect();
                if (r.width > 2 && r.height > 2) {
                    dispose();
                    onSelected.accept(r);
                }
            }
        });

        overlay.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                endPoint = e.getPoint();
                overlay.repaint();
            }
        });

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                }
            }
        });

        this.setVisible(true);
        this.requestFocus();
    }

    private Rectangle getSelectionRect() {
        int x = Math.min(startPoint.x, endPoint.x);
        int y = Math.min(startPoint.y, endPoint.y);
        int w = Math.abs(endPoint.x - startPoint.x);
        int h = Math.abs(endPoint.y - startPoint.y);
        return new Rectangle(x, y, w, h);
    }

    /**
     * Opens a selector over the entire screen for picking monitor position.
     */
    public static void selectOnScreen(Consumer<Rectangle> onSelected) {
        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        new RegionSelector("Select Monitor Position", screenBounds, onSelected);
    }

    /**
     * Opens a selector over the MC window area for picking capture region.
     * The returned rectangle is relative to the MC window client area.
     */
    public static void selectOnMCWindow(Consumer<Rectangle> onSelected) {
        if (!xyz.duncanruns.jingle.Jingle.getMainInstance().isPresent()) {
            JOptionPane.showMessageDialog(null, "No Minecraft instance detected.", "ThinCapture", JOptionPane.WARNING_MESSAGE);
            return;
        }

        com.sun.jna.platform.win32.WinDef.HWND hwnd = xyz.duncanruns.jingle.Jingle.getMainInstance().get().hwnd;
        com.sun.jna.platform.win32.WinDef.RECT rect = new com.sun.jna.platform.win32.WinDef.RECT();
        xyz.duncanruns.jingle.win32.User32.INSTANCE.GetWindowRect(hwnd, rect);

        Rectangle mcBounds = new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);

        new RegionSelector("Select MC Region", mcBounds, region -> {
            // Region is already relative to the MC window since the overlay was positioned there
            onSelected.accept(region);
        });
    }
}