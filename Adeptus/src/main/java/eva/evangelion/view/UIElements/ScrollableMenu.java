package eva.evangelion.view.UIElements;


import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * A scrollable menu with configurable positioning, button styling,
 * and interactive styles (hover, pressed). All styling methods affect
 * the entire menu uniformly.
 */
public class ScrollableMenu extends ScrollPane {






    private final VBox buttonContainer;
    private boolean useVerticalCentering = true;

    // Positioning ratios
    private double leftMarginRatio;
    private double widthRatio;
    private double topMarginRatio;
    private double heightRatio;

    // Button styling defaults
    private Font buttonFont = Font.font("System", 14);
    private Color buttonTextFill = Color.BLACK;
    private String buttonNormalStyle = "-fx-background-color: #e0e0e0; -fx-background-radius: 5;";
    private String buttonHoverStyle = "-fx-background-color: #c0c0c0; -fx-background-radius: 5;";
    private String buttonPressedStyle = "-fx-background-color: #a0a0a0; -fx-background-radius: 5;";
    private double buttonPrefHeight = 40;
    private double buttonMinWidth = 0;
    private double buttonMaxWidth = Double.MAX_VALUE;
    private Insets buttonPadding = new Insets(5, 10, 5, 10);

    // VBox styling
    private double vboxSpacing = 10;
    private Insets vboxPadding = new Insets(10);

    // Menu (ScrollPane) styling
    private Color menuBackgroundColor = Color.TRANSPARENT;
    private String menuBorderStyle = "";

    /**
     * Full constructor with positioning and vertical centering option.
     */
    public ScrollableMenu(double leftMarginRatio, double widthRatio,
                          double topMarginRatio, double heightRatio, boolean useVerticalCentering) {
        this.leftMarginRatio = leftMarginRatio;
        this.widthRatio = widthRatio;
        this.topMarginRatio = topMarginRatio;
        this.heightRatio = heightRatio;
        this.useVerticalCentering = useVerticalCentering;

        buttonContainer = new VBox(vboxSpacing);
        buttonContainer.setFillWidth(true);
        buttonContainer.setPadding(vboxPadding);

        setContent(buttonContainer);
        setFitToWidth(true);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        applyMenuStyle();

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) bindToScene(newScene);
        });
    }

    // Convenience constructors
    public ScrollableMenu(double leftMarginRatio, double widthRatio,
                          double topMarginRatio, double heightRatio) {
        this(leftMarginRatio, widthRatio, topMarginRatio, heightRatio, true);
    }

    public ScrollableMenu(double leftMarginRatio, double widthRatio) {
        this(leftMarginRatio, widthRatio, 0, 1.0, false);
    }

    public ScrollableMenu() {
        this(0.10, 0.20, 0.10, 0.80, true);
    }

    private void bindToScene(Scene scene) {
        layoutXProperty().bind(scene.widthProperty().multiply(leftMarginRatio));
        prefWidthProperty().bind(scene.widthProperty().multiply(widthRatio));
        if (useVerticalCentering) {
            layoutYProperty().bind(scene.heightProperty().multiply(topMarginRatio));
            prefHeightProperty().bind(scene.heightProperty().multiply(heightRatio));
        } else {
            layoutYProperty().bind(scene.heightProperty().multiply(topMarginRatio));
            prefHeightProperty().bind(scene.heightProperty().multiply(heightRatio));
        }
    }

    // ========== Button management ==========

    public Button createButton(String text, EventHandler<ActionEvent> action) {
        Button btn = createStyledButton(text);
        btn.setOnAction(action);
        buttonContainer.getChildren().add(btn);
        return btn;
    }

    // Add this method to your ScrollableMenu class
    public void addButton(Button button) {
        // Apply current menu styles to this button
        applyButtonStyle(button);
        attachInteractiveStyles(button);
        buttonContainer.getChildren().add(button);
    }

