package eva.evangelion.view.UIElements;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/** Small helpers shared by the creator screens. */
public final class FormKit {
    private FormKit() {}

    /** A styled ScrollableContainer sized as a fraction of `parent` (not of the scene). */
    public static ScrollableContainer panel(Region parent, double left, double width, double top, double height) {
        ScrollableContainer c = new ScrollableContainer(left, width, top, height, true, false);
        c.bindToRegion(parent);
        c.setFitToHeight(true);
        applyPanelStyle(c);
        return c;
    }

    public static void applyPanelStyle(ScrollableContainer c) {
        c.setContainerPadding(new Insets(10));
        c.setSpacing(5);
        c.setBackgroundColor(Color.rgb(255, 255, 255, 0.95));
        c.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");
        c.setStyle("-fx-text-fill: black;");
    }

    public static Label section(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: black;");
        return l;
    }

    public static Label hint(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555;");
        return l;
    }

    public static HBox row(String label, Node input) {
        Label l = new Label(label);
        l.setMinWidth(130);
        l.setPrefWidth(130);
        HBox box = new HBox(8, l, input);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static void alert(Alert.AlertType type, String message) {
        new Alert(type, message).showAndWait();
    }

    public static boolean confirm(String header, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(header);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }
}