package eva.evangelion.state;

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
    public static final Path GAMES_DIR  = Paths.get("Active", "Games");
    public static final Path ACTIVE_DIR = GAMES_DIR.resolve("activegame");
    public static final Path DM_DIR     = GAMES_DIR.resolve("DMgame");
    public static final Path SAVED_DIR  = GAMES_DIR.resolve("saved");
    private static final Path TMP_DIR   = GAMES_DIR.resolve("tmp");   // outside the watched folders
    public static final String FILE_NAME = "gamestate.ser";
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,50}");

    private GameStateStore() {}

    public static Path file(Path dir) { return dir.resolve(FILE_NAME); }

    public static void ensureDirs() throws IOException {
        Files.createDirectories(ACTIVE_DIR);
        Files.createDirectories(DM_DIR);
        Files.createDirectories(SAVED_DIR);
        Files.createDirectories(TMP_DIR);
    }

    /** One-time: copy the old single-file location into activegame if activegame has nothing yet. */
    public static void migrateLegacy(Path legacyFile) throws IOException {
        if (Files.exists(legacyFile) && !Files.exists(file(ACTIVE_DIR))) {
            Files.copy(legacyFile, file(ACTIVE_DIR));
        }
    }

    private static Path savedFile(String name) {
        if (name == null || !VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid name (use 1-50 letters, digits, '_' or '-').");
        }
        return SAVED_DIR.resolve(name + ".ser");
    }

    /** Writes to a temp file and moves it into place, so watchers never see a half-written file. */
    private static void writeTo(GameState state, Path target) throws IOException {
        Files.createDirectories(TMP_DIR);
        Path tmp = Files.createTempFile(TMP_DIR, "state", ".tmp");
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
        if (!Files.isDirectory(SAVED_DIR)) return names;
        try (Stream<Path> s = Files.list(SAVED_DIR)) {
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
        Path f = file(ACTIVE_DIR);
        if (!Files.exists(f)) return null;
        String name = "_backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Files.copy(f, SAVED_DIR.resolve(name + ".ser"), REPLACE_EXISTING);
        return name;
    }

    /** Backs up activegame, then overwrites it with a copy of source carrying a NEW sessionId. */
    public static GameState replaceActive(GameState source) throws IOException {
        backupActive();
        GameState fresh = source.deepCopy();
        fresh.newSession();
        write(fresh, ACTIVE_DIR);
        return fresh;
    }
}