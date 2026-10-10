package eva.evangelion.view;

import eva.evangelion.items.Weapon.*;
import eva.evangelion.view.UIElements.BetterButton;
import eva.evangelion.view.UIElements.FormKit;
import eva.evangelion.view.UIElements.IntField;
import eva.evangelion.view.UIElements.ScrollableContainer;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Creates a WeaponType plus its basic attack profile. */
public class WeaponTypeCreatorScreen extends Pane {

    private final Set<String> reservedKeys = new HashSet<>();
    private final Runnable onSaved;

    private final TextField nameField = new TextField("My Weapon Type");
    private final CheckBox ranged = new CheckBox("Ranged");
    private final ComboBox<Weapon.Hand> hands = new ComboBox<>();
    private final IntField baseCost = new IntField(0);
    private final IntField dice = new IntField(1);
    private final IntField dicePower = new IntField(6);
    private final IntField power = new IntField(0);
    private final IntField ammo = new IntField(0);
    private final IntField ammoCost = new IntField(1);
    private final IntField minRange = new IntField(0);
    private final IntField maxRange = new IntField(1);
    private final IntField basePenetration = new IntField(0);
    private final IntField baseArea = new IntField(-1);
    private final Map<Weapon.WeaponProperty, CheckBox> propertyBoxes = new LinkedHashMap<>();
    private final Label previewLabel = new Label();

    /**
     * @param reservedNames built-in type names that cannot be reused
     * @param onSaved       called after a successful save so the main screen can reload its type lists
     */
    public WeaponTypeCreatorScreen(Collection<String> reservedNames, Runnable onSaved) {
        this.onSaved = onSaved;
        for (String n : reservedNames) reservedKeys.add(CustomContentStore.safeName(n).toLowerCase(Locale.ROOT));
        setPrefSize(900, 800);

        ScrollableContainer form = FormKit.panel(this, 0.03, 0.46, 0.03, 0.92);
        ScrollableContainer side = FormKit.panel(this, 0.52, 0.45, 0.03, 0.92);

        hands.getItems().setAll(Weapon.Hand.values());
        hands.setValue(Weapon.Hand.ONE_HANDED);

        form.addNode(FormKit.section("Weapon Type"));
        form.addNode(FormKit.row("Type name", nameField));
        form.addNode(ranged);
        form.addNode(FormKit.row("Hands", hands));
        form.addNode(FormKit.row("Base cost", baseCost));
        form.addNode(FormKit.section("Basic attack damage"));
        form.addNode(FormKit.row("Dice", dice));
        form.addNode(FormKit.row("Dice power", dicePower));
        form.addNode(FormKit.row("Power bonus", power));
        form.addNode(FormKit.section("Ammo and range"));
        form.addNode(FormKit.row("Ammo capacity", ammo));
        form.addNode(FormKit.row("Ammo cost", ammoCost));
        form.addNode(FormKit.row("Min range", minRange));
        form.addNode(FormKit.row("Max range", maxRange));
        form.addNode(FormKit.section("Weapon-wide bonuses"));
        form.addNode(FormKit.row("Penetration", basePenetration));
        form.addNode(FormKit.row("Area", baseArea));
        form.addNode(FormKit.hint("Area: -1 = none, -2 = line, 0 or more = area rating."));
        form.addNode(FormKit.section("Base properties"));
        for (Weapon.WeaponProperty p : Weapon.WeaponProperty.values()) {
            CheckBox cb = new CheckBox(p.name().charAt(0) + p.name().substring(1).toLowerCase());
            cb.setOnAction(e -> refreshPreview());
            propertyBoxes.put(p, cb);
            form.addNode(cb);
        }

        BetterButton saveBtn = new BetterButton("Save Weapon Type");
        saveBtn.setSuccessStyle();
        saveBtn.setOnAction(e -> save());
        BetterButton loadBtn = new BetterButton("Load Existing");
        loadBtn.setOnAction(e -> loadExisting());
        BetterButton resetBtn = new BetterButton("Reset");
        resetBtn.setDangerStyle();
        resetBtn.setOnAction(e -> setFrom(new CustomWeaponType() {{ displayName = "My Weapon Type"; }}));

        previewLabel.setWrapText(true);
        side.addNode(FormKit.section("Preview"));
        side.addNode(previewLabel);
        side.addNode(saveBtn);
        side.addNode(loadBtn);
        side.addNode(resetBtn);
        side.addNode(FormKit.hint("Type is saved to " + CustomContentStore.WEAPON_TYPES_DIR.getPath()
                + ", its basic attack to " + CustomContentStore.TYPE_PROFILES_DIR.getPath()));

        getChildren().addAll(form, side);

        ranged.setOnAction(e -> {
            if (ranged.isSelected() && ammo.getInt() == 0) ammo.setInt(4);
            updateRangedState();
            refreshPreview();
        });
        hands.valueProperty().addListener((o, a, b) -> refreshPreview());
        nameField.textProperty().addListener((o, a, b) -> refreshPreview());
        for (IntField f : List.of(baseCost, dice, dicePower, power, ammo, ammoCost,
                minRange, maxRange, basePenetration, baseArea)) {
            f.textProperty().addListener((o, a, b) -> refreshPreview());
        }
        updateRangedState();
        refreshPreview();
    }

    private void updateRangedState() {
        ammo.setDisable(!ranged.isSelected());
        ammoCost.setDisable(!ranged.isSelected());
    }

    private CustomWeaponType buildType() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Type name cannot be empty.");
        if (reservedKeys.contains(CustomContentStore.safeName(name).toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException("\"" + name + "\" is a built-in weapon type name.");
        if (dice.getInt() < 1 || dicePower.getInt() < 1) throw new IllegalArgumentException("Dice and dice power must be at least 1.");
        if (baseCost.getInt() < 0) throw new IllegalArgumentException("Base cost cannot be negative.");
        if (baseArea.getInt() < -2) throw new IllegalArgumentException("Area must be -2 (line), -1 (none) or 0+.");
        if (minRange.getInt() < 0 || maxRange.getInt() < Math.max(1, minRange.getInt()))
            throw new IllegalArgumentException("Range must satisfy 0 <= min <= max and max >= 1.");
        if (ranged.isSelected() && (ammo.getInt() < 1 || ammoCost.getInt() < 0))
            throw new IllegalArgumentException("Ranged types need ammo capacity of at least 1 and a non-negative ammo cost.");

        CustomWeaponType t = new CustomWeaponType();
        t.displayName = name;
        t.ranged = ranged.isSelected();
        t.hands = hands.getValue();
        t.baseCost = baseCost.getInt();
        t.dice = dice.getInt();
        t.dicePower = dicePower.getInt();
        t.power = power.getInt();
        t.ammo = t.ranged ? ammo.getInt() : 0;
        t.ammoCost = t.ranged ? ammoCost.getInt() : 0;
        t.minRange = minRange.getInt();
        t.maxRange = maxRange.getInt();
        t.basePenetration = basePenetration.getInt();
        t.baseArea = baseArea.getInt();
        for (Map.Entry<Weapon.WeaponProperty, CheckBox> e : propertyBoxes.entrySet()) {
            if (e.getValue().isSelected()) t.baseProperties.add(e.getKey());
        }
        return t;
    }

    private void refreshPreview() {
        try {
            CustomWeaponType t = buildType();
            StringBuilder sb = new StringBuilder();
            sb.append(t.displayName).append(t.ranged ? " (ranged)" : " (melee)").append('\n');
            sb.append("Hands: ").append(t.hands).append("   Cost: ").append(t.baseCost).append('\n');
            sb.append("Base penetration: ").append(t.basePenetration).append("   Base area: ").append(t.baseArea).append('\n');
            if (t.ranged) sb.append("Ammo: ").append(t.ammo).append("   Range: ").append(t.minRange).append(" - ").append(t.maxRange).append('\n');
            sb.append("Properties: ").append(t.baseProperties.isEmpty() ? "None" : t.baseProperties.toString()).append("\n\n");
            sb.append("Basic attack that will be saved:\n").append(AttackProfileCreatorUI.describe(t.toBasicProfile()));
            previewLabel.setStyle("-fx-text-fill: black;");
            previewLabel.setText(sb.toString());
        } catch (IllegalArgumentException ex) {
            previewLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
            previewLabel.setText(ex.getMessage());
        }
    }

    private void save() {
        CustomWeaponType t;
        try {
            t = buildType();
        } catch (IllegalArgumentException ex) {
            FormKit.alert(Alert.AlertType.WARNING, ex.getMessage());
            return;
        }
        File typeFile = CustomContentStore.fileFor(CustomContentStore.WEAPON_TYPES_DIR, t.displayName);
        File profileFile = CustomContentStore.fileFor(CustomContentStore.TYPE_PROFILES_DIR, t.displayName);
        if (typeFile.exists() && !FormKit.confirm("Overwrite weapon type", typeFile.getName() + " already exists. Overwrite it?")) return;
        try {
            // Profile first: a type only shows up in the lists once its file exists.
            CustomContentStore.save(profileFile, t.toBasicProfile());
            CustomContentStore.save(typeFile, t);
        } catch (IOException ex) {
            FormKit.alert(Alert.AlertType.ERROR, "Failed to save: " + ex.getMessage());
            return;
        }
        CustomContentStore.clearCache();
        if (onSaved != null) onSaved.run();
        FormKit.alert(Alert.AlertType.INFORMATION, "Saved " + typeFile.getPath() + "\nand " + profileFile.getPath());
    }

    private void loadExisting() {
        List<CustomWeaponType> all = CustomContentStore.loadAllWeaponTypes();
        if (all.isEmpty()) {
            FormKit.alert(Alert.AlertType.INFORMATION, "No custom weapon types found.");
            return;
        }
        List<String> names = all.stream().map(t -> t.displayName).toList();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setHeaderText("Load a custom weapon type into the form");
        dialog.showAndWait().ifPresent(n -> all.stream().filter(t -> t.displayName.equals(n)).findFirst().ifPresent(this::setFrom));
    }

    private void setFrom(CustomWeaponType t) {
        nameField.setText(t.displayName);
        ranged.setSelected(t.ranged);
        hands.setValue(t.hands);
        baseCost.setInt(t.baseCost);
        dice.setInt(t.dice);
        dicePower.setInt(t.dicePower);
        power.setInt(t.power);
        ammo.setInt(t.ammo);
        ammoCost.setInt(t.ammoCost);
        minRange.setInt(t.minRange);
        maxRange.setInt(t.maxRange);
        basePenetration.setInt(t.basePenetration);
        baseArea.setInt(t.baseArea);
        for (Map.Entry<Weapon.WeaponProperty, CheckBox> e : propertyBoxes.entrySet()) {
            e.getValue().setSelected(t.baseProperties.contains(e.getKey()));
        }
        updateRangedState();
        refreshPreview();
    }
}