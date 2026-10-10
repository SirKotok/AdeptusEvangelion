package eva.evangelion.view;

import eva.evangelion.items.Weapon.AttackProfile;
import eva.evangelion.items.Weapon.CustomContentStore;
import eva.evangelion.units.battle.Effect;
import eva.evangelion.view.UIElements.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds an AttackProfile by hand and saves it to Custom/attack_profiles/special. */
public class AttackProfileCreatorUI extends Pane {

    private final TextField nameField = new TextField("New Profile");
    private final IntField dice = new IntField(1);
    private final IntField dicePower = new IntField(6);
    private final IntField power = new IntField(0);
    private final IntField stamina = new IntField(1);
    private final IntField atp = new IntField(0);
    private final IntField ammoCost = new IntField(0);
    private final IntField penetration = new IntField(0);
    private final IntField strain = new IntField(0);
    private final IntField area = new IntField(-1);
    private final IntField multihit = new IntField(1);
    private final IntField minRange = new IntField(0);
    private final IntField maxRange = new IntField(1);
    private final CheckBox ranged = new CheckBox("Ranged");
    private final CheckBox countsAsAttack = new CheckBox("Counts as an attack");

    private final VBox effectsBox = new VBox(8);
    private final List<EffectEditor> effectRows = new ArrayList<>();
    private final Label previewLabel = new Label();

    public AttackProfileCreatorUI() {
        setPrefSize(900, 800);

        ScrollableContainer form = FormKit.panel(this, 0.03, 0.56, 0.03, 0.92);
        ScrollableContainer side = FormKit.panel(this, 0.62, 0.35, 0.03, 0.92);

        countsAsAttack.setSelected(true);

        form.addNode(FormKit.section("Attack Profile"));
        form.addNode(FormKit.row("Name", nameField));
        form.addNode(FormKit.row("Dice", dice));
        form.addNode(FormKit.row("Dice power", dicePower));
        form.addNode(FormKit.row("Power", power));
        form.addNode(FormKit.row("Stamina", stamina));
        form.addNode(FormKit.row("Ammo cost", ammoCost));
        form.addNode(FormKit.row("Penetration", penetration));
        form.addNode(FormKit.row("Strain", strain));
        form.addNode(FormKit.row("Area", area));
        form.addNode(FormKit.hint("Area: -1 = normal attack, -2 = line, 0 or more = area rating."));
        form.addNode(FormKit.row("Multihit", multihit));
        form.addNode(FormKit.row("ATP", atp));
        form.addNode(ranged);
        form.addNode(FormKit.row("Min range", minRange));
        form.addNode(FormKit.row("Max range", maxRange));
        form.addNode(countsAsAttack);
        form.addNode(FormKit.hint("Not counting as an attack means it can be used once per turn without using the attack limit."));

        form.addNode(FormKit.section("On-hit effects"));
        form.addNode(FormKit.hint("Every delta is typed by hand. Negative values are allowed."));
        BetterButton addEffect = new BetterButton("Add effect");
        addEffect.setOnAction(e -> { addEffectRow(null); refreshPreview(); });
        form.addNode(addEffect);
        form.addNode(effectsBox);

        BetterButton saveBtn = new BetterButton("Save Profile");
        saveBtn.setSuccessStyle();
        saveBtn.setOnAction(e -> save());
        BetterButton loadBtn = new BetterButton("Load Existing");
        loadBtn.setOnAction(e -> loadExisting());
        BetterButton resetBtn = new BetterButton("Reset");
        resetBtn.setDangerStyle();
        resetBtn.setOnAction(e -> reset());

        previewLabel.setWrapText(true);
        side.addNode(FormKit.section("Preview"));
        side.addNode(previewLabel);
        side.addNode(saveBtn);
        side.addNode(loadBtn);
        side.addNode(resetBtn);
        side.addNode(FormKit.hint("Saved to " + CustomContentStore.SPECIAL_PROFILES_DIR.getPath()));

        getChildren().addAll(form, side);

        watch(dice, dicePower, power, stamina, atp, ammoCost, penetration, strain, area, multihit, minRange, maxRange);
        nameField.textProperty().addListener((o, a, b) -> refreshPreview());
        ranged.setOnAction(e -> refreshPreview());
        countsAsAttack.setOnAction(e -> refreshPreview());
        refreshPreview();
    }

    private void watch(IntField... fields) {
        for (IntField f : fields) f.textProperty().addListener((o, a, b) -> refreshPreview());
    }

