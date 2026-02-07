package xyz.vibzz.jingle.thincapture;

import com.google.common.io.Resources;
import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.JingleAppLaunch;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.plugin.PluginEvents;
import xyz.duncanruns.jingle.plugin.PluginManager;
import xyz.duncanruns.jingle.win32.User32;
import xyz.vibzz.jingle.thincapture.config.BackgroundConfig;
import xyz.vibzz.jingle.thincapture.config.CaptureConfig;
import xyz.vibzz.jingle.thincapture.frame.BackgroundFrame;
import xyz.vibzz.jingle.thincapture.frame.CaptureFrame;
import xyz.vibzz.jingle.thincapture.ui.ThinCapturePluginPanel;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ThinCapture {
    public static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static ThinCaptureOptions options = null;
    private static final List<CaptureFrame> frames = new ArrayList<>();
    private static final List<BackgroundFrame> bgFrames = new ArrayList<>();
    private static boolean capturesShowing = false;

    public static ThinCaptureOptions getOptions() {
        return options;
    }

    public static List<BackgroundFrame> getBgFrames() {
        return bgFrames;
    }

    public static void main(String[] args) throws IOException {
        JingleAppLaunch.launchWithDevPlugin(args, PluginManager.JinglePluginData.fromString(
                Resources.toString(Resources.getResource(ThinCapture.class, "/jingle.plugin.json"), Charset.defaultCharset())
        ), ThinCapture::initialize);
    }

    public static void initialize() {
        Optional<ThinCaptureOptions> loaded = ThinCaptureOptions.load();
        if (loaded.isPresent()) {
            options = loaded.get();
        } else {
            options = new ThinCaptureOptions();
            Jingle.log(Level.ERROR, "Failed to load ThinCapture options, using defaults.");
        }

        for (CaptureConfig config : options.captures) {
            frames.add(new CaptureFrame(config.name));
        }

        for (BackgroundConfig bg : options.backgrounds) {
            BackgroundFrame frame = new BackgroundFrame();
            if (bg.imagePath != null && !bg.imagePath.trim().isEmpty()) {
                frame.loadImage(bg.imagePath);
            }
            bgFrames.add(frame);
        }

        ThinCapturePluginPanel pluginPanel = new ThinCapturePluginPanel();
        JingleGUI.addPluginTab("Thin BT Captures", pluginPanel.mainPanel, pluginPanel::onSwitchTo);

        PluginEvents.START_TICK.register(ThinCapture::detectThinBT);
        PluginEvents.STOP.register(ThinCapture::stop);

        Jingle.log(Level.INFO, "ThinCapture Plugin Initialized (" + options.captures.size() + " captures, " + options.backgrounds.size() + " backgrounds)");
    }

    public static void addCapture(String name) {
        options.captures.add(new CaptureConfig(name));
        frames.add(new CaptureFrame(name));
    }

    public static void removeCapture(int index) {
        if (index < 0 || index >= options.captures.size()) return;
        options.captures.remove(index);
        CaptureFrame frame = frames.remove(index);
        if (frame.isShowing()) frame.hideCapture();
        frame.dispose();
    }

    public static void renameCapture(int index, String newName) {
        if (index < 0 || index >= options.captures.size()) return;
        options.captures.get(index).name = newName;
        frames.get(index).setTitle("ThinCapture " + newName);
    }

    public static void addBackground(String name) {
        BackgroundConfig config = new BackgroundConfig(name);
        config.enabled = true;
        options.backgrounds.add(config);
        bgFrames.add(new BackgroundFrame());
    }

    public static void removeBackground(int index) {
        if (index < 0 || index >= options.backgrounds.size()) return;
        options.backgrounds.remove(index);
        BackgroundFrame frame = bgFrames.remove(index);
        if (frame.isShowing()) frame.hideBackground();
        frame.dispose();
    }

    public static void renameBackground(int index, String newName) {
        if (index < 0 || index >= options.backgrounds.size()) return;
        options.backgrounds.get(index).name = newName;
    }

    public static BackgroundFrame getBgFrame(int index) {
        if (index < 0 || index >= bgFrames.size()) return null;
        return bgFrames.get(index);
    }

    private static void detectThinBT() {
        try {
            if (!Jingle.getMainInstance().isPresent()) {
                if (capturesShowing) hideCaptureWindows();
                return;
            }

            WinDef.HWND hwnd = Jingle.getMainInstance().get().hwnd;
            WinDef.RECT rect = new WinDef.RECT();
            User32.INSTANCE.GetClientRect(hwnd, rect);
            int w = rect.right - rect.left;
            int h = rect.bottom - rect.top;

            boolean isThinBT = (w == options.thinBTWidth && h == options.thinBTHeight);

            if (isThinBT && !capturesShowing) {
                showCaptureWindows();
            } else if (!isThinBT && capturesShowing) {
                hideCaptureWindows();
            } else if (isThinBT) {
                for (BackgroundFrame bf : bgFrames) {
                    if (bf.isShowing()) bf.sendBehindMC();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void showCaptureWindows() {
        capturesShowing = true;

        // Phase 1: Position everything
        for (int i = 0; i < options.backgrounds.size() && i < bgFrames.size(); i++) {
            BackgroundConfig bg = options.backgrounds.get(i);
            BackgroundFrame bf = bgFrames.get(i);
            if (bg.enabled && bg.imagePath != null && !bg.imagePath.trim().isEmpty()) {
                bf.positionBackground(bg.x, bg.y, bg.width, bg.height);
            }
        }

        List<CaptureFrame> toShow = new ArrayList<>();
        for (int i = 0; i < options.captures.size() && i < frames.size(); i++) {
            CaptureConfig c = options.captures.get(i);
            CaptureFrame f = frames.get(i);
            if (c.enabled) {
                f.setFilterOptions(c.textOnly, c.textThreshold, c.transparentBg, parseColor(c.bgColor), c.bgImagePath);
                f.positionCapture(
                        new Rectangle(c.screenX, c.screenY, c.screenW, c.screenH),
                        new Rectangle(c.captureX, c.captureY, c.captureW, c.captureH)
                );
                toShow.add(f);
            }
        }

        // Phase 2: Show everything
        for (int i = 0; i < options.backgrounds.size() && i < bgFrames.size(); i++) {
            BackgroundConfig bg = options.backgrounds.get(i);
            BackgroundFrame bf = bgFrames.get(i);
            if (bg.enabled && bg.imagePath != null && !bg.imagePath.trim().isEmpty()) {
                bf.showBackground();
            }
        }
        for (CaptureFrame f : toShow) {
            f.showCapture();
        }
    }

    private static void hideCaptureWindows() {
        capturesShowing = false;
        for (BackgroundFrame bf : bgFrames) {
            if (bf.isShowing()) bf.hideBackground();
        }
        for (CaptureFrame f : frames) {
            if (f.isShowing()) f.hideCapture();
        }
    }

    private static Color parseColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }

    public static void updateFpsLimit() {
        int fps = options.fpsLimit;
        for (CaptureFrame f : frames) {
            f.restartDrawingTask(fps);
        }
    }

    private static void stop() {
        EXECUTOR.shutdown();
        for (CaptureFrame f : frames) {
            f.dispose();
        }
        for (BackgroundFrame bf : bgFrames) {
            bf.dispose();
        }
        if (options != null) if (!options.trySave()) Jingle.log(Level.ERROR, "Failed to save ThinCapture options!");
    }
}