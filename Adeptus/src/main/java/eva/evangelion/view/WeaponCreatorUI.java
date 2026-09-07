package eva.evangelion.view;

import eva.evangelion.items.Weapon.*;
import eva.evangelion.view.UIElements.BetterButton;
import eva.evangelion.view.UIElements.ScrollableContainer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class WeaponCreatorUI extends Pane {

    // ---- Base weapon data (updated with basePenetration and baseArea) ----
    private enum BaseWeaponType {
        // Melee
        KNIFE("Knife", false, Weapon.Hand.ONE_HANDED, 0, "2d3", 0, 0, 0, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.SMALL, Weapon.WeaponProperty.SWIFT, Weapon.WeaponProperty.THROWING)),
        REACH_WEAPON("Reach Weapon", false, Weapon.Hand.ONE_HANDED, 0, "1d10", 0, 0, 0, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.REACH)),
        SLASHING("Slashing Weapon", false, Weapon.Hand.ONE_HANDED, 0, "1d10", 0, 0, 0, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.PROVEN)),
        LARGE_SLASHING("Large Slashing Weapon", false, Weapon.Hand.TWO_HANDED, 0, "2d6", 0, 0, 0, 0, 0, -1,
                Arrays.asList(Weapon.WeaponProperty.PROVEN)),
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
        final int baseArea; // -2 = line, -1 = none, >=0 = area value
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

    private ComboBox<Weapon.Tech> techCombo;
    private ComboBox<Weapon.Tech> secondTechCombo;
    private VBox customizationBox;
    private Map<Weapon.Customisation, CheckBox> customCheckBoxes = new LinkedHashMap<>();

    private Label statsLabel;
    private ScrollableContainer statsScrollContainer;

    private Weapon currentWeapon;

    // ---- Constructor ----
    public WeaponCreatorUI() {
        setPrefSize(900, 800);
        setStyle("-fx-background-color: #f4f4f4;");

        // ---- Name Field ----
        Label nameLabel = new Label("Weapon Name:");
        nameLabel.setLayoutX(20);
        nameLabel.setLayoutY(20);
        nameField = new TextField("My Weapon");
        nameField.setLayoutX(150);
        nameField.setLayoutY(15);
        nameField.setPrefWidth(250);

        // ---- Weapon Type Selection ----
        Label meleeLabel = new Label("Melee Weapon:");
        meleeLabel.setLayoutX(20);
        meleeLabel.setLayoutY(60);
        meleeTypeCombo = new ComboBox<>();
        meleeTypeCombo.getItems().addAll(
                Arrays.stream(BaseWeaponType.values())
                        .filter(t -> !t.ranged)
                        .collect(Collectors.toList())
        );
        meleeTypeCombo.setPrefWidth(250);
        meleeTypeCombo.setLayoutX(150);
        meleeTypeCombo.setLayoutY(55);
        meleeTypeCombo.setValue(BaseWeaponType.KNIFE);

        Label rangedLabel = new Label("Ranged Weapon:");
        rangedLabel.setLayoutX(20);
        rangedLabel.setLayoutY(100);
        rangedTypeCombo = new ComboBox<>();
        rangedTypeCombo.getItems().addAll(
                Arrays.stream(BaseWeaponType.values())
                        .filter(t -> t.ranged)
                        .collect(Collectors.toList())
        );
        rangedTypeCombo.setPrefWidth(250);
        rangedTypeCombo.setLayoutX(150);
        rangedTypeCombo.setLayoutY(95);
        rangedTypeCombo.setValue(null);

        // Current type initially melee
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
        techLabel.setLayoutX(20);
        techLabel.setLayoutY(140);
        techCombo = new ComboBox<>();
        techCombo.setPrefWidth(250);
        techCombo.setLayoutX(150);
        techCombo.setLayoutY(135);

        // ---- Second Technology (Double Edged) ----
        Label secondTechLabel = new Label("Second Tech:");
        secondTechLabel.setLayoutX(20);
        secondTechLabel.setLayoutY(180);
        secondTechCombo = new ComboBox<>();
        secondTechCombo.setPrefWidth(250);
        secondTechCombo.setLayoutX(150);
        secondTechCombo.setLayoutY(175);
        secondTechCombo.setDisable(true);
        secondTechCombo.setVisible(false);

        updateTechCombo();

        // ---- Customisations ----
        Label customLabel = new Label("Customisations:");
        customLabel.setStyle("-fx-font-weight: bold;");
        customLabel.setLayoutX(20);
        customLabel.setLayoutY(220);

        customizationBox = new VBox(5);
        customizationBox.setPadding(new Insets(5));
        customizationBox.setLayoutX(20);
        customizationBox.setLayoutY(250);
        customizationBox.setPrefWidth(450);

        // Create checkboxes with pretty labels
        for (Weapon.Customisation c : Weapon.Customisation.values()) {
            if (c == Weapon.Customisation.REINFORCED) continue;
            CheckBox cb = new CheckBox(getCustomisationLabel(c));
            cb.setSelected(false);
            cb.setOnAction(e -> updateStats());
            customCheckBoxes.put(c, cb);

            if (c == Weapon.Customisation.DOUBLE_EDGED) {
                cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    secondTechCombo.setDisable(!newVal);
                    secondTechCombo.setVisible(newVal);
                    if (!newVal) secondTechCombo.setValue(null);
                    updateStats();
                });
            }

            customizationBox.getChildren().add(cb);
        }

        // ---- Stats Display (ScrollableContainer with ratios) ----
        statsLabel = new Label();
        statsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: black;");
        statsLabel.setWrapText(true);

        // Use ScrollableContainer exactly as in MainMenu
        statsScrollContainer = new ScrollableContainer(0.3, 0.6, 0.3, 0.4); // left, width, top, height ratios
        statsScrollContainer.setContainerPadding(new Insets(10));
        statsScrollContainer.setSpacing(5);
        statsScrollContainer.setBackgroundColor(Color.rgb(255, 255, 255, 0.95));
        statsScrollContainer.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");
        statsScrollContainer.addNode(statsLabel);

        // ---- Action Buttons ----
        BetterButton saveBtn = new BetterButton("Save Weapon");
        saveBtn.setSuccessStyle();
        saveBtn.setLayoutX(600);
        saveBtn.setLayoutY(750);
        saveBtn.setOnAction(e -> saveWeapon());

        BetterButton clearBtn = new BetterButton("Clear");
        clearBtn.setDangerStyle();
        clearBtn.setLayoutX(750);
        clearBtn.setLayoutY(750);
        clearBtn.setOnAction(e -> clearAll());

        // ---- Add all nodes to pane ----
        getChildren().addAll(
                nameLabel, nameField,
                meleeLabel, meleeTypeCombo,
                rangedLabel, rangedTypeCombo,
                techLabel, techCombo,
                secondTechLabel, secondTechCombo,
                customLabel, customizationBox,
                statsScrollContainer,
                saveBtn, clearBtn
        );

        // ---- Listeners ----
        techCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateStats());
        secondTechCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateStats());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> updateStats());

        // Initial update
        updateCustomisationAvailability();
        updateStats();
    }

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
                case EXTRA_AMMO, BAYONET, ENHANCED_BAYONET, TELESCOPIC_SIGHT, AUTO_LOADER -> isRanged;
                default -> true;
            };
            cb.setDisable(!allowed);
            if (!allowed) cb.setSelected(false);
        }
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

        secondTechCombo.getItems().clear();
        secondTechCombo.getItems().addAll(techCombo.getItems());
        secondTechCombo.setValue(null);
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

        for (Map.Entry<Weapon.Customisation, CheckBox> entry : customCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                Weapon.Customisation c = entry.getKey();
                w.Customisations.add(c);
                switch (c) {
                    case DOUBLE_EDGED -> {
                        Weapon.Tech secondTech = secondTechCombo.getValue();
                        if (secondTech != null && secondTech != Weapon.Tech.NONE) {
                            w.Technology.add(secondTech);
                        }
                    }
                    case ANTI_ARMOR -> w.setBasePenetration(w.getBasePenetration() + 1);
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
                    default -> { /* other cases no direct stat change */ }
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
        stats.append("Hands: ").append(currentWeapon.getHands()).append("\n");
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
                stats.append(" (Secondary: ").append(currentWeapon.Technology.get(1)).append(")");
            }
        }
        stats.append("\n");

        stats.append("Damage: ").append(getDamageString(currentWeapon)).append("\n");

        int pen = currentWeapon.getBasePenetration() + getTechPenetrationBonus(currentWeapon);
        stats.append("Penetration: ").append(pen).append("\n");

        int area = currentWeapon.getBaseArea();
        if (area == -2) stats.append("Area: Line\n");
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
        File dir = new File("Created/weapons");
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
        updateCustomisationAvailability();
        updateStats();
    }

    // Static launcher
    public static void launchStandalone() {
        Stage stage = new Stage();
        stage.setTitle("Weapon Creator");
        WeaponCreatorUI ui = new WeaponCreatorUI();
        Scene scene = new Scene(ui, 900, 800);
        stage.setScene(scene);
        stage.show();
    }
}