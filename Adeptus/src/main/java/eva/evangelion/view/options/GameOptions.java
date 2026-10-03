package eva.evangelion.view.options;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Properties;

/** Client-side options that survive restarts. Speed / fast actions are NOT part of it. */
public class GameOptions {
    private static final Path FILE = Paths.get("options.properties");

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
        if (!Files.exists(FILE)) return o;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
        } catch (IOException e) {
            System.err.println("Could not read options: " + e.getMessage());
            return o;
        }
        o.viewportWidth = readDouble(p, "viewportWidth", o.viewportWidth);
        o.viewportHeight = readDouble(p, "viewportHeight", o.viewportHeight);
        o.bottomPanelHeight = readDouble(p, "bottomPanelHeight", o.bottomPanelHeight);
        o.arrowLifetimeTurns = (int) readDouble(p, "arrowLifetimeTurns", o.arrowLifetimeTurns);
        o.sanitize();
        return o;
    }

    public void save() {
        sanitize();
        Properties p = new Properties();
        p.setProperty("viewportWidth", String.valueOf(viewportWidth));
        p.setProperty("viewportHeight", String.valueOf(viewportHeight));
        p.setProperty("bottomPanelHeight", String.valueOf(bottomPanelHeight));
        p.setProperty("arrowLifetimeTurns", String.valueOf(arrowLifetimeTurns));
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