// Also make applyButtonStyle and attachInteractiveStyles accessible (they are private, change to package-private or keep as is, but we call from inside class)
// Ensure that buttonContainer is accessible (it is).

    public boolean removeButton(Button button) {
        return buttonContainer.getChildren().remove(button);
    }

    public void removeButton(int index) {
        buttonContainer.getChildren().remove(index);
    }

    public void clearButtons() {
        buttonContainer.getChildren().clear();
    }

    public int getButtonCount() {
        return buttonContainer.getChildren().size();
    }

    public VBox getButtonContainer() {
        return buttonContainer;
    }

    /**
     * Applies current button style settings to all existing buttons.
     */
    public void restyleAllButtons() {
        for (var node : buttonContainer.getChildren()) {
            if (node instanceof Button) {
                applyButtonStyle((Button) node);
            }
        }
    }

    private Button createStyledButton(String text) {
        Button btn = new Button(text);
        applyButtonStyle(btn);
        attachInteractiveStyles(btn);
        return btn;
    }

    private void applyButtonStyle(Button btn) {
        btn.setFont(buttonFont);
        btn.setTextFill(buttonTextFill);
        btn.setPrefHeight(buttonPrefHeight);
        if (buttonMinWidth > 0) btn.setMinWidth(buttonMinWidth);
        btn.setMaxWidth(buttonMaxWidth);
        btn.setPadding(buttonPadding);
        btn.setStyle(buttonNormalStyle);
    }

    private void attachInteractiveStyles(Button btn) {
        // Hover effect
        btn.setOnMouseEntered(e -> {
            if (!btn.isDisabled()) btn.setStyle(buttonHoverStyle);
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisabled()) btn.setStyle(buttonNormalStyle);
        });
        // Pressed effect (mouse down)
        btn.setOnMousePressed(e -> {
            if (!btn.isDisabled()) btn.setStyle(buttonPressedStyle);
        });
        btn.setOnMouseReleased(e -> {
            if (!btn.isDisabled() && btn.isHover()) {
                btn.setStyle(buttonHoverStyle);
            } else if (!btn.isDisabled()) {
                btn.setStyle(buttonNormalStyle);
            }
        });
    }

    private void applyMenuStyle() {
        buttonContainer.setSpacing(vboxSpacing);
        buttonContainer.setPadding(vboxPadding);
        if (menuBackgroundColor != null) {
            setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            buttonContainer.setStyle("-fx-background-color: " + toCssColor(menuBackgroundColor) + ";");
        }
        if (menuBorderStyle != null && !menuBorderStyle.isEmpty()) {
            buttonContainer.setStyle((buttonContainer.getStyle() != null ? buttonContainer.getStyle() + " " : "") + menuBorderStyle);
        }
    }

    private String toCssColor(Color c) {
        return String.format("rgba(%d,%d,%d,%f)",
                (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255), c.getOpacity());
    }

    // ========== Global styling methods ==========

    // Button font
    public void setButtonFont(Font font) {
        this.buttonFont = font;
        restyleAllButtons();
    }

    public void setButtonFont(String family, double size) {
        setButtonFont(Font.font(family, size));
    }

    public void setButtonFontBold(double size) {
        setButtonFont(Font.font("System", FontWeight.BOLD, size));
    }

    // Button text color
    public void setButtonTextFill(Color color) {
        this.buttonTextFill = color;
        restyleAllButtons();
    }

    // Normal button style (CSS)
    public void setButtonNormalStyle(String cssStyle) {
        this.buttonNormalStyle = cssStyle;
        restyleAllButtons();
    }

    // Hover button style (CSS)
    public void setButtonHoverStyle(String cssStyle) {
        this.buttonHoverStyle = cssStyle;
        restyleAllButtons();
    }

    // Pressed button style (CSS)
    public void setButtonPressedStyle(String cssStyle) {
        this.buttonPressedStyle = cssStyle;
        restyleAllButtons();
    }

    // Convenience: set normal background color (keeps hover/pressed if previously set)
    public void setButtonBackgroundColor(Color color) {
        this.buttonNormalStyle = "-fx-background-color: " + toCssColor(color) + "; -fx-background-radius: 5;";
        restyleAllButtons();
    }

    // Button size
    public void setButtonPrefHeight(double height) {
        this.buttonPrefHeight = height;
        restyleAllButtons();
    }

    public void setButtonMinWidth(double width) {
        this.buttonMinWidth = width;
        restyleAllButtons();
    }

    public void setButtonMaxWidth(double width) {
        this.buttonMaxWidth = width;
        restyleAllButtons();
    }

    // Button padding
    public void setButtonPadding(double top, double right, double bottom, double left) {
        this.buttonPadding = new Insets(top, right, bottom, left);
        restyleAllButtons();
    }

    public void setButtonPadding(Insets insets) {
        this.buttonPadding = insets;
        restyleAllButtons();
    }

    // VBox spacing between buttons
    public void setSpacing(double spacing) {
        this.vboxSpacing = spacing;
        buttonContainer.setSpacing(spacing);
    }

    // Padding around the whole button area inside the scroll pane
    public void setMenuPadding(double top, double right, double bottom, double left) {
        this.vboxPadding = new Insets(top, right, bottom, left);
        buttonContainer.setPadding(vboxPadding);
    }

    public void setMenuPadding(Insets insets) {
        this.vboxPadding = insets;
        buttonContainer.setPadding(vboxPadding);
    }

    // Background color of the menu (the area behind buttons)
    public void setMenuBackgroundColor(Color color) {
        this.menuBackgroundColor = color;
        if (color == null) {
            buttonContainer.setStyle("");
        } else {
            buttonContainer.setStyle("-fx-background-color: " + toCssColor(color) + ";");
        }
    }

    // Additional border style for the menu container
    public void setMenuBorderStyle(String cssBorder) {
        this.menuBorderStyle = cssBorder;
        String current = buttonContainer.getStyle();
        if (current == null) current = "";
        if (cssBorder != null && !cssBorder.isEmpty()) {
            buttonContainer.setStyle(current + " " + cssBorder);
        }
    }

    // Reset all button styles to default
    public void resetButtonStyles() {
        buttonFont = Font.font("System", 14);
        buttonTextFill = Color.BLACK;
        buttonNormalStyle = "-fx-background-color: #e0e0e0; -fx-background-radius: 5;";
        buttonHoverStyle = "-fx-background-color: #c0c0c0; -fx-background-radius: 5;";
        buttonPressedStyle = "-fx-background-color: #a0a0a0; -fx-background-radius: 5;";
        buttonPrefHeight = 40;
        buttonMinWidth = 0;
        buttonMaxWidth = Double.MAX_VALUE;
        buttonPadding = new Insets(5, 10, 5, 10);
        restyleAllButtons();
    }

    /**
     * Sets the given sound on all BetterButton children currently in the menu.
     * Ordinary Buttons are ignored.
     */
    public void setAllButtonsSound(AudioClip sound) {
        for (Node node : buttonContainer.getChildren()) {
            if (node instanceof ToggleUIButton) {
                ((ToggleUIButton) node).setSound(sound);
            }
        }
    }

    public void setBaseStyle() {
        ScrollableMenu menu = this;
        menu.setButtonFont("Arial", 16);
        menu.setButtonTextFill(Color.WHITE);
        menu.setButtonBackgroundColor(Color.DARKBLUE);
        menu.setButtonPrefHeight(50);
        menu.setButtonPadding(10, 20, 10, 20);
        menu.setSpacing(15);
        menu.setMenuPadding(20, 20, 20, 20);
        menu.setMenuBackgroundColor(Color.LIGHTGRAY);
        menu.setMenuBorderStyle("-fx-border-color: black; -fx-border-width: 2; -fx-border-radius: 10;");
        menu.setButtonHoverStyle("-fx-background-color: #2980b9; -fx-background-radius: 8; -fx-text-fill: white;");
        menu.setButtonPressedStyle("-fx-background-color: #1c6ea4; -fx-background-radius: 8; -fx-text-fill: white;");
        menu.setButtonNormalStyle("-fx-background-color: #3498db; -fx-background-radius: 8; -fx-text-fill: white;");
    }



}