package eva.evangelion.view.UIElements;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;

public class WeaponIconPicker extends ScrollableContainer {
    public static final String DEFAULT_ICON = "weapon_0.png";

    private final TilePane tiles = new TilePane();
    private final StringProperty selected = new SimpleStringProperty(DEFAULT_ICON);

    public WeaponIconPicker(Region parent, double left, double width, double top, double height) {
        super(left, width, top, height, true, false);
        bindToRegion(parent);
        setFitToHeight(true);
        FormKit.applyPanelStyle(this);

        tiles.setHgap(10);
        tiles.setVgap(10);
        tiles.setPadding(new Insets(10));
        tiles.setPrefColumns(4);
        tiles.setStyle("-fx-background-color: white;");

        selected.addListener((obs, old, now) -> highlight(now));
        loadIcons();
        highlight(selected.get());
        addNode(tiles);
    }

    public String getSelectedIcon() { return selected.get(); }
    public StringProperty selectedIconProperty() { return selected; }
    public void reset() { selected.set(DEFAULT_ICON); }

    public static Image loadIcon(String name) {
        try (InputStream in = WeaponIconPicker.class.getResourceAsStream("/weapon_icons/" + name)) {
            return in == null ? null : new Image(in);
        } catch (IOException e) {
            return null;
        }
    }

    private void loadIcons() {
        try {
            URL dir = WeaponIconPicker.class.getResource("/weapon_icons/");
            if (dir != null && "file".equals(dir.getProtocol())) {
                File[] files = new File(dir.toURI()).listFiles((d, n) -> n.toLowerCase().endsWith(".png"));
                if (files != null) {
                    Arrays.sort(files, Comparator.comparing(File::getName));
                    for (File f : files) addIcon(f.getName());
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading weapon icons: " + e.getMessage());
        }
        addIcon(DEFAULT_ICON);
    }

    private void addIcon(String name) {
        Image img = loadIcon(name);
        if (img == null) return;
        ImageView view = new ImageView(img);
        view.setFitWidth(64);
        view.setFitHeight(64);
        view.setPreserveRatio(true);
        StackPane cell = new StackPane(view);
        cell.setPadding(new Insets(3));
        cell.setUserData(name);
        cell.setOnMouseClicked(e -> selected.set(name));
        tiles.getChildren().add(cell);
    }

    private void highlight(String name) {
        for (Node n : tiles.getChildren()) {
            boolean on = n.getUserData().equals(name);
            n.setStyle(on
                    ? "-fx-border-color: #2c3e50; -fx-border-width: 3; -fx-border-radius: 5;"
                    : "-fx-border-color: transparent; -fx-border-width: 3;");
        }
    }
}