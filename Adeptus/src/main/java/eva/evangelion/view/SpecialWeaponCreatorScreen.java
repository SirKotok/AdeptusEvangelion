package eva.evangelion.view;

import eva.evangelion.items.Weapon.*;
import eva.evangelion.view.UIElements.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Builds a weapon that skips the normal profiles and uses saved special attack profiles. */
public class SpecialWeaponCreatorScreen extends Pane {

    private static final class ProfileItem {
        final String key;
        final AttackProfile profile;
        final BooleanProperty selected = new SimpleBooleanProperty(false);

        ProfileItem(String key, AttackProfile profile) {
            this.key = key;
            this.profile = profile;
        }

        @Override public String toString() {
            return profile.name + "  (" + profile.Dice + "d" + profile.Dicepower + ")";
        }
    }

    private final Consumer<Weapon> saveHandler;

    private final TextField nameField = new TextField("My Special Weapon");
    private final ComboBox<Weapon.Hand> hands = new ComboBox<>();
    private final CheckBox ranged = new CheckBox("Ranged");
    private final IntField baseCost = new IntField(0);
    private final IntField ammo = new IntField(0);
    private final Map<Weapon.WeaponProperty, CheckBox> propertyBoxes = new LinkedHashMap<>();
    private final ListView<ProfileItem> profileList = new ListView<>();
    private final Label previewLabel = new Label();
    private final ImageView iconPreview = new ImageView();
    private final WeaponIconPicker icons;

    public SpecialWeaponCreatorScreen(Consumer<Weapon> saveHandler) {
        this.saveHandler = saveHandler;
        setPrefSize(900, 800);

        ScrollableContainer form = FormKit.panel(this, 0.03, 0.30, 0.03, 0.92);
        ScrollableContainer profiles = FormKit.panel(this, 0.35, 0.28, 0.03, 0.62);
        ScrollableContainer actions = FormKit.panel(this, 0.35, 0.28, 0.68, 0.27);
        ScrollableContainer preview = FormKit.panel(this, 0.65, 0.32, 0.03, 0.47);
        icons = new WeaponIconPicker(this, 0.65, 0.32, 0.53, 0.42);

        hands.getItems().setAll(Weapon.Hand.values());
        hands.setValue(Weapon.Hand.ONE_HANDED);

        form.addNode(FormKit.section("Special Weapon"));
        form.addNode(FormKit.row("Name", nameField));
        form.addNode(FormKit.row("Hands", hands));
        form.addNode(ranged);
        form.addNode(FormKit.row("Base cost", baseCost));
        form.addNode(FormKit.row("Ammo capacity", ammo));
        form.addNode(FormKit.hint("Damage, ammo cost, penetration, area and range come from the chosen attack profiles."));
        form.addNode(FormKit.section("Properties"));
        for (Weapon.WeaponProperty p : Weapon.WeaponProperty.values()) {
            CheckBox cb = new CheckBox(p.name().charAt(0) + p.name().substring(1).toLowerCase());
            cb.setOnAction(e -> refreshPreview());
            propertyBoxes.put(p, cb);
            form.addNode(cb);
        }

        profileList.setPrefHeight(360);
        profileList.setPlaceholder(new Label("No profiles in " + CustomContentStore.SPECIAL_PROFILES_DIR.getPath()
                + "\nCreate one on the Attack Profile screen."));
        profileList.setCellFactory(CheckBoxListCell.forListView(item -> item.selected));
        BetterButton refreshBtn = new BetterButton("Refresh List");
        refreshBtn.setOnAction(e -> refreshProfiles());
        profiles.addNode(FormKit.section("Attack profiles"));
        profiles.addNode(FormKit.hint("Tick the profiles this weapon should have."));
        profiles.addNode(profileList);
        profiles.addNode(refreshBtn);

        BetterButton saveBtn = new BetterButton("Save Weapon");
        saveBtn.setSuccessStyle();
        saveBtn.setOnAction(e -> onSave());
        BetterButton clearBtn = new BetterButton("Clear");
        clearBtn.setDangerStyle();
        clearBtn.setOnAction(e -> clearAll());
        actions.addNode(saveBtn);
        actions.addNode(clearBtn);

        iconPreview.setFitWidth(64);
        iconPreview.setFitHeight(64);
        iconPreview.setPreserveRatio(true);
        previewLabel.setWrapText(true);
        preview.addNode(iconPreview);
        preview.addNode(previewLabel);

        getChildren().addAll(form, profiles, actions, preview, icons);

        ranged.setOnAction(e -> { ammo.setDisable(!ranged.isSelected()); refreshPreview(); });
        hands.valueProperty().addListener((o, a, b) -> refreshPreview());
        nameField.textProperty().addListener((o, a, b) -> refreshPreview());
        baseCost.textProperty().addListener((o, a, b) -> refreshPreview());
        ammo.textProperty().addListener((o, a, b) -> refreshPreview());
        icons.selectedIconProperty().addListener((o, a, b) -> {
            iconPreview.setImage(WeaponIconPicker.loadIcon(b));
        });
        iconPreview.setImage(WeaponIconPicker.loadIcon(icons.getSelectedIcon()));

        ammo.setDisable(true);
        refreshProfiles();
    }

