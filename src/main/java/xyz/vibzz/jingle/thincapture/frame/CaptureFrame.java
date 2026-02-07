package xyz.vibzz.jingle.thincapture.frame;

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
import xyz.vibzz.jingle.thincapture.ThinCapture;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CaptureFrame extends JFrame {
    private static final int SHOW_FLAGS = User32.SWP_NOACTIVATE | User32.SWP_NOSENDCHANGING;
    private static final WinDef.DWORD SRCCOPY = new WinDef.DWORD(0x00CC0020);

    private static final int GWL_EXSTYLE = -20;
    private static final int WS_EX_LAYERED = 0x00080000;
    private static final int ULW_ALPHA = 0x00000002;
    private static final int AC_SRC_OVER = 0x00;
    private static final int AC_SRC_ALPHA = 0x01;

    private static final int SW_HIDE = 0;
    private static final int SW_SHOWNOACTIVATE = 4;

    private final String name;
    private final WinDef.HWND frameHwnd;
    private boolean currentlyShowing = false;
    private ScheduledFuture<?> redrawTask;

    private Rectangle windowBounds = new Rectangle();
    private Rectangle captureRegion = new Rectangle();

    private boolean textOnly = false;
    private int textThreshold = 200;
    private boolean transparentBg = true;
    private Color bgColor = Color.BLACK;
    private BufferedImage bgImage = null;
    private BufferedImage scaledBgImage = null;
    private int lastBgScaleW = -1;
    private int lastBgScaleH = -1;

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
        com.sun.jna.platform.win32.User32.INSTANCE.ShowWindow(frameHwnd, SW_HIDE);

        restartDrawingTask(ThinCapture.getOptions().fpsLimit);
    }

    public void setFilterOptions(boolean textOnly, int textThreshold, boolean transparentBg, Color bgColor, String bgImagePath) {
        this.textOnly = textOnly;
        this.textThreshold = textThreshold;
        this.transparentBg = transparentBg;
        this.bgColor = bgColor;
        loadBgImage(bgImagePath);
    }

    private void loadBgImage(String path) {
        if (path == null || path.trim().isEmpty()) {
            bgImage = null;
            scaledBgImage = null;
            lastBgScaleW = -1;
            lastBgScaleH = -1;
            return;
        }
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                bgImage = ImageIO.read(file);
                scaledBgImage = null;
                lastBgScaleW = -1;
                lastBgScaleH = -1;
            } else {
                bgImage = null;
                scaledBgImage = null;
                Jingle.log(Level.WARN, "ThinCapture " + name + ": background image not found: " + path);
            }
        } catch (Exception e) {
            bgImage = null;
            scaledBgImage = null;
            Jingle.log(Level.WARN, "ThinCapture " + name + ": failed to load background image: " + e.getMessage());
        }
    }

    private BufferedImage getScaledBgImage(int w, int h) {
        if (bgImage == null) return null;
        if (scaledBgImage != null && lastBgScaleW == w && lastBgScaleH == h) {
            return scaledBgImage;
        }
        scaledBgImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaledBgImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(bgImage, 0, 0, w, h, null);
        g2.dispose();
        lastBgScaleW = w;
        lastBgScaleH = h;
        return scaledBgImage;
    }

    private void enableLayeredWindow() {
        int exStyle = com.sun.jna.platform.win32.User32.INSTANCE.GetWindowLong(frameHwnd, GWL_EXSTYLE);
        com.sun.jna.platform.win32.User32.INSTANCE.SetWindowLong(frameHwnd, GWL_EXSTYLE, exStyle | WS_EX_LAYERED);
    }

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

    private void tickFiltered(WinDef.HWND hwnd) {
        try {
            WinDef.HDC srcDC = User32.INSTANCE.GetDC(hwnd);

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

            int outW = windowBounds.width;
            int outH = windowBounds.height;
            BufferedImage output = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);

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

            boolean useBgImage = bgImage != null && !transparentBg;
            BufferedImage bgScaled = useBgImage ? getScaledBgImage(outW, outH) : null;

            int solidBgArgb;
            if (transparentBg) {
                solidBgArgb = 0x00000000;
            } else {
                solidBgArgb = (0xFF << 24) | (bgColor.getRed() << 16) | (bgColor.getGreen() << 8) | bgColor.getBlue();
            }

            for (int y = 0; y < outH; y++) {
                for (int x = 0; x < outW; x++) {
                    int rgb = scaled.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    if (r >= textThreshold && g >= textThreshold && b >= textThreshold) {
                        output.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
                    } else {
                        if (useBgImage && bgScaled != null) {
                            output.setRGB(x, y, bgScaled.getRGB(x, y));
                        } else {
                            output.setRGB(x, y, solidBgArgb);
                        }
                    }
                }
            }

            updateLayered(output, outW, outH);

        } catch (Exception e) {
            Jingle.log(Level.DEBUG, "ThinCapture " + name + " filter error: " + e.getMessage());
        }
    }

    private void updateLayered(BufferedImage image, int width, int height) {
        WinDef.HDC screenDC = com.sun.jna.platform.win32.User32.INSTANCE.GetDC(null);
        WinDef.HDC memDC = GDI32Extra.INSTANCE.CreateCompatibleDC(screenDC);

        WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
        bmi.bmiHeader.biSize = bmi.bmiHeader.size();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = -height;
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        com.sun.jna.ptr.PointerByReference ppvBits = new com.sun.jna.ptr.PointerByReference();
        WinDef.HBITMAP hBitmap = com.sun.jna.platform.win32.GDI32.INSTANCE.CreateDIBSection(
                memDC, bmi, WinGDI.DIB_RGB_COLORS, ppvBits, null, 0
        );

        com.sun.jna.platform.win32.WinNT.HANDLE oldBitmap = com.sun.jna.platform.win32.GDI32.INSTANCE.SelectObject(memDC, hBitmap);

        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        byte[] bgraData = new byte[width * height * 4];
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;

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

        com.sun.jna.platform.win32.GDI32.INSTANCE.SelectObject(memDC, oldBitmap);
        com.sun.jna.platform.win32.GDI32.INSTANCE.DeleteObject(hBitmap);
        com.sun.jna.platform.win32.GDI32.INSTANCE.DeleteDC(memDC);
        com.sun.jna.platform.win32.User32.INSTANCE.ReleaseDC(null, screenDC);
    }

    public void positionCapture(Rectangle screenPos, Rectangle capture) {
        this.windowBounds = screenPos;
        this.captureRegion = capture;

        if (textOnly) {
            enableLayeredWindow();
        } else {
            disableLayeredWindow();
        }

        User32.INSTANCE.SetWindowPos(
                frameHwnd,
                new WinDef.HWND(new Pointer(0)),
                screenPos.x,
                screenPos.y,
                screenPos.width,
                screenPos.height,
                SHOW_FLAGS
        );
    }

    public void showCapture() {
        if (!Jingle.getMainInstance().isPresent()) return;
        Jingle.log(Level.DEBUG, "Showing ThinCapture " + name + "...");
        com.sun.jna.platform.win32.User32.INSTANCE.ShowWindow(frameHwnd, SW_SHOWNOACTIVATE);
        this.currentlyShowing = true;
        tick();
    }

    public void hideCapture() {
        Jingle.log(Level.DEBUG, "Hiding ThinCapture " + name + "...");
        currentlyShowing = false;
        com.sun.jna.platform.win32.User32.INSTANCE.ShowWindow(frameHwnd, SW_HIDE);
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