package xyz.vibzz.jingle.thincapture;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinUser;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.WindowStateUtil;
import xyz.duncanruns.jingle.win32.GDI32Extra;
import xyz.duncanruns.jingle.win32.User32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CaptureFrame extends JFrame {
    private static final int SHOW_FLAGS = User32.SWP_NOACTIVATE | User32.SWP_NOSENDCHANGING;
    private static final int SWP_SHOWWINDOW = 0x0040;
    private static final int SWP_HIDEWINDOW = 0x0080;
    private static final int SWP_NOMOVE = 0x0002;
    private static final int SWP_NOSIZE = 0x0001;
    private static final WinDef.DWORD SRCCOPY = new WinDef.DWORD(0x00CC0020);

    // Win32 constants for layered windows
    private static final int GWL_EXSTYLE = -20;
    private static final int WS_EX_LAYERED = 0x00080000;
    private static final int WS_EX_TRANSPARENT = 0x00000020;
    private static final int ULW_ALPHA = 0x00000002;
    private static final int AC_SRC_OVER = 0x00;
    private static final int AC_SRC_ALPHA = 0x01;

    private final String name;
    private final WinDef.HWND frameHwnd;
    private boolean currentlyShowing = false;
    private ScheduledFuture<?> redrawTask;

    private Rectangle windowBounds = new Rectangle();
    private Rectangle captureRegion = new Rectangle();

    // Filtering options
    private boolean textOnly = false;
    private int textThreshold = 200;
    private boolean transparentBg = true;
    private Color bgColor = Color.BLACK;

    public CaptureFrame(String name) {
        super();
        this.name = name;
        this.setResizable(false);
        this.setTitle("ThinCapture " + name);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setAlwaysOnTop(true);
        this.setFocusableWindowState(false);
        this.setUndecorated(true);

        this.setVisible(true);
        frameHwnd = new WinDef.HWND(Native.getWindowPointer(this));
        WindowStateUtil.setHwndBorderless(frameHwnd);

        restartDrawingTask(ThinCapture.getOptions().fpsLimit);
        this.setVisible(false);
    }

    public void setFilterOptions(boolean textOnly, int textThreshold, boolean transparentBg, Color bgColor) {
        this.textOnly = textOnly;
        this.textThreshold = textThreshold;
        this.transparentBg = transparentBg;
        this.bgColor = bgColor;
    }

    /**
     * Makes the window a layered window for per-pixel transparency.
     */
    private void enableLayeredWindow() {
        int exStyle = com.sun.jna.platform.win32.User32.INSTANCE.GetWindowLong(frameHwnd, GWL_EXSTYLE);
        com.sun.jna.platform.win32.User32.INSTANCE.SetWindowLong(frameHwnd, GWL_EXSTYLE, exStyle | WS_EX_LAYERED);
    }

    /**
     * Removes the layered window style for direct GDI rendering.
     */
    private void disableLayeredWindow() {
        int exStyle = com.sun.jna.platform.win32.User32.INSTANCE.GetWindowLong(frameHwnd, GWL_EXSTYLE);
        com.sun.jna.platform.win32.User32.INSTANCE.SetWindowLong(frameHwnd, GWL_EXSTYLE, exStyle & ~WS_EX_LAYERED);
    }

    private void tick() {
        if (!currentlyShowing) return;
        if (!Jingle.getMainInstance().isPresent()) return;

        WinDef.HWND hwnd = Jingle.getMainInstance().get().hwnd;

        if (textOnly) {
            tickFiltered(hwnd);
        } else {
            tickDirect(hwnd);
        }
    }

    /**
     * Direct GDI blit — no filtering, fastest path.
     */
    private void tickDirect(WinDef.HWND hwnd) {
        WinDef.HDC srcDC = User32.INSTANCE.GetDC(hwnd);
        WinDef.HDC dstDC = User32.INSTANCE.GetDC(frameHwnd);

        GDI32Extra.INSTANCE.SetStretchBltMode(dstDC, 3);
        GDI32Extra.INSTANCE.StretchBlt(
                dstDC,
                0, 0,
                windowBounds.width, windowBounds.height,
                srcDC,
                captureRegion.x, captureRegion.y,
                captureRegion.width, captureRegion.height,
                SRCCOPY
        );

        User32.INSTANCE.ReleaseDC(hwnd, srcDC);
        User32.INSTANCE.ReleaseDC(frameHwnd, dstDC);
    }

    /**
     * Filtered path — captures to BufferedImage, filters pixels,
     * then uses UpdateLayeredWindow for per-pixel transparency.
     */
    private void tickFiltered(WinDef.HWND hwnd) {
        try {
            // Capture from MC window
            WinDef.HDC srcDC = User32.INSTANCE.GetDC(hwnd);
            WinDef.HDC memDC = GDI32Extra.INSTANCE.CreateCompatibleDC(srcDC);

            // Create a DIB section we can read pixels from
            BufferedImage capture = new BufferedImage(
                    captureRegion.width, captureRegion.height, BufferedImage.TYPE_INT_ARGB
            );

            // Use Robot for reliable capture
            WinDef.RECT winRect = new WinDef.RECT();
            User32.INSTANCE.GetWindowRect(hwnd, winRect);

            User32.INSTANCE.ReleaseDC(hwnd, srcDC);

            Robot robot = new Robot();
            BufferedImage rawCapture = robot.createScreenCapture(
                    new Rectangle(
                            winRect.left + captureRegion.x,
                            winRect.top + captureRegion.y,
                            captureRegion.width,
                            captureRegion.height
                    )
            );

            // Build filtered ARGB image at output size
            int outW = windowBounds.width;
            int outH = windowBounds.height;
            BufferedImage output = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);

            // Scale raw capture to output size first
            BufferedImage scaled;
            if (outW != captureRegion.width || outH != captureRegion.height) {
                scaled = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = scaled.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.drawImage(rawCapture, 0, 0, outW, outH, null);
                g2.dispose();
            } else {
                scaled = rawCapture;
            }

            int bgArgb;
            if (transparentBg) {
                bgArgb = 0x00000000;
            } else {
                bgArgb = (0xFF << 24) | (bgColor.getRed() << 16) | (bgColor.getGreen() << 8) | bgColor.getBlue();
            }

            for (int y = 0; y < outH; y++) {
                for (int x = 0; x < outW; x++) {
                    int rgb = scaled.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    if (r >= textThreshold && g >= textThreshold && b >= textThreshold) {
                        // Bright pixel — keep as text
                        output.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
                    } else {
                        output.setRGB(x, y, bgArgb);
                    }
                }
            }

            // Use UpdateLayeredWindow for per-pixel alpha
            updateLayered(output, outW, outH);

        } catch (Exception e) {
            Jingle.log(Level.DEBUG, "ThinCapture " + name + " filter error: " + e.getMessage());
        }
    }

    /**
     * Updates the layered window with a 32-bit ARGB BufferedImage for per-pixel transparency.
     */
    private void updateLayered(BufferedImage image, int width, int height) {
        WinDef.HDC screenDC = com.sun.jna.platform.win32.User32.INSTANCE.GetDC(null);
        WinDef.HDC memDC = GDI32Extra.INSTANCE.CreateCompatibleDC(screenDC);

        WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
        bmi.bmiHeader.biSize = bmi.bmiHeader.size();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = -height; // top-down
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        WinDef.HDC hdc = memDC;
        com.sun.jna.ptr.PointerByReference ppvBits = new com.sun.jna.ptr.PointerByReference();
        WinDef.HBITMAP hBitmap = com.sun.jna.platform.win32.GDI32.INSTANCE.CreateDIBSection(
                hdc, bmi, WinGDI.DIB_RGB_COLORS, ppvBits, null, 0
        );

        com.sun.jna.platform.win32.WinNT.HANDLE oldBitmap = com.sun.jna.platform.win32.GDI32.INSTANCE.SelectObject(memDC, hBitmap);

        // Copy pixel data to the DIB
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        // Convert ARGB to BGRA (premultiplied alpha) for UpdateLayeredWindow
        byte[] bgraData = new byte[width * height * 4];
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;

            // Premultiply alpha
            r = (r * a) / 255;
            g = (g * a) / 255;
            b = (b * a) / 255;

            int offset = i * 4;
            bgraData[offset] = (byte) b;
            bgraData[offset + 1] = (byte) g;
            bgraData[offset + 2] = (byte) r;
            bgraData[offset + 3] = (byte) a;
        }

        ppvBits.getValue().write(0, bgraData, 0, bgraData.length);

        WinUser.POINT ptSrc = new WinUser.POINT(0, 0);
        WinUser.SIZE sizeWnd = new WinUser.SIZE(width, height);
        WinUser.POINT ptDst = new WinUser.POINT(windowBounds.x, windowBounds.y);

        WinUser.BLENDFUNCTION blend = new WinUser.BLENDFUNCTION();
        blend.BlendOp = AC_SRC_OVER;
        blend.BlendFlags = 0;
        blend.SourceConstantAlpha = (byte) 255;
        blend.AlphaFormat = AC_SRC_ALPHA;

        com.sun.jna.platform.win32.User32.INSTANCE.UpdateLayeredWindow(
                frameHwnd,
                screenDC,
                ptDst,
                sizeWnd,
                memDC,
                ptSrc,
                0,
                blend,
                ULW_ALPHA
        );

        // Cleanup
        com.sun.jna.platform.win32.GDI32.INSTANCE.SelectObject(memDC, oldBitmap);
        com.sun.jna.platform.win32.GDI32.INSTANCE.DeleteObject(hBitmap);
        com.sun.jna.platform.win32.GDI32.INSTANCE.DeleteDC(memDC);
        com.sun.jna.platform.win32.User32.INSTANCE.ReleaseDC(null, screenDC);
    }

    public void showCapture(Rectangle screenPos, Rectangle capture) {
        if (!Jingle.getMainInstance().isPresent()) return;
        Jingle.log(Level.DEBUG, "Showing ThinCapture " + name + "...");

        this.windowBounds = screenPos;
        this.captureRegion = capture;

        if (textOnly) {
            enableLayeredWindow();
        } else {
            disableLayeredWindow();
        }

        // Position the window off-screen first, then render a frame before showing
        User32.INSTANCE.SetWindowPos(
                frameHwnd,
                new WinDef.HWND(new Pointer(0)),
                screenPos.x,
                screenPos.y,
                screenPos.width,
                screenPos.height,
                SHOW_FLAGS | SWP_SHOWWINDOW
        );

        // Render first frame immediately, then mark as showing
        this.currentlyShowing = true;
        tick();
    }

    public void hideCapture() {
        Jingle.log(Level.DEBUG, "Hiding ThinCapture " + name + "...");
        currentlyShowing = false;

        User32.INSTANCE.SetWindowPos(
                frameHwnd,
                new WinDef.HWND(new Pointer(0)),
                0, 0, 0, 0,
                SWP_HIDEWINDOW | User32.SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOSIZE
        );
    }

    public void restartDrawingTask(int fpsLimit) {
        if (redrawTask != null) {
            redrawTask.cancel(false);
        }
        long delay = 1_000_000_000L / fpsLimit;
        redrawTask = ThinCapture.EXECUTOR.scheduleAtFixedRate(this::tick, delay, delay, TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean isShowing() {
        return currentlyShowing;
    }

    @Override
    public void dispose() {
        if (redrawTask != null) redrawTask.cancel(false);
        super.dispose();
    }
}