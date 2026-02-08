package xyz.vibzz.jingle.thincapture.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.function.Consumer;

/**
 * A fullscreen transparent overlay that lets the user drag-select or edit a rectangle.
 * Supports two modes:
 *   - Create mode: drag to draw a new rectangle
 *   - Edit mode: move/resize an existing rectangle via drag and corner handles
 */
public class RegionSelector extends JFrame {
    private static final int HANDLE_SIZE = 8;
    private static final int EDGE_TOLERANCE = 6;
    private static final int MOVE_HANDLE_SIZE = 24;

    private Point startPoint = null;
    private Point endPoint = null;
    private final Consumer<Rectangle> onSelected;

    // Edit mode state
    private final boolean editMode;
    private Rectangle editRect;
    private DragType dragType = DragType.NONE;
    private Point dragStart = null;
    private Rectangle dragOrigRect = null;

    private enum DragType {
        NONE, MOVE,
        RESIZE_NW, RESIZE_NE, RESIZE_SW, RESIZE_SE,
        RESIZE_N, RESIZE_S, RESIZE_W, RESIZE_E
    }

    /**
     * Create mode constructor — drag to select a new region.
     */
    public RegionSelector(String title, Rectangle bounds, Consumer<Rectangle> onSelected) {
        this(title, bounds, null, onSelected);
    }

    /**
     * Full constructor — if initialRect is non-null, starts in edit mode.
     */
    public RegionSelector(String title, Rectangle bounds, Rectangle initialRect, Consumer<Rectangle> onSelected) {
        super();
        this.onSelected = onSelected;
        this.editMode = initialRect != null;
        this.editRect = initialRect != null ? new Rectangle(initialRect) : null;

        this.setUndecorated(true);
        this.setAlwaysOnTop(true);
        this.setBackground(new Color(0, 0, 0, 80));
        this.setBounds(bounds);
        this.setTitle(title);

        JPanel overlay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(0, 0, 0, 80));
                g2.fillRect(0, 0, getWidth(), getHeight());

