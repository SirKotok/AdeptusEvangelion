package eva.evangelion.view.UIElements;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class ScrollableContainer extends ScrollPane {

    private final VBox contentBox;
    private boolean useVerticalCentering = true;

    private double leftMarginRatio;
    private double widthRatio;
    private double topMarginRatio;
    private double heightRatio;

    private double vboxSpacing = 10;
    private Insets vboxPadding = new Insets(10);
    private Color backgroundColor = Color.TRANSPARENT;
    private String borderStyle = "";

    // NEW: flag to control scene binding
    private boolean bindToScene = true;

    // NEW: constructor with bindToScene parameter
    public ScrollableContainer(double leftMarginRatio, double widthRatio,
                               double topMarginRatio, double heightRatio,
                               boolean useVerticalCentering, boolean bindToScene) {
        this.leftMarginRatio = leftMarginRatio;
        this.widthRatio = widthRatio;
        this.topMarginRatio = topMarginRatio;
        this.heightRatio = heightRatio;
        this.useVerticalCentering = useVerticalCentering;
        this.bindToScene = bindToScene;

        contentBox = new VBox(vboxSpacing);
        contentBox.setFillWidth(true);
        contentBox.setPadding(vboxPadding);
        applyVBoxStyle();

        setContent(contentBox);
        setFitToWidth(true);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) bindToScene(newScene);
        });
    }

    // Existing constructors call the new one with bindToScene = true
    public ScrollableContainer(double leftMarginRatio, double widthRatio,
                               double topMarginRatio, double heightRatio,
                               boolean useVerticalCentering) {
        this(leftMarginRatio, widthRatio, topMarginRatio, heightRatio, useVerticalCentering, true);
    }

    public ScrollableContainer(double leftMarginRatio, double widthRatio,
                               double topMarginRatio, double heightRatio) {
        this(leftMarginRatio, widthRatio, topMarginRatio, heightRatio, true, true);
    }

    public ScrollableContainer(double leftMarginRatio, double widthRatio) {
        this(leftMarginRatio, widthRatio, 0, 1.0, false, true);
    }

    public ScrollableContainer() {
        this(0.10, 0.20, 0.10, 0.80, true, true);
    }

    // NEW: setter to enable/disable binding after construction
    public void setBindToScene(boolean bindToScene) {
        this.bindToScene = bindToScene;
        // If binding is turned off, we must unbind any existing bindings
        if (!bindToScene) {
            layoutXProperty().unbind();
            layoutYProperty().unbind();
            prefWidthProperty().unbind();
            prefHeightProperty().unbind();
        } else {
            // Re‑bind if a scene is already set
            Scene scene = getScene();
            if (scene != null) bindToScene(scene);
        }
    }

    private void bindToScene(Scene scene) {
        if (!bindToScene) return;   // NEW: skip binding if flag is false

        layoutXProperty().bind(scene.widthProperty().multiply(leftMarginRatio));
        prefWidthProperty().bind(scene.widthProperty().multiply(widthRatio));
        layoutYProperty().bind(scene.heightProperty().multiply(topMarginRatio));
        prefHeightProperty().bind(scene.heightProperty().multiply(heightRatio));
    }

    // ========== Node management (unchanged) ==========
    public void addNode(Node node) { contentBox.getChildren().add(node); }
    public void addNode(int index, Node node) { contentBox.getChildren().add(index, node); }
    public boolean removeNode(Node node) { return contentBox.getChildren().remove(node); }
    public Node removeNode(int index) { return contentBox.getChildren().remove(index); }
    public void clearNodes() { contentBox.getChildren().clear(); }
    public int getNodeCount() { return contentBox.getChildren().size(); }
    public VBox getContentBox() { return contentBox; }

    // ========== Styling – fixed method names ==========
    public void setSpacing(double spacing) {
        this.vboxSpacing = spacing;
        contentBox.setSpacing(spacing);
    }

    // Renamed to avoid conflict with Region.setPadding (final)
    public void setContainerPadding(Insets insets) {
        this.vboxPadding = insets;
        contentBox.setPadding(vboxPadding);
    }

    public void setContainerPadding(double top, double right, double bottom, double left) {
        setContainerPadding(new Insets(top, right, bottom, left));
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        applyVBoxStyle();
    }

    public void setBorderStyle(String cssBorder) {
        this.borderStyle = cssBorder;
        applyVBoxStyle();
    }

    private void applyVBoxStyle() {
        StringBuilder style = new StringBuilder();
        if (backgroundColor != null && backgroundColor != Color.TRANSPARENT) {
            style.append("-fx-background-color: ")
                    .append(toCssColor(backgroundColor))
                    .append(";");
        }
        if (borderStyle != null && !borderStyle.isEmpty()) {
            style.append(" ").append(borderStyle);
        }
        contentBox.setStyle(style.toString());
    }

    public void resetStyle() {
        vboxSpacing = 10;
        vboxPadding = new Insets(10);
        backgroundColor = Color.TRANSPARENT;
        borderStyle = "";
        contentBox.setSpacing(vboxSpacing);
        contentBox.setPadding(vboxPadding);
        applyVBoxStyle();
    }

    // Convenience method to toggle fit-to-width (calls final superclass method)
    public void setFitWidth(boolean fit) {
        setFitToWidth(fit);
    }

    // Other scroll pane settings (no name clash)
    public void setVerticalScrollBarPolicy(ScrollBarPolicy policy) { setVbarPolicy(policy); }
    public void setHorizontalScrollBarPolicy(ScrollBarPolicy policy) { setHbarPolicy(policy); }

    private String toCssColor(Color c) {
        return String.format("rgba(%d,%d,%d,%f)",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255), c.getOpacity());
    }
}