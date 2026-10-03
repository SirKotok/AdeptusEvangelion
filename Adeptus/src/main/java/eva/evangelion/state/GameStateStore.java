package eva.evangelion.state;

import eva.evangelion.view.options.GameOptions;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.*;

public final class GameStateStore {
    /** Root of the folders that never move: DMgame, saved, tmp. */
    private static final Path ROOT_DIR = Paths.get("Active", "Games");
    private static final Path DEFAULT_ACTIVE_DIR = ROOT_DIR.resolve("activegame");
    /** The folder holding the shared active game. The only thing the main menu can change. */
    private static volatile Path activeDir = initialActiveDir();

    public static final String FILE_NAME = "gamestate.ser";
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,50}");

    private GameStateStore() {}

    // ---- folder layout ----

    // "games dir" in these names means the ACTIVE GAME folder
    public static Path defaultGamesDir() { return DEFAULT_ACTIVE_DIR; }
    public static Path gamesDir()  { return activeDir; }
    public static Path activeDir() { return activeDir; }
    public static Path dmDir()     { return ROOT_DIR.resolve("DMgame"); }
    public static Path savedDir()  { return ROOT_DIR.resolve("saved"); }
    private static Path tmpDir()   { return ROOT_DIR.resolve("tmp"); }   // outside the watched folders

    public static boolean isDefaultGamesDir() { return activeDir.equals(DEFAULT_ACTIVE_DIR); }

    private static Path initialActiveDir() {
        String saved = GameOptions.loadGamesDirectory();
        if (saved == null) return DEFAULT_ACTIVE_DIR;
        try {
            return Paths.get(saved);
        } catch (InvalidPathException e) {
            System.err.println("Ignoring invalid games folder in options: " + saved);
            return DEFAULT_ACTIVE_DIR;
        }
    }

    /**
     * Makes this folder the active game folder (gamestate.ser lives directly in it)
     * and remembers it in options.properties.
     * Games already open keep the folder they were started with. Nothing is moved or copied.
     * @throws IOException if the folder can't be created or written to; the old folder stays active then
     */
    public static void setGamesDir(Path dir) throws IOException {
        Path target = dir.equals(DEFAULT_ACTIVE_DIR) ? DEFAULT_ACTIVE_DIR : dir.toAbsolutePath().normalize();
        Files.createDirectories(target);
        if (!Files.isWritable(target)) throw new IOException("Folder is not writable: " + target);
        activeDir = target;
        GameOptions.saveGamesDirectory(target.equals(DEFAULT_ACTIVE_DIR) ? null : target.toString());
    }

    public static void resetGamesDir() throws IOException {
        setGamesDir(DEFAULT_ACTIVE_DIR);
    }

    public static Path file(Path dir) { return dir.resolve(FILE_NAME); }

    public static void ensureDirs() throws IOException {
        // fixed folders first, so an unreachable custom folder can't stop them being created
        Files.createDirectories(dmDir());
        Files.createDirectories(savedDir());
        Files.createDirectories(tmpDir());
        Files.createDirectories(activeDir());
    }

    /** One-time: copy the old single-file location into activegame. Only for the default folder. */
    public static void migrateLegacy(Path legacyFile) throws IOException {
        if (!isDefaultGamesDir()) return;
        if (Files.exists(legacyFile) && !Files.exists(file(activeDir()))) {
            Files.copy(legacyFile, file(activeDir()));
        }
    }

    private static Path savedFile(String name) {
        if (name == null || !VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid name (use 1-50 letters, digits, '_' or '-').");
        }
        return savedDir().resolve(name + ".ser");
    }

    /** Writes to a temp file and moves it into place, so watchers never see a half-written file. */
    private static void writeTo(GameState state, Path target) throws IOException {
        Files.createDirectories(tmpDir());
        Path tmp = Files.createTempFile(tmpDir(), "state", ".tmp");
        try {
            state.saveToFile(tmp);
            try {
                Files.move(tmp, target, REPLACE_EXISTING, ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public static void write(GameState state, Path dir) throws IOException {
        Files.createDirectories(dir);
        writeTo(state, file(dir));
    }

    // ---- saved folder ----

    public static void saveAs(GameState state, String name, boolean overwrite) throws IOException {
        Path target = savedFile(name);
        if (!overwrite && Files.exists(target)) throw new FileAlreadyExistsException(target.toString());
        writeTo(state, target);
    }

    /** @return the saved state, or null if there is no save with that name */
    public static GameState loadSave(String name) throws IOException {
        try {
            return GameState.loadFromFile(savedFile(name));
        } catch (ClassNotFoundException e) {
            throw new IOException("Save uses classes that no longer exist: " + e.getMessage(), e);
        }
    }

    public static List<String> listSaves() throws IOException {
        List<String> names = new ArrayList<>();
        Path saved = savedDir();
        if (!Files.isDirectory(saved)) return names;
        try (Stream<Path> s = Files.list(saved)) {
            s.map(p -> p.getFileName().toString())
                    .filter(n -> n.toLowerCase().endsWith(".ser"))
                    .forEach(n -> names.add(n.substring(0, n.length() - 4)));
        }
        Collections.sort(names);
        return names;
    }

    // ---- activegame ----

    /** Copies the current activegame file into saved/ as _backup_<timestamp>, so replace/revert can be undone. */
    public static String backupActive() throws IOException {
        Path f = file(activeDir());
        if (!Files.exists(f)) return null;
        String name = "_backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Files.copy(f, savedDir().resolve(name + ".ser"), REPLACE_EXISTING);
        return name;
    }

    /** Backs up activegame, then overwrites it with a copy of source carrying a NEW sessionId. */
    public static GameState replaceActive(GameState source) throws IOException {
        backupActive();
        GameState fresh = source.deepCopy();
        fresh.newSession();
        write(fresh, activeDir());
        return fresh;
    }
}