    private void addEffectRow(Effect preset) {
        EffectEditor row = new EffectEditor(this::refreshPreview);
        if (preset != null) row.load(preset);
        row.setOnRemove(() -> {
            effectRows.remove(row);
            effectsBox.getChildren().remove(row);
            refreshPreview();
        });
        effectRows.add(row);
        effectsBox.getChildren().add(row);
    }

    /** Validates the form and builds the profile. Throws IllegalArgumentException with a readable message. */
    private AttackProfile buildProfile() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Profile name cannot be empty.");
        if (dice.getInt() < 0 || dicePower.getInt() < 0) throw new IllegalArgumentException("Dice and dice power cannot be negative.");
        if (multihit.getInt() < 1) throw new IllegalArgumentException("Multihit must be at least 1.");
        if (area.getInt() < -2) throw new IllegalArgumentException("Area must be -2 (line), -1 (none) or 0+.");
        if (minRange.getInt() < 0 || maxRange.getInt() < minRange.getInt())
            throw new IllegalArgumentException("Range must satisfy 0 <= min <= max.");

        AttackProfile p = new AttackProfile(name, dice.getInt(), dicePower.getInt(), power.getInt(),
                stamina.getInt(), atp.getInt(), ranged.isSelected(), AttackProfile.ProfileTypes.SPECIAL);
        p.AmmoCost = ammoCost.getInt();
        p.Penetration = penetration.getInt();
        p.Strain = strain.getInt();
        p.AreaType = area.getInt();
        p.multihit = multihit.getInt();
        p.MinRange = minRange.getInt();
        p.MaxRange = maxRange.getInt();
        p.countsAsAttack = countsAsAttack.isSelected();
        for (EffectEditor r : effectRows) p.onHitEffects.add(r.toEffect());
        return p;
    }

    private void refreshPreview() {
        try {
            previewLabel.setStyle("-fx-text-fill: black;");
            previewLabel.setText(describe(buildProfile()));
        } catch (IllegalArgumentException ex) {
            previewLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
            previewLabel.setText(ex.getMessage());
        }
    }

    private void save() {
        AttackProfile p;
        try {
            p = buildProfile();
        } catch (IllegalArgumentException ex) {
            FormKit.alert(Alert.AlertType.WARNING, ex.getMessage());
            return;
        }
        File file = CustomContentStore.fileFor(CustomContentStore.SPECIAL_PROFILES_DIR, p.name);
        if (file.exists() && !FormKit.confirm("Overwrite profile", file.getName() + " already exists. Overwrite it?")) return;
        try {
            CustomContentStore.save(file, p);
            FormKit.alert(Alert.AlertType.INFORMATION, "Profile saved to " + file.getPath());
        } catch (IOException ex) {
            FormKit.alert(Alert.AlertType.ERROR, "Failed to save: " + ex.getMessage());
        }
    }

    private void loadExisting() {
        Map<String, AttackProfile> all = CustomContentStore.loadAllSpecialProfiles();
        if (all.isEmpty()) {
            FormKit.alert(Alert.AlertType.INFORMATION, "No saved profiles found.");
            return;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(all.keySet().iterator().next(), all.keySet());
        dialog.setHeaderText("Load a saved profile into the form");
        dialog.showAndWait().ifPresent(key -> setFrom(all.get(key)));
    }

    private void setFrom(AttackProfile p) {
        nameField.setText(p.name);
        dice.setInt(p.Dice);
        dicePower.setInt(p.Dicepower);
        power.setInt(p.Power);
        stamina.setInt(p.Stamina);
        atp.setInt(p.ATP);
        ammoCost.setInt(p.AmmoCost);
        penetration.setInt(p.Penetration);
        strain.setInt(p.Strain);
        area.setInt(p.AreaType);
        multihit.setInt(p.multihit);
        minRange.setInt(p.MinRange);
        maxRange.setInt(p.MaxRange);
        ranged.setSelected(p.isRanged());
        countsAsAttack.setSelected(p.countsAsAttack);
        effectRows.clear();
        effectsBox.getChildren().clear();
        for (Effect e : p.onHitEffects) addEffectRow(e);
        refreshPreview();
    }

    private void reset() {
        setFrom(new AttackProfile("New Profile", 1, 6, 0, 1, 0, false, AttackProfile.ProfileTypes.SPECIAL));
    }

    /** Readable summary, also used by the other creator screens. */
    public static String describe(AttackProfile p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.name).append('\n');
        sb.append("Damage: ").append(p.Dice).append('d').append(p.Dicepower);
        if (p.Power != 0) sb.append(p.Power > 0 ? " +" : " ").append(p.Power);
        sb.append(" + S\n");
        sb.append("Stamina: ").append(p.Stamina).append("   ATP: ").append(p.ATP).append('\n');
        if (p.isRanged() || p.AmmoCost != 0) sb.append("Ammo cost: ").append(p.AmmoCost).append('\n');
        sb.append("Penetration: ").append(p.Penetration).append("   Strain: ").append(p.Strain).append('\n');
        sb.append("Area: ").append(p.AreaType == -2 ? "Line" : p.AreaType >= 0 ? String.valueOf(p.AreaType) : "None").append('\n');
        sb.append("Multihit: ").append(p.multihit).append('\n');
        sb.append("Range: ").append(p.MinRange).append(" - ").append(p.MaxRange)
                .append(p.isRanged() ? " (ranged)" : " (melee)").append('\n');
        sb.append("Counts as attack: ").append(p.countsAsAttack ? "Yes" : "No").append('\n');
        if (p.onHitEffects.isEmpty()) {
            sb.append("On hit: none\n");
        } else {
            for (Effect e : p.onHitEffects) {
                sb.append("On hit: ").append(EffectEditor.describe(e)).append('\n');
            }
        }
        return sb.toString();
    }

    /** One editable on-hit effect. */
    private final class EffectRow extends VBox {
        private final TextField name = new TextField("New Effect");
        private final ComboBox<Effect.EffectEnd> end = new ComboBox<>();
        private final IntField accuracy = new IntField(0);
        private final IntField attackStrength = new IntField(0);
        private final IntField armor = new IntField(0);
        private final IntField reflexes = new IntField(0);
        private final IntField speed = new IntField(0);
        private final IntField maxToughness = new IntField(0);
        private final CheckBox stacks = new CheckBox("Stacks");

        EffectRow() {
            super(5);
            setPadding(new Insets(8));
            setStyle("-fx-border-color: #95a5a6; -fx-border-radius: 6; -fx-background-color: rgba(0,0,0,0.03);");

            end.getItems().setAll(Effect.EffectEnd.values());
            end.setValue(Effect.EffectEnd.ROUND_END);
            stacks.setSelected(true);

            GridPane grid = new GridPane();
            grid.setHgap(8);
            grid.setVgap(5);
            addDelta(grid, 0, 0, "Accuracy", accuracy);
            addDelta(grid, 1, 0, "Attack Str.", attackStrength);
            addDelta(grid, 0, 1, "Armor", armor);
            addDelta(grid, 1, 1, "Reflexes", reflexes);
            addDelta(grid, 0, 2, "Speed", speed);
            addDelta(grid, 1, 2, "Max Tough.", maxToughness);

            BetterButton remove = new BetterButton("Remove effect");
            remove.setDangerStyle();
            remove.setOnAction(e -> {
                effectRows.remove(this);
                effectsBox.getChildren().remove(this);
                refreshPreview();
            });

            getChildren().addAll(FormKit.row("Effect name", name), FormKit.row("Ends", end), grid, stacks, remove);

            name.textProperty().addListener((o, a, b) -> refreshPreview());
            end.valueProperty().addListener((o, a, b) -> refreshPreview());
            stacks.setOnAction(e -> refreshPreview());
            watch(accuracy, attackStrength, armor, reflexes, speed, maxToughness);
        }

        private void addDelta(GridPane grid, int col, int row, String label, IntField field) {
            grid.add(new Label(label), col * 2, row);
            grid.add(field, col * 2 + 1, row);
        }

        Effect toEffect() {
            String n = name.getText().trim();
            if (n.isEmpty()) throw new IllegalArgumentException("Every on-hit effect needs a name.");
            Effect e = new Effect(n, end.getValue())
                    .withAccuracy(accuracy.getInt())
                    .withAttackStrength(attackStrength.getInt())
                    .withArmor(armor.getInt())
                    .withReflexes(reflexes.getInt())
                    .withSpeed(speed.getInt())
                    .withMaxToughness(maxToughness.getInt());
            e.setStacks(stacks.isSelected());
            return e;
        }

        void load(Effect e) {
            name.setText(e.getName());
            end.setValue(e.getEffectEnd());
            accuracy.setInt(e.getDeltaAccuracy());
            attackStrength.setInt(e.getDeltaAttackStrength());
            armor.setInt(e.getDeltaArmor());
            reflexes.setInt(e.getDeltaReflexes());
            speed.setInt(e.getDeltaSpeed());
            maxToughness.setInt(e.getDeltaMaxToughness());
            stacks.setSelected(e.isStacks());
        }
    }
}