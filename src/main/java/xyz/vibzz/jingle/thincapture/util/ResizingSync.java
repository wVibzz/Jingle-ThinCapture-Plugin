package xyz.vibzz.jingle.thincapture.util;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.script.CustomizableManager;

import java.awt.*;

/**
 * Reads Thin BT and Planar Abuse sizes from Jingle's Resizing script
 * customizable storage. Values are stored as "WIDTHxHEIGHT" strings.
 * Returns null if the key is missing or malformed.
 */
public final class ResizingSync {
    private static final String SCRIPT_NAME = "Resizing";
    private static final String THIN_BT_KEY = "thin_bt";
    private static final String PLANAR_ABUSE_KEY = "planar_abuse";

    private ResizingSync() {}

    /**
     * @return a Dimension with width/height from the Resizing script, or null if unavailable
     */
    public static Dimension getThinBTSize() {
        return parseSize(CustomizableManager.get(SCRIPT_NAME, THIN_BT_KEY));
    }

    /**
     * @return a Dimension with width/height from the Resizing script, or null if unavailable
     */
    public static Dimension getPlanarAbuseSize() {
        return parseSize(CustomizableManager.get(SCRIPT_NAME, PLANAR_ABUSE_KEY));
    }

    private static Dimension parseSize(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            String[] parts = value.toLowerCase().split("x");
            if (parts.length != 2) return null;
            int w = Integer.parseInt(parts[0].trim());
            int h = Integer.parseInt(parts[1].trim());
            if (w > 0 && h > 0) return new Dimension(w, h);
        } catch (NumberFormatException e) {
            Jingle.log(Level.DEBUG, "ThinCapture: failed to parse resizing size: " + value);
        }
        return null;
    }
}