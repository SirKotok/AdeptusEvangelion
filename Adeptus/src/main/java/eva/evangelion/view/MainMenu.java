package eva.evangelion.view;

import eva.evangelion.gameboard.Battlefield;
import eva.evangelion.units.type.EvangelionIO;
import eva.evangelion.view.UIElements.ToggleUIButton;
import eva.evangelion.view.UIElements.ScrollableContainer;
import eva.evangelion.view.UIElements.ScrollableMenu;
import eva.evangelion.view.UIElements.UIManager;
import eva.evangelion.view.UIElements.BetterButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class MainMenu {

    private Stage mainStage;
    private Scene mainScene;
    private Pane root;

    private ScrollableContainer unitsContainer;
    private ScrollableContainer weaponsContainer;
    private ScrollableContainer battlefieldsContainer;
    private ScrollableContainer gameContainer;

    private VBox unitsMainContent;
    private VBox evangelionsContent;
    private VBox angelsContent;
    private VBox otherUnitsContent;

    static {
        createFolder("sounds");
        createFolder("Custom/weapon_profiles");
        createFolder("Custom/upgrades");
        createFolder("Active/loadouts");
        createFolder("Active/weapons");
        createFolder("Created/evangelions");
        createFolder("Created/angels");
    }

    private static void createFolder(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("Created folder: " + dir.getAbsolutePath());
            } else {
                System.err.println("Could not create folder: " + dir.getAbsolutePath());
            }
        }
    }

    public MainMenu() throws IOException {
        // Load click sound
        AudioClip clickSound = null;
        File soundFile = new File("sounds/click.wav");
        if (soundFile.exists()) {
            clickSound = new AudioClip(soundFile.toURI().toString());
        } else {
            try {
                clickSound = new AudioClip(
                        Objects.requireNonNull(getClass().getResource("/eva/click.wav")).toString()
                );
            } catch (Exception e) {
                System.err.println("Click sound not found.");
            }
        }
        if (clickSound != null) {
            BetterButton.setDefaultSound(clickSound);
        }

        root = new Pane();
        setBackground();

        // ---------- Left Menu ----------
        ScrollableMenu menu = new ScrollableMenu(0.05, 0.20, 0.10, 0.80);
        menu.setBaseStyle();

        // ---------- Right Containers ----------
        double contLeft = 0.30;
        double contWidth = 0.60;
        double contTop = 0.10;
        double contHeight = 0.80;

        unitsContainer = createUnitsContainer(contLeft, contWidth, contTop, contHeight);
        weaponsContainer = createWeaponsContainer(contLeft, contWidth, contTop, contHeight);
        battlefieldsContainer = createBattlefieldsContainer(contLeft, contWidth, contTop, contHeight);
        gameContainer = createGameContainer(contLeft, contWidth, contTop, contHeight);

        unitsContainer.setVisible(false);
        weaponsContainer.setVisible(false);
        battlefieldsContainer.setVisible(false);
        gameContainer.setVisible(false);

        // ---------- Menu Buttons ----------
        ToggleUIButton unitsBtn = new ToggleUIButton("mainGroup", unitsContainer, "Units");
        ToggleUIButton weaponsBtn = new ToggleUIButton("mainGroup", weaponsContainer, "Weapons");
        ToggleUIButton battlefieldsBtn = new ToggleUIButton("mainGroup", battlefieldsContainer, "Battlefields");
        ToggleUIButton gameBtn = new ToggleUIButton("mainGroup", gameContainer, "Game");

        BetterButton exitBtn = new BetterButton("Exit");
        exitBtn.setDangerStyle();
        exitBtn.setOnAction(e -> System.exit(0));

        menu.addButton(unitsBtn);
        menu.addButton(weaponsBtn);
        menu.addButton(battlefieldsBtn);
        menu.addButton(gameBtn);
        menu.addButton(exitBtn);

        UIManager manager = UIManager.getInstance();
        manager.register(unitsBtn, "mainGroup", unitsContainer);
        manager.register(weaponsBtn, "mainGroup", weaponsContainer);
        manager.register(battlefieldsBtn, "mainGroup", battlefieldsContainer);
        manager.register(gameBtn, "mainGroup", gameContainer);

        root.getChildren().addAll(menu, unitsContainer, weaponsContainer,
                battlefieldsContainer, gameContainer);

        mainScene = new Scene(root, 900, 700);
        mainStage = new Stage();
        mainStage.setScene(mainScene);
        mainStage.setTitle("Main Menu");
        mainStage.show();
    }

    // ================== Background ==================

    private void showEvangelionList(String action, boolean isEdit) {
        List<String> files = EvangelionIO.listFiles();
        if (files.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("No saved Evangelions found.");
            alert.showAndWait();
            return;
        }

        Stage listStage = new Stage();
        listStage.setTitle("Select Evangelion");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));

        Label label = new Label("Select an Evangelion to " + action + ":");
        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(files);

        BetterButton confirmBtn = new BetterButton(action);
        confirmBtn.setPrimaryStyle();
        confirmBtn.setOnAction(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("Please select an Evangelion.");
                alert.showAndWait();
                return;
            }
            listStage.close();
            if (isEdit) {
                // new EvangelionEditor(selected);
            } else {
                // new EvangelionCustomEditor(selected);
            }
        });

        BetterButton cancelBtn = new BetterButton("Cancel");
        cancelBtn.setDangerStyle();
        cancelBtn.setOnAction(e -> listStage.close());

        HBox btnBox = new HBox(10);
        btnBox.getChildren().addAll(confirmBtn, cancelBtn);

        layout.getChildren().addAll(label, listView, btnBox);
        Scene scene = new Scene(layout, 400, 300);
        listStage.setScene(scene);
        listStage.show();
    }

    private void setBackground() {
        Image backgroundImage = new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/eva/background.png")),
                256, 256, false, false
        );
        BackgroundImage background = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.REPEAT,
                BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                null
        );
        root.setBackground(new Background(background));
    }

    // ================== Container Factories ==================

    private ScrollableContainer createUnitsContainer(double left, double width,
                                                     double top, double height) {
        ScrollableContainer container = new ScrollableContainer(left, width, top, height);
        container.setContainerPadding(new Insets(20));
        container.setSpacing(15);
        container.setBackgroundColor(Color.rgb(240, 240, 240, 0.9));
        container.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

        unitsMainContent = new VBox(15);
        unitsMainContent.setPadding(new Insets(10));
        unitsMainContent.setFillWidth(true);
        unitsMainContent.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Units");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        unitsMainContent.getChildren().add(title);

        BetterButton evaBtn = new BetterButton("Evangelions");
        evaBtn.setPrimaryStyle();
        BetterButton angelsBtn = new BetterButton("Angels");
        angelsBtn.setPrimaryStyle();
        BetterButton otherBtn = new BetterButton("Other");
        otherBtn.setPrimaryStyle();

        evaBtn.setOnAction(e -> showUnitSubContent("Evangelions"));
        angelsBtn.setOnAction(e -> showUnitSubContent("Angels"));
        otherBtn.setOnAction(e -> showUnitSubContent("Other"));

        unitsMainContent.getChildren().addAll(evaBtn, angelsBtn, otherBtn);

        evangelionsContent = createEvangelionsSubContent();
        angelsContent = createAngelsSubContent();
        otherUnitsContent = createOtherUnitsSubContent();

        container.addNode(unitsMainContent);
        return container;
    }

    private void showUnitSubContent(String type) {
        unitsContainer.clearNodes();

        VBox subContent = switch (type) {
            case "Evangelions" -> evangelionsContent;
            case "Angels"      -> angelsContent;
            case "Other"       -> otherUnitsContent;
            default -> unitsMainContent;
        };

        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(5));
        wrapper.setFillWidth(true);

        BetterButton backBtn = new BetterButton("← Back to Units");
        backBtn.setSecondaryStyle();
        backBtn.setOnAction(e -> {
            unitsContainer.clearNodes();
            unitsContainer.addNode(unitsMainContent);
        });
        wrapper.getChildren().addAll(backBtn, subContent);

        unitsContainer.addNode(wrapper);
    }

    private VBox createEvangelionsSubContent() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(5));
        vbox.setAlignment(Pos.TOP_LEFT);

        Label label = new Label("Evangelions");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        BetterButton createBtn = new BetterButton("Create New Evangelion (Random)");
        createBtn.setSuccessStyle();
        createBtn.setOnAction(e -> new EvangelionMaker());

        BetterButton customCreateBtn = new BetterButton("Create Custom Evangelion");
        customCreateBtn.setPrimaryStyle();

        BetterButton editBtn = new BetterButton("Edit Existing Evangelion");
        editBtn.setPrimaryStyle();
        editBtn.setOnAction(e -> showEvangelionList("View", true));

        BetterButton customEditBtn = new BetterButton("Customise Evangelion");
        customEditBtn.setPrimaryStyle();
        customEditBtn.setOnAction(e -> showEvangelionList("Customise", false));

        vbox.getChildren().addAll(label, createBtn, customCreateBtn, editBtn, customEditBtn);
        return vbox;
    }

    private VBox createAngelsSubContent() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(5));
        vbox.setAlignment(Pos.TOP_LEFT);
        Label label = new Label("Angels");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        BetterButton createBtn = new BetterButton("Create New Angel");
        createBtn.setSuccessStyle();
        BetterButton viewBtn = new BetterButton("View Existing Angels");
        viewBtn.setPrimaryStyle();
        createBtn.setOnAction(e -> System.out.println("Create Angel"));
        viewBtn.setOnAction(e -> System.out.println("View Angels"));
        vbox.getChildren().addAll(label, createBtn, viewBtn);
        return vbox;
    }

    private VBox createOtherUnitsSubContent() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(5));
        vbox.setAlignment(Pos.TOP_LEFT);
        Label label = new Label("Other Units");
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        BetterButton createBtn = new BetterButton("Create New Unit");
        createBtn.setSuccessStyle();
        BetterButton viewBtn = new BetterButton("View Existing Units");
        viewBtn.setPrimaryStyle();
        createBtn.setOnAction(e -> System.out.println("Create Other Unit"));
        viewBtn.setOnAction(e -> System.out.println("View Other Units"));
        vbox.getChildren().addAll(label, createBtn, viewBtn);
        return vbox;
    }

    private ScrollableContainer createWeaponsContainer(double left, double width,
                                                       double top, double height) {
        ScrollableContainer container = new ScrollableContainer(left, width, top, height);
        container.setContainerPadding(new Insets(20));
        container.setSpacing(15);
        container.setBackgroundColor(Color.rgb(240, 240, 240, 0.9));
        container.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

        VBox content = new VBox(15);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Weapons");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        BetterButton createBtn = new BetterButton("Create New Weapon");
        createBtn.setSuccessStyle();
        createBtn.setOnAction(e -> WeaponCreatorUI.launchStandalone());
        content.getChildren().addAll(title, createBtn);
        container.addNode(content);
        return container;
    }

    private ScrollableContainer createBattlefieldsContainer(double left, double width,
                                                            double top, double height) {
        ScrollableContainer container = new ScrollableContainer(left, width, top, height);
        container.setContainerPadding(new Insets(20));
        container.setSpacing(15);
        container.setBackgroundColor(Color.rgb(240, 240, 240, 0.9));
        container.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

        VBox content = new VBox(15);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Battlefields");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        BetterButton createBtn = new BetterButton("Create New Battlefield");
        createBtn.setSuccessStyle();
        createBtn.setOnAction(e -> {
            try {
                BattlefieldCreator creator = new BattlefieldCreator();
                creator.createNewMaker(mainStage, new Battlefield(30, 30));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        BetterButton viewBtn = new BetterButton("View Existing Battlefields");
        viewBtn.setPrimaryStyle();
        viewBtn.setOnAction(e -> System.out.println("View Battlefields"));

        content.getChildren().addAll(title, createBtn, viewBtn);
        container.addNode(content);
        return container;
    }

    private ScrollableContainer createGameContainer(double left, double width,
                                                    double top, double height) {
        ScrollableContainer container = new ScrollableContainer(left, width, top, height);
        container.setContainerPadding(new Insets(20));
        container.setSpacing(15);
        container.setBackgroundColor(Color.rgb(240, 240, 240, 0.9));
        container.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

        VBox content = new VBox(15);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Game");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // ---- Mode Selection ----
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton createRadio = new RadioButton("Create");
        createRadio.setToggleGroup(modeGroup);
        createRadio.setSelected(true);
        RadioButton connectRadio = new RadioButton("Connect");
        connectRadio.setToggleGroup(modeGroup);

        HBox modeBox = new HBox(20, createRadio, connectRadio);
        modeBox.setAlignment(Pos.CENTER_LEFT);

        // ---- Mode-specific input area ----
        VBox modeSpecificBox = new VBox(10);
        modeSpecificBox.setAlignment(Pos.CENTER_LEFT);

        // Create mode inputs
        HBox playerCountBox = new HBox(10);
        playerCountBox.setAlignment(Pos.CENTER_LEFT);
        Label playerCountLabel = new Label("Number of Players:");
        Spinner<Integer> playerSpinner = new Spinner<>(1, 13, 1);
        playerCountBox.getChildren().addAll(playerCountLabel, playerSpinner);

        // Connect mode inputs
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("Player Name:");
        TextField nameField = new TextField("Player1");
        nameBox.getChildren().addAll(nameLabel, nameField);

        // Show/hide based on radio selection
        modeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            modeSpecificBox.getChildren().clear();
            if (newToggle == createRadio) {
                modeSpecificBox.getChildren().add(playerCountBox);
            } else {
                modeSpecificBox.getChildren().add(nameBox);
            }
        });
        // Initial content
        modeSpecificBox.getChildren().add(playerCountBox);

        // ---- Speed controls (same as before) ----
        Label speedLabel = new Label("Speed: 1.0x");
        Slider speedSlider = new Slider(0.1, 2.0, 1.0);
        speedSlider.setBlockIncrement(0.1);
        speedSlider.setMajorTickUnit(0.5);
        speedSlider.setMinorTickCount(4);
        speedSlider.setSnapToTicks(false);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            speedLabel.setText("Speed: " + String.format("%.1f", newVal.doubleValue()) + "x");
        });

        CheckBox fastCheck = new CheckBox("Fast Actions (instant)");
        fastCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            speedSlider.setDisable(newVal);
        });

        VBox speedBox = new VBox(5, speedLabel, speedSlider, fastCheck);
        speedBox.setAlignment(Pos.CENTER_LEFT);

        // ---- Confirm Button ----
        BetterButton confirmBtn = new BetterButton("Confirm");
        confirmBtn.setSuccessStyle();
        confirmBtn.setOnAction(e -> {
            double speed = speedSlider.getValue();
            boolean fast = fastCheck.isSelected();
            Battlefield dummyBattlefield = new Battlefield(50, 50);  // will be ignored in connect mode

            if (createRadio.isSelected()) {
                int playerCount = playerSpinner.getValue();
                Game.startGame(dummyBattlefield, speed, fast, playerCount, "DM", true);
            } else { // Connect mode
                String playerName = nameField.getText().trim();
                if (playerName.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setContentText("Player name cannot be empty.");
                    alert.showAndWait();
                    return;
                }
                // Check if game file exists
                File gameFile = new File("Active/gamestate.ser");
                if (!gameFile.exists()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("No game file found");
                    alert.showAndWait();
                    return;
                }
                Game.startGame(dummyBattlefield, speed, fast, 1, playerName, false);
            }
        });

        content.getChildren().addAll(title, modeBox, modeSpecificBox, speedBox, confirmBtn);
        container.addNode(content);
        return container;
    }

    public Stage getMainStage() {
        return mainStage;
    }
}