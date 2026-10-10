package eva.evangelion.view.UIElements;

import eva.evangelion.units.battle.Effect;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/** Form for building one Effect by hand. Every delta is typed in, negatives allowed. */
public class EffectEditor extends VBox {
    private final Runnable onChange;

    private final TextField name = new TextField("New Effect");
    private final ComboBox<Effect.EffectEnd> end = new ComboBox<>();
    private final IntField accuracy = new IntField(0);
    private final IntField attackStrength = new IntField(0);
    private final IntField armor = new IntField(0);
    private final IntField reflexes = new IntField(0);
    private final IntField speed = new IntField(0);
    private final IntField maxToughness = new IntField(0);
    private final CheckBox stacks = new CheckBox("Stacks");
    private BetterButton removeBtn;

    /** @param onChange called whenever any field changes; may be null */
    public EffectEditor(Runnable onChange) {
        super(5);
        this.onChange = onChange != null ? onChange : () -> {};
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

        getChildren().addAll(FormKit.row("Effect name", name), FormKit.row("Ends", end), grid, stacks);

        name.textProperty().addListener((o, a, b) -> this.onChange.run());
        end.valueProperty().addListener((o, a, b) -> this.onChange.run());
        stacks.setOnAction(e -> this.onChange.run());
        for (IntField f : List.of(accuracy, attackStrength, armor, reflexes, speed, maxToughness)) {
            f.textProperty().addListener((o, a, b) -> this.onChange.run());
        }
    }

    private void addDelta(GridPane grid, int col, int row, String label, IntField field) {
        grid.add(new Label(label), col * 2, row);
        grid.add(field, col * 2 + 1, row);
    }

    /** Shows a "Remove effect" button that runs the given action. */
    public void setOnRemove(Runnable onRemove) {
        if (removeBtn == null) {
            removeBtn = new BetterButton("Remove effect");
            removeBtn.setDangerStyle();
            getChildren().add(removeBtn);
        }
        removeBtn.setOnAction(e -> onRemove.run());
    }

    /** @throws IllegalArgumentException with a readable message if the form is invalid */
    public Effect toEffect() {
        String n = name.getText().trim();
        if (n.isEmpty()) throw new IllegalArgumentException("Every effect needs a name.");
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

    public void load(Effect e) {
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

    /** One-line readable summary, e.g. "Burn [ROUND_END] Armor -2, Speed +1". */
    public static String describe(Effect e) {
        List<String> parts = new ArrayList<>();
        addPart(parts, "Accuracy", e.getDeltaAccuracy());
        addPart(parts, "Attack Str.", e.getDeltaAttackStrength());
        addPart(parts, "Armor", e.getDeltaArmor());
        addPart(parts, "Reflexes", e.getDeltaReflexes());
        addPart(parts, "Speed", e.getDeltaSpeed());
        addPart(parts, "Max Tough.", e.getDeltaMaxToughness());
        return e.getName() + " [" + e.getEffectEnd() + (e.isStacks() ? "" : ", no stack") + "] "
                + (parts.isEmpty() ? "no stat changes" : String.join(", ", parts));
    }

    private static void addPart(List<String> parts, String label, int value) {
        if (value != 0) parts.add(String.format("%s %+d", label, value));
    }
}