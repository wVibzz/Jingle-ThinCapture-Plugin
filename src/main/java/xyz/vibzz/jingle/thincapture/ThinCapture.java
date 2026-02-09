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
import xyz.vibzz.jingle.thincapture.ui.BackgroundsPluginPanel;
import xyz.vibzz.jingle.thincapture.ui.PlanarAbusePluginPanel;
import xyz.vibzz.jingle.thincapture.ui.ThinCapturePluginPanel;
import xyz.vibzz.jingle.thincapture.util.ResizingSync;

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

    // Thin BT frames
    private static final List<CaptureFrame> frames = new ArrayList<>();
    private static final List<BackgroundFrame> bgFrames = new ArrayList<>();
    private static boolean thinBTShowing = false;

    // Planar Abuse frames
    private static final List<CaptureFrame> planarFrames = new ArrayList<>();
    private static final List<BackgroundFrame> planarBgFrames = new ArrayList<>();
    private static boolean planarShowing = false;

    // EyeSee frames
    private static final List<BackgroundFrame> eyeSeeBgFrames = new ArrayList<>();
    private static boolean eyeSeeShowing = false;

    // Background management
    private static boolean backgroundsShowing = false;

    private enum ActiveBgType { NONE, THIN_BT, PLANAR, EYESEE }
    private static ActiveBgType activeBgType = ActiveBgType.NONE;

    public static ThinCaptureOptions getOptions() {
        return options;
    }

    /**
     * Returns the effective Thin BT width, synced from the Resizing script.
     * Falls back to the saved option value if the script key is not found.
     */
    public static int getEffectiveThinBTWidth() {
        Dimension d = ResizingSync.getThinBTSize();
        return d != null ? d.width : options.thinBTWidth;
    }

    public static int getEffectiveThinBTHeight() {
        Dimension d = ResizingSync.getThinBTSize();
        return d != null ? d.height : options.thinBTHeight;
    }

    public static int getEffectivePlanarWidth() {
        Dimension d = ResizingSync.getPlanarAbuseSize();
        return d != null ? d.width : options.planarAbuseWidth;
    }

    public static int getEffectivePlanarHeight() {
        Dimension d = ResizingSync.getPlanarAbuseSize();
        return d != null ? d.height : options.planarAbuseHeight;
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

        // Log synced sizes
        Dimension thinBT = ResizingSync.getThinBTSize();
        Dimension planar = ResizingSync.getPlanarAbuseSize();
        Jingle.log(Level.INFO, "ThinCapture sizes: Thin BT=" +
                (thinBT != null ? thinBT.width + "x" + thinBT.height : "not found in Resizing script, fallback " + options.thinBTWidth + "x" + options.thinBTHeight) +
                ", Planar=" +
                (planar != null ? planar.width + "x" + planar.height : "not found in Resizing script, fallback " + options.planarAbuseWidth + "x" + options.planarAbuseHeight));

        // Initialize Thin BT frames
        for (CaptureConfig config : options.captures) {
            frames.add(new CaptureFrame(config.name));
        }
        for (BackgroundConfig bg : options.backgrounds) {
            bgFrames.add(createBgFrame(bg));
        }

        // Initialize Planar Abuse frames
        for (CaptureConfig config : options.planarAbuseCaptures) {
            planarFrames.add(new CaptureFrame(config.name));
        }
        for (BackgroundConfig bg : options.planarAbuseBackgrounds) {
            planarBgFrames.add(createBgFrame(bg));
        }

        // Initialize EyeSee frames
        for (BackgroundConfig bg : options.eyeSeeBackgrounds) {
            eyeSeeBgFrames.add(createBgFrame(bg));
        }

        // Add plugin tabs
        ThinCapturePluginPanel thinPanel = new ThinCapturePluginPanel();
        JingleGUI.addPluginTab("Thin Captures", thinPanel.mainPanel, thinPanel::onSwitchTo);

        PlanarAbusePluginPanel planarPanel = new PlanarAbusePluginPanel();
        JingleGUI.addPluginTab("Wide Captures", planarPanel.mainPanel, planarPanel::onSwitchTo);

        BackgroundsPluginPanel bgPanel = new BackgroundsPluginPanel();
        JingleGUI.addPluginTab("Backgrounds", bgPanel.mainPanel, bgPanel::onSwitchTo);

        // Register events
        PluginEvents.START_TICK.register(ThinCapture::detectResize);
        PluginEvents.SHOW_PROJECTOR.register(ThinCapture::onShowProjector);
        PluginEvents.DUMP_PROJECTOR.register(ThinCapture::onDumpProjector);
        PluginEvents.STOP.register(ThinCapture::stop);

        Jingle.log(Level.INFO, "ThinCapture Plugin Initialized (" +
                options.captures.size() + " thin captures, " +
                options.planarAbuseCaptures.size() + " planar captures, " +
                options.eyeSeeBackgrounds.size() + " eyesee backgrounds)");
    }

    private static BackgroundFrame createBgFrame(BackgroundConfig bg) {
        BackgroundFrame frame = new BackgroundFrame();
        frame.setUseImage(bg.useImage);
        if (bg.useImage) {
            if (bg.imagePath != null && !bg.imagePath.trim().isEmpty()) {
                frame.loadImage(bg.imagePath);
            }
        } else {
            frame.setBgColor(parseColor(bg.bgColor));
        }
        return frame;
    }

    // ===== Thin BT Methods =====

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

    // ===== Planar Abuse Methods =====

    public static void addPlanarCapture(String name) {
        options.planarAbuseCaptures.add(new CaptureConfig(name));
        planarFrames.add(new CaptureFrame(name));
    }

    public static void removePlanarCapture(int index) {
        if (index < 0 || index >= options.planarAbuseCaptures.size()) return;
        options.planarAbuseCaptures.remove(index);
        CaptureFrame frame = planarFrames.remove(index);
        if (frame.isShowing()) frame.hideCapture();
        frame.dispose();
    }

    public static void renamePlanarCapture(int index, String newName) {
        if (index < 0 || index >= options.planarAbuseCaptures.size()) return;
        options.planarAbuseCaptures.get(index).name = newName;
        planarFrames.get(index).setTitle("ThinCapture " + newName);
    }

    public static void addPlanarBackground(String name) {
        BackgroundConfig config = new BackgroundConfig(name);
        config.enabled = true;
        options.planarAbuseBackgrounds.add(config);
        planarBgFrames.add(new BackgroundFrame());
    }

    public static void removePlanarBackground(int index) {
        if (index < 0 || index >= options.planarAbuseBackgrounds.size()) return;
        options.planarAbuseBackgrounds.remove(index);
        BackgroundFrame frame = planarBgFrames.remove(index);
        if (frame.isShowing()) frame.hideBackground();
        frame.dispose();
    }

    public static void renamePlanarBackground(int index, String newName) {
        if (index < 0 || index >= options.planarAbuseBackgrounds.size()) return;
        options.planarAbuseBackgrounds.get(index).name = newName;
    }

    public static BackgroundFrame getPlanarBgFrame(int index) {
        if (index < 0 || index >= planarBgFrames.size()) return null;
        return planarBgFrames.get(index);
    }

    // ===== EyeSee Methods =====

    public static void addEyeSeeBackground(String name) {
        BackgroundConfig config = new BackgroundConfig(name);
        config.enabled = true;
        options.eyeSeeBackgrounds.add(config);
        eyeSeeBgFrames.add(new BackgroundFrame());
    }

    public static void removeEyeSeeBackground(int index) {
        if (index < 0 || index >= options.eyeSeeBackgrounds.size()) return;
        options.eyeSeeBackgrounds.remove(index);
        BackgroundFrame frame = eyeSeeBgFrames.remove(index);
        if (frame.isShowing()) frame.hideBackground();
        frame.dispose();
    }

    public static void renameEyeSeeBackground(int index, String newName) {
        if (index < 0 || index >= options.eyeSeeBackgrounds.size()) return;
        options.eyeSeeBackgrounds.get(index).name = newName;
    }

    public static BackgroundFrame getEyeSeeBgFrame(int index) {
        if (index < 0 || index >= eyeSeeBgFrames.size()) return null;
        return eyeSeeBgFrames.get(index);
    }

    // ===== Background Management =====

    private static void showAllBackgrounds() {
        if (backgroundsShowing) return;
        backgroundsShowing = true;
        Jingle.log(Level.DEBUG, "Showing all ThinCapture backgrounds...");

        positionAndShowBgList(options.backgrounds, bgFrames);
        positionAndShowBgList(options.planarAbuseBackgrounds, planarBgFrames);
        if (options.eyeSeeEnabled) {
            positionAndShowBgList(options.eyeSeeBackgrounds, eyeSeeBgFrames);
        }
    }

    private static void positionAndShowBgList(List<BackgroundConfig> configs, List<BackgroundFrame> bgList) {
        for (int i = 0; i < configs.size() && i < bgList.size(); i++) {
            BackgroundConfig bg = configs.get(i);
            BackgroundFrame bf = bgList.get(i);
            if (bg.enabled) {
                bf.setUseImage(bg.useImage);
                if (bg.useImage) {
                    if (bg.imagePath != null && !bg.imagePath.trim().isEmpty()) {
                        bf.loadImage(bg.imagePath);
                    }
                } else {
                    bf.setBgColor(parseColor(bg.bgColor));
                }

                bf.positionBackground(bg.x, bg.y, bg.width, bg.height);
                if (!bf.isShowing()) {
                    bf.showBackground();
                }
            }
        }
    }

    private static void hideAllBackgrounds() {
        if (!backgroundsShowing) return;
        backgroundsShowing = false;
        activeBgType = ActiveBgType.NONE;
        Jingle.log(Level.DEBUG, "Hiding all ThinCapture backgrounds...");

        hideBgList(bgFrames);
        hideBgList(planarBgFrames);
        hideBgList(eyeSeeBgFrames);
    }

    private static void hideBgList(List<BackgroundFrame> bgList) {
        for (BackgroundFrame bf : bgList) {
            if (bf.isShowing()) bf.hideBackground();
        }
    }

    private static void reorderBackgrounds() {
        if (!Jingle.getMainInstance().isPresent()) return;
        WinDef.HWND current = Jingle.getMainInstance().get().hwnd;

        if (activeBgType == ActiveBgType.THIN_BT) {
            current = chainBgListBehind(current, bgFrames);
            current = chainBgListBehind(current, eyeSeeBgFrames);
            current = chainBgListBehind(current, planarBgFrames);
        } else if (activeBgType == ActiveBgType.PLANAR) {
            current = chainBgListBehind(current, planarBgFrames);
            current = chainBgListBehind(current, eyeSeeBgFrames);
            current = chainBgListBehind(current, bgFrames);
        } else {
            current = chainBgListBehind(current, eyeSeeBgFrames);
            current = chainBgListBehind(current, bgFrames);
            current = chainBgListBehind(current, planarBgFrames);
        }
    }

    private static WinDef.HWND chainBgListBehind(WinDef.HWND insertAfter, List<BackgroundFrame> bgList) {
        WinDef.HWND current = insertAfter;
        for (BackgroundFrame bf : bgList) {
            if (bf.isShowing()) {
                bf.sendBehind(current);
                current = bf.getHwnd();
            }
        }
        return current;
    }

    private static void setActiveBgType(ActiveBgType type) {
        if (activeBgType == type) return;
        activeBgType = type;
        reorderBackgrounds();
    }

    // ===== EyeSee Projector Events =====

    private static void onShowProjector() {
        if (!options.eyeSeeEnabled) return;
        eyeSeeShowing = true;
        if (options.preloadBackgrounds) {
            setActiveBgType(ActiveBgType.EYESEE);
        } else {
            positionAndShowBgList(options.eyeSeeBackgrounds, eyeSeeBgFrames);
        }
    }

    private static void onDumpProjector() {
        if (!options.eyeSeeEnabled) return;
        eyeSeeShowing = false;
        if (options.preloadBackgrounds) {
            if (thinBTShowing) setActiveBgType(ActiveBgType.THIN_BT);
            else if (planarShowing) setActiveBgType(ActiveBgType.PLANAR);
            else setActiveBgType(ActiveBgType.NONE);
        } else {
            hideBgList(eyeSeeBgFrames);
        }
    }

    // ===== Resize Detection =====

    private static void detectResize() {
        try {
            if (!Jingle.getMainInstance().isPresent()) {
                if (thinBTShowing) hideThinBTCaptures();
                if (planarShowing) hidePlanarCaptures();
                if (backgroundsShowing) hideAllBackgrounds();
                return;
            }

            WinDef.HWND hwnd = Jingle.getMainInstance().get().hwnd;

            WinDef.HWND foreground = com.sun.jna.platform.win32.User32.INSTANCE.GetForegroundWindow();
            boolean mcFocused = foreground != null && foreground.equals(hwnd);

            if (options.preloadBackgrounds) {
                if (mcFocused) {
                    if (!backgroundsShowing) {
                        showAllBackgrounds();
                    }
                    reorderBackgrounds();
                } else {
                    if (backgroundsShowing) {
                        hideAllBackgrounds();
                    }
                }
            }

            WinDef.RECT rect = new WinDef.RECT();
            User32.INSTANCE.GetClientRect(hwnd, rect);
            int w = rect.right - rect.left;
            int h = rect.bottom - rect.top;

            // Sizes are always synced from the Resizing script (with fallback)
            int thinW = getEffectiveThinBTWidth();
            int thinH = getEffectiveThinBTHeight();
            int planarW = getEffectivePlanarWidth();
            int planarH = getEffectivePlanarHeight();

            boolean isThinBT = (w == thinW && h == thinH);
            boolean isPlanar = (w == planarW && h == planarH);

            if (isThinBT && !thinBTShowing) {
                showThinBTCaptures();
                if (!eyeSeeShowing) setActiveBgType(ActiveBgType.THIN_BT);
            } else if (!isThinBT && thinBTShowing) {
                hideThinBTCaptures();
                if (activeBgType == ActiveBgType.THIN_BT) setActiveBgType(ActiveBgType.NONE);
            }

            if (isPlanar && !planarShowing) {
                showPlanarCaptures();
                if (!eyeSeeShowing) setActiveBgType(ActiveBgType.PLANAR);
            } else if (!isPlanar && planarShowing) {
                hidePlanarCaptures();
                if (activeBgType == ActiveBgType.PLANAR) setActiveBgType(ActiveBgType.NONE);
            }
        } catch (Exception ignored) {}
    }

    // ===== Thin BT Show/Hide =====

    private static void showThinBTCaptures() {
        thinBTShowing = true;

        if (!options.preloadBackgrounds) {
            positionAndShowBgList(options.backgrounds, bgFrames);
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

        for (CaptureFrame f : toShow) {
            f.showCapture();
        }
    }

    private static void hideThinBTCaptures() {
        thinBTShowing = false;
        if (!options.preloadBackgrounds) {
            hideBgList(bgFrames);
        }
        for (CaptureFrame f : frames) {
            if (f.isShowing()) f.hideCapture();
        }
    }

    // ===== Planar Abuse Show/Hide =====

    private static void showPlanarCaptures() {
        planarShowing = true;

        if (!options.preloadBackgrounds) {
            positionAndShowBgList(options.planarAbuseBackgrounds, planarBgFrames);
        }

        List<CaptureFrame> toShow = new ArrayList<>();
        for (int i = 0; i < options.planarAbuseCaptures.size() && i < planarFrames.size(); i++) {
            CaptureConfig c = options.planarAbuseCaptures.get(i);
            CaptureFrame f = planarFrames.get(i);
            if (c.enabled) {
                f.setFilterOptions(c.textOnly, c.textThreshold, c.transparentBg, parseColor(c.bgColor), c.bgImagePath);
                f.positionCapture(
                        new Rectangle(c.screenX, c.screenY, c.screenW, c.screenH),
                        new Rectangle(c.captureX, c.captureY, c.captureW, c.captureH)
                );
                toShow.add(f);
            }
        }

        for (CaptureFrame f : toShow) {
            f.showCapture();
        }
    }

    private static void hidePlanarCaptures() {
        planarShowing = false;
        if (!options.preloadBackgrounds) {
            hideBgList(planarBgFrames);
        }
        for (CaptureFrame f : planarFrames) {
            if (f.isShowing()) f.hideCapture();
        }
    }

    // ===== Utilities =====

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
        int planarFps = options.planarAbuseFpsLimit;
        for (CaptureFrame f : planarFrames) {
            f.restartDrawingTask(planarFps);
        }
    }

    private static void stop() {
        EXECUTOR.shutdown();
        for (CaptureFrame f : frames) f.dispose();
        for (BackgroundFrame bf : bgFrames) bf.dispose();
        for (CaptureFrame f : planarFrames) f.dispose();
        for (BackgroundFrame bf : planarBgFrames) bf.dispose();
        for (BackgroundFrame bf : eyeSeeBgFrames) bf.dispose();
        if (options != null) if (!options.trySave()) Jingle.log(Level.ERROR, "Failed to save ThinCapture options!");
    }
}