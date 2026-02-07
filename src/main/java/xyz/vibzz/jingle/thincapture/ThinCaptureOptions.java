package xyz.vibzz.jingle.thincapture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ThinCaptureOptions {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path OPTIONS_PATH = Jingle.FOLDER.resolve("thincapture.json");

    public int thinBTWidth = 280;
    public int thinBTHeight = 1000;
    public int fpsLimit = 30;

    // Entity Counter capture
    public boolean entityEnabled = false;
    public int entityScreenX = 0;
    public int entityScreenY = 0;
    public int entityScreenW = 300;
    public int entityScreenH = 20;
    public int entityCaptureX = 2;
    public int entityCaptureY = 90;
    public int entityCaptureW = 300;
    public int entityCaptureH = 20;
    public boolean entityTextOnly = false;
    public int entityTextThreshold = 200;
    public boolean entityTransparentBg = true;
    public String entityBgColor = "#000000";

    // Pie Chart capture
    public boolean pieEnabled = false;
    public int pieScreenX = 0;
    public int pieScreenY = 100;
    public int pieScreenW = 200;
    public int pieScreenH = 200;
    public int pieCaptureX = 0;
    public int pieCaptureY = 0;
    public int pieCaptureW = 200;
    public int pieCaptureH = 200;
    public boolean pieTextOnly = false;
    public int pieTextThreshold = 200;
    public boolean pieTransparentBg = true;
    public String pieBgColor = "#000000";

    public static Optional<ThinCaptureOptions> load() {
        if (!Files.exists(OPTIONS_PATH)) return Optional.of(new ThinCaptureOptions());
        try {
            return Optional.of(FileUtil.readJson(OPTIONS_PATH, ThinCaptureOptions.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean trySave() {
        try {
            FileUtil.writeString(OPTIONS_PATH, GSON.toJson(this));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}