package xyz.vibzz.jingle.thincapture.frame;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.WindowStateUtil;
import xyz.duncanruns.jingle.win32.User32;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

public class BackgroundFrame extends JFrame {
    private static final int SWP_NOMOVE = 0x0002;
    private static final int SWP_NOSIZE = 0x0001;
    private static final int SW_HIDE = 0;
    private static final int SW_SHOWNOACTIVATE = 4;

    private final WinDef.HWND frameHwnd;
    private boolean currentlyShowing = false;
    private BufferedImage bgImage = null;
    private boolean useImage = true;
    private Color bgColor = Color.BLACK;

    public BackgroundFrame() {
        super("ThinCapture Background");
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setFocusableWindowState(false);
        this.setUndecorated(true);
        this.setBackground(Color.BLACK);

        this.setVisible(true);
        frameHwnd = new WinDef.HWND(Native.getWindowPointer(this));
        WindowStateUtil.setHwndBorderless(frameHwnd);
        com.sun.jna.platform.win32.User32.INSTANCE.ShowWindow(frameHwnd, SW_HIDE);
    }

    public WinDef.HWND getHwnd() {
        return frameHwnd;
    }

    public void setUseImage(boolean useImage) {
        this.useImage = useImage;
        repaint();
    }

    public void setBgColor(Color color) {
        this.bgColor = color != null ? color : Color.BLACK;
        repaint();
    }

    public void loadImage(String path) {
        if (path == null || path.trim().isEmpty()) {
            bgImage = null;
            return;
        }
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                bgImage = ImageIO.read(file);
            } else {
                bgImage = null;
                Jingle.log(Level.WARN, "ThinCapture Background: image not found: " + path);
            }
        } catch (Exception e) {
            bgImage = null;
            Jingle.log(Level.WARN, "ThinCapture Background: failed to load image: " + e.getMessage());
        }
    }

    public void positionBackground(int x, int y, int width, int height) {
        setScaledWindowPos(frameHwnd, x, y, width, height);
    }

    public void showBackground() {
        Jingle.log(Level.DEBUG, "Showing ThinCapture Background...");
        com.sun.jna.platform.win32.User32.INSTANCE.ShowWindow(frameHwnd, SW_SHOWNOACTIVATE);
        currentlyShowing = true;
    }

    /**
     * Places this background window directly behind the given window in the Z-order.
     */
    public void sendBehind(WinDef.HWND insertAfter) {
        if (!currentlyShowing) return;

        User32.INSTANCE.SetWindowPos(
                frameHwnd,
                insertAfter,
                0, 0, 0, 0,
                User32.SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOSIZE
        );
    }

    public void hideBackground() {
        Jingle.log(Level.DEBUG, "Hiding ThinCapture Background...");
        currentlyShowing = false;
        com.sun.jna.platform.win32.User32.INSTANCE.ShowWindow(frameHwnd, SW_HIDE);
    }

    @Override
    public void paint(Graphics g) {
        if (useImage && bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), null);
        } else {
            g.setColor(bgColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    @Override
    public boolean isShowing() {
        return currentlyShowing;
    }

    @Override
    public void dispose() {
        currentlyShowing = false;
        super.dispose();
    }

    private static void setScaledWindowPos(WinDef.HWND hwnd, int x, int y, int w, int h) {
        AffineTransform tx = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration()
                .getDefaultTransform();
        double scaleX = tx.getScaleX();
        double scaleY = tx.getScaleY();

        User32.INSTANCE.SetWindowPos(
                hwnd,
                new WinDef.HWND(new Pointer(0)),
                (int) (x * scaleX),
                (int) (y * scaleY),
                (int) (w * scaleX),
                (int) (h * scaleY),
                User32.SWP_NOACTIVATE | User32.SWP_NOSENDCHANGING
        );
    }
}