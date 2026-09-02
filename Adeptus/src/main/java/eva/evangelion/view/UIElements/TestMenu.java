package com.evangelion.adeptusevangelion;



import eva.evangelion.view.UIElements.ScrollableContainer;
import eva.evangelion.view.UIElements.ScrollableMenu;
import eva.evangelion.view.UIElements.ToggleUIButton;
import eva.evangelion.view.UIElements.UIManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class TestMenu {

    private Scene mainScene;
    private Stage mainStage;
    private Pane root;
    private ScrollableMenu menu;
    private ScrollableContainer gameSetupContainer;
    private ScrollableContainer settingsContainer;
    private double currentFontSize = 14.0;  // default

    public Stage getMainStage() {
        return mainStage;
    }

    public TestMenu() throws IOException {
        mainStage = new Stage();
        mainStage.setTitle("Main Menu");

        root = new Pane();

        // Left menu
        menu = new ScrollableMenu(0.10, 0.30, 0.10, 0.80);
        menu.setBaseStyle();
        // Apply initial font to menu buttons
        menu.setButtonFont("System", currentFontSize);

        // --- Game Setup Container ---
        gameSetupContainer = new ScrollableContainer(0.40, 0.30, 0.10, 0.80);
        gameSetupContainer.setVisible(false);
        gameSetupContainer.setContainerPadding(new Insets(15));
        gameSetupContainer.setSpacing(15);
        gameSetupContainer.setBackgroundColor(Color.LIGHTGRAY);
        gameSetupContainer.setBorderStyle("-fx-border-color: darkgreen; -fx-border-width: 2; -fx-border-radius: 8;");

        // Title
        Label title = new Label("Game Setup");
        title.setStyle("-fx-font-weight: bold;");
        gameSetupContainer.addNode(title);

        // Number of players
        HBox playerCountBox = new HBox(10);
        playerCountBox.setAlignment(Pos.CENTER_LEFT);
        Label playerCountLabel = new Label("Number of Players:");
        Spinner<Integer> playerSpinner = new Spinner<>(1, 6, 1);
        playerSpinner.setEditable(true);
        playerCountBox.getChildren().addAll(playerCountLabel, playerSpinner);
        gameSetupContainer.addNode(playerCountBox);

        // Dynamic player area
        VBox playersArea = new VBox(10);
        gameSetupContainer.addNode(playersArea);

        // Global Nerv Resources
        HBox nervBox = new HBox(10);
        nervBox.setAlignment(Pos.CENTER_LEFT);
        Label nervLabel = new Label("Nerv Resources (global):");
        TextField nervField = new TextField("10");
        nervBox.getChildren().addAll(nervLabel, nervField);
        gameSetupContainer.addNode(nervBox);

        // Battlefield selection
        HBox battlefieldBox = new HBox(10);
        battlefieldBox.setAlignment(Pos.CENTER_LEFT);
        Label battlefieldLabel = new Label("Battlefield:");
        ComboBox<String> battlefieldCombo = new ComboBox<>();
        battlefieldCombo.getItems().addAll("One", "Two", "Three");
        battlefieldCombo.setValue("One");
        battlefieldBox.getChildren().addAll(battlefieldLabel, battlefieldCombo);
        gameSetupContainer.addNode(battlefieldBox);

        // Start button
        Button startGameBtn = new Button("Start Game");
        startGameBtn.setOnAction(e -> {
            System.out.println("Starting game with:");
            System.out.println("  Players: " + playerSpinner.getValue());
            System.out.println("  Nerv Resources: " + nervField.getText());
            System.out.println("  Battlefield: " + battlefieldCombo.getValue());
        });
        gameSetupContainer.addNode(startGameBtn);

        // Rebuild player fields when spinner changes
        Runnable rebuildPlayers = () -> {
            int numPlayers = playerSpinner.getValue();
            playersArea.getChildren().clear();
            for (int i = 1; i <= numPlayers; i++) {
                GridPane playerGrid = new GridPane();
                playerGrid.setHgap(10);
                playerGrid.setVgap(8);
                playerGrid.setPadding(new Insets(5, 0, 5, 0));
                playerGrid.setStyle("-fx-border-color: #aaa; -fx-border-width: 1; -fx-padding: 5;");

                Label playerLabel = new Label("Player " + i);
                playerLabel.setStyle("-fx-font-weight: bold;");
                GridPane.setConstraints(playerLabel, 0, 0, 2, 1);

                Label nameLabel = new Label("Name:");
                TextField nameField = new TextField("Player" + i);
                GridPane.setConstraints(nameLabel, 0, 1);
                GridPane.setConstraints(nameField, 1, 1);

                Label doomLabel = new Label("Doom:");
                TextField doomField = new TextField("0");
                GridPane.setConstraints(doomLabel, 0, 2);
                GridPane.setConstraints(doomField, 1, 2);

                Label fateLabel = new Label("Fate:");
                TextField fateField = new TextField("0");
                GridPane.setConstraints(fateLabel, 0, 3);
                GridPane.setConstraints(fateField, 1, 3);

                playerGrid.getChildren().addAll(playerLabel, nameLabel, nameField,
                        doomLabel, doomField, fateLabel, fateField);
                playersArea.getChildren().add(playerGrid);
            }
            // Apply current font size to newly created nodes
            applyFontSizeToAllNodes();
        };
        rebuildPlayers.run();
        playerSpinner.valueProperty().addListener((obs, oldVal, newVal) -> rebuildPlayers.run());

        // --- Settings Container (with text size slider) ---
        settingsContainer = new ScrollableContainer(0.40, 0.30, 0.10, 0.80);
        settingsContainer.setVisible(false);
        settingsContainer.setContainerPadding(new Insets(15));
        settingsContainer.setSpacing(12);
        settingsContainer.setBackgroundColor(Color.LIGHTBLUE);
        settingsContainer.setBorderStyle("-fx-border-color: navy; -fx-border-width: 2; -fx-border-radius: 8;");

        settingsContainer.addNode(new Label("Game Settings"));
        settingsContainer.addNode(new Label("Player Name:"));
        TextField nameField = new TextField();
        nameField.setPromptText("Enter your name");
        settingsContainer.addNode(nameField);

        settingsContainer.addNode(new Label("Difficulty:"));
        ComboBox<String> difficultyCombo = new ComboBox<>();
        difficultyCombo.getItems().addAll("Easy", "Normal", "Hard");
        difficultyCombo.setValue("Normal");
        settingsContainer.addNode(difficultyCombo);

        CheckBox fullscreenCheck = new CheckBox("Fullscreen mode");
        settingsContainer.addNode(fullscreenCheck);

        // --- Global text size slider ---
        HBox fontSizeBox = new HBox(10);
        fontSizeBox.setAlignment(Pos.CENTER_LEFT);
        Label fontSizeLabel = new Label("Text Size:");
        Slider fontSizeSlider = new Slider(8, 24, currentFontSize);
        fontSizeSlider.setShowTickLabels(true);
        fontSizeSlider.setShowTickMarks(true);
        fontSizeSlider.setMajorTickUnit(4);
        fontSizeSlider.setBlockIncrement(1);
        Label fontSizeValue = new Label(String.format("%.0f", currentFontSize));
        fontSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentFontSize = newVal.doubleValue();
            fontSizeValue.setText(String.format("%.0f", currentFontSize));
            applyGlobalFontSize();
        });
        fontSizeBox.getChildren().addAll(fontSizeLabel, fontSizeSlider, fontSizeValue);
        settingsContainer.addNode(fontSizeBox);

        Button applyBtn = new Button("Apply Settings");
        applyBtn.setOnAction(e -> {
            System.out.println("Name: " + nameField.getText());
            System.out.println("Difficulty: " + difficultyCombo.getValue());
            System.out.println("Fullscreen: " + fullscreenCheck.isSelected());
        });
        settingsContainer.addNode(applyBtn);

        // --- Left menu buttons ---
        ToggleUIButton startBtn = new ToggleUIButton("mainGroup", gameSetupContainer, "Start Game");
        ToggleUIButton settingsBtn = new ToggleUIButton("mainGroup", settingsContainer, "Settings");
        Button exitBtn = new Button("Exit");
        exitBtn.setOnAction(e -> System.exit(0));

        menu.addButton(startBtn);
        menu.addButton(settingsBtn);
        menu.addButton(exitBtn);

        // Register with UIManager
        UIManager manager = UIManager.getInstance();
        manager.register(startBtn, "mainGroup", gameSetupContainer);
        manager.register(settingsBtn, "mainGroup", settingsContainer);

        // Add all to root
        root.getChildren().addAll(menu, gameSetupContainer, settingsContainer);

        mainScene = new Scene(root, 800, 600);
        mainStage.setScene(mainScene);

        // Apply initial font size to everything
        applyGlobalFontSize();
    }

    // ---------- Global font size control ----------
    private void applyGlobalFontSize() {
        // Update left menu buttons (ScrollableMenu)
        menu.setButtonFont("System", currentFontSize);

        // Update all nodes inside the root pane
        applyFontSizeToNode(root, currentFontSize);
    }

    private void applyFontSizeToNode(Node node, double size) {
        if (node instanceof Label) {
            ((Label) node).setStyle("-fx-font-size: " + size + "px; -fx-text-fill: black;");
        } else if (node instanceof Button) {
            ((Button) node).setStyle("-fx-font-size: " + size + "px; -fx-text-fill: black;");
        } else if (node instanceof TextField) {
            ((TextField) node).setStyle("-fx-font-size: " + size + "px; -fx-text-fill: black;");
        } else if (node instanceof CheckBox) {
            ((CheckBox) node).setStyle("-fx-font-size: " + size + "px; -fx-text-fill: black;");
        } else if (node instanceof ComboBox) {
            ((ComboBox<?>) node).setStyle("-fx-font-size: " + size + "px;");
            // Also style the editor if it exists
            if (((ComboBox<?>) node).getEditor() != null) {
                ((ComboBox<?>) node).getEditor().setStyle("-fx-font-size: " + size + "px; -fx-text-fill: black;");
            }
        } else if (node instanceof Spinner) {
            ((Spinner<?>) node).setStyle("-fx-font-size: " + size + "px;");
            ((Spinner<?>) node).getEditor().setStyle("-fx-font-size: " + size + "px; -fx-text-fill: black;");
        } else if (node instanceof ToggleUIButton) {
            // ToggleUIButton is a custom Button subclass
            ((ToggleUIButton) node).setStyle("-fx-font-size: " + size + "px; -fx-text-fill: black;");
        }

        // Recursively process children
        if (node instanceof Pane || node instanceof VBox || node instanceof HBox || node instanceof GridPane || node instanceof ScrollPane) {
            for (Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                applyFontSizeToNode(child, size);
            }
        }
    }

    private void applyFontSizeToAllNodes() {
        applyFontSizeToNode(root, currentFontSize);
    }
}


