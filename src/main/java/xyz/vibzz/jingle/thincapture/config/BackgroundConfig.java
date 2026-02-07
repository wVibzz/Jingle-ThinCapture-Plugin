package xyz.vibzz.jingle.thincapture.config;

public class BackgroundConfig {
    public String name = "Background";
    public boolean enabled = false;
    public String imagePath = "";
    public int x = 0;
    public int y = 0;
    public int width = 1920;
    public int height = 1080;

    @SuppressWarnings("unused") // Required by Gson for deserialization
    public BackgroundConfig() {}

    public BackgroundConfig(String name) {
        this.name = name;
    }
}