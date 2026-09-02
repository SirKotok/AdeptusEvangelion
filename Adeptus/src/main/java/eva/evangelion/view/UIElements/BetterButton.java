package eva.evangelion.view.UIElements;

import javafx.scene.control.Button;
import javafx.scene.media.AudioClip;


public class BetterButton extends Button {

    private static AudioClip defaultSound = null;
    private AudioClip sound;

    private String normalStyle;
    private String pressedStyle;

    public BetterButton(String text) {
        super(text);
        this.sound = defaultSound;

        // Default styles
        setPrimaryStyle(); // sets normal and pressed
        setupPressedEvents();
    }

    @Override
    public void fire() {
        playSound();
        System.out.println("Pressed "+this.getText()+" button!");
        super.fire();
    }

    // ---------- Sound management ----------
    public static void setDefaultSound(AudioClip sound) {
        defaultSound = sound;
    }

    public static AudioClip getDefaultSound() {
        return defaultSound;
    }

    public void setSound(AudioClip sound) {
        this.sound = sound;
    }

    public AudioClip getSound() {
        return sound;
    }

    protected void playSound() {
        if (sound != null) {
            sound.play();
        }
    }

    // ---------- Style management ----------
    /**
     * Sets both normal and pressed styles.
     */
    public void setButtonStyle(String normal, String pressed) {
        this.normalStyle = normal;
        this.pressedStyle = pressed;
        applyNormalStyle();
    }

    public void setNormalStyle(String style) {
        this.normalStyle = style;
        applyNormalStyle();
    }

    public void setPressedStyle(String style) {
        this.pressedStyle = style;
    }

    private void applyNormalStyle() {
        setStyle(normalStyle);
    }

    private void applyPressedStyle() {
        setStyle(pressedStyle);
    }

    /**
     * Sets up mouse event handlers to change style on press/release.
     */
    private void setupPressedEvents() {
        setOnMousePressed(e -> {
            if (!isDisabled()) applyPressedStyle();
        });
        setOnMouseReleased(e -> {
            if (!isDisabled()) applyNormalStyle();
        });
        setOnMouseExited(e -> {
            if (!isDisabled()) applyNormalStyle();
        });
    }

    // ---------- Preset styles ----------
    public void setPrimaryStyle() {
        setButtonStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 15; -fx-cursor: hand;",
                "-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 15; -fx-cursor: hand;"
        );
    }

    public void setSuccessStyle() {
        setButtonStyle(
                "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 15; -fx-cursor: hand;",
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 15; -fx-cursor: hand;"
        );
    }

    public void setDangerStyle() {
        setButtonStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 15; -fx-cursor: hand;",
                "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 15; -fx-cursor: hand;"
        );
    }

    public void setSecondaryStyle() {
        setButtonStyle(
                "-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 15; -fx-cursor: hand;",
                "-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 15; -fx-cursor: hand;"
        );
    }

    /**
     * Special style for selected state (e.g., brush size) – same on press/release.
     */
    public void setSelectedStyle() {
        setButtonStyle(
                "-fx-background-color: #888888; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 15;",
                "-fx-background-color: #888888; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 15;"
        );
    }
}