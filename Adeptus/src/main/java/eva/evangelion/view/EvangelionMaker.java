package eva.evangelion.view;

import eva.evangelion.units.customisation.Feature;
import eva.evangelion.units.customisation.FeatureFactory;
import eva.evangelion.units.customisation.Upgrade;
import eva.evangelion.units.type.EvangelionIO;
import eva.evangelion.units.type.EvangelionType;
import eva.evangelion.view.UIElements.BetterButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EvangelionMaker {

    private Stage stage;
    private VBox mainLayout;
    private TextField nameField;
    private ComboBox<String> classCombo;
    private List<RollSlot> rollSlots; // 4 roll slots
    private ManualSlot manualSlot;    // 1 manual slot
    private VBox slotsContainer;
    private ScrollPane featureScrollPane;
    private VBox featureDisplayArea;
    private ColorPicker primaryColorPicker;
    private ColorPicker secondaryColorPicker;
    private List<Feature> allFeatures = FeatureFactory.getAllFeatures();
    private Map<Feature.FeatureType, List<Feature>> featuresByType;

    // Track selected features for each slot (including manual)
    private Map<Integer, Feature> selectedFeatureMap = new HashMap<>(); // key: slot index 0-4 (0-3 rolls, 4 manual)
    private Map<Integer, Integer> rollValueMap = new HashMap<>(); // slot index -> roll value (0 for manual)
    private Set<Feature.FeatureType> usedTypes = new HashSet<>();

    private static final int ROLL_SLOTS_COUNT = 4;
    private static final int MANUAL_SLOT_INDEX = 4;

    public EvangelionMaker() {
        featuresByType = allFeatures.stream()
                .collect(Collectors.groupingBy(Feature::getFeatureType));

        stage = new Stage();
        stage.setTitle("Evangelion Maker – Random Roll");

        mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.TOP_LEFT);

        // ---- Name ----
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.getChildren().addAll(new Label("Name:"), nameField = new TextField("My Eva"));
        mainLayout.getChildren().add(nameBox);

        // ---- Game Class ----
        HBox classBox = new HBox(10);
        classBox.setAlignment(Pos.CENTER_LEFT);
        classBox.getChildren().addAll(new Label("Game Class:"), classCombo = new ComboBox<>());
        classCombo.getItems().addAll(EvangelionType.getGameClasses());
        classCombo.setValue(classCombo.getItems().get(0));
        mainLayout.getChildren().add(classBox);

        HBox colorBox = new HBox(20);
        colorBox.setAlignment(Pos.CENTER_LEFT);
        primaryColorPicker = new ColorPicker(Color.WHITE);
        secondaryColorPicker = new ColorPicker(Color.BLACK);
        primaryColorPicker.setMaxWidth(120);
        secondaryColorPicker.setMaxWidth(120);
        colorBox.getChildren().addAll(
                new Label("Primary Color:"), primaryColorPicker,
                new Label("Secondary Color:"), secondaryColorPicker
        );
        mainLayout.getChildren().add(colorBox);


        // ---- Slots (roll buttons + selected feature display) ----
        slotsContainer = new VBox(10);
        slotsContainer.setPadding(new Insets(5));
        mainLayout.getChildren().add(slotsContainer);

        rollSlots = new ArrayList<>();
        for (int i = 0; i < ROLL_SLOTS_COUNT; i++) {
            RollSlot slot = new RollSlot(i);
            rollSlots.add(slot);
            slotsContainer.getChildren().add(slot.getUI());
        }

        // Manual slot (5th slot)
        manualSlot = new ManualSlot(MANUAL_SLOT_INDEX);
        slotsContainer.getChildren().add(manualSlot.getUI());

        // ---- Feature picker area (displays eligible features) ----
        featureDisplayArea = new VBox(5);
        featureDisplayArea.setPadding(new Insets(10));
        featureDisplayArea.setStyle("-fx-border-color: #aaa; -fx-border-width: 1; -fx-border-radius: 5;");
        featureDisplayArea.setVisible(false);

        featureScrollPane = new ScrollPane(featureDisplayArea);
        featureScrollPane.setFitToWidth(true);
        featureScrollPane.setPrefHeight(300);
        featureScrollPane.setVisible(false);
        mainLayout.getChildren().add(featureScrollPane);

        // ---- Action buttons ----
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        BetterButton createBtn = new BetterButton("Create");
        createBtn.setSuccessStyle();
        createBtn.setOnAction(e -> saveEvangelion());
        BetterButton cancelBtn = new BetterButton("Cancel");
        cancelBtn.setDangerStyle();
        cancelBtn.setOnAction(e -> stage.close());
        actionBox.getChildren().addAll(createBtn, cancelBtn);
        mainLayout.getChildren().add(actionBox);

        Scene scene = new Scene(mainLayout, 700, 900);
        stage.setScene(scene);
        stage.show();
    }

    // Abstract base for slots (common behavior)
    private abstract class Slot {
        protected int index;
        protected BetterButton actionButton;
        protected Label selectedLabel;

        protected BetterButton clearButton;
        protected HBox ui;

        Slot(int index) {
            this.index = index;
            selectedLabel = new Label("No Feature selected");
            selectedLabel.setStyle("-fx-text-fill: #888;");

            clearButton = new BetterButton("Clear");
            clearButton.setSecondaryStyle();
            clearButton.setDisable(true);
            clearButton.setOnAction(e -> clearSlot(index));
            ui = new HBox(10);
            ui.setAlignment(Pos.CENTER_LEFT);
            // We'll add children in subclasses
        }

        HBox getUI() { return ui; }

        void setSelected(Feature feature, int roll) {
            selectedLabel.setText(feature.getName() + (roll > 0 ? " (rolled " + roll + ")" : " (Manual)"));
            selectedLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

            clearButton.setDisable(false);
            // Mark button as selected (green) - subclasses implement
            onSelected();
        }

        void clear() {
            selectedLabel.setText("No Feature selected");
            selectedLabel.setStyle("-fx-text-fill: #888;");

            clearButton.setDisable(true);
            selectedFeatureMap.remove(index);
            rollValueMap.remove(index);
            usedTypes.removeIf(t -> {
                // Only remove if no other slot uses this type
                return selectedFeatureMap.values().stream()
                        .noneMatch(f -> f.getFeatureType() == t);
            });
            onClear();
        }

        abstract void onSelected();
        abstract void onClear();
    }

    // Roll slot (4 of these)
    private class RollSlot extends Slot {
        private int rollValue = -1;

        RollSlot(int index) {
            super(index);
            actionButton = new BetterButton("Roll #" + (index + 1));
            actionButton.setPrimaryStyle();
            actionButton.setOnAction(e -> performRoll(index, actionButton));
            ui.getChildren().addAll(actionButton, selectedLabel, clearButton);
        }

        @Override
        void onSelected() {
            actionButton.setText(rollValue + " ✓");
            // Set style to green, and override hover/pressed to stay green
            actionButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
            // Remove hover/pressed effects by setting new event handlers
            actionButton.setOnMouseEntered(e -> {});
            actionButton.setOnMouseExited(e -> {});
            actionButton.setOnMousePressed(e -> {});
            actionButton.setOnMouseReleased(e -> {});
        }

        @Override
        void onClear() {
            actionButton.setText(""+rollValue);
            actionButton.setPrimaryStyle(); // reset to default style with hover/pressed
        }

        void performRoll(int index, BetterButton button) {
            if (selectedFeatureMap.containsKey(index)) {
                // already selected, do nothing
                return;
            }
            int roll = (rollValue == -1) ? (int)(Math.random() * 100) + 1 : rollValue;
            rollValue = roll;
            button.setText(""+roll);

            rollValueMap.put(index, roll);

            // Find eligible Features (filter out types already used)
            List<Feature> eligible = allFeatures.stream()
                    .filter(f -> f.getStartNumber() <= roll && roll <= f.getEndNumber())
                    .filter(f -> !usedTypes.contains(f.getFeatureType()))
                    .collect(Collectors.toList());

            if (eligible.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("No Feature matches this roll (" + roll + "). Please try again or use Manual pick.");
                alert.showAndWait();
                rollValueMap.remove(index);
                button.setText("Roll #" + (index + 1));
                rollValue = -1;
                return;
            }

            // Show eligible features in the picker area
            showFeaturePicker(index, roll, eligible);
        }
    }

    // Manual slot (5th slot)
    private class ManualSlot extends Slot {
        ManualSlot(int index) {
            super(index);
            actionButton = new BetterButton("Manual Pick");
            actionButton.setPrimaryStyle();
            actionButton.setOnAction(e -> openManualPicker());
            ui.getChildren().addAll(actionButton, selectedLabel, clearButton);
        }

        @Override
        void onSelected() {
            actionButton.setText("Manual ✓");
            actionButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
            actionButton.setOnMouseEntered(e -> {});
            actionButton.setOnMouseExited(e -> {});
            actionButton.setOnMousePressed(e -> {});
            actionButton.setOnMouseReleased(e -> {});
        }

        @Override
        void onClear() {
            actionButton.setText("Manual Pick");
            actionButton.setPrimaryStyle();
        }

        void openManualPicker() {
            if (selectedFeatureMap.containsKey(index)) {
                // already selected, do nothing
                return;
            }
            // Find free slots: if manual is already used, then we shouldn't open
            // But we check at the start
            featureScrollPane.setVisible(true);
            featureDisplayArea.setVisible(true);
            featureDisplayArea.getChildren().clear();

            Label header = new Label("Manual Selection – Choose any Feature (type not already used):");
            header.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
            featureDisplayArea.getChildren().add(header);

            // Filter out types already used
            List<Feature> available = allFeatures.stream()
                    .filter(f -> !usedTypes.contains(f.getFeatureType()))
                    .collect(Collectors.toList());

            if (available.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("All Feature types are already used. Please clear a slot first.");
                alert.showAndWait();
                featureScrollPane.setVisible(false);
                featureDisplayArea.setVisible(false);
                return;
            }

            // Group by type
            Map<Feature.FeatureType, List<Feature>> grouped = available.stream()
                    .collect(Collectors.groupingBy(Feature::getFeatureType));

            for (Feature.FeatureType type : Feature.FeatureType.values()) {
                List<Feature> features = grouped.get(type);
                if (features == null || features.isEmpty()) continue;

                Label typeLabel = new Label(type.toString());
                typeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 5 0 2 0;");
                featureDisplayArea.getChildren().add(typeLabel);

                FlowPane flow = new FlowPane(10, 10);
                for (Feature f : features) {
                    BetterButton btn = new BetterButton(f.getName() + " (" + f.getStartNumber() + "-" + f.getEndNumber() + ")");
                    btn.setSecondaryStyle();
                    Tooltip tip = new Tooltip(f.getDescription());
                    Tooltip.install(btn, tip);
                    btn.setOnAction(e -> {
                        // Assign to manual slot (index = MANUAL_SLOT_INDEX)
                        assignFeature(MANUAL_SLOT_INDEX, f, 0);
                        featureScrollPane.setVisible(false);
                        featureDisplayArea.setVisible(false);
                    });
                    flow.getChildren().add(btn);
                }
                featureDisplayArea.getChildren().add(flow);
            }

            BetterButton cancelPick = new BetterButton("Cancel");
            cancelPick.setDangerStyle();
            cancelPick.setOnAction(e -> {
                featureScrollPane.setVisible(false);
                featureDisplayArea.setVisible(false);
            });
            featureDisplayArea.getChildren().add(cancelPick);
        }
    }

    private void showFeaturePicker(int slotIndex, int roll, List<Feature> eligible) {
        featureDisplayArea.getChildren().clear();
        featureScrollPane.setVisible(true);
        featureDisplayArea.setVisible(true);

        Label header = new Label("Slot " + (slotIndex + 1) + " – Roll: " + roll +
                " – Select a Feature from the list:");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        featureDisplayArea.getChildren().add(header);

        // Group by FeatureType
        Map<Feature.FeatureType, List<Feature>> grouped = eligible.stream()
                .collect(Collectors.groupingBy(Feature::getFeatureType));

        for (Feature.FeatureType type : Feature.FeatureType.values()) {
            List<Feature> features = grouped.get(type);
            if (features == null || features.isEmpty()) continue;

            Label typeLabel = new Label(type.toString());
            typeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 5 0 2 0;");
            featureDisplayArea.getChildren().add(typeLabel);

            FlowPane flow = new FlowPane(10, 10);
            for (Feature f : features) {
                BetterButton btn = new BetterButton(f.getName());
                btn.setSecondaryStyle();
                Tooltip tip = new Tooltip(f.getDescription());
                Tooltip.install(btn, tip);
                btn.setOnAction(e -> {
                    assignFeature(slotIndex, f, roll);
                    featureScrollPane.setVisible(false);
                    featureDisplayArea.setVisible(false);
                });
                flow.getChildren().add(btn);
            }
            featureDisplayArea.getChildren().add(flow);
        }

        BetterButton cancelPick = new BetterButton("Cancel");
        cancelPick.setDangerStyle();
        cancelPick.setOnAction(e -> {
            featureScrollPane.setVisible(false);
            featureDisplayArea.setVisible(false);
        });
        featureDisplayArea.getChildren().add(cancelPick);
    }

    private void assignFeature(int slotIndex, Feature feature, int roll) {
        // Check if this type already used
        if (usedTypes.contains(feature.getFeatureType())) {
            // Find which slot uses it
            Optional<Integer> existingSlot = selectedFeatureMap.entrySet().stream()
                    .filter(e -> e.getValue().getFeatureType() == feature.getFeatureType())
                    .map(Map.Entry::getKey)
                    .findFirst();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setContentText("Feature type " + feature.getFeatureType() + " is already used in Slot " +
                    (existingSlot.isPresent() ? (existingSlot.get() + 1) : "?") +
                    ". Do you want to replace it?");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
            // Clear the existing slot
            if (existingSlot.isPresent()) {
                clearSlot(existingSlot.get());
            }
        }

        // If slot already has a feature, clear it first
        if (selectedFeatureMap.containsKey(slotIndex)) {
            clearSlot(slotIndex);
        }

        selectedFeatureMap.put(slotIndex, feature);
        rollValueMap.put(slotIndex, roll);
        usedTypes.add(feature.getFeatureType());

        // Update the appropriate slot UI
        if (slotIndex == MANUAL_SLOT_INDEX) {
            manualSlot.setSelected(feature, roll);
        } else {
            rollSlots.get(slotIndex).setSelected(feature, roll);
        }
        featureScrollPane.setVisible(false);
        featureDisplayArea.setVisible(false);
    }

    private void clearSlot(int index) {
        if (selectedFeatureMap.containsKey(index)) {
            Feature f = selectedFeatureMap.get(index);
            usedTypes.remove(f.getFeatureType());
            // Re-check if other slots use this type
            boolean stillUsed = selectedFeatureMap.values().stream()
                    .anyMatch(other -> other != f && other.getFeatureType() == f.getFeatureType());
            if (stillUsed) {
                usedTypes.add(f.getFeatureType());
            }
        }
        selectedFeatureMap.remove(index);
        rollValueMap.remove(index);
        if (index == MANUAL_SLOT_INDEX) {
            manualSlot.clear();
        } else {
            rollSlots.get(index).clear();
        }
        // Close picker if open
        featureScrollPane.setVisible(false);
        featureDisplayArea.setVisible(false);
    }

    private void saveEvangelion() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("Please enter a name.");
            alert.showAndWait();
            return;
        }


        String primaryHex = toHex(primaryColorPicker.getValue());
        String secondaryHex = toHex(secondaryColorPicker.getValue());


        // Need exactly 5 features (4 rolls + 1 manual)
        if (selectedFeatureMap.size() < 5) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("You must select exactly 5 Features (4 rolls + 1 manual).");
            alert.showAndWait();
            return;
        }

        String gameClass = classCombo.getValue();
        if (gameClass == null) gameClass = "Standard";

        List<Upgrade> upgrades = new ArrayList<>(selectedFeatureMap.values());
        EvangelionType eva = new EvangelionType(name, gameClass, upgrades, primaryHex, secondaryHex);

        try {
            EvangelionIO.save(eva);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Evangelion saved successfully!");
            alert.showAndWait();
            stage.close();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Failed to save: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private String toHex(Color color) {
        if (color == null) return "#ffffff";
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

}