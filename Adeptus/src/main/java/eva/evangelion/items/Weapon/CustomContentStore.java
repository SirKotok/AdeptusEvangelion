package eva.evangelion.items.Weapon;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Reads and writes everything under the Custom folder. */
public final class CustomContentStore {
    public static final File WEAPON_TYPES_DIR = new File("Custom/weapon_types");
    public static final File TYPE_PROFILES_DIR = new File("Custom/attack_profiles/type");
    public static final File SPECIAL_PROFILES_DIR = new File("Custom/attack_profiles/special");

    // Only our own classes plus basic JDK types may be deserialized from Custom files.
    private static final ObjectInputFilter LOAD_FILTER = ObjectInputFilter.Config.createFilter(
            "eva.evangelion.**;java.lang.*;java.util.*;!*");

    private static final Map<String, AttackProfile> TYPE_PROFILE_CACHE = new HashMap<>();

    private CustomContentStore() {}

    public static String safeName(String name) {
        return name.trim().replaceAll("[^a-zA-Z0-9]", "_");
    }

    public static File fileFor(File dir, String name) {
        return new File(dir, safeName(name) + ".ser");
    }

    public static void save(File file, Serializable obj) throws IOException {
        Path target = file.toPath().toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), "save", ".tmp");
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(tmp))) {
            oos.writeObject(obj);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public static <T> T load(File file, Class<T> type) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(file.toPath())))) {
            ois.setObjectInputFilter(LOAD_FILTER);
            return type.cast(ois.readObject());
        }
    }

    /** All special attack profiles, keyed by file name without extension. */
    public static Map<String, AttackProfile> loadAllSpecialProfiles() {
        return loadAll(SPECIAL_PROFILES_DIR, AttackProfile.class);
    }

    public static List<CustomWeaponType> loadAllWeaponTypes() {
        return new ArrayList<>(loadAll(WEAPON_TYPES_DIR, CustomWeaponType.class).values());
    }

    /** A fresh copy of the saved basic profile for a custom type, or null if there is none. */
    public static synchronized AttackProfile loadTypeBasicProfile(String typeName) {
        if (typeName == null) return null;
        String key = safeName(typeName);
        AttackProfile cached = TYPE_PROFILE_CACHE.get(key);
        if (cached == null) {
            File file = fileFor(TYPE_PROFILES_DIR, typeName);
            if (!file.isFile()) return null;
            try {
                cached = load(file, AttackProfile.class);
                TYPE_PROFILE_CACHE.put(key, cached);
            } catch (Exception e) {
                System.err.println("Could not read " + file.getPath() + ": " + e);
                return null;
            }
        }
        return cached.copy();
    }

    public static synchronized void clearCache() {
        TYPE_PROFILE_CACHE.clear();
    }

    private static <T> Map<String, T> loadAll(File dir, Class<T> type) {
        Map<String, T> result = new LinkedHashMap<>();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".ser"));
        if (files == null) return result;
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File f : files) {
            try {
                String n = f.getName();
                result.put(n.substring(0, n.length() - 4), load(f, type));
            } catch (Exception e) {
                System.err.println("Skipping unreadable custom file " + f.getPath() + ": " + e);
            }
        }
        return result;
    }
}