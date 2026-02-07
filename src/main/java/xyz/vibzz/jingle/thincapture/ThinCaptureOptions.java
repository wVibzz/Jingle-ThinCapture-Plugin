package xyz.vibzz.jingle.thincapture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.FileUtil;
import xyz.vibzz.jingle.thincapture.config.BackgroundConfig;
import xyz.vibzz.jingle.thincapture.config.CaptureConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ThinCaptureOptions {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path OPTIONS_PATH = Jingle.FOLDER.resolve("thincapture.json");

    public int thinBTWidth = 280;
    public int thinBTHeight = 1000;
    public int fpsLimit = 30;
    public List<CaptureConfig> captures = new ArrayList<>();
    public List<BackgroundConfig> backgrounds = new ArrayList<>();

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