    /** Re-reads Custom/attack_profiles/special, keeping ticked profiles ticked. */
    public void refreshProfiles() {
        Set<String> ticked = profileList.getItems().stream()
                .filter(i -> i.selected.get()).map(i -> i.key).collect(Collectors.toSet());
        List<ProfileItem> fresh = new ArrayList<>();
        for (Map.Entry<String, AttackProfile> e : CustomContentStore.loadAllSpecialProfiles().entrySet()) {
            ProfileItem item = new ProfileItem(e.getKey(), e.getValue());
            item.selected.set(ticked.contains(item.key));
            item.selected.addListener((o, a, b) -> refreshPreview());
            fresh.add(item);
        }
        profileList.getItems().setAll(fresh);
        refreshPreview();
    }

    private Weapon buildWeapon() {
        List<AttackProfile> chosen = new ArrayList<>();
        for (ProfileItem i : profileList.getItems()) {
            if (i.selected.get()) chosen.add(i.profile.copy());
        }
        if (chosen.isEmpty()) throw new IllegalArgumentException("Tick at least one attack profile.");

        String name = nameField.getText().trim().isEmpty() ? "Unnamed" : nameField.getText().trim();
        Weapon w = new Weapon(name, "Special", hands.getValue());
        w.setRanged(ranged.isSelected());
        int capacity = ranged.isSelected() ? Math.max(0, ammo.getInt()) : 0;
        w.setAmmo(capacity);
        w.setMaxAmmo(capacity);
        w.doesNormalProfiles = false;
        w.Technology.clear();
        w.Technology.add(Weapon.Tech.NONE);   // getCurrentTech() reads index 0
        for (Map.Entry<Weapon.WeaponProperty, CheckBox> e : propertyBoxes.entrySet()) {
            if (e.getValue().isSelected()) w.WeaponProperties.add(e.getKey());
        }
        w.setDisplayIcon(icons.getSelectedIcon());

        // Weapon-level range mirrors the widest profile range; each profile keeps its own range.
        int min = Integer.MAX_VALUE;
        int max = 1;
        for (AttackProfile p : chosen) {
            min = Math.min(min, p.MinRange);
            max = Math.max(max, p.MaxRange);
            w.SpecialProfiles.add(p);
        }
        w.setMinRange(min == Integer.MAX_VALUE ? 0 : min);
        w.setMaxRange(max);
        return w;
    }

    private void refreshPreview() {
        try {
            Weapon w = buildWeapon();
            StringBuilder sb = new StringBuilder();
            sb.append(w.getName()).append('\n');
            sb.append("Hands: ").append(w.getHands()).append("   Cost: ").append(baseCost.getInt()).append('\n');
            sb.append("Ranged: ").append(w.isRanged() ? "Yes" : "No");
            if (w.isRanged()) sb.append("   Ammo: ").append(w.getMaxAmmo());
            sb.append('\n');
            sb.append("Properties: ").append(w.WeaponProperties.isEmpty() ? "None" : w.WeaponProperties.toString()).append("\n\n");
            for (AttackProfile p : w.SpecialProfiles) sb.append(AttackProfileCreatorUI.describe(p)).append('\n');
            previewLabel.setStyle("-fx-text-fill: black;");
            previewLabel.setText(sb.toString());
        } catch (IllegalArgumentException ex) {
            previewLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
            previewLabel.setText(ex.getMessage());
        }
    }

    private void onSave() {
        Weapon w;
        try {
            w = buildWeapon();
        } catch (IllegalArgumentException ex) {
            FormKit.alert(Alert.AlertType.WARNING, ex.getMessage());
            return;
        }
        saveHandler.accept(w);
    }

    private void clearAll() {
        nameField.setText("My Special Weapon");
        hands.setValue(Weapon.Hand.ONE_HANDED);
        ranged.setSelected(false);
        ammo.setInt(0);
        ammo.setDisable(true);
        baseCost.setInt(0);
        propertyBoxes.values().forEach(cb -> cb.setSelected(false));
        profileList.getItems().forEach(i -> i.selected.set(false));
        icons.reset();
        refreshPreview();
    }
}