                Rectangle r = getCurrentRect();
                if (r != null && r.width > 0 && r.height > 0) {
                    g2.setComposite(AlphaComposite.Clear);
                    g2.fillRect(r.x, r.y, r.width, r.height);
                    g2.setComposite(AlphaComposite.SrcOver);

                    g2.setColor(Color.RED);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRect(r.x, r.y, r.width, r.height);

                    String label = r.width + " x " + r.height;
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                    FontMetrics fm = g2.getFontMetrics();
                    int labelX = r.x + (r.width - fm.stringWidth(label)) / 2;
                    int labelY = r.y - 6;
                    if (labelY < 16) labelY = r.y + r.height + fm.getHeight() + 4;
                    g2.drawString(label, labelX, labelY);

                    if (editMode) {
                        drawHandles(g2, r);
                    }
                }

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
                String msg = editMode
                        ? "Drag to move. Drag corners/edges to resize. Press ENTER to confirm, ESC to cancel."
                        : "Click and drag to select. Press ESC to cancel.";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, 30);
            }
        };
        overlay.setOpaque(false);
        this.setContentPane(overlay);

        if (editMode) {
            setupEditListeners(overlay);
        } else {
            setupCreateListeners(overlay);
        }

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && editMode && editRect != null) {
                    dispose();
                    onSelected.accept(new Rectangle(editRect));
                }
            }
        });

        this.setVisible(true);
        this.requestFocus();
    }

    private Rectangle getCurrentRect() {
        if (editMode) return editRect;
        if (startPoint != null && endPoint != null) return getSelectionRect();
        return null;
    }

    private void setupCreateListeners(JPanel overlay) {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

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
    }

    private void setupEditListeners(JPanel overlay) {
        overlay.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (editRect == null) return;
                dragStart = e.getPoint();
                dragOrigRect = new Rectangle(editRect);
                dragType = hitTest(e.getPoint(), editRect);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragType = DragType.NONE;
                dragStart = null;
                dragOrigRect = null;
            }
        });

        overlay.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragType == DragType.NONE || dragStart == null || dragOrigRect == null) return;

                int dx = e.getX() - dragStart.x;
                int dy = e.getY() - dragStart.y;

                Rectangle r = new Rectangle(dragOrigRect);

                switch (dragType) {
                    case MOVE:
                        r.x += dx;
                        r.y += dy;
                        break;
                    case RESIZE_NW:
                        r.x += dx; r.y += dy; r.width -= dx; r.height -= dy;
                        break;
                    case RESIZE_NE:
                        r.y += dy; r.width += dx; r.height -= dy;
                        break;
                    case RESIZE_SW:
                        r.x += dx; r.width -= dx; r.height += dy;
                        break;
                    case RESIZE_SE:
                        r.width += dx; r.height += dy;
                        break;
                    case RESIZE_N:
                        r.y += dy; r.height -= dy;
                        break;
                    case RESIZE_S:
                        r.height += dy;
                        break;
                    case RESIZE_W:
                        r.x += dx; r.width -= dx;
                        break;
                    case RESIZE_E:
                        r.width += dx;
                        break;
                }

                if (r.width >= 4 && r.height >= 4) {
                    editRect = r;
                }

                overlay.repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (editRect == null) return;
                DragType hit = hitTest(e.getPoint(), editRect);
                overlay.setCursor(getCursorForDragType(hit));
            }
        });
    }

    private DragType hitTest(Point p, Rectangle r) {
        int x = p.x, y = p.y;

        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        int mhs = MOVE_HANDLE_SIZE / 2;
        if (x >= cx - mhs && x <= cx + mhs && y >= cy - mhs && y <= cy + mhs) {
            return DragType.MOVE;
        }

        boolean nearLeft   = Math.abs(x - r.x) <= EDGE_TOLERANCE;
        boolean nearRight  = Math.abs(x - (r.x + r.width)) <= EDGE_TOLERANCE;
        boolean nearTop    = Math.abs(y - r.y) <= EDGE_TOLERANCE;
        boolean nearBottom = Math.abs(y - (r.y + r.height)) <= EDGE_TOLERANCE;
        boolean inX = x >= r.x - EDGE_TOLERANCE && x <= r.x + r.width + EDGE_TOLERANCE;
        boolean inY = y >= r.y - EDGE_TOLERANCE && y <= r.y + r.height + EDGE_TOLERANCE;

        if (nearLeft && nearTop) return DragType.RESIZE_NW;
        if (nearRight && nearTop) return DragType.RESIZE_NE;
        if (nearLeft && nearBottom) return DragType.RESIZE_SW;
        if (nearRight && nearBottom) return DragType.RESIZE_SE;

        if (nearTop && inX) return DragType.RESIZE_N;
        if (nearBottom && inX) return DragType.RESIZE_S;
        if (nearLeft && inY) return DragType.RESIZE_W;
        if (nearRight && inY) return DragType.RESIZE_E;

        return DragType.NONE;
    }

    private Cursor getCursorForDragType(DragType dt) {
        switch (dt) {
            case MOVE:      return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            case RESIZE_NW: return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            case RESIZE_NE: return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            case RESIZE_SW: return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
            case RESIZE_SE: return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
            case RESIZE_N:  return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
            case RESIZE_S:  return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
            case RESIZE_W:  return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
            case RESIZE_E:  return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            default:        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
    }

    private void drawHandles(Graphics2D g2, Rectangle r) {
        g2.setColor(Color.WHITE);
        int hs = HANDLE_SIZE;
        drawHandle(g2, r.x, r.y, hs);
        drawHandle(g2, r.x + r.width, r.y, hs);
        drawHandle(g2, r.x, r.y + r.height, hs);
        drawHandle(g2, r.x + r.width, r.y + r.height, hs);
        drawHandle(g2, r.x + r.width / 2, r.y, hs);
        drawHandle(g2, r.x + r.width / 2, r.y + r.height, hs);
        drawHandle(g2, r.x, r.y + r.height / 2, hs);
        drawHandle(g2, r.x + r.width, r.y + r.height / 2, hs);

        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        int ms = MOVE_HANDLE_SIZE;
        int mhs = ms / 2;

        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillRoundRect(cx - mhs, cy - mhs, ms, ms, 4, 4);
        g2.setColor(Color.RED);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(cx - mhs, cy - mhs, ms, ms, 4, 4);

        g2.setColor(new Color(60, 60, 60));
        g2.setStroke(new BasicStroke(1.5f));
        int arrowLen = ms / 3;
        int tipSize = 3;
        g2.drawLine(cx, cy - arrowLen, cx, cy + arrowLen);
        g2.drawLine(cx, cy - arrowLen, cx - tipSize, cy - arrowLen + tipSize);
        g2.drawLine(cx, cy - arrowLen, cx + tipSize, cy - arrowLen + tipSize);
        g2.drawLine(cx, cy + arrowLen, cx - tipSize, cy + arrowLen - tipSize);
        g2.drawLine(cx, cy + arrowLen, cx + tipSize, cy + arrowLen - tipSize);
        g2.drawLine(cx - arrowLen, cy, cx + arrowLen, cy);
        g2.drawLine(cx - arrowLen, cy, cx - arrowLen + tipSize, cy - tipSize);
        g2.drawLine(cx - arrowLen, cy, cx - arrowLen + tipSize, cy + tipSize);
        g2.drawLine(cx + arrowLen, cy, cx + arrowLen - tipSize, cy - tipSize);
        g2.drawLine(cx + arrowLen, cy, cx + arrowLen - tipSize, cy + tipSize);

        g2.setStroke(new BasicStroke(2));
    }

    private void drawHandle(Graphics2D g2, int cx, int cy, int size) {
        g2.fillRect(cx - size / 2, cy - size / 2, size, size);
        g2.setColor(Color.RED);
        g2.drawRect(cx - size / 2, cy - size / 2, size, size);
        g2.setColor(Color.WHITE);
    }

    private Rectangle getSelectionRect() {
        int x = Math.min(startPoint.x, endPoint.x);
        int y = Math.min(startPoint.y, endPoint.y);
        int w = Math.abs(endPoint.x - startPoint.x);
        int h = Math.abs(endPoint.y - startPoint.y);
        return new Rectangle(x, y, w, h);
    }

    // ===== Static helpers =====

    /**
     * Gets the display scale factor from the default screen's AffineTransform.
     * On Java 9+ at 125% DPI this returns 1.25.
     * On Java 8 this returns 1.0 (Java 8 is not DPI-aware).
     */
    private static double getDisplayScaleX() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration()
                .getDefaultTransform().getScaleX();
    }

    private static double getDisplayScaleY() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration()
                .getDefaultTransform().getScaleY();
    }

    public static void selectOnScreen(Consumer<Rectangle> onSelected) {
        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        new RegionSelector("Select Monitor Position", screenBounds, onSelected);
    }

    public static void editOnScreen(Rectangle current, Consumer<Rectangle> onSelected) {
        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        new RegionSelector("Edit Monitor Position", screenBounds, current, onSelected);
    }

    /**
     * Opens a region selector overlay positioned over the MC window's client area.
     * The returned rectangle is in MC client pixel coordinates.
     *
     * GetWindowRect returns physical screen coords.
     * Java's setBounds expects logical coords (physical / displayScale on Java 9+).
     * We use ClientToScreen to find the client area's screen position,
     * then convert to Java logical coords for the overlay.
     */
    public static void selectOnMCWindow(Consumer<Rectangle> onSelected) {
        if (!xyz.duncanruns.jingle.Jingle.getMainInstance().isPresent()) {
            JOptionPane.showMessageDialog(null, "No Minecraft instance detected.", "ThinCapture", JOptionPane.WARNING_MESSAGE);
            return;
        }
        com.sun.jna.platform.win32.WinDef.HWND hwnd = xyz.duncanruns.jingle.Jingle.getMainInstance().get().hwnd;

        Rectangle mcBounds = getMCClientBoundsInLogicalCoords(hwnd);
        if (mcBounds == null) return;

        // Get MC client size for converting overlay coords back to client pixels
        com.sun.jna.platform.win32.WinDef.RECT clientRect = new com.sun.jna.platform.win32.WinDef.RECT();
        xyz.duncanruns.jingle.win32.User32.INSTANCE.GetClientRect(hwnd, clientRect);
        int clientW = clientRect.right - clientRect.left;
        int clientH = clientRect.bottom - clientRect.top;

        // Ratio: overlay logical pixels -> MC client pixels
        double toClientX = mcBounds.width > 0 ? (double) clientW / mcBounds.width : 1.0;
        double toClientY = mcBounds.height > 0 ? (double) clientH / mcBounds.height : 1.0;

        new RegionSelector("Select MC Region", mcBounds, r -> {
            onSelected.accept(new Rectangle(
                    (int) (r.x * toClientX),
                    (int) (r.y * toClientY),
                    (int) (r.width * toClientX),
                    (int) (r.height * toClientY)
            ));
        });
    }

    public static void editOnMCWindow(Rectangle current, Consumer<Rectangle> onSelected) {
        if (!xyz.duncanruns.jingle.Jingle.getMainInstance().isPresent()) {
            JOptionPane.showMessageDialog(null, "No Minecraft instance detected.", "ThinCapture", JOptionPane.WARNING_MESSAGE);
            return;
        }
        com.sun.jna.platform.win32.WinDef.HWND hwnd = xyz.duncanruns.jingle.Jingle.getMainInstance().get().hwnd;

        Rectangle mcBounds = getMCClientBoundsInLogicalCoords(hwnd);
        if (mcBounds == null) return;

        com.sun.jna.platform.win32.WinDef.RECT clientRect = new com.sun.jna.platform.win32.WinDef.RECT();
        xyz.duncanruns.jingle.win32.User32.INSTANCE.GetClientRect(hwnd, clientRect);
        int clientW = clientRect.right - clientRect.left;
        int clientH = clientRect.bottom - clientRect.top;

        double toClientX = mcBounds.width > 0 ? (double) clientW / mcBounds.width : 1.0;
        double toClientY = mcBounds.height > 0 ? (double) clientH / mcBounds.height : 1.0;
        double toOverlayX = toClientX > 0 ? 1.0 / toClientX : 1.0;
        double toOverlayY = toClientY > 0 ? 1.0 / toClientY : 1.0;

        // Convert current MC client rect to overlay coords for editing
        Rectangle overlayRect = new Rectangle(
                (int) (current.x * toOverlayX),
                (int) (current.y * toOverlayY),
                (int) (current.width * toOverlayX),
                (int) (current.height * toOverlayY)
        );

        new RegionSelector("Edit MC Region", mcBounds, overlayRect, r -> {
            onSelected.accept(new Rectangle(
                    (int) (r.x * toClientX),
                    (int) (r.y * toClientY),
                    (int) (r.width * toClientX),
                    (int) (r.height * toClientY)
            ));
        });
    }

    private static Rectangle getMCClientBoundsInLogicalCoords(com.sun.jna.platform.win32.WinDef.HWND hwnd) {
        // Full window rect in physical screen coords
        com.sun.jna.platform.win32.WinDef.RECT winRect = new com.sun.jna.platform.win32.WinDef.RECT();
        xyz.duncanruns.jingle.win32.User32.INSTANCE.GetWindowRect(hwnd, winRect);
        int winW = winRect.right - winRect.left;
        int winH = winRect.bottom - winRect.top;

        // Client area size in client pixels
        com.sun.jna.platform.win32.WinDef.RECT clientRect = new com.sun.jna.platform.win32.WinDef.RECT();
        xyz.duncanruns.jingle.win32.User32.INSTANCE.GetClientRect(hwnd, clientRect);
        int clientW = clientRect.right - clientRect.left;
        int clientH = clientRect.bottom - clientRect.top;

        // Use DWMWA_EXTENDED_FRAME_BOUNDS or compute frame sizes
        // For standard windows: borders are symmetric left/right/bottom, title bar is on top
        // Physical frame sizes = (winSize - clientSize_in_physical)
        // clientSize_in_physical might differ from clientW at non-100% DPI if DWM is scaling
        //
        // Simplest robust approach: use the window style to compute frame sizes
        int style = com.sun.jna.platform.win32.User32.INSTANCE.GetWindowLong(hwnd, -16); // GWL_STYLE
        int exStyle = com.sun.jna.platform.win32.User32.INSTANCE.GetWindowLong(hwnd, -20); // GWL_EXSTYLE

        // Use AdjustWindowRectEx to figure out how much the frame adds
        com.sun.jna.platform.win32.WinDef.RECT frameRect = new com.sun.jna.platform.win32.WinDef.RECT();
        frameRect.left = 0;
        frameRect.top = 0;
        frameRect.right = 0;
        frameRect.bottom = 0;
        com.sun.jna.platform.win32.User32.INSTANCE.AdjustWindowRectEx(
                frameRect,
                new com.sun.jna.platform.win32.WinDef.DWORD(style),
                new com.sun.jna.platform.win32.WinDef.BOOL(false),
                new com.sun.jna.platform.win32.WinDef.DWORD(exStyle)
        );
        // frameRect now contains negative left/top (border + title) and positive right/bottom
        // e.g. left=-8, top=-31, right=8, bottom=8
        int borderLeft = -frameRect.left;
        int borderTop = -frameRect.top;

        // The client area starts at (winRect.left + borderLeft, winRect.top + borderTop)
        // in physical screen coords
        int clientScreenX = winRect.left + borderLeft;
        int clientScreenY = winRect.top + borderTop;

        // Physical size of the client area on screen (may differ from clientW/H if DWM is scaling)
        int physClientW = winW - borderLeft - frameRect.right;
        int physClientH = winH - borderTop - frameRect.bottom;

        // Convert to Java logical coords
        double scaleX = getDisplayScaleX();
        double scaleY = getDisplayScaleY();

        return new Rectangle(
                (int) (clientScreenX / scaleX),
                (int) (clientScreenY / scaleY),
                (int) (physClientW / scaleX),
                (int) (physClientH / scaleY)
        );
    }
}