package eva.evangelion.view.options;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Properties;

/** Client-side options that survive restarts. Speed / fast actions are NOT part of it. */
public class GameOptions {
    private static final Path FILE = Paths.get("options.properties");
    private static final String GAMES_DIR_KEY = "gamesDirectory";

    public static final double MIN_VIEWPORT = 100;
    public static final double MIN_BOTTOM = 80, MAX_BOTTOM = 300;
    public static final int MAX_ARROW_LIFETIME = 50;

    /** -1 = never set by the user, use the default size. */
    public double viewportWidth = -1;
    public double viewportHeight = -1;
    public double bottomPanelHeight = 150;
    /** Action arrows are removed after this many turns. 0 = never remove. */
    public int arrowLifetimeTurns = 3;

    public static GameOptions load() {
        GameOptions o = new GameOptions();
        Properties p = readFile();
        o.viewportWidth = readDouble(p, "viewportWidth", o.viewportWidth);
        o.viewportHeight = readDouble(p, "viewportHeight", o.viewportHeight);
        o.bottomPanelHeight = readDouble(p, "bottomPanelHeight", o.bottomPanelHeight);
        o.arrowLifetimeTurns = (int) readDouble(p, "arrowLifetimeTurns", o.arrowLifetimeTurns);
        o.sanitize();
        return o;
    }

    /** Overwrites only this object's keys; everything else in the file (e.g. the games folder) is kept. */
    public void save() {
        sanitize();
        Properties p = readFile();
        p.setProperty("viewportWidth", String.valueOf(viewportWidth));
        p.setProperty("viewportHeight", String.valueOf(viewportHeight));
        p.setProperty("bottomPanelHeight", String.valueOf(bottomPanelHeight));
        p.setProperty("arrowLifetimeTurns", String.valueOf(arrowLifetimeTurns));
        writeFile(p);
    }

    /** @return the saved games folder, or null if the default should be used */
    public static String loadGamesDirectory() {
        String v = readFile().getProperty(GAMES_DIR_KEY);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /** @param dir the folder to remember, or null/blank to go back to the default */
    public static void saveGamesDirectory(String dir) {
        Properties p = readFile();
        if (dir == null || dir.isBlank()) p.remove(GAMES_DIR_KEY);
        else p.setProperty(GAMES_DIR_KEY, dir);
        writeFile(p);
    }

    private static Properties readFile() {
        Properties p = new Properties();
        if (!Files.exists(FILE)) return p;
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
        } catch (IOException e) {
            System.err.println("Could not read options: " + e.getMessage());
        }
        return p;
    }

    private static void writeFile(Properties p) {
        try {
            Path tmp = Paths.get(FILE + ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                p.store(out, "Adeptus Evangelion client options");
            }
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Could not save options: " + e.getMessage());
        }
    }

    private void sanitize() {
        if (viewportWidth > 0) viewportWidth = Math.max(MIN_VIEWPORT, viewportWidth);
        if (viewportHeight > 0) viewportHeight = Math.max(MIN_VIEWPORT, viewportHeight);
        bottomPanelHeight = Math.max(MIN_BOTTOM, Math.min(MAX_BOTTOM, bottomPanelHeight));
        arrowLifetimeTurns = Math.max(0, Math.min(MAX_ARROW_LIFETIME, arrowLifetimeTurns));
    }

    private static double readDouble(Properties p, String key, double fallback) {
        try {
            String v = p.getProperty(key);
            return v == null ? fallback : Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}