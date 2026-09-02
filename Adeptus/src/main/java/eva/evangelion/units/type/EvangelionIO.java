package eva.evangelion.units.type;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EvangelionIO {

    private static final String BASE_DIR = "Custom/evangelions";

    static {
        File dir = new File(BASE_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    public static void save(EvangelionType eva) throws IOException {
        String filename = eva.getName().replaceAll("[^a-zA-Z0-9_-]", "_") + ".ser";
        Path path = Paths.get(BASE_DIR, filename);
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(path))) {
            oos.writeObject(eva);
        }
    }

    public static EvangelionType load(String filename) throws IOException, ClassNotFoundException {
        Path path = Paths.get(BASE_DIR, filename);
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(path))) {
            return (EvangelionType) ois.readObject();
        }
    }

    public static List<String> listFiles() {
        File dir = new File(BASE_DIR);
        String[] files = dir.list((d, name) -> name.endsWith(".ser"));
        return files != null ? Arrays.asList(files) : new ArrayList<>();
    }
}