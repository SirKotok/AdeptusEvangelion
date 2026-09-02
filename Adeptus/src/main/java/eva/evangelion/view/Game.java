package eva.evangelion.view;

import eva.evangelion.gameboard.Battlefield;
import eva.evangelion.gameboard.GameBoard;
import eva.evangelion.gameboard.Sector;
import eva.evangelion.gameboard.SectorType;
import eva.evangelion.state.GameState;
import eva.evangelion.state.Queue;
import eva.evangelion.state.QueuePosition;
import eva.evangelion.state.actions.Action;
import eva.evangelion.state.actions.MoveAction;
import eva.evangelion.state.actions.DMChoosePlayerAction;
import eva.evangelion.units.battle.Evangelion;
import eva.evangelion.units.battle.FieldUnit;
import eva.evangelion.units.battle.Unit;
import eva.evangelion.units.type.EvangelionType;
import eva.evangelion.view.UIElements.BetterButton;
import eva.evangelion.util.DirectoryWatcher;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.util.Duration;
import eva.evangelion.state.actions.DMCreateUnitAction;
import eva.evangelion.state.actions.DMDeleteUnitAction;
import eva.evangelion.view.shape.Arrow;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import kotlin.Triple;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {

    // ============================================================
    //   FIELDS
    // ============================================================

    private final Stage stage;
    private final BorderPane root;
    private final StackPane centerStack;
    private final List<Arrow> arrows = new ArrayList<>();
    public final VBox battlefieldTab;
    public final VBox inventoryTab;
    public final VBox chatTab;
    public final VBox dmTab;
    private TextArea chatArea;
    private TextArea dmConsoleOutput;
    private final Battlefield battlefield;
    private Pane viewport;
    private Slider hSlider, vSlider;
    private GridPane grid;
    private Rectangle clip;
    private double boardPixelWidth, boardPixelHeight;
    private Pane boardContainer;
    private final BooleanProperty dmMode = new SimpleBooleanProperty(false);
    private VBox namePanel;
    private VBox turnChoicePanel;   // NEW: panel for "Choose Next Turn" buttons

    // ---- Player & Active Player ----
    private String currentPlayer;
    private String activePlayer;               // local, not stored in GameState
    private int currentActionNumber = 0;

    // ---- Action processing speed ----
    private double actionSpeed = 1.0;           // default 1x speed
    private boolean fastActions = false;        // if true, force instant processing

    // ---- UI top bar ----
    private HBox topBar;
    private HBox confirmContainer;
    private Label activePlayerLabel;
    private Label currentPlayerLabel;

    // ---- GameState persistence ----
    private static final String GAME_STATE_DIR = "Active";
    private static final String GAME_STATE_FILE = "gamestate.ser";

    // ---- Units ----
    private final ObservableList<FieldUnit> UnitList = FXCollections.observableArrayList();
    private final Map<FieldUnit, Node> unitCircles = new HashMap<>();

    // ---- Gameplay ----
    private Queue queue;
    private int CurrentRound = 0;
    private int CurrentTurn = 0;
    private GameState gamestate;
    private FieldUnit selectedUnit = null;

    // ---- Pending action for confirmation ----
    private Action pendingAction = null;

    // ---- Sequential action processing ----
    private final List<Action> pendingActions = new ArrayList<>();
    private boolean isProcessing = false;
    private Timeline processingTimeline = null;

    // ============================================================
    //   CONSTRUCTORS
    // ============================================================


    public Game(Battlefield battlefield, String playerName, double speed, boolean fast, int playernumber) {
        this.battlefield = battlefield;
        this.actionSpeed = speed;
        this.fastActions = fast;

        root = new BorderPane();
        centerStack = new StackPane();

        battlefieldTab = buildBattlefieldTab();
        inventoryTab = buildInventoryTab();
        chatTab = buildChatTab();
        dmTab = buildDmTab();

        centerStack.getChildren().addAll(battlefieldTab, inventoryTab, chatTab, dmTab);
        showTab(battlefieldTab);
        root.setCenter(centerStack);
        root.setBottom(buildButtonBar());

        buildTopBar();
        root.setTop(topBar);

        Scene scene = new Scene(root, 1100, 800);
        stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Adeptus Evangelion");
        stage.show();

        this.currentPlayer = (playerName != null && !playerName.isEmpty()) ? playerName : "Player";
        if (this.currentPlayer.equalsIgnoreCase("DM")) {
            dmMode.set(true);
        }
        updateCurrentPlayerLabel();
        updateActivePlayerDisplay();
        updateTopBarColor();
        initializeGameplay();

        initializeUnits(playernumber);
        setupBoardInteraction();
        updateNamePanels();

        startGameStateWatcher();
        loadInitialGameState();
    }


    // ============================================================
    //   GAME STATE PERSISTENCE
    // ============================================================

    private void startGameStateWatcher() {
        Path dir = Paths.get(GAME_STATE_DIR);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        DirectoryWatcher watcher = new DirectoryWatcher(dir, GAME_STATE_FILE, () -> {
            LogMessage("GameState file changed, reloading...");
            reloadGameState();
        });
        Thread watcherThread = new Thread(watcher);
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    private void loadInitialGameState() {
        Path file = Paths.get(GAME_STATE_DIR, GAME_STATE_FILE);
        if (Files.exists(file)) {
            reloadGameState();
        } else {
            gamestate = new GameState();
            saveGameState();
        }
    }

    private void reloadGameState() {
        Path file = Paths.get(GAME_STATE_DIR, GAME_STATE_FILE);
        if (!Files.exists(file)) return;
        try {
            GameState newState = GameState.loadFromFile(file);
            if (newState != null) {
                processNewState(newState);
            }
        } catch (IOException | ClassNotFoundException e) {
            LogMessage("Failed to load GameState: " + e.getMessage());
        }
    }

    private void saveGameState() {
        Path file = Paths.get(GAME_STATE_DIR, GAME_STATE_FILE);
        try {
            gamestate.saveToFile(file);
        } catch (IOException e) {
            LogMessage("Failed to save GameState: " + e.getMessage());
        }
    }

    // ---- processNewState: collects new actions and starts sequential processing ----
    private void processNewState(GameState state) {
        int newCount = state.getActionCount();
        if (newCount > currentActionNumber) {
            List<Action> actions = state.getActions();
            for (int i = currentActionNumber; i < newCount && i < actions.size(); i++) {
                pendingActions.add(actions.get(i));
            }
            currentActionNumber = newCount;
            LogMessage("Added " + (newCount - currentActionNumber) + " new actions to processing queue.");
        }
        this.gamestate = state;

        if (!isProcessing && !pendingActions.isEmpty()) {
            startProcessing();
        }
    }

    // ============================================================
    //   SEQUENTIAL ACTION PROCESSING
    // ============================================================

    private void startProcessing() {
        if (isProcessing || pendingActions.isEmpty()) return;
        isProcessing = true;
        if (fastActions) {
            for (Action action : pendingActions) {
                processAction(action);
                LogMessage("Processed action (fast): " + action);
            }
            pendingActions.clear();
            isProcessing = false;
            LogMessage("Finished fast processing of all actions.");
            return;
        }

        processingTimeline = new Timeline();
        double cumulativeTime = 0;
        for (int i = 0; i < pendingActions.size(); i++) {
            Action action = pendingActions.get(i);
            double delay = action.getTime() / actionSpeed;
            cumulativeTime += delay;

            KeyFrame kf = new KeyFrame(Duration.seconds(cumulativeTime), e -> {
                processAction(action);
                LogMessage("Processed action: " + action);
            });
            processingTimeline.getKeyFrames().add(kf);
        }

        processingTimeline.setOnFinished(e -> {
            pendingActions.clear();
            isProcessing = false;
            processingTimeline = null;
            LogMessage("Finished processing all actions.");
            if (!pendingActions.isEmpty()) {
                startProcessing();
            }
        });

        processingTimeline.play();
        LogMessage("Started sequential processing of " + pendingActions.size() + " actions.");
    }

    // ---- processAction: handles both MoveAction and DMChoosePlayerAction ----
    private void processAction(Action action) {
        if (action instanceof MoveAction) {
            LogMessage("PROCESSING MOVEMENT ACTION");
            processMoveAction((MoveAction) action);
        } else if (action instanceof DMChoosePlayerAction) {
            LogMessage("PROCESSING DM CHOOSE PLAYER ACTION");
            processDMChoosePlayerAction((DMChoosePlayerAction) action);
        } else if (action instanceof DMCreateUnitAction) {
            processDMCreateUnitAction((DMCreateUnitAction) action);
        } else if (action instanceof DMDeleteUnitAction) {
            processDMDeleteUnitAction((DMDeleteUnitAction) action);
        } else {
            LogMessage("Unknown action type: " + action.getClass().getSimpleName());
        }
        QueuePosition next = queue.currentPosition();
        if (next != null) {
            LogMessage("Setting active player to "+next.getUnitID()+" at process action end, current position in queue is "+queue.getQueue().indexOf(next));
            setActivePlayer(next.getUnitID());
        }
    }

    // ---- Move action execution ----
    private void processMoveAction(MoveAction movement) {
        FieldUnit actor = getUnitFromName(movement.getActor());
        if (actor == null || !actor.isExists()) {
            LogMessage("MoveAction failed: Actor not found: " + movement.getActor());
            return;
        }

        int oldX = actor.getX();
        int oldY = actor.getY();
        int deltaX = movement.getDeltaX();
        int deltaY = movement.getDeltaY();
        int newX = oldX + deltaX;
        int newY = oldY + deltaY;

        if (newX < 0 || newX >= battlefield.sizeX || newY < 0 || newY >= battlefield.sizeY) {
            LogMessage("MoveAction failed: Target (" + newX + "," + newY + ") out of bounds.");
            return;
        }

        FieldUnit occupying = getUnitAt(newX, newY);
        if (occupying != null && occupying != actor) {
            LogMessage("MoveAction failed: Cell occupied by " + occupying.getName());
            return;
        }

        actor.setX(newX);
        actor.setY(newY);

        double visualDuration = fastActions ? 0 : movement.getTime() / actionSpeed;
        processMoveVisuals(actor, oldX, oldY, newX, newY, visualDuration);

        LogMessage("Moved " + actor.getName() + " from (" + oldX + "," + oldY + ") to (" + newX + "," + newY + ")");

        activateQueue(movement);
        setActorToNext(movement);


    }



    protected void setActorToNext(Action action) {
        QueuePosition nextPos = new QueuePosition(action.getActor(), false);
        LogMessage("Adding queue position for " + nextPos.getUnitID()+ " at movement");
        nextPos.setActionNumber(-1);
        queue.addPosition(nextPos, 0);
    }
    protected void activateQueue(Action action){
        QueuePosition current = queue.currentPosition();
        if (current != null) {
            current.setActionNumber(action.getActionNumber());
            LogMessage("Set current position (" + current.getUnitID() + ") action number to " + action.getActionNumber());
        }
    }
    protected void DMQueueInsertion (Action action) {
        setNextTurnToThis();
        QueuePosition current = queue.currentPosition();
        current.setActionNumber(action.getActionNumber());
        LogMessage("Set current position (" + current.getUnitID() + ") action number to " + action.getActionNumber());
        current.setUnitID("DM_INSERTION");
        LogMessage("Renamed ID to "+current.getUnitID()+" at action "+action);
    }
    protected void setNextTurnToThis(){
        LogMessage("Add next turn equal to this");
        QueuePosition next = new QueuePosition(queue.currentPosition().getUnitID(), queue.currentPosition().Reaction);
        next.setActionNumber(-1);
        queue.addPosition(next, 0);
    }

    // ---- DMChoosePlayerAction execution ----

    private void processDMCreateUnitAction(DMCreateUnitAction action) {

        DMQueueInsertion(action);

        String name = action.getUnitName();
        int x = action.getX();
        int y = action.getY();

        if (x < 0 || x >= battlefield.sizeX || y < 0 || y >= battlefield.sizeY) {
            LogMessage("DMCreateUnitAction failed: Coordinates (" + x + "," + y + ") out of bounds.");
            return;
        }

        for (FieldUnit u : UnitList) {
            if (u.isExists() && u.getName().equals(name)) {
                LogMessage("DMCreateUnitAction failed: Unit with name '" + name + "' already exists.");
                return;
            }
        }

        FieldUnit unit = createFieldUnit(name, action.getUnit(), x, y);
        double visualDuration = fastActions ? 0.1 : action.getTime() / actionSpeed;
        animateCreateUnit(unit, visualDuration);
        LogMessage("DMCreateUnitAction: Created unit '" + name + "' at (" + x + "," + y + ").");
    }

    private void processDMDeleteUnitAction(DMDeleteUnitAction action) {
        DMQueueInsertion(action);

        int x = action.getX();
        int y = action.getY();

        FieldUnit unitToDelete = getUnitAt(x, y);
        if (unitToDelete == null) {
            LogMessage("DMDeleteUnitAction failed: No unit at (" + x + "," + y + ").");
            return;
        }

        double visualDuration = fastActions ? 0.1 : action.getTime() / actionSpeed;
        animateDestroyUnit(unitToDelete, visualDuration, () -> {
            removeFieldUnit(unitToDelete);
            LogMessage("DMDeleteUnitAction: Deleted unit '" + unitToDelete.getName() + "' at (" + x + "," + y + ").");
        });

    }

    private void processDMChoosePlayerAction(DMChoosePlayerAction action) {
        QueuePosition current = queue.currentPosition();
        if (current != null) {
            current.setActionNumber(action.getActionNumber());
            LogMessage("Set current position (" + current.getUnitID() + ") action number to " + action.getActionNumber());
            current.setUnitID("DM_Choose");
            LogMessage("Renamed ID to "+current.getUnitID());
            String chosen = action.getChosenPlayer();
            QueuePosition nextPos = new QueuePosition(chosen, false);
            nextPos.setActionNumber(-1);
            LogMessage("Adding queue position for " + nextPos.getUnitID() + " at DMPlayerAction");
            queue.addPosition(nextPos, 0);
        } else {
            LogMessage("DMChoosePlayerAction failed: No current queue position.");
        }
    }

    // ============================================================
    //   UNIT MANAGEMENT
    // ============================================================

    public void initializeUnits(int pn) {

    }

    public FieldUnit getUnitFromName(String s) {
        for (FieldUnit unit : UnitList) {
            if (unit.getName().equals(s)) return unit;
        }
        LogMessage("No Units Found with Name " + s);
        return null;
    }

    public FieldUnit createFieldUnit(String name, Unit unit, int x, int y) {
        FieldUnit newUnit = new FieldUnit(name, unit, x, y);
        UnitList.add(newUnit);
        refreshUnitPositions();
        updateNamePanels();
        return newUnit;
    }

    public void removeFieldUnit(FieldUnit unit) {
        unit.setExists(false);
        refreshUnitPositions();
        updateNamePanels();
    }

    protected void updateNamePanels(){
        if (turnChoicePanel != null) {
            updateTurnChoiceUI(turnChoicePanel);
        }
        if (namePanel != null) {
            updateNameSelectionUI(namePanel);
        }
    }


    private FieldUnit getUnitAt(int x, int y) {
        for (FieldUnit u : UnitList) {
            if (u.isExists() && u.getX() == x && u.getY() == y) {
                return u;
            }
        }
        return null;
    }

    // ============================================================
    //   GAMEPLAY LOGIC
    // ============================================================

    private void initializeGameplay() {
        queue = new Queue();
        QueuePosition initial = new QueuePosition("DM", false);
        initial.setActionNumber(-1);
        queue.addPosition(initial, 0);
        LogMessage("adding queue position for "+initial.getUnitID()+" at initialization");
        setActivePlayer("DM");
        gamestate = new GameState();
    }

    private void SendAction(Action action) {
        gamestate.addAction(action);
        saveGameState();
        LogMessage("Action sent: " + action);
    }

    // ============================================================
    //   BOARD INTERACTIONS
    // ============================================================

    private void setupBoardInteraction() {
        grid.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;

            Node clickedNode = event.getPickResult().getIntersectedNode();
            Integer row = GridPane.getRowIndex(clickedNode);
            Integer col = GridPane.getColumnIndex(clickedNode);
            LogMessage("Clicked at (" + row + "," + col + ")");

            if (row == null || col == null) {
                Node parent = clickedNode.getParent();
                while (parent != null && !(parent instanceof Pane)) {
                    parent = parent.getParent();
                }
                if (parent != null) {
                    row = GridPane.getRowIndex(parent);
                    col = GridPane.getColumnIndex(parent);
                }
                if (row == null || col == null) {
                    return;
                }
            }
            handleSectorClick(col, row);
        });
        grid.setOnContextMenuRequested(event -> event.consume());
    }

    private void handleSectorClick(int x, int y) {
        FieldUnit unitAtLocation = getUnitAt(x, y);
        if (unitAtLocation != null) {
            selectedUnit = unitAtLocation;
            LocalMessage("Selected unit: " + selectedUnit.getName());
            return;
        }
        if (selectedUnit != null) {
            // Check if it's this player's turn
            QueuePosition current = queue.currentPosition();
            if (current == null) {
                LocalMessage("No current turn in queue.");
                return;
            }
            String currentUnitID = current.getUnitID();
            if (!currentPlayer.equalsIgnoreCase("DM") && !currentUnitID.equals(currentPlayer)) {
                LocalMessage("It's not your turn. Current turn: " + currentUnitID);
                return;
            }
            if (!currentPlayer.equalsIgnoreCase("DM") && !selectedUnit.getName().equals(currentPlayer)) {
                LocalMessage("You can only move units that belong to you.");
                return;
            }

            pendingAction = new MoveAction(currentActionNumber, selectedUnit.getName(),
                    x - selectedUnit.getX(), y - selectedUnit.getY());

            createConfirmButton(pendingAction);
            LocalMessage("Move pending. Click Confirm to send.");
        } else {
            LocalMessage("No unit selected. Click on a unit to select it.");
        }
    }

    // ============================================================
    //   CONFIRM BUTTON ON TOP BAR
    // ============================================================

    private void createConfirmButton(Action action) {
        confirmContainer.getChildren().clear();

        BetterButton confirmBtn = new BetterButton("Confirm");
        confirmBtn.setSuccessStyle();
        confirmBtn.setPrefHeight(20);
        confirmBtn.setOnAction(e -> {
            SendAction(action);
            pendingAction = null;
            confirmContainer.getChildren().clear();
        });

        confirmContainer.getChildren().addAll(confirmBtn);
    }

    // ============================================================
    //   CONSOLE COMMANDS (DM / Debug)
    // ============================================================

    private void processConsoleCommand(String command, TextArea output) {
        if (command == null || command.trim().isEmpty()) return;
        String[] parts = command.trim().split(" ");
        String action = parts[0].toLowerCase();

        switch (action) {
            case "create":
                if (parts.length < 4) {
                    output.appendText("Error: Usage: create <name> <x> <y>\n");
                    return;
                }
                StringBuilder nameBuilder = new StringBuilder();
                int x = -1, y = -1;
                try {
                    y = Integer.parseInt(parts[parts.length - 1]);
                    x = Integer.parseInt(parts[parts.length - 2]);
                    for (int j = 1; j < parts.length - 2; j++) {
                        if (j > 1) nameBuilder.append(" ");
                        nameBuilder.append(parts[j]);
                    }
                } catch (NumberFormatException ex) {
                    output.appendText("Error: x and y must be integers.\n");
                    return;
                }
                String name = nameBuilder.toString();
                if (name.isEmpty()) {
                    output.appendText("Error: Name cannot be empty.\n");
                    return;
                }
                if (x < 0 || x >= battlefield.sizeX || y < 0 || y >= battlefield.sizeY) {
                    output.appendText("Error: Coordinates out of bounds (0.." + (battlefield.sizeX-1) + ", 0.." + (battlefield.sizeY-1) + ").\n");
                    return;
                }
                for (FieldUnit u : UnitList) {
                    if (u.isExists() && u.getName().equals(name)) {
                        output.appendText("Error: Unit with name '" + name + "' already exists.\n");
                        return;
                    }
                }
                DMCreateUnitAction createAction = new DMCreateUnitAction(
                        currentActionNumber, "DM", name, x, y
                );
                SendAction(createAction);
                output.appendText("Sent DMCreateUnitAction for '" + name + "' at (" + x + ", " + y + ").\n");
                break;
            case "actions":
                List<Action> allActions = gamestate.getActions();
                if (allActions.isEmpty()) {
                    output.appendText("No actions in GameState.\n");
                } else {
                    StringBuilder sb = new StringBuilder("All actions in GameState:\n");
                    for (int i = 0; i < allActions.size(); i++) {
                        Action a = allActions.get(i);
                        sb.append("  ").append(i).append(": ActionNumber=").append(a.getActionNumber())
                                .append(", Type=").append(a.getClass().getSimpleName());

                        if (a instanceof MoveAction) {
                            MoveAction ma = (MoveAction) a;
                            sb.append(", actor=").append(ma.getActor())
                                    .append(", delta=(").append(ma.getDeltaX()).append(",").append(ma.getDeltaY()).append(")");
                        } else if (a instanceof DMChoosePlayerAction) {
                            DMChoosePlayerAction da = (DMChoosePlayerAction) a;
                            sb.append(", actor=").append(da.getActor())
                                    .append(", chosenPlayer=").append(da.getChosenPlayer());
                        } else if (a instanceof DMCreateUnitAction) {
                            DMCreateUnitAction ca = (DMCreateUnitAction) a;
                            sb.append(", actor=").append(ca.getActor())
                                    .append(", name=").append(ca.getUnitName())
                                    .append(", x=").append(ca.getX())
                                    .append(", y=").append(ca.getY());
                        } else if (a instanceof DMDeleteUnitAction) {
                            DMDeleteUnitAction da = (DMDeleteUnitAction) a;
                            sb.append(", actor=").append(da.getActor())
                                    .append(", x=").append(da.getX())
                                    .append(", y=").append(da.getY());
                        }
                        sb.append("\n");
                    }
                    output.appendText(sb.toString());
                }
                break;
            case "queue":
                StringBuilder queueOutput = new StringBuilder("Queue positions (in order):\n");
                List<QueuePosition> positions = queue.getQueue(); // getQueue() returns a list
                for (int i = 0; i < positions.size(); i++) {
                    QueuePosition pos = positions.get(i);
                    queueOutput.append("  Position ").append(i)
                            .append(": UnitID=").append(pos.getUnitID())
                            .append(", ActionNumber=").append(pos.getActionNumber())
                            .append("\n");
                }
                output.appendText(queueOutput.toString());
                break;
            case "delete":
                if (parts.length < 2) {
                    output.appendText("Error: Usage: delete <name>\n");
                    return;
                }
                StringBuilder delNameBuilder = new StringBuilder();
                for (int j = 1; j < parts.length; j++) {
                    if (j > 1) delNameBuilder.append(" ");
                    delNameBuilder.append(parts[j]);
                }
                String delName = delNameBuilder.toString();
                if (delName.isEmpty()) {
                    output.appendText("Error: Name cannot be empty.\n");
                    return;
                }
                FieldUnit found = null;
                for (FieldUnit u : UnitList) {
                    if (u.isExists() && u.getName().equals(delName)) {
                        found = u;
                        break;
                    }
                }
                if (found == null) {
                    output.appendText("Error: Unit '" + delName + "' not found.\n");
                    return;
                }
                DMDeleteUnitAction deleteAction = new DMDeleteUnitAction(
                        currentActionNumber, "DM", found.getX(), found.getY()
                );
                SendAction(deleteAction);
                output.appendText("Sent DMDeleteUnitAction for '" + delName + "' at (" + found.getX() + ", " + found.getY() + ").\n");
                break;

            case "active":
                if (parts.length < 2) {
                    output.appendText("Error: Usage: active <playerName>\n");
                    return;
                }
                StringBuilder activeNameBuilder = new StringBuilder();
                for (int j = 1; j < parts.length; j++) {
                    if (j > 1) activeNameBuilder.append(" ");
                    activeNameBuilder.append(parts[j]);
                }
                String activeName = activeNameBuilder.toString();
                if (activeName.isEmpty()) {
                    output.appendText("Error: Player name cannot be empty.\n");
                    return;
                }
                // Instead of directly setting, create a DMChoosePlayerAction
                DMChoosePlayerAction dmAction = new DMChoosePlayerAction(currentActionNumber, "DM", activeName);
                SendAction(dmAction);
                output.appendText("DM chose next player: " + activeName + " and created dmAction "+dmAction.getActionNumber());
                break;

            case "progress":
                output.appendText("Manually reloading GameState and processing new actions...\n");
                reloadGameState();
                break;

            case "help":
                output.appendText("Available commands:\n");
                output.appendText("  create <name> <x> <y>  – creates a new unit\n");
                output.appendText("  delete <name>          – deletes a unit\n");
                output.appendText("  active <playerName>    – DM chooses next player\n");
                output.appendText("  progress               – reloads GameState and processes new actions\n");
                output.appendText("  queue                  – shows current queue positions\n");
                output.appendText("  actions                – shows all actions and their numbers in gamestate\n");
                output.appendText("  help                   – shows this help\n");
                break;

            default:
                output.appendText("Unknown command: " + action + ". Type 'help' for a list.\n");
                break;
        }
    }

    private void processDMCommand(String command, TextArea output) {
        processConsoleCommand(command, output);
    }

    // ---- Setters for player fields ----
    private void setActivePlayer(String player) {
        if (player == null || player.isEmpty()) return;
        this.activePlayer = player;
        updateActivePlayerDisplay();
        LogMessage("Active player changed to: " + player);
    }

    public void setCurrentPlayer(String name) {
        if (name == null || name.isEmpty()) return;
        this.currentPlayer = name;
        if (this.currentPlayer.equalsIgnoreCase("DM")) {
            dmMode.set(true);
        }
        updateCurrentPlayerLabel();
        updateActivePlayerDisplay();
        LogMessage("Current player set to: " + name);
    }

    // ============================================================
    //   UI HELPERS (messages)
    // ============================================================

    public void LocalMessage(String msg) {
        System.out.println("Local " + msg);
        if (chatArea != null) {
            chatArea.appendText(msg + "\n");
        }
    }

    public void GlobalMessage(String msg) {
        System.out.println("Global " + msg);
        if (chatArea != null) {
            chatArea.appendText(msg + "\n");
        }
    }



    // ============================================================
    //   UI CONSTRUCTION (all UI elements at the bottom)
    // ============================================================

    // ---- Top Bar ----
    private void buildTopBar() {
        topBar = new HBox(15);
        topBar.setPadding(new Insets(5, 15, 5, 15));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #444444;");

        Label activeLabel = new Label("Active Player: ");
        activeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        activePlayerLabel = new Label("None");
        activePlayerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        currentPlayerLabel = new Label();
        currentPlayerLabel.setStyle("-fx-text-fill: lightgray;");

        confirmContainer = new HBox(10);
        confirmContainer.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(confirmContainer, Priority.ALWAYS);

        topBar.getChildren().addAll(activeLabel, activePlayerLabel, currentPlayerLabel, confirmContainer);
    }

    private void updateTopBarColor() {
        if (topBar == null) return;
        if (activePlayer != null && activePlayer.equals(currentPlayer)) {
            topBar.setStyle("-fx-background-color: #2e7d32;");
        } else {
            topBar.setStyle("-fx-background-color: #444444;");
        }
    }

    private void updateActivePlayerDisplay() {
        if (activePlayerLabel == null) return;
        if (activePlayer == null || activePlayer.isEmpty()) {
            activePlayerLabel.setText("None");
        } else {
            activePlayerLabel.setText(activePlayer);
        }
        updateTopBarColor();
    }

    private void updateCurrentPlayerLabel() {
        if (currentPlayerLabel != null) {
            currentPlayerLabel.setText(" (You: " + currentPlayer + ")");
        }
        if (namePanel != null) {
            TextField nameField = findNameField(namePanel);
            if (nameField != null) {
                nameField.setText(currentPlayer);
            }
        }
        updateTopBarColor();
    }

    // ---- Build DM Tab (with new "Choose Next Turn" panel) ----
    // ---- New field for message log ----
    private TextArea msgLogOutput;   // right panel

    // ---- Modified buildDmTab() ----
    private VBox buildDmTab() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(10));
        wrapper.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("DM Screen");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        wrapper.getChildren().add(title);

        // ---- Name panel (unchanged) ----
        namePanel = new VBox(10);
        namePanel.setPadding(new Insets(10));
        namePanel.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #888; -fx-border-width: 1;");
        ScrollPane nameScrollPane = new ScrollPane(namePanel);
        nameScrollPane.setFitToWidth(true);
        nameScrollPane.setPrefHeight(150);
        nameScrollPane.setMaxHeight(200);
        nameScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        nameScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // ---- Turn choice panel (unchanged) ----
        Label turnLabel = new Label("Choose Next Turn");
        turnLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        turnChoicePanel = new VBox(10);
        turnChoicePanel.setPadding(new Insets(10));
        turnChoicePanel.setStyle("-fx-background-color: #d0e0f0; -fx-border-color: #888; -fx-border-width: 1;");
        ScrollPane turnScrollPane = new ScrollPane(turnChoicePanel);
        turnScrollPane.setFitToWidth(true);
        turnScrollPane.setPrefHeight(150);
        turnScrollPane.setMaxHeight(200);
        turnScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        turnScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        wrapper.getChildren().addAll(nameScrollPane, turnLabel, turnScrollPane);

        // ---- NEW: Split console area into two panels ----
        HBox consoleSplit = new HBox(10);
        consoleSplit.setPadding(new Insets(5, 0, 5, 0));
        consoleSplit.setAlignment(Pos.TOP_LEFT);

        // Left: Command Console
        VBox leftConsole = new VBox(5);
        leftConsole.setAlignment(Pos.TOP_LEFT);
        leftConsole.setPrefWidth(400);
        HBox.setHgrow(leftConsole, Priority.ALWAYS);

        Label cmdLabel = new Label("Command Console");
        cmdLabel.setStyle("-fx-font-weight: bold;");
        dmConsoleOutput = new TextArea();
        dmConsoleOutput.setEditable(false);
        dmConsoleOutput.setWrapText(true);
        dmConsoleOutput.setPrefRowCount(12);
        dmConsoleOutput.setText("Command console ready.\n");
        VBox.setVgrow(dmConsoleOutput, Priority.ALWAYS);

        TextField commandInput = new TextField();
        commandInput.setPromptText("Enter DM command...");

        BetterButton sendCmdBtn = new BetterButton("Send");
        sendCmdBtn.setPrimaryStyle();
        sendCmdBtn.setOnAction(e -> {
            String cmd = commandInput.getText().trim();
            if (!cmd.isEmpty()) {
                dmConsoleOutput.appendText("> " + cmd + "\n");
                processDMCommand(cmd, dmConsoleOutput);  // output goes to command console
                commandInput.clear();
            }
        });
        commandInput.setOnAction(e -> sendCmdBtn.fire());

        HBox inputRow = new HBox(10, commandInput, sendCmdBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        leftConsole.getChildren().addAll(cmdLabel, dmConsoleOutput, inputRow);

        // Right: Message Log
        VBox rightConsole = new VBox(5);
        rightConsole.setAlignment(Pos.TOP_LEFT);
        rightConsole.setPrefWidth(400);
        HBox.setHgrow(rightConsole, Priority.ALWAYS);

        Label msgLabel = new Label("Message Log");
        msgLabel.setStyle("-fx-font-weight: bold;");
        msgLogOutput = new TextArea();
        msgLogOutput.setEditable(false);
        msgLogOutput.setWrapText(true);
        msgLogOutput.setPrefRowCount(12);
        msgLogOutput.setText("Message log ready.\n");
        VBox.setVgrow(msgLogOutput, Priority.ALWAYS);

        rightConsole.getChildren().addAll(msgLabel, msgLogOutput);

        consoleSplit.getChildren().addAll(leftConsole, rightConsole);
        wrapper.getChildren().add(consoleSplit);

        // ---- Set Current Player button (below the split) ----
        BetterButton setCurrentBtn = new BetterButton("Set as Current Player");
        setCurrentBtn.setPrimaryStyle();
        setCurrentBtn.setOnAction(e -> {
            TextField nameField = findNameField(namePanel);
            if (nameField != null) {
                String name = nameField.getText().trim();
                if (!name.isEmpty()) {
                    setCurrentPlayer(name);
                    dmConsoleOutput.appendText("Current player set to: " + name + "\n");
                } else {
                    dmConsoleOutput.appendText("Please enter a name.\n");
                }
            }
        });

        HBox setRow = new HBox(10, setCurrentBtn);
        setRow.setAlignment(Pos.CENTER_LEFT);
        wrapper.getChildren().add(setRow);

        // ---- Listeners for unit list changes ----
        UnitList.addListener((ListChangeListener<FieldUnit>) change -> {
            if (namePanel != null) {
                updateNameSelectionUI(namePanel);
            }
            if (turnChoicePanel != null) {
                updateTurnChoiceUI(turnChoicePanel);
            }
        });

        return wrapper;
    }

    // ---- Updated LogMessage to use the message log panel ----
    public void LogMessage(String msg) {
        System.out.println("Log " + msg);
        if (msgLogOutput != null) {
            msgLogOutput.appendText(msg + "\n");
        }
    }

    // ---- Helper: find TextField in a VBox (used for namePanel) ----
    private TextField findNameField(VBox panel) {
        for (Node node : panel.getChildren()) {
            if (node instanceof HBox) {
                HBox row = (HBox) node;
                for (Node child : row.getChildren()) {
                    if (child instanceof TextField) {
                        return (TextField) child;
                    }
                }
            }
        }
        return null;
    }

    // ---- Update name selection UI (existing) ----
    private void updateNameSelectionUI(VBox panel) {
        panel.getChildren().clear();

        TextField nameField = new TextField();
        nameField.setPromptText("Enter your name");
        nameField.setPrefWidth(150);
        nameField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        HBox nameRow = new HBox(5, new Label("Player:"), nameField);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        nameField.setText(currentPlayer);

        VBox buttonList = new VBox(5);
        buttonList.setAlignment(Pos.CENTER_LEFT);

        for (FieldUnit unit : UnitList) {
            if (unit.isExists()) {
                BetterButton btn = new BetterButton(unit.getName());
                btn.setPrimaryStyle();
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setOnAction(e -> setCurrentPlayer(unit.getName()));
                buttonList.getChildren().add(btn);
            }
        }

        panel.getChildren().addAll(nameRow, buttonList);
    }

    // ---- New: Update turn choice UI ----
    private void updateTurnChoiceUI(VBox panel) {
        panel.getChildren().clear();

        Label info = new Label("Click a unit to make them the next player:");
        info.setStyle("-fx-font-size: 12px;");
        panel.getChildren().add(info);

        VBox buttonList = new VBox(5);
        buttonList.setAlignment(Pos.CENTER_LEFT);

        for (FieldUnit unit : UnitList) {
            if (unit.isExists()) {
                BetterButton btn = new BetterButton(unit.getName());
                btn.setPrimaryStyle();
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setOnAction(e -> {
                    // Create a DMChoosePlayerAction for this unit
                    DMChoosePlayerAction action = new DMChoosePlayerAction(currentActionNumber, "DM", unit.getName());
                    SendAction(action);
                    LogMessage("DM chose " + unit.getName() + " as the next player. AN = "+currentActionNumber);
                });
                buttonList.getChildren().add(btn);
            }
        }

        panel.getChildren().add(buttonList);
    }

    // ---- Other Tabs (unchanged) ----
    private VBox buildInventoryTab() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(10));
        wrapper.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Inventory");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new BetterButton("Progressive Knife") {{ setPrimaryStyle(); }},
                new BetterButton("Pallet Rifle") {{ setPrimaryStyle(); }},
                new BetterButton("Shield") {{ setPrimaryStyle(); }}
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefViewportWidth(800);
        scrollPane.setPrefViewportHeight(600);
        scrollPane.setStyle("-fx-background: #f0f0f0; -fx-background-color: #f0f0f0;");

        wrapper.getChildren().addAll(title, scrollPane);
        return wrapper;
    }

    private VBox buildChatTab() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(10));
        wrapper.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Chat");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefRowCount(20);
        chatArea.setWrapText(true);
        chatArea.setText("Welcome to the game!\n");

        TextField inputField = new TextField();
        inputField.setPromptText("Type a message...");

        BetterButton sendBtn = new BetterButton("Send");
        sendBtn.setPrimaryStyle();
        sendBtn.setOnAction(e -> {
            String msg = inputField.getText().trim();
            if (!msg.isEmpty()) {
                if (msg.equalsIgnoreCase("/DM")) {
                    dmMode.set(!dmMode.get());
                    chatArea.appendText("[DM Mode " + (dmMode.get() ? "ON" : "OFF") + "]\n");
                } else {
                    chatArea.appendText("You: " + msg + "\n");
                }
                inputField.clear();
            }
        });

        inputField.setOnAction(e -> sendBtn.fire());

        HBox inputRow = new HBox(10, inputField, sendBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        wrapper.getChildren().addAll(title, chatArea, inputRow);
        return wrapper;
    }
    private HBox bottomPanel;
    private double bottomPanelHeight = 150;
    // ---- Battlefield tab ----
    private VBox buildBattlefieldTab() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(10));
        wrapper.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Battlefield");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        wrapper.getChildren().add(title);

        GameBoard board = createGameBoardFromBattlefield(battlefield);
        if (board == null) {
            wrapper.getChildren().add(new Label("No battlefield data available."));
            return wrapper;
        }

        // --- Existing viewport setup ---
        viewport = new Pane();
        double viewWidth = Math.min(battlefield.sizeX * 20, 800);
        double viewHeight = Math.min(battlefield.sizeY * 20, 600);
        viewport.setPrefSize(viewWidth, viewHeight);
        viewport.setMaxSize(viewWidth, viewHeight);
        wrapper.setStyle(
                "-fx-background-color: lightgray; " +
                        "-fx-border-color: #2c3e50; " +
                        "-fx-border-width: 4px; " +
                        "-fx-border-style: solid;"
        );

        boardContainer = new Pane();
        grid = board.Board;
        grid.setDisable(false);
        boardContainer.getChildren().add(grid);
        viewport.getChildren().add(boardContainer);

        hSlider = new Slider();
        vSlider = new Slider();
        hSlider.setOrientation(Orientation.HORIZONTAL);
        vSlider.setOrientation(Orientation.VERTICAL);

        BorderPane scrollContainer = new BorderPane();
        scrollContainer.setCenter(viewport);
        BorderPane.setAlignment(viewport, Pos.CENTER);
        scrollContainer.setBottom(hSlider);
        scrollContainer.setRight(vSlider);

        boardPixelWidth = board.boardwidth * 20;
        boardPixelHeight = board.boardheight * 20;

        clip = new Rectangle(viewWidth, viewHeight);
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);

        updateSliderRanges();

        // --- Mouse dragging logic (unchanged) ---
        viewport.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            viewport.setCursor(Cursor.DEFAULT);
        });
        viewport.setOnMouseExited(e -> viewport.setCursor(Cursor.DEFAULT));
        viewport.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.SECONDARY) return;
            viewport.setCursor(Cursor.CLOSED_HAND);
            viewport.setUserData(new double[]{
                    e.getSceneX(),
                    e.getSceneY(),
                    hSlider.getValue(),
                    vSlider.getValue()
            });
        });
        viewport.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.SECONDARY) return;
            viewport.setCursor(Cursor.OPEN_HAND);
            viewport.setUserData(null);
        });
        viewport.setOnMouseDragged(e -> {
            if (e.getButton() != MouseButton.SECONDARY) return;
            double[] data = (double[]) viewport.getUserData();
            if (data == null) return;
            double startX = data[0];
            double startY = data[1];
            double startH = data[2];
            double startV = data[3];

            double deltaX = e.getSceneX() - startX;
            double deltaY = e.getSceneY() - startY;

            double newH = startH - deltaX;
            double newV = startV + deltaY;

            hSlider.setValue(newH);
            vSlider.setValue(newV);
        });

        // --- NEW: bottom menu panel ---
        bottomPanel = new HBox(15);
        bottomPanel.setPadding(new Insets(10, 5, 5, 5));
        bottomPanel.setAlignment(Pos.CENTER_LEFT);
        bottomPanel.setStyle("-fx-background-color: #3a3a3a; -fx-border-color: #555; -fx-border-width: 1 0 0 0;");
        bottomPanel.setMinHeight(0);               // allow shrinking
        bottomPanel.setPrefHeight(bottomPanelHeight);
        bottomPanel.setMaxHeight(bottomPanelHeight);

        // Left: vertical buttons
        VBox buttonBox = new VBox(8);
        buttonBox.setAlignment(Pos.TOP_LEFT);
        buttonBox.setPadding(new Insets(5));
        buttonBox.setMinHeight(0);

        BetterButton moveBtn = new BetterButton("Move");
        moveBtn.setPrimaryStyle();
        moveBtn.setPrefWidth(100);
        BetterButton attackBtn = new BetterButton("Attack");
        attackBtn.setPrimaryStyle();
        attackBtn.setPrefWidth(100);
        BetterButton atPowerBtn = new BetterButton("ATPowers");
        atPowerBtn.setPrimaryStyle();
        atPowerBtn.setPrefWidth(100);
        BetterButton otherBtn = new BetterButton("Other");
        otherBtn.setPrimaryStyle();
        otherBtn.setPrefWidth(100);

        buttonBox.getChildren().addAll(moveBtn, attackBtn, atPowerBtn, otherBtn);

        // Right: content stack
        StackPane contentStack = new StackPane();
        contentStack.setPadding(new Insets(5));
        HBox.setHgrow(contentStack, Priority.ALWAYS);
        contentStack.setMinHeight(0);
        // --- Section panes ---
        // Move section
        VBox moveContent = new VBox(8);
        moveContent.setPadding(new Insets(10));
        moveContent.setStyle("-fx-background-color: #FFFACD; -fx-border-color: #ccc;");
        moveContent.setAlignment(Pos.TOP_LEFT);
        Label moveLabel = new Label("Move Actions");
        moveLabel.setStyle("-fx-font-weight: bold;");
        BetterButton runBtn = new BetterButton("Run");
        runBtn.setPrimaryStyle();
        BetterButton maneuverBtn = new BetterButton("Maneuver");
        maneuverBtn.setPrimaryStyle();
        moveContent.getChildren().addAll(moveLabel, runBtn, maneuverBtn);

        // Attack section
        VBox attackContent = new VBox(8);
        attackContent.setPadding(new Insets(10));
        attackContent.setStyle("-fx-background-color: #FFCCCC; -fx-border-color: #ccc;");
        attackContent.setAlignment(Pos.TOP_LEFT);
        Label attackLabel = new Label("Attack Actions");
        attackLabel.setStyle("-fx-font-weight: bold;");
        attackContent.getChildren().add(attackLabel);  // no buttons yet

        // AT Power section
        VBox atPowerContent = new VBox(8);
        atPowerContent.setPadding(new Insets(10));
        atPowerContent.setStyle("-fx-background-color: #CCE5FF; -fx-border-color: #ccc;");
        atPowerContent.setAlignment(Pos.TOP_LEFT);
        Label atPowerLabel = new Label("AT Powers");
        atPowerLabel.setStyle("-fx-font-weight: bold;");
        atPowerContent.getChildren().add(atPowerLabel);

        // Other section
        VBox otherContent = new VBox(8);
        otherContent.setPadding(new Insets(10));
        otherContent.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc;");
        otherContent.setAlignment(Pos.TOP_LEFT);
        Label otherLabel = new Label("Other Actions");
        otherLabel.setStyle("-fx-font-weight: bold;");
        otherContent.getChildren().add(otherLabel);

        // Add all to stack, initially only Move visible
        contentStack.getChildren().addAll(moveContent, attackContent, atPowerContent, otherContent);
        moveContent.setVisible(true);
        attackContent.setVisible(false);
        atPowerContent.setVisible(false);
        otherContent.setVisible(false);

        // --- Button actions ---
        moveBtn.setOnAction(e -> {
            moveContent.setVisible(true);
            attackContent.setVisible(false);
            atPowerContent.setVisible(false);
            otherContent.setVisible(false);
        });
        attackBtn.setOnAction(e -> {
            moveContent.setVisible(false);
            attackContent.setVisible(true);
            atPowerContent.setVisible(false);
            otherContent.setVisible(false);
        });
        atPowerBtn.setOnAction(e -> {
            moveContent.setVisible(false);
            attackContent.setVisible(false);
            atPowerContent.setVisible(true);
            otherContent.setVisible(false);
        });
        otherBtn.setOnAction(e -> {
            moveContent.setVisible(false);
            attackContent.setVisible(false);
            atPowerContent.setVisible(false);
            otherContent.setVisible(true);
        });

        bottomPanel.getChildren().addAll(buttonBox, contentStack);

        // --- Main layout: title + scrollContainer + bottomPanel ---
        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(scrollContainer);
        mainPane.setBottom(bottomPanel);
        VBox.setVgrow(mainPane, Priority.ALWAYS);

        wrapper.getChildren().add(mainPane);
        return wrapper;
    }

    // ---- Bottom Button Bar (unchanged) ----
    private HBox buildButtonBar() {
        HBox bar = new HBox(15);
        bar.setPadding(new Insets(10));
        bar.setAlignment(Pos.CENTER);
        bar.setStyle("-fx-background-color: #2c3e50;");

        BetterButton bfBtn = new BetterButton("Battlefield");
        bfBtn.setPrimaryStyle();
        bfBtn.setOnAction(e -> showTab(battlefieldTab));

        BetterButton invBtn = new BetterButton("Inventory");
        invBtn.setPrimaryStyle();
        invBtn.setOnAction(e -> showTab(inventoryTab));

        BetterButton chatBtn = new BetterButton("Chat");
        chatBtn.setPrimaryStyle();
        chatBtn.setOnAction(e -> showTab(chatTab));

        BetterButton dmBtn = new BetterButton("DM Screen");
        dmBtn.setPrimaryStyle();
        dmBtn.setOnAction(e -> showTab(dmTab));
        dmBtn.disableProperty().bind(dmMode.not());

        BetterButton optionsBtn = new BetterButton("Options");
        optionsBtn.setPrimaryStyle();
        optionsBtn.setOnAction(e -> showOptionsDialog());

        BetterButton exitBtn = new BetterButton("Exit");
        exitBtn.setDangerStyle();
        exitBtn.setOnAction(e -> stage.close());

        bar.getChildren().addAll(bfBtn, invBtn, chatBtn, dmBtn, optionsBtn, exitBtn);
        return bar;
    }

    // ---- Options Dialog (unchanged) ----
    private void showOptionsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Options");
        dialog.setHeaderText("Action Processing Speed & Viewport Settings");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // ---- Speed controls ----
        Label speedLabel = new Label("Speed: " + String.format("%.1f", actionSpeed) + "x");
        Slider speedSlider = new Slider(0.1, 2.0, actionSpeed);
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
        fastCheck.setSelected(fastActions);
        fastCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            speedSlider.setDisable(newVal);
        });
        speedSlider.setDisable(fastActions);

        // ---- Viewport resize controls ----
        Label viewportLabel = new Label("Viewport Size (pixels):");
        viewportLabel.setStyle("-fx-font-weight: bold;");

        TextField widthField = new TextField(String.valueOf((int) viewport.getPrefWidth()));
        TextField heightField = new TextField(String.valueOf((int) viewport.getPrefHeight()));
        widthField.setPromptText("Width");
        heightField.setPromptText("Height");

        GridPane resizeGrid = new GridPane();
        resizeGrid.setHgap(10);
        resizeGrid.setVgap(10);
        resizeGrid.add(new Label("Width:"), 0, 0);
        resizeGrid.add(widthField, 1, 0);
        resizeGrid.add(new Label("Height:"), 0, 1);
        resizeGrid.add(heightField, 1, 1);

        BetterButton applyResizeBtn = new BetterButton("Apply Resize");
        applyResizeBtn.setPrimaryStyle();
        applyResizeBtn.setOnAction(e -> {
            try {
                double w = Double.parseDouble(widthField.getText());
                double h = Double.parseDouble(heightField.getText());
                resizeViewport(w, h);
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter valid numbers.");
                alert.showAndWait();
            }
        });

        HBox resizeBtnBox = new HBox(applyResizeBtn);
        resizeBtnBox.setAlignment(Pos.CENTER_LEFT);

        // ---- Bottom panel height control ----
        Label heightLabel = new Label("Bottom Panel Height: " + (int)bottomPanelHeight + " px");
        Slider heightSlider = new Slider(80, 300, bottomPanelHeight);
        heightSlider.setBlockIncrement(10);
        heightSlider.setMajorTickUnit(50);
        heightSlider.setMinorTickCount(4);
        heightSlider.setShowTickLabels(true);
        heightSlider.setShowTickMarks(true);
        heightSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double val = newVal.doubleValue();
            bottomPanelHeight = val;
            heightLabel.setText("Bottom Panel Height: " + (int)val + " px");
            if (bottomPanel != null) {
                bottomPanel.setPrefHeight(val);
                bottomPanel.setMaxHeight(val);
            }
        });

        // ---- Assemble all ----
        content.getChildren().addAll(
                speedLabel, speedSlider, fastCheck,
                new Separator(),
                viewportLabel,
                resizeGrid, resizeBtnBox,
                new Separator(),
                heightLabel, heightSlider
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                this.actionSpeed = speedSlider.getValue();
                this.fastActions = fastCheck.isSelected();
                LogMessage("Speed set to " + actionSpeed + "x, FastActions: " + fastActions);
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showTab(VBox tab) {
        for (Node node : centerStack.getChildren()) {
            node.setVisible(node == tab);
        }
    }

    // ---- Helper: Board creation ----
    private GameBoard createGameBoardFromBattlefield(Battlefield bf) {
        if (bf == null) return null;
        int width = bf.sizeX > 0 ? bf.sizeX : 30;
        int height = bf.sizeY > 0 ? bf.sizeY : 30;

        GridPane grid = new GridPane();
        BorderPane container = new BorderPane();
        GameBoard board = new GameBoard(grid, container, width, height);

        for (Sector sector : board.sectors) {
            sector.setType(SectorType.Blank);
        }

        if (bf.SpecialTiles != null) {
            for (Triple<Integer, Integer, SectorType> triple : bf.SpecialTiles) {
                int x = triple.getFirst();
                int y = triple.getSecond();
                Sector sector = board.getSector(x, y);
                if (sector != null) {
                    sector.setType(triple.getThird());
                }
            }
        }

        board.UpdateBoardColors();
        return board;
    }

    private void updateSliderRanges() {
        double viewWidth = viewport.getPrefWidth();
        double viewHeight = viewport.getPrefHeight();

        double hMax = Math.max(0, boardPixelWidth - viewWidth);
        double vMax = Math.max(0, boardPixelHeight - viewHeight);

        hSlider.setMin(0);
        hSlider.setMax(hMax);
        vSlider.setMin(-vMax);
        vSlider.setMax(0);

        hSlider.setDisable(hMax == 0);
        vSlider.setDisable(vMax == 0);
        hSlider.setVisible(hMax > 0);
        vSlider.setVisible(vMax > 0);

        // Unbind first to avoid conflicts
        boardContainer.layoutXProperty().unbind();
        boardContainer.layoutYProperty().unbind();

        if (hMax > 0) {
            // If there is scrollable area, bind to slider
            boardContainer.layoutXProperty().bind(hSlider.valueProperty().multiply(-1));
            // Ensure the slider value is within bounds (clamp)
            double val = hSlider.getValue();
            if (val < 0) val = 0;
            if (val > hMax) val = hMax;
            hSlider.setValue(val);
        } else {
            // Board fits horizontally – center it
            boardContainer.setLayoutX((viewWidth - boardPixelWidth) / 2.0);
            // Set slider to 0 (will be disabled/ hidden)
            hSlider.setValue(0);
        }

        if (vMax > 0) {
            boardContainer.layoutYProperty().bind(vSlider.valueProperty());
            double val = vSlider.getValue();
            if (val < -vMax) val = -vMax;
            if (val > 0) val = 0;
            vSlider.setValue(val);
        } else {
            boardContainer.setLayoutY((viewHeight - boardPixelHeight) / 2.0);
            vSlider.setValue(0);
        }
    }

    private void resizeViewport(double newWidth, double newHeight) {
        newWidth = Math.max(newWidth, 100);
        newHeight = Math.max(newHeight, 100);
        viewport.setPrefSize(newWidth, newHeight);
        viewport.setMaxSize(newWidth, newHeight);
        updateSliderRanges();
    }

    private void showResizeDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Viewport Size");
        dialog.setHeaderText("Set the visible area size (in pixels)");

        TextField widthField = new TextField(String.valueOf((int) viewport.getPrefWidth()));
        TextField heightField = new TextField(String.valueOf((int) viewport.getPrefHeight()));
        widthField.setPromptText("Width");
        heightField.setPromptText("Height");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));
        gridPane.add(new Label("Width:"), 0, 0);
        gridPane.add(widthField, 1, 0);
        gridPane.add(new Label("Height:"), 0, 1);
        gridPane.add(heightField, 1, 1);

        dialog.getDialogPane().setContent(gridPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    double w = Double.parseDouble(widthField.getText());
                    double h = Double.parseDouble(heightField.getText());
                    resizeViewport(w, h);
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter valid numbers.");
                    alert.showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ---- Visual helpers (UI) ----
    public void refreshUnitPositions() {
        List<Node> toRemove = new ArrayList<>();
        for (Node node : grid.getChildren()) {
            if (node.getClass().getSimpleName().equals("Circle")) {
                boolean found = false;
                for (FieldUnit u : UnitList) {
                    if (u.isExists() && unitCircles.get(u) == node) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    toRemove.add(node);
                }
            }
        }
        LogMessage("Removing " + toRemove.size() + " circles");
        grid.getChildren().removeAll(toRemove);

        for (FieldUnit u : UnitList) {
            if (!u.isExists()) continue;
            Node circle = unitCircles.get(u);
            if (circle == null) {
                circle = u.getUnitCircle();
                unitCircles.put(u, circle);
            }
            int x = u.getX();
            int y = u.getY();
            if (x < 0 || x >= battlefield.sizeX || y < 0 || y >= battlefield.sizeY) continue;
            GridPane.setRowIndex(circle, y);
            GridPane.setColumnIndex(circle, x);
            GridPane.setHalignment(circle, HPos.CENTER);
            GridPane.setValignment(circle, VPos.CENTER);
            if (!grid.getChildren().contains(circle)) {
                grid.getChildren().add(circle);
            }
        }
    }

    public void processMoveVisuals(FieldUnit unit, int Sx, int Sy, int Ex, int Ey, double duration) {
        if (duration <= 0) {
            Node circle = unitCircles.get(unit);
            if (circle != null) {
                GridPane.setRowIndex(circle, Ey);
                GridPane.setColumnIndex(circle, Ex);
                circle.setTranslateX(0);
                circle.setTranslateY(0);
            }
            return;
        }

        drawAnimatedArrow(Color.BLACK, Sx, Sy, Ex, Ey, duration);
        animateUnitMovement(unit, Sx, Sy, Ex, Ey, duration);
    }

    private void drawAnimatedArrow(Color color, int oldX, int oldY, int newX, int newY, double duration) {
        double oldCx = oldX * 20 + 10;
        double oldCy = oldY * 20 + 10;
        double newCx = newX * 20 + 10;
        double newCy = newY * 20 + 10;

        Arrow arrow = new Arrow(boardContainer, color, oldCx, oldCy, oldCx, oldCy);
        arrows.add(arrow);
        DoubleProperty progress = new SimpleDoubleProperty(0);
        progress.addListener((obs, oldVal, newVal) -> {
            double fraction = newVal.doubleValue();
            double cx = oldCx + (newCx - oldCx) * fraction;
            double cy = oldCy + (newCy - oldCy) * fraction;
            arrow.setEnd(cx, cy);
        });

        Timeline arrowTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progress, 0)),
                new KeyFrame(Duration.seconds(duration), new KeyValue(progress, 1))
        );
        arrowTimeline.play();
    }


    private void animateCreateUnit(FieldUnit unit, double durationSeconds){
        animateFadeIn(unit.getUnitCircle(), durationSeconds);
    }
    private void animateDestroyUnit(FieldUnit unit, double durationSeconds, Runnable onFinish){
        animateFadeOut(unit.getUnitCircle(), durationSeconds, onFinish);
    }

    private void animateFadeIn(Node node, double durationSeconds) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.seconds(durationSeconds), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void animateFadeOut(Node node, double durationSeconds, Runnable onFinish) {
        FadeTransition ft = new FadeTransition(Duration.seconds(durationSeconds), node);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            if (onFinish != null) onFinish.run();
        });
        ft.play();
    }
    private void animateUnitMovement(FieldUnit unit, int oldX, int oldY, int newX, int newY, double duration) {
        Node circle = unitCircles.get(unit);
        if (circle == null) return;

        GridPane.setRowIndex(circle, newY);
        GridPane.setColumnIndex(circle, newX);

        grid.applyCss();
        grid.layout();

        double deltaX = (oldX - newX) * 20.0;
        double deltaY = (oldY - newY) * 20.0;
        circle.setTranslateX(deltaX);
        circle.setTranslateY(deltaY);

        TranslateTransition tt = new TranslateTransition(Duration.seconds(duration), circle);
        tt.setToX(0);
        tt.setToY(0);
        tt.play();
    }

    public void DrawArrow(Color color, int Sx, int Sy, int Ex, int Ey) {
        drawAnimatedArrow(color, Sx, Sy, Ex, Ey, 0.5);
    }

    // ============================================================
    //   LAUNCH METHODS
    // ============================================================


    public static void startGame(Battlefield field, String playerName, double speed, boolean fast, int playernumber) {
        new Game(field, playerName, speed, fast, playernumber);
    }



}