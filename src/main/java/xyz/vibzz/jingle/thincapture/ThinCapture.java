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

import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThinCapture {
    public static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static CaptureFrame entityFrame = null;
    private static CaptureFrame pieChartFrame = null;
    private static ThinCaptureOptions options = null;
    private static boolean capturesShowing = false;

    public static ThinCaptureOptions getOptions() {
        return options;
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

        entityFrame = new CaptureFrame("Entity Counter");
        pieChartFrame = new CaptureFrame("Pie Chart");

        ThinCapturePluginPanel gui = new ThinCapturePluginPanel();
        JingleGUI.addPluginTab("ThinCapture", gui.mainPanel, gui::onSwitchTo);

        PluginEvents.STOP.register(ThinCapture::stop);

        EXECUTOR.scheduleAtFixedRate(ThinCapture::detectThinBT, 500, 200, TimeUnit.MILLISECONDS);

        Jingle.log(Level.INFO, "ThinCapture Plugin Initialized");
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
            }
        } catch (Exception ignored) {
        }
    }

    private static void showCaptureWindows() {
        capturesShowing = true;
        if (options.entityEnabled) {
            entityFrame.setFilterOptions(
                    options.entityTextOnly,
                    options.entityTextThreshold,
                    options.entityTransparentBg,
                    parseColor(options.entityBgColor)
            );
            entityFrame.showCapture(
                    new Rectangle(options.entityScreenX, options.entityScreenY, options.entityScreenW, options.entityScreenH),
                    new Rectangle(options.entityCaptureX, options.entityCaptureY, options.entityCaptureW, options.entityCaptureH)
            );
        }
        if (options.pieEnabled) {
            pieChartFrame.setFilterOptions(
                    options.pieTextOnly,
                    options.pieTextThreshold,
                    options.pieTransparentBg,
                    parseColor(options.pieBgColor)
            );
            pieChartFrame.showCapture(
                    new Rectangle(options.pieScreenX, options.pieScreenY, options.pieScreenW, options.pieScreenH),
                    new Rectangle(options.pieCaptureX, options.pieCaptureY, options.pieCaptureW, options.pieCaptureH)
            );
        }
    }

    private static java.awt.Color parseColor(String hex) {
        try {
            return java.awt.Color.decode(hex);
        } catch (Exception e) {
            return java.awt.Color.BLACK;
        }
    }

    private static void hideCaptureWindows() {
        capturesShowing = false;
        if (entityFrame.isShowing()) entityFrame.hideCapture();
        if (pieChartFrame.isShowing()) pieChartFrame.hideCapture();
    }

    public static void updateFpsLimit() {
        int fps = options.fpsLimit;
        if (entityFrame != null) entityFrame.restartDrawingTask(fps);
        if (pieChartFrame != null) pieChartFrame.restartDrawingTask(fps);
    }

    private static void stop() {
        EXECUTOR.shutdown();
        if (entityFrame != null) entityFrame.dispose();
        if (pieChartFrame != null) pieChartFrame.dispose();
        if (options != null) if (!options.trySave()) Jingle.log(Level.ERROR, "Failed to save ThinCapture options!");
    }
}