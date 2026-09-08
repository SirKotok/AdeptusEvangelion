package eva.evangelion.view;

import eva.evangelion.items.Weapon.*;
import eva.evangelion.view.UIElements.BetterButton;
import eva.evangelion.view.UIElements.ScrollableContainer;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class WeaponCreatorUI extends Pane {

    private static final double INPUT_CONTAINER_LEFT = 0.05;
    private static final double INPUT_CONTAINER_WIDTH = 0.3;
    private static final double INPUT_CONTAINER_TOP = 0.05;
    private static final double INPUT_CONTAINER_HEIGHT = 0.3;

    private static final double CUSTOM_CONTAINER_LEFT = 0.05;
    private static final double CUSTOM_CONTAINER_WIDTH = 0.3;
    private static final double CUSTOM_CONTAINER_TOP = 0.40;
    private static final double CUSTOM_CONTAINER_HEIGHT = 0.27;

    private static final double ACTION_CONTAINER_LEFT = 0.05;
    private static final double ACTION_CONTAINER_WIDTH = 0.3;
    private static final double ACTION_CONTAINER_TOP = 0.7;
    private static final double ACTION_CONTAINER_HEIGHT = 0.12;

    private static final double STATS_CONTAINER_LEFT = 0.4;
    private static final double STATS_CONTAINER_WIDTH = 0.24;
    private static final double STATS_CONTAINER_TOP = 0.04;
    private static final double STATS_CONTAINER_HEIGHT = 0.4;

    private static final double DESCRIPTION_CONTAINER_LEFT = 0.65;
    private static final double DESCRIPTION_CONTAINER_WIDTH = 0.25;
    private static final double DESCRIPTION_CONTAINER_TOP = 0.04;
    private static final double DESCRIPTION_CONTAINER_HEIGHT = 0.4;

    private static final double ICON_CONTAINER_LEFT = 0.4;
    private static final double ICON_CONTAINER_WIDTH = 0.5;
    private static final double ICON_CONTAINER_TOP = 0.45;
    private static final double ICON_CONTAINER_HEIGHT = 0.4;

    // ---- Base weapon data ----
    private enum BaseWeaponType {
        // Melee
        KNIFE("Knife", false, Weapon.Hand.ONE_HANDED, 0, "2d3", 0, 0, 0, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.SMALL, Weapon.WeaponProperty.SWIFT, Weapon.WeaponProperty.THROWING)),
        REACH_WEAPON("Reach Weapon", false, Weapon.Hand.ONE_HANDED, 0, "1d10", 0, 0, 0, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.REACH)),
        SHIELD("Shield", false, Weapon.Hand.ONE_HANDED, 1, "1d6", 0, 0, 0, 0, 0, -1,
                Collections.emptyList()),
        SLASHING("Slashing Weapon", false, Weapon.Hand.ONE_HANDED, 0, "1d10", 0, 0, 0, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.PROVEN)),
        CRUSHING("Crushing Weapon", false, Weapon.Hand.ONE_HANDED, 0, "1d10", 0, 0, 0, 0, 1, -1,
                Collections.emptyList()),
        LARGE_SLASHING("Large Slashing Weapon", false, Weapon.Hand.TWO_HANDED, 0, "2d6", 0, 0, 0, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.PROVEN)),
        LARGE_CRUSHING("Large Crushing Weapon", false, Weapon.Hand.TWO_HANDED, 0, "2d6", 0, 0, 0, 0, 1, -1,
                Collections.emptyList()),
        LARGE_REACH("Large Reach Weapon", false, Weapon.Hand.TWO_HANDED, 0, "2d6", 0, 0, 0, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.THROWING, Weapon.WeaponProperty.REACH)),
        // Ranged
        PISTOL("Pistol", true, Weapon.Hand.ONE_HANDED, 0, "2d3", 6, 1, 3, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.CQB, Weapon.WeaponProperty.SMALL)),
        SMG("SMG", true, Weapon.Hand.ONE_HANDED, 0, "1d10", 4, 2, 4, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.SWIFT)),
        ASSAULT_RIFLE("Assault Rifle", true, Weapon.Hand.TWO_HANDED, 0, "2d6", 4, 2, 5, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.SPRAY)),
        SHOTGUN("Shotgun", true, Weapon.Hand.TWO_HANDED, 0, "2d6", 3, 1, 3, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.CQB)),
        SNIPER_RIFLE("Sniper Rifle", true, Weapon.Hand.TWO_HANDED, 0, "1d10", 3, 4, 8, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.PRECISE)),
        MACHINE_GUN("Machine Gun", true, Weapon.Hand.TWO_HANDED, 0, "2d6", 3, 3, 6, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.SPRAY, Weapon.WeaponProperty.SWIFT)),
        BAZOOKA("Bazooka", true, Weapon.Hand.TWO_HANDED, 1, "2d6+2", 1, 3, 6, 0, 2, 2,
                Arrays.asList(Weapon.WeaponProperty.AREA));

        final String displayName;
        final boolean ranged;
        final Weapon.Hand hands;
        final int baseCost;
        final String damage;
        final int ammo;
        final int minRange;
        final int maxRange;
        final int powerBonus;
        final int basePenetration;
        final int baseArea;
        final List<Weapon.WeaponProperty> baseProperties;

        BaseWeaponType(String displayName, boolean ranged, Weapon.Hand hands, int baseCost,
                       String damage, int ammo, int minRange, int maxRange, int powerBonus,
                       int basePenetration, int baseArea,
                       List<Weapon.WeaponProperty> baseProperties) {
            this.displayName = displayName;
            this.ranged = ranged;
            this.hands = hands;
            this.baseCost = baseCost;
            this.damage = damage;
            this.ammo = ammo;
            this.minRange = minRange;
            this.maxRange = maxRange;
            this.powerBonus = powerBonus;
            this.basePenetration = basePenetration;
            this.baseArea = baseArea;
            this.baseProperties = baseProperties;
        }
    }

    // ---- UI Components ----
    private TextField nameField;
    private ComboBox<BaseWeaponType> meleeTypeCombo;
    private ComboBox<BaseWeaponType> rangedTypeCombo;
    private BaseWeaponType currentType;
    private Label descriptionLabel;
    private ComboBox<Weapon.Tech> techCombo;
    private ComboBox<Weapon.Tech> secondTechCombo;
    private VBox customizationBox;
    private Map<Weapon.Customisation, CheckBox> customCheckBoxes = new LinkedHashMap<>();

    private Label statsLabel;
    private ScrollableContainer statsScrollContainer;

    // Icon selection
    private String selectedIcon = "weapon_0.png";
    private TilePane iconTilePane;
    private ImageView previewIconView;

    private Weapon currentWeapon;

    // New containers
    private ScrollableContainer descriptionContainer;
    private ScrollableContainer inputContainer;
    private ScrollableContainer customContainer;
    private ScrollableContainer actionContainer;

    // ---- Constructor ----
    public WeaponCreatorUI() {
        setPrefSize(900, 800);
        setStyle("-fx-background-color: #f4f4f4;");

        inputContainer = createContainer(INPUT_CONTAINER_LEFT, INPUT_CONTAINER_WIDTH, INPUT_CONTAINER_TOP, INPUT_CONTAINER_HEIGHT);
        customContainer = createContainer(CUSTOM_CONTAINER_LEFT, CUSTOM_CONTAINER_WIDTH, CUSTOM_CONTAINER_TOP, CUSTOM_CONTAINER_HEIGHT);
        actionContainer = createContainer(ACTION_CONTAINER_LEFT, ACTION_CONTAINER_WIDTH, ACTION_CONTAINER_TOP, ACTION_CONTAINER_HEIGHT);
        descriptionContainer = createContainer(DESCRIPTION_CONTAINER_LEFT, DESCRIPTION_CONTAINER_WIDTH, DESCRIPTION_CONTAINER_TOP, DESCRIPTION_CONTAINER_HEIGHT);
        // Existing stats and icon containers (also using constants)
        statsScrollContainer = createContainer(STATS_CONTAINER_LEFT, STATS_CONTAINER_WIDTH, STATS_CONTAINER_TOP, STATS_CONTAINER_HEIGHT);
        ScrollableContainer iconScrollContainer = createContainer(ICON_CONTAINER_LEFT, ICON_CONTAINER_WIDTH, ICON_CONTAINER_TOP, ICON_CONTAINER_HEIGHT);

        inputContainer.setContainerPadding(new Insets(10));
        inputContainer.setSpacing(5);
        inputContainer.setBackgroundColor(Color.rgb(255, 255, 255, 0.95));
        inputContainer.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

        descriptionContainer.setContainerPadding(new Insets(10));
        descriptionContainer.setSpacing(5);
        descriptionContainer.setBackgroundColor(Color.rgb(255, 255, 255, 0.95));
        descriptionContainer.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

        customContainer.setContainerPadding(new Insets(10));
        customContainer.setSpacing(5);
        customContainer.setBackgroundColor(Color.rgb(255, 255, 255, 0.95));
        customContainer.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");


        actionContainer.setContainerPadding(new Insets(10));
        actionContainer.setSpacing(5);
        actionContainer.setBackgroundColor(Color.rgb(255, 255, 255, 0.95));
        actionContainer.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

        // ---- Name Field ----
        Label nameLabel = new Label("Weapon Name:");
        nameField = new TextField("My Weapon");
        nameField.setPrefWidth(200); // adjust as needed

        // -- description label -- //
        descriptionLabel = new Label("");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");
        descriptionContainer.addNode(descriptionLabel);


        // ---- Weapon Type Selection ----
        Label meleeLabel = new Label("Melee Weapon:");
        meleeTypeCombo = new ComboBox<>();
        meleeTypeCombo.getItems().addAll(
                Arrays.stream(BaseWeaponType.values())
                        .filter(t -> !t.ranged)
                        .collect(Collectors.toList())
        );
        meleeTypeCombo.setPrefWidth(200);
        meleeTypeCombo.setValue(BaseWeaponType.KNIFE);

        Label rangedLabel = new Label("Ranged Weapon:");
        rangedTypeCombo = new ComboBox<>();
        rangedTypeCombo.getItems().addAll(
                Arrays.stream(BaseWeaponType.values())
                        .filter(t -> t.ranged)
                        .collect(Collectors.toList())
        );
        rangedTypeCombo.setPrefWidth(200);
        rangedTypeCombo.setValue(null);

        currentType = BaseWeaponType.KNIFE;

        meleeTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentType = newVal;
                rangedTypeCombo.setValue(null);
                updateTechCombo();
                updateCustomisationAvailability();
                updateStats();
            }
        });
        rangedTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentType = newVal;
                meleeTypeCombo.setValue(null);
                updateTechCombo();
                updateCustomisationAvailability();
                updateStats();
            }
        });

        // ---- Technology Selection ----
        Label techLabel = new Label("Technology:");
        techCombo = new ComboBox<>();
        techCombo.setPrefWidth(200);

        // ---- Second Technology (Double Edged / Enhanced Bayonet) ----
        Label secondTechLabel = new Label("Second Tech:");
        secondTechCombo = new ComboBox<>();
        secondTechCombo.setPrefWidth(200);
        secondTechCombo.setDisable(true);
        secondTechCombo.setVisible(false);

        // Display "NONE" when value is null
        secondTechCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Weapon.Tech item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("NONE");
                } else {
                    setText(item.toString());
                }
            }
        });
        secondTechCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Weapon.Tech item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("NONE");
                } else {
                    setText(item.toString());
                }
            }
        });

        updateTechCombo();

        // Add all input components to the inputContainer
        inputContainer.addNode(nameLabel);
        inputContainer.addNode(nameField);
        inputContainer.addNode(meleeLabel);
        inputContainer.addNode(meleeTypeCombo);
        inputContainer.addNode(rangedLabel);
        inputContainer.addNode(rangedTypeCombo);
        inputContainer.addNode(techLabel);
        inputContainer.addNode(techCombo);
        inputContainer.addNode(secondTechLabel);
        inputContainer.addNode(secondTechCombo);

        // ---- Customisations ----
        Label customLabel = new Label("Customisations:");
        customLabel.setStyle("-fx-font-weight: bold;");

        customizationBox = new VBox(5);
        customizationBox.setPadding(new Insets(5));
        customizationBox.setPrefWidth(380);

        // Create checkboxes with pretty labels
        for (Weapon.Customisation c : Weapon.Customisation.values()) {
            if (c == Weapon.Customisation.REINFORCED) continue;
            if (c == Weapon.Customisation.ENHANCED_BAYONET) continue; // removed separate checkbox
            CheckBox cb = new CheckBox(getCustomisationLabel(c));
            cb.setSelected(false);
            cb.setOnAction(e -> {
                updateStats();
                if (c == Weapon.Customisation.DOUBLE_EDGED || c == Weapon.Customisation.BAYONET) {
                    updateSecondTechComboState();
                }
                updateDescription(getCustomisationDescription(c));
            });
            customCheckBoxes.put(c, cb);
            customizationBox.getChildren().add(cb);
        }

        // Add customisation label and box to customContainer
        customContainer.addNode(customLabel);
        customContainer.addNode(customizationBox);

        // ---- Action Buttons ----
        BetterButton saveBtn = new BetterButton("Save Weapon");
        saveBtn.setSuccessStyle();
        saveBtn.setOnAction(e -> showSaveDialog());

        BetterButton clearBtn = new BetterButton("Clear");
        clearBtn.setDangerStyle();
        clearBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Clear");
            confirm.setHeaderText("Clear All Weapon Choices");
            confirm.setContentText("This will reset all fields to their default values. Continue?");
            confirm.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    clearAll();
                }
            });
        });

        // Add buttons to actionContainer
        actionContainer.addNode(saveBtn);
        actionContainer.addNode(clearBtn);

        // ---- Stats Display (ScrollableContainer) ----
        statsLabel = new Label();
        statsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: black;");
        statsLabel.setWrapText(true);


        statsScrollContainer.setContainerPadding(new Insets(10));
        statsScrollContainer.setSpacing(5);
        statsScrollContainer.setBackgroundColor(Color.rgb(255, 255, 255, 0.95));
        statsScrollContainer.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

        previewIconView = new ImageView();
        previewIconView.setFitWidth(64);
        previewIconView.setFitHeight(64);
        previewIconView.setPreserveRatio(true);
        try {
            Image defaultImg = new Image(getClass().getResourceAsStream("/weapon_icons/" + selectedIcon));
            previewIconView.setImage(defaultImg);
        } catch (Exception e) {
            // ignore
        }

        statsScrollContainer.addNode(previewIconView);
        statsScrollContainer.addNode(statsLabel);

        // ---- Weapon Icon Selection (ScrollableContainer) ----

        iconScrollContainer.setContainerPadding(new Insets(10));
        iconScrollContainer.setSpacing(5);
        iconScrollContainer.setBackgroundColor(Color.rgb(255, 255, 255, 0.95));
        iconScrollContainer.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

        iconTilePane = new TilePane();
        iconTilePane.setHgap(10);
        iconTilePane.setVgap(10);
        iconTilePane.setPadding(new Insets(10));
        iconTilePane.setPrefColumns(4);
        iconTilePane.setStyle("-fx-background-color: white;");

        loadWeaponIcons();
        iconScrollContainer.addNode(iconTilePane);

        // ---- Add containers to main pane ----
        getChildren().addAll(
                inputContainer,
                customContainer,
                actionContainer,
                statsScrollContainer,
                iconScrollContainer,
                descriptionContainer
        );

        // ---- Listeners ----
        techCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateStats();
            updateSecondTechComboState();
            updateDescription(getTechDescription(newVal));
        });
        secondTechCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateStats();
            updateDescription(getTechDescription(newVal));
        });
        nameField.textProperty().addListener((obs, oldVal, newVal) -> updateStats());

        // Initial update
        updateCustomisationAvailability();
        updateStats();
        updateDescription("");
    }

    private void showSaveDialog() {
        if (currentWeapon == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("No weapon to save.");
            alert.showAndWait();
            return;
        }

        // Create a custom dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirm Save");
        dialog.setHeaderText("Save Weapon");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        // Create content: label + text field
        Label nameLabel = new Label("Weapon Name:");
        TextField nameField = new TextField(currentWeapon.getName());
        nameField.setPrefWidth(250);

        Label fileStatusLabel = new Label();
        fileStatusLabel.setWrapText(true);
        fileStatusLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");

        VBox content = new VBox(10, nameLabel, nameField, fileStatusLabel);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        // Get the OK button and cast to Button for text changes
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        // Helper to update status based on current name field text
        Runnable updateStatus = () -> {
            String trimmed = nameField.getText().trim();
            if (trimmed.isEmpty()) {
                fileStatusLabel.setText("Name cannot be empty.");
                okButton.setDisable(true);
                return;
            }
            File dir = new File("Active/weapons");
            if (!dir.exists()) dir.mkdirs();
            String fileName = trimmed.replaceAll("[^a-zA-Z0-9]", "_") + ".ser";
            File file = new File(dir, fileName);
            if (file.exists()) {
                fileStatusLabel.setText("File already exists. It will be OVERWRITTEN.");
                okButton.setText("Overwrite");
            } else {
                fileStatusLabel.setText("New file will be created.");
                okButton.setText("Save");
            }
            okButton.setDisable(false);
        };

        // Update when text changes
        nameField.textProperty().addListener((obs, oldVal, newVal) -> updateStatus.run());

        // Set initial status
        updateStatus.run();

        dialog.setResultConverter(buttonType -> buttonType);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            String newName = nameField.getText().trim();
            if (!newName.isEmpty()) {
                currentWeapon.setName(newName);
            }
            saveWeapon();
        }
    }

    // ---- Helper Methods ----
    private String getCustomisationLabel(Weapon.Customisation c) {
        String name = c.name().toLowerCase().replace('_', ' ');
        return Arrays.stream(name.split(" "))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private void updateCustomisationAvailability() {
        if (currentType == null) return;
        boolean isRanged = currentType.ranged;
        for (Map.Entry<Weapon.Customisation, CheckBox> entry : customCheckBoxes.entrySet()) {
            Weapon.Customisation c = entry.getKey();
            CheckBox cb = entry.getValue();
            boolean allowed = switch (c) {
                case ANTI_ARMOR -> true;
                case BALANCED, DOUBLE_EDGED, EXPLOSIVE, THROWING -> !isRanged;
                case EXTRA_AMMO, BAYONET, TELESCOPIC_SIGHT, AUTO_LOADER -> isRanged;
                default -> true;
            };
            cb.setDisable(!allowed);
            if (!allowed) {
                cb.setSelected(false);
            }
        }
        updateSecondTechComboState();
    }

    private void updateTechCombo() {
        if (currentType == null) return;
        techCombo.getItems().clear();
        if (currentType.ranged) {
            techCombo.getItems().addAll(Weapon.Tech.GAUSS, Weapon.Tech.N2SHELL, Weapon.Tech.MASER, Weapon.Tech.POSITRON);
        } else {
            techCombo.getItems().addAll(Weapon.Tech.CHAIN, Weapon.Tech.PROGRESSIVE, Weapon.Tech.POLYTHERMIC, Weapon.Tech.SUPERCONDUCTIVE);
        }
        techCombo.setValue(techCombo.getItems().get(0));
        updateSecondTechComboState();
    }

    private void updateSecondTechComboState() {
        boolean showSecondTech = false;
        boolean isMelee = currentType != null && !currentType.ranged;
        boolean isRanged = currentType != null && currentType.ranged;

        CheckBox doubleEdgedCb = customCheckBoxes.get(Weapon.Customisation.DOUBLE_EDGED);
        boolean doubleEdgedSelected = doubleEdgedCb != null && doubleEdgedCb.isSelected() && isMelee;

        CheckBox bayonetCb = customCheckBoxes.get(Weapon.Customisation.BAYONET);
        boolean bayonetSelected = bayonetCb != null && bayonetCb.isSelected() && isRanged;

        showSecondTech = doubleEdgedSelected || bayonetSelected;

        secondTechCombo.setVisible(showSecondTech);
        secondTechCombo.setDisable(!showSecondTech);
        if (!showSecondTech) {
            secondTechCombo.setValue(null);
            return;
        }

        List<Weapon.Tech> secondTechOptions = new ArrayList<>();
        if (doubleEdgedSelected) {
            Weapon.Tech primaryTech = techCombo.getValue();
            if (primaryTech == Weapon.Tech.PROGRESSIVE || primaryTech == Weapon.Tech.CHAIN) {
                secondTechOptions.add(Weapon.Tech.POLYTHERMIC);
                secondTechOptions.add(Weapon.Tech.SUPERCONDUCTIVE);
            } else if (primaryTech == Weapon.Tech.POLYTHERMIC || primaryTech == Weapon.Tech.SUPERCONDUCTIVE) {
                secondTechOptions.add(Weapon.Tech.PROGRESSIVE);
                secondTechOptions.add(Weapon.Tech.CHAIN);
            }
            secondTechCombo.getItems().setAll(secondTechOptions);
            if (!secondTechOptions.isEmpty() && (secondTechCombo.getValue() == null || !secondTechOptions.contains(secondTechCombo.getValue()))) {
                secondTechCombo.setValue(secondTechOptions.get(0));
            }
        } else if (bayonetSelected) {
            // NONE (null) plus all melee techs
            secondTechOptions.add(null);
            secondTechOptions.add(Weapon.Tech.CHAIN);
            secondTechOptions.add(Weapon.Tech.PROGRESSIVE);
            secondTechOptions.add(Weapon.Tech.POLYTHERMIC);
            secondTechOptions.add(Weapon.Tech.SUPERCONDUCTIVE);
            secondTechCombo.getItems().setAll(secondTechOptions);
            if (secondTechCombo.getValue() != null && !secondTechOptions.contains(secondTechCombo.getValue())) {
                secondTechCombo.setValue(null);
            }
        }
    }

    private void loadWeaponIcons() {
        try {
            URL resourceDir = getClass().getResource("/weapon_icons/");
            if (resourceDir == null) {
                System.err.println("Could not find /weapon_icons/ directory, using default icon.");
                addIcon("weapon_0.png");
                return;
            }

            File dir = new File(resourceDir.toURI());
            if (dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
                if (files != null) {
                    Arrays.sort(files, Comparator.comparing(File::getName));
                    for (File file : files) {
                        addIcon(file.getName());
                    }
                }
            } else {
                System.err.println("Resource directory is not a filesystem directory, using default icon.");
                addIcon("weapon_0.png");
            }
        } catch (Exception e) {
            System.err.println("Error loading weapon icons: " + e.getMessage());
            addIcon("weapon_0.png");
        }
    }
    private ScrollableContainer createContainer(double left, double width, double top, double height) {
        ScrollableContainer container = new ScrollableContainer(left, width, top, height);
        container.setContainerPadding(new Insets(10));
        container.setSpacing(5);
        container.setBackgroundColor(Color.rgb(255, 255, 255, 0.95));
        container.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");
        container.setStyle("-fx-text-fill: black;");
        return container;
    }

    private void addIcon(String iconName) {
        try {
            Image img = new Image(getClass().getResourceAsStream("/weapon_icons/" + iconName));
            ImageView view = new ImageView(img);
            view.setFitWidth(64);
            view.setFitHeight(64);
            view.setPreserveRatio(true);
            view.setUserData(iconName);
            view.setOnMouseClicked(e -> selectIcon(iconName, view));

            if (iconName.equals(selectedIcon)) {
                selectIcon(iconName, view);
            }
            iconTilePane.getChildren().add(view);
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + iconName);
        }
    }

    private void selectIcon(String iconName, ImageView selectedView) {
        selectedIcon = iconName;
        for (var node : iconTilePane.getChildren()) {
            if (node instanceof ImageView iv) {
                iv.setStyle("-fx-border-color: transparent; -fx-border-width: 0;");
            }
        }
        selectedView.setStyle("-fx-border-color: #2c3e50; -fx-border-width: 3; -fx-border-radius: 5;");
        try {
            Image img = new Image(getClass().getResourceAsStream("/weapon_icons/" + iconName));
            previewIconView.setImage(img);
        } catch (Exception e) {
            // ignore
        }
    }

    private void updateDescription(String text) {
        descriptionLabel.setText(text);
    }

    private String getTechDescription(Weapon.Tech tech) {
        if (tech == null) return "";
        return switch (tech) {
            case NONE -> "No check chosen";
            case CHAIN -> "Chain weapons use thousands of blades to rip angels apart.\n\nYou deal 1 Strain per DoS on your Attack Test with a Chain Weapon (Maximum 3 Strain).\nThis is applied even if your target successfully Guards.\n";
            case PROGRESSIVE -> "Progressive weapons use sonic vibrations to enhance their cutting power or impact.\n\nYou have +2 Penetration with a Progressive Weapon.";
            case POLYTHERMIC -> "Polythermic weapons use superheated ceramic to burn flesh.\n\nYou have +1 Penetration with a Polythermic Weapon.\nWhen you roll damage with a Polythermic Weapon, you can choose to Overheat it, changing your weapon’s damage dice to a stronger profile.\n(1d6 or 2d3 -> 3d3)\n(1d10 or 2d6 -> 4d3+1)\n\nAfter using this ability, you take a -2 penalty to damage on your next attack with this weapon.";
            case SUPERCONDUCTIVE -> "Superconductive weapons electrify the target, momentarily stunning them.\n\nYou have +1 Penetration with a Superconductive weapon.\n If you deal damage with a Superconductive weapon, you can choose to apply:\n- a -10 Penalty to the target’s next Attack Test\n- a -10 Penalty to the target’s next Guard attempt.\n\nMultiple applications of the same penalty do not stack.";
            case GAUSS -> "Gauss Weapons use electromagnetism to launch powerful rounds at hypersonic velocity.\n\nYou deal an additional +1 damage per DoS on your attack test (maximum +4 Damage).";
            case N2SHELL -> "N2 Shell Weapons detonate with an anti-matter explosion.\n\nAttacks you make with a N2 Weapon have the Area (0) Property.\nIf the weapon or attack is already Area, you instead gain +1 damage.\nYou can spend 1 extra Ammo when attacking with an N2 Weapon to increase the Area rating of this weapon by 1.";
            case MASER -> "Maser Weapons fire concentrated beams of microwave energy.\n\nMaser Weapons have +1 Penetration.\nBy spending 1 extra Ammo, your attack gains the Line property and an additional +1 Penetration.";
            case POSITRON -> "Positron Weapons pierce all manner of protections with ease.\n\nPositron Weapons have +2 Penetration.";
            default -> "";
        };
    }

    private String getCustomisationDescription(Weapon.Customisation c) {
        return switch (c) {
            case ANTI_ARMOR -> "Increase your Penetration by +1";
            case BALANCED -> "This weapon gains Defensive 10.\n(Highest Defensive value gives bonus to Guard when weapon is in hand)";
            case DOUBLE_EDGED -> "You can choose a second technology (Options change depending on the first Technology).\nAt Turn start you can set the technology this weapon uses for the turn";
            case EXPLOSIVE -> "One time per battle you can forgo rolling and choose to deal maximum damage.";
            case THROWING -> "The Weapon gains the Throwing property.\n\nIf your weapon is already Throwing, you extend its Range by 1 and the weapon returns to you on a Miss or the target’s successful Guard.";
            case EXTRA_AMMO -> " If the weapon has 5 or more Ammo capacity, it gains +2 Ammo, otherwise it gains +1 Ammo.";
            case BAYONET -> "The weapon incorporates a Knife inside of itself.\nApplying a Technology to the knife will increase requisition cost by 1";
            case TELESCOPIC_SIGHT -> "When attacking an enemy at Range 3 or greater, your weapon gains Precise if it wasn’t Precise already.\n\nIf your weapon is already Precise, you instead gain +1 Damage on all Attack Actions you make at Range 3 or greater with this weapon, except for Blitz and Full Auto.";
            case AUTO_LOADER -> "Your weapon gains Spray if it did not have Spray already when making attacks at Range 3 or closer.\n\nIf your weapon already has Spray, once per Battle you can make a Blitz or Full Auto Attack with this weapon without expending Ammo."; //TODO This cant be applied to weapons that have the area property
            case REINFORCED -> "Increase the Ablative Value of the Shield by 1.  This may be purchased twice, for a total Ablative Value of 3.\n"; //TODO ADD SHIELDS
            default -> "";
        };
    }

    private Weapon buildWeapon() {
        if (currentType == null) return null;
        Weapon.Tech primaryTech = techCombo.getValue();
        if (primaryTech == null) primaryTech = Weapon.Tech.NONE;

        Weapon w = new Weapon(nameField.getText().trim().isEmpty() ? "Unnamed" : nameField.getText().trim(),
                currentType.displayName, currentType.hands);
        w.setRanged(currentType.ranged);
        w.setAmmo(currentType.ammo);
        w.setMaxAmmo(currentType.ammo);
        w.CurrentTech = primaryTech;
        w.Technology.clear();
        if (primaryTech != Weapon.Tech.NONE) w.Technology.add(primaryTech);

        w.WeaponProperties.addAll(currentType.baseProperties);
        w.setBasePenetration(currentType.basePenetration);
        w.setBaseArea(currentType.baseArea);
        w.setDisplayIcon(selectedIcon);

        for (Map.Entry<Weapon.Customisation, CheckBox> entry : customCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                Weapon.Customisation c = entry.getKey();
                switch (c) {
                    case DOUBLE_EDGED -> {
                        w.Customisations.add(Weapon.Customisation.DOUBLE_EDGED);
                        Weapon.Tech secondTech = secondTechCombo.getValue();
                        if (secondTech != null) w.Technology.add(secondTech);
                    }
                    case BAYONET -> {
                        Weapon.Tech secondTech = secondTechCombo.getValue();
                        if (secondTech != null) {
                            w.Customisations.add(Weapon.Customisation.ENHANCED_BAYONET);
                            w.Technology.add(secondTech);
                        } else {
                            w.Customisations.add(Weapon.Customisation.BAYONET);
                        }
                    }
                    default -> {
                        w.Customisations.add(c);
                        switch (c) {
                          //  case ANTI_ARMOR -> w.setBasePenetration(w.getBasePenetration() + 1);
                            case BALANCED -> w.setDefensive(10);
                            case EXTRA_AMMO -> {
                                if (w.isRanged()) {
                                    w.setMaxAmmo(w.getMaxAmmo() + (w.getMaxAmmo() >= 5 ? 2 : 1));
                                    w.setAmmo(w.getMaxAmmo());
                                }
                            }
                            case EXPLOSIVE -> {
                                if (!w.isRanged()) {
                                    w.setAmmo(1);
                                    w.setMaxAmmo(1);
                                }
                            }
                            case THROWING -> {
                                if (!w.isRanged() && !w.WeaponProperties.contains(Weapon.WeaponProperty.THROWING)) {
                                    w.WeaponProperties.add(Weapon.WeaponProperty.THROWING);
                                }
                            }
                            default -> { }
                        }
                    }
                }
            }
        }
        return w;
    }

    private void updateStats() {
        currentWeapon = buildWeapon();
        if (currentWeapon == null) {
            statsLabel.setText("Select a weapon type.");
            return;
        }

        int cost = computeCost(currentWeapon);
        StringBuilder stats = new StringBuilder();
        stats.append("Cost: ").append(cost).append(" Requisition\n");
        stats.append("Hands: ").append(currentWeapon.getHands().equals(Weapon.Hand.TWO_HANDED) ? "Two Handed" : "One Handed").append("\n");
        stats.append("Ranged: ").append(currentWeapon.isRanged() ? "Yes" : "No").append("\n");
        if (currentWeapon.isRanged()) {
            stats.append("Ammo: ").append(currentWeapon.getAmmo()).append(" / ").append(currentWeapon.getMaxAmmo()).append("\n");
            stats.append("Range: ").append(getRangeString(currentWeapon)).append("\n");
        }
        stats.append("Technology: ");
        if (currentWeapon.Technology.isEmpty()) {
            stats.append("None");
        } else {
            stats.append(currentWeapon.Technology.get(0));
            if (currentWeapon.Technology.size() > 1) {
                stats.append("\n");
                if (currentWeapon.Customisations.contains(Weapon.Customisation.ENHANCED_BAYONET)) {
                stats.append("(Knife: ").append(currentWeapon.Technology.get(1)).append(")");
                } else {
                    stats.append("(Secondary: ").append(currentWeapon.Technology.get(1)).append(")");
                }
            }
        }
        stats.append("\n");
        stats.append("Damage: ").append(getDamageString(currentWeapon)).append("\n");
        int pen = currentWeapon.getBasePenetration() + getTechPenetrationBonus(currentWeapon) + (currentWeapon.Customisations.contains(Weapon.Customisation.ANTI_ARMOR) ? 1 : 0);
        stats.append("Penetration: ").append(pen).append("\n");
        int area = currentWeapon.getBaseArea();
        if (area == -2) stats.append("Line\n");
        else if (area >= 0) stats.append("Area: ").append(area).append("\n");
        else stats.append("Area: None\n");
        stats.append("Defensive: ").append(currentWeapon.getDefensive()).append("\n");
        stats.append("Properties: ");
        if (currentWeapon.WeaponProperties.isEmpty()) {
            stats.append("None");
        } else {
            stats.append(currentWeapon.WeaponProperties.stream()
                    .map(p -> p.name().charAt(0) + p.name().substring(1).toLowerCase().replace('_', ' '))
                    .collect(Collectors.joining(", ")));
        }
        stats.append("\n");

        statsLabel.setText(stats.toString());
    }

    private int getTechPenetrationBonus(Weapon w) {
        int bonus = 0;
        if (w.CurrentTech == Weapon.Tech.PROGRESSIVE || w.CurrentTech == Weapon.Tech.POSITRON) {
            bonus += 2;
        } else if (w.CurrentTech == Weapon.Tech.POLYTHERMIC || w.CurrentTech == Weapon.Tech.SUPERCONDUCTIVE ||
                w.CurrentTech == Weapon.Tech.MASER) {
            bonus += 1;
        }
        return bonus;
    }

    private int computeCost(Weapon w) {
        int cost = currentType.baseCost;
        for (Weapon.Customisation c : w.Customisations) {
            if (c == Weapon.Customisation.ENHANCED_BAYONET) cost += 2;
            else cost += 1;
        }
        return cost;
    }

    private String getRangeString(Weapon w) {
        if (currentType != null) {
            return currentType.minRange + " - " + currentType.maxRange;
        }
        return "N/A";
    }

    private String getDamageString(Weapon w) {
        if (currentType != null) {
            String dmg = currentType.damage;
            if (currentType.powerBonus > 0) dmg += "+" + currentType.powerBonus;
            return dmg + " + S";
        }
        return "N/A";
    }

    private void saveWeapon() {
        if (currentWeapon == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("No weapon to save.");
            alert.showAndWait();
            return;
        }
        File dir = new File("Active/weapons");
        if (!dir.exists()) dir.mkdirs();
        String fileName = currentWeapon.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".ser";
        File file = new File(dir, fileName);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(currentWeapon);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Weapon saved to " + file.getPath());
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Failed to save: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void clearAll() {
        nameField.setText("My Weapon");
        meleeTypeCombo.setValue(BaseWeaponType.KNIFE);
        rangedTypeCombo.setValue(null);
        currentType = BaseWeaponType.KNIFE;
        techCombo.setValue(techCombo.getItems().get(0));
        secondTechCombo.setValue(null);
        for (CheckBox cb : customCheckBoxes.values()) {
            cb.setSelected(false);
        }
        secondTechCombo.setDisable(true);
        secondTechCombo.setVisible(false);
        selectedIcon = "weapon_0.png";
        for (var node : iconTilePane.getChildren()) {
            if (node instanceof ImageView iv && iv.getUserData().equals(selectedIcon)) {
                selectIcon(selectedIcon, iv);
                break;
            }
        }
        updateCustomisationAvailability();
        updateStats();
        updateDescription("Weapon Cleared");
    }

    public static void launchStandalone() {
        Stage stage = new Stage();
        stage.setTitle("Weapon Creator");
        WeaponCreatorUI ui = new WeaponCreatorUI();
        Scene scene = new Scene(ui, 900, 800);
        stage.setScene(scene);
        stage.show();
    }
}