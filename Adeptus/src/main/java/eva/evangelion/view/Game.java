package eva.evangelion.view;

import eva.evangelion.gameboard.Battlefield;
import eva.evangelion.gameboard.GameBoard;
import eva.evangelion.gameboard.Sector;
import eva.evangelion.units.battle.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import eva.evangelion.items.Weapon.FieldItem;
import eva.evangelion.state.actions.*;
import eva.evangelion.view.UIElements.ScrollableContainer;
import eva.evangelion.items.Weapon.Item;
import eva.evangelion.units.active.Slot;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.ClipboardContent;
import eva.evangelion.gameboard.SectorType;
import eva.evangelion.state.GameState;
import eva.evangelion.state.Queue;
import eva.evangelion.state.QueuePosition;
import eva.evangelion.state.QueuePosition.ReactionType;
import eva.evangelion.units.type.EvangelionIO;
import eva.evangelion.units.type.EvangelionType;
import eva.evangelion.view.UIElements.BetterButton;
import eva.evangelion.util.DirectoryWatcher;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.util.Duration;
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
import javafx.stage.Stage;
import kotlin.Triple;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class Game {

    // ============================================================
    //   FIELDS
    // ============================================================

    private GameState.GAME_MODE gameMode = GameState.GAME_MODE.CLASSIC;
    private boolean showAllUnits = false;


    private final Stage stage;
    private GameBoard gameBoard;          // reference to the main board
    private Item draggedItem;            // item being dragged
    private Slot draggedFromSlot;        // slot from which the item is dragged
    private Label descriptionLabel;      // description display
    private final BorderPane root;
    private final StackPane centerStack;
    private final List<Arrow> arrows = new ArrayList<>();
    public final VBox battlefieldTab;
    public VBox inventoryTab;
    public final VBox chatTab;
    public final VBox dmTab;
    private final VBox weaponCreatorTab;
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
    // ---- Field items ----
    private final ObservableList<FieldItem> FieldItemList = FXCollections.observableArrayList();
    private final Map<FieldItem, Node> fieldItemNodes = new HashMap<>();
    private FieldItem draggedFromField = null;   // non-null when dragging from the mini-battlefield

    // ---- Gameplay ----
    private Queue queue;
    private int CurrentRound = 0;
    private int CurrentTurn = 0;
    private GameState gamestate;
    private FieldUnit selectedUnit = null;

    // ---- Pending action for confirmation ----
    private Action confirmAction = null;

    // ---- Sequential action processing ----
    private final List<Action> pendingActions = new ArrayList<>();
    private boolean isProcessing = false;
    private Timeline processingTimeline = null;

    // ============================================================
    //   CONSTRUCTORS
    // ============================================================
    private int startingplayernumber;

    public Game(Battlefield battlefield, String playerName, double speed, boolean fast,
                int playernumber, boolean startnew, GameState.GAME_MODE gameMode) {
        this.gameMode = (gameMode != null) ? gameMode : GameState.GAME_MODE.CLASSIC;

        System.out.println("STARTING GAME WITH "+playernumber+" PLAYERNUMBER AND STARTNEW = "+startnew);
        this.battlefield = battlefield;
        this.actionSpeed = speed;
        this.fastActions = fast;
        startingplayernumber = playernumber;
        root = new BorderPane();
        centerStack = new StackPane();

        battlefieldTab = buildBattlefieldTab();
        buildInventoryTab();
        chatTab = buildChatTab();
        dmTab = buildDmTab();

        weaponCreatorTab = new VBox();
        weaponCreatorTab.setPadding(new Insets(10));
        WeaponCreatorUI weaponCreatorUI = new WeaponCreatorUI();
        weaponCreatorTab.getChildren().add(weaponCreatorUI);

        centerStack.getChildren().addAll(battlefieldTab, inventoryTab, chatTab, dmTab, weaponCreatorTab);
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

        FieldUnit angel = createFieldUnit("MrAngel", new Angel(), 4, 5, 1);

        initializeGameplay(startnew);
        initializeUnits(playernumber);


        setupBoardInteraction();
        updateDMUIScreens();

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
            // Run reload on the JavaFX thread to avoid concurrent modification
            Platform.runLater(() -> {
                LogMessage("GameState file changed, reloading...");
                reloadGameState();
            });
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
            if (startingplayernumber == -1) {
                startingplayernumber++;
               for (Action act : newState.getActions()) {
                   if (act instanceof createDMSetUpPopUpAction) startingplayernumber++;
               }
            }
            if (newState != null) {
                if (newState.getGameMode() != null) {
                    gameMode = newState.getGameMode();
                }
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
            LogMessage("Current Action Number is "+currentActionNumber);
            LogMessage("Added " + (newCount - currentActionNumber) + " new actions to processing queue.");
            currentActionNumber = newCount;
            LogMessage("Current Action Number now = " + currentActionNumber);
        }
        this.gamestate = state;
        System.out.println("gamestate set to new state");
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
            List<Action> actionsCopy;
            synchronized (pendingActions) {   // optional, but safe
                actionsCopy = new ArrayList<>(pendingActions);
                pendingActions.clear();
            }
            for (Action action : actionsCopy) {
                processAction(action);
                LogMessage("Processed action (fast): " + action);
            }
            isProcessing = false;

            LogMessage("Finished fast processing of all actions.");
            // If more actions were added during processing, start another cycle
            if (!pendingActions.isEmpty()) {
                startProcessing();
            }
            checkShowPopUp();
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
            checkShowPopUp();
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
            processMoveAction((MoveAction) action);
        } else if (action instanceof DMChoosePlayerAction) {
            processDMChoosePlayerAction((DMChoosePlayerAction) action);
        } else if (action instanceof DMCreateUnitAction) {
            processDMCreateUnitAction((DMCreateUnitAction) action);
        } else if (action instanceof EndRoundAction) {
            processForcedRoundEnd((EndRoundAction) action);
        } else if (action instanceof DMDeleteUnitAction) {
            processDMDeleteUnitAction((DMDeleteUnitAction) action);
        }  else if (action instanceof createDMSetUpPopUpAction) {
            processCreateDMSetUp((createDMSetUpPopUpAction) action);
        } else if (action instanceof AddPlayerAction) {
            processAddInitialPlayerAction((AddPlayerAction) action);
        } else if (action instanceof PLAYERCreateEvangelionAction) {
            processPLAYERCreateEvangelion((PLAYERCreateEvangelionAction) action);
        } else if (action instanceof InventoryItemTransferAction) {
            processInventoryItemTransferAction((InventoryItemTransferAction) action);
        } else if (action instanceof SwitchTeamAction) {
            processSwitchTeamAction((SwitchTeamAction) action);
        } else if (action instanceof EndTurnAction) {
            processEndTurnAction((EndTurnAction) action);
        } else
        {
            LogMessage("Unknown action type: " + action.getClass().getSimpleName());
        }
        LogMessage(outputQueue());
        LogMessage(outputActions());
        rebuildInventoryTab();
        QueuePosition next = queue.currentPosition();
        if (next != null) {
            LogMessage("Setting active player to "+next.getUnitID()+" at process action end, current position in queue is "
                    +queue.getQueue().indexOf(next));
            setActivePlayer(next.getUnitID());
        }
    }
    private void setBackgroundForInventory(ScrollableContainer mainContainer) {
        LogMessage("Adding evangelion picture as background");
        Image backgroundImage = new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/eva/evapicture.png"))
        );
        BackgroundImage background = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                // cover = true so it scales to fill the VBox without tiling
                new BackgroundSize(1.0, 1.0, true, true, false, true)
        );

        mainContainer.getContentBox().setBackground(new Background(background));
    }

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
    private void processSwitchTeamAction(SwitchTeamAction action) {
        DMQueueInsertion(action, "SWITCH_TEAM");

        FieldUnit unit = getUnitFromName(action.getTargetName());
        if (unit == null || !unit.isExists()) {
            LogMessage("SwitchTeamAction failed: unit not found: " + action.getTargetName());
            return;
        }
        int oldTeam = unit.getTeam();
        unit.setTeam(action.getNewTeam());
        LogMessage("Switched team of " + unit.getName() + " from " + oldTeam + " to " + action.getNewTeam());

    }

    private void processEndTurnAction(EndTurnAction action) {
        FieldUnit actor = getUnitFromName(action.getActor());
        if (actor == null || !actor.isExists()) {
            LogMessage("TurnEndAction failed: Actor not found: " + action.getActor());
            return;
        }
        actor.ClearEffects(Effect.EffectEnd.TURN_END);
        actor.setTurnDone(true);

        if (actor.getUnit() instanceof Evangelion) {
            processEndPlayerTurn(action);
        } else if (actor.getUnit() instanceof Angel) {
            processAngelTurnEnd(action);
        }

        if (isRoundEnd()) processEndRound();
        updateDMUIScreens();
    }

    private void processAngelTurnEnd(EndTurnAction action) {
        FieldUnit actor = getUnitFromName(action.getActor());
        activateQueue(action);
        LogMessage("Turn ended for " + actor.getName()
                + " (action #" + action.getActionNumber() + ").");
        setDMToNext();
    }

    private void processEndPlayerTurn(EndTurnAction action) {
        FieldUnit actor = getUnitFromName(action.getActor());
        activateQueue(action);
        LogMessage("Turn ended for " + actor.getName()
                + " (action #" + action.getActionNumber() + ").");
        switch (gameMode) {
            case CLASSIC -> {
                resetTurn(getClassicAngel());
                CurrentTurn++;
                setClassicAngelToNext();
            }
            //TODO add handling for other gamemodes
            default -> setDMToNext();
        }
    }

    private void resetTurn(FieldUnit unit){
        unit.setStamina(unit.getMaxStamina());
        unit.setTurnDone(false);
        unit.setATP(unit.getMaxATP());
    }

    private void processEndRound() {
        for (FieldUnit unit : UnitList) {
            unit.ClearEffects(Effect.EffectEnd.ROUND_END);
            unit.setUsedGuard(false);
        }
        CurrentTurn = 1;
        CurrentRound++;
        switch (gameMode) {
            case CLASSIC -> {
                for (FieldUnit unit : UnitList) {
                    if (!unit.equals(getClassicAngel())) resetTurn(unit);
                }
            }
            //TODO add handling for other gamemodes
            default -> {
                for (FieldUnit unit : UnitList) {
                    resetTurn(unit);
                }
            }
        }
    }

    private FieldUnit getClassicAngel(){
        for (FieldUnit unit : UnitList){
            if (unit.getUnit() instanceof Angel) return unit;
        }
        return null;
    }
    private List<FieldUnit> getEvangelionsAndAngels(){
        List<FieldUnit> units = new ArrayList<>();
        for (FieldUnit unit : UnitList){
            if (unit.getUnit() instanceof Angel || unit.getUnit() instanceof Evangelion) units.add(unit);
        }
        return units;
    }



    private boolean isRoundEnd(){
        switch (gameMode) {
            case CLASSIC -> {
                for (FieldUnit unit : getEvangelionsAndAngels())
                {
                    if (!unit.isTurnDone()) return false;
                }
            }
            case FFA, TEAM -> {
                for (FieldUnit unit : UnitList) {
                    if (!unit.isTurnDone()) return false;
                }
            }
            case CUSTOM -> {
                return false;
            }
        }
        return true;
    }


    private void processInventoryItemTransferAction(InventoryItemTransferAction action) {

        LogMessage("Start processing inventory action for " + action.getActor()+" + current player = "+currentPlayer);

        FieldUnit actor = getUnitFromName(action.getActor());
        if (actor == null || !actor.isExists()) {
            LogMessage("ItemTransferActionFailed since Actor Doesnt Exist" + action.getActor());
            return;
        }

        if (action.getTransferType().equals(InventoryItemTransferAction.TRANSFER_TYPE.INVENTORY)) {
            List<Slot> evaslots = ((Evangelion) actor.getUnit()).getSlots();

            int from = action.getSlotFrom();
            int to = action.getSlotTo();
            if (evaslots.size() < from || evaslots.size() < to) {
                LogMessage("ItemTransferActionFailed - Impossible Slots Error");
                activateQueue(action);
                setActorToNext(action);
                return;
            }
            Item fromItem = evaslots.get(from).getItem();
            Item toItem = evaslots.get(to).getItem();
            if (fromItem != null) {
                if (evaslots.get(to).canFit(fromItem) && ( toItem == null || evaslots.get(from).canFit(toItem))) {
                    evaslots.get(from).setItem(toItem);
                    evaslots.get(to).setItem(fromItem);
                    LogMessage("Items switched between from slot "+from+" to "+to);
                } else LogMessage("ItemTransferAction Failed since SLOT CANT FIT ITEM");
            } else LogMessage("ItemTransferAction Failed since Item being transferred doesn't exist");

            activateQueue(action);
            setActorToNext(action);
            return;
        }


        int actorX = actor.getX();
        int actorY = actor.getY();
        int deltaX = action.getDeltaX();
        int deltaY = action.getDeltaY();
        int itemX = actorX + deltaX;
        int itemY = actorY + deltaY;

        if (itemX < 0 || itemX >= battlefield.sizeX || itemY < 0 || itemY >= battlefield.sizeY) {
            LogMessage("Put or Drop action failed: Target item (" + itemX + "," + itemY + ") out of bounds.");
            return;
        }

        switch (action.getTransferType()){
            case INVENTORY -> LogMessage("ERROR, INVENTORY ACTION SPOTTED AT IMPOSSIBLE PLACE");
            case UNIT2BOARD -> {
                List<Slot> evaslots = ((Evangelion) actor.getUnit()).getSlots();
                int from = action.getSlotFrom();
                if (evaslots.get(from).getItem() == null) {
                    LogMessage("ERROR, PICKING UP ITEM TO OCCUPIED SLOT");
                }
                Item PutDown = evaslots.get(from).getItem();
                processDropVisuals(actorX, actorY, itemX, itemY);
                createFieldItemAt(PutDown, itemX, itemY);
                evaslots.get(from).setItem(null);
            }
            case BOARD2UNIT -> {
                FieldItem item = getFieldItemAt(itemX, itemY);
                Item item1 = item.getItem();
                List<Slot> evaslots = ((Evangelion) actor.getUnit()).getSlots();
                int to = action.getSlotTo();
                if (evaslots.get(to).getItem() != null) {
                    LogMessage("ERROR, PICKING UP ITEM TO OCCUPIED SLOT");
                }
                processPickUpVisuals(itemX, itemY, actorX, actorY);
                evaslots.get(to).setItem(item1);
                deleteFieldItemsAt(itemX, itemY);

            }
        }

        activateQueue(action);
        setActorToNext(action);
    }



    protected void setDMToNext() {
        QueuePosition nextPos = new QueuePosition("DM", false);
        LogMessage("Adding queue position for " + nextPos.getUnitID());
        nextPos.setActionNumber(-1);
        queue.addPosition(nextPos, 1);
    }
    protected void setClassicAngelToNext() {
        if (!(getClassicAngel() != null && getClassicAngel().Exists)) {
            LogMessage("NO ANGEL FOUND");
            return;
        }
        QueuePosition nextPos = new QueuePosition(getClassicAngel().getName(), false);
        LogMessage("Adding queue position for " + nextPos.getUnitID());
        nextPos.setActionNumber(-1);
        queue.addPosition(nextPos, 1);
    }


    protected void setActorToNext(Action action) {
        QueuePosition nextPos = new QueuePosition(action.getActor(), false);
        LogMessage("Adding queue position for " + nextPos.getUnitID()+ " at action#"+action.getActionNumber());
        nextPos.setActionNumber(-1);
        queue.addPosition(nextPos, 1);
    }
    protected void activateQueue(Action action){
        QueuePosition current = queue.currentPosition();
        if (current != null) {
            current.setActionNumber(action.getActionNumber());
            current.setTurn(CurrentTurn);
            current.setRound(CurrentRound);
            LogMessage("Set current position (" + current.getUnitID() + ") action number to " + action.getActionNumber());
        }
    }

    protected void DMQueueInsertion (Action action, String NAME) {
        addTurnHere(action);
        QueuePosition current = queue.currentPosition();
        activateQueue(action);
        current.setUnitID(NAME);
        LogMessage("Renamed ID to "+current.getUnitID()+" at action "+action);
    }
    protected void addTurnHere(Action action){
        QueuePosition newPos = new QueuePosition(action.getActor(), false);
        LogMessage("Add Basic Turn for "+newPos.getUnitID());
        queue.addPosition(newPos, 0);
    }

    protected void setNextTurnToThis(){
        QueuePosition next = new QueuePosition(queue.currentPosition().getUnitID(), queue.currentPosition().Reaction);
        if (queue.currentPosition().Reaction) {
            next.setReactionType(queue.currentPosition().getReactionType());
            next.setSkippable(queue.currentPosition().isSkippable());
            next.setDescription(queue.currentPosition().getDescription());
        }
        LogMessage("Add next turn equal to this: "+next.getUnitID()+" reaction = "+next.Reaction);
        next.setActionNumber(-1);
        queue.addPosition(next, 1);
    }


    private void processForcedRoundEnd(EndRoundAction action) {
        DMQueueInsertion(action, "ROUND_END");
        processEndRound();
        updateDMUIScreens();
    }


    private void processDMCreateUnitAction(DMCreateUnitAction action) {

        DMQueueInsertion(action, "DM_CREATE_UNIT_INSERT");

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

        FieldUnit unit = createFieldUnit(name, action.getUnit(), x, y, action.getTeam());

        double visualDuration = fastActions ? 0.1 : action.getTime() / actionSpeed;
        animateCreateUnit(unit, visualDuration);

        LogMessage("DMCreateUnitAction: Created unit '" + name + "' at (" + x + "," + y + ").");
    }

    private void processDMDeleteUnitAction(DMDeleteUnitAction action) {
        DMQueueInsertion(action, "DM_DELETE_UNIT_INSERT");

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
        // Whatever is at that cell, wipe field items there too.
        deleteFieldItemsAt(x, y);
    }

    private void processDMChoosePlayerAction(DMChoosePlayerAction action) {
        QueuePosition current = queue.currentPosition();
        if (current != null) {
            activateQueue(action);
            current.setUnitID("DM_Choose");
            LogMessage("Renamed ID to "+current.getUnitID());
            String chosen = action.getChosenPlayer();
            FieldUnit chosenUnit = getUnitFromName(chosen);
            if (chosenUnit != null && chosenUnit.isTurnDone()) {
                LogMessage("Changed turn done to false for "+chosen);
                chosenUnit.setTurnDone(false);   // NEW
            }
            QueuePosition nextPos = new QueuePosition(chosen, false);
            nextPos.setActionNumber(-1);
            LogMessage("Adding queue position for " + nextPos.getUnitID() + " at DMPlayerAction");
            queue.addPosition(nextPos, 1);
            CurrentTurn++;
        } else {
            LogMessage("DMChoosePlayerAction failed: No current queue position.");
        }
    }


    private String outputQueue(){
        StringBuilder queueOutput = new StringBuilder("Queue positions (in order):\n");
        List<QueuePosition> positions = queue.getQueue(); // getQueue() returns a list
        for (int i = 0; i < positions.size(); i++) {
            QueuePosition pos = positions.get(i);
            queueOutput.append(" Position ").append(i).append(" Round = ").append(pos.getRound()).append(" Turn =").append(pos.getTurn())
                    .append(": UnitID=").append(pos.getUnitID())
                    .append(", ActionNumber=").append(pos.getActionNumber())
                    .append(", Reaction=").append(pos.isReaction());
            if (pos.getReactionType() != null) {
                queueOutput.append(", ReactionType=").append(pos.getReactionType().toString())
                        .append(", skippable=").append(pos.isSkippable());
            }
            queueOutput.append("\n");
        }
        return queueOutput.toString();
    }



    private String outputActions(){
        List<Action> allActions = gamestate.getActions();
        if (allActions.isEmpty()) {
            return "Action List is Empty";
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
            return sb.toString();
    }
    }

    private void processCreateDMSetUp(createDMSetUpPopUpAction action) {
        DMQueueInsertion(action, "START");
        QueuePosition reaction = QueuePosition.createReactionTurn(
                "DM",
                ReactionType.DM_SETUP_PLAYER,
                "Setup player", action.getActionNumber()
        );
        reaction.setSkippable(false);
        LogMessage("ADDING SETUP, current queue = "+outputQueue());
        queue.addPosition(reaction, 0);
        LogMessage("ADDED SETUP, new queue = "+outputQueue());

    }

    private void processAddInitialPlayerAction(AddPlayerAction action) {
        LogMessage("Processing AddPlayer");
        QueuePosition current = getCurrentPosition();
        LogMessage("current position is at "+queue.getNUMPOSof(current));
        if (current != null && current.Reaction && current.getReactionType() == ReactionType.DM_SETUP_PLAYER) {
            activateQueue(action);
        } else {
            LogMessage("Warning: INITIALCreateUnitRequestAction processed but current position is not DM_SETUP_PLAYER reaction.");
        }

        String playerName = action.getUnitName();
        QueuePosition playerReaction = QueuePosition.createReactionTurn(
                playerName,
                ReactionType.PLAYER_SETUP,
                "Chose your evangelion out of created options", action.getActionNumber()
        );
        playerReaction.setSkippable(false);
        LogMessage("current starting players = "+startingplayernumber);
        queue.addPosition(playerReaction,startingplayernumber-1);
        LogMessage("Added PLAYER_SETUP reaction turn for " + playerName+" at position "+queue.getNUMPOSof(playerReaction));
    }

    private void processPLAYERCreateEvangelion(PLAYERCreateEvangelionAction action) {
        QueuePosition current = getCurrentPosition();
        if (current != null && current.Reaction && current.getReactionType() == ReactionType.PLAYER_SETUP) {
            activateQueue(action);
        } else {
            LogMessage("Warning: PLAYERCreateEvangelionAction processed but current position is not PLAYER_SETUP reaction.");
        }

        String playerName = action.getActor();
        int x = action.getX();
        int y = action.getY();
        Unit unit = action.getUnit();
        FieldUnit newUnit = createFieldUnit(playerName, unit, x, y, action.getTeam());

        LogMessage("Created Evangelion for " + playerName + " at (" + x + "," + y + ")");

        List<QueuePosition> positions = queue.getQueue();
        if (positions.isEmpty()) {
            QueuePosition dmTurn = new QueuePosition("DM", false);
            dmTurn.setActionNumber(-1);
            addPositionAfterCurrent(dmTurn);
            LogMessage("Queue empty after player setup, added DM turn.");
        }
        if (playerName.equals(currentPlayer)) rebuildInventoryTab();

    }

    private void addUnitToTeam(FieldUnit newUnit, int team) {
        newUnit.setTeam(team);
        LogMessage("Assigned team " + team + " to unit " + newUnit.getName());
    }


    // ============================================================
    //   UNIT MANAGEMENT
    // ============================================================

    public void initializeUnits(int pn) {
        for (int i = 1; i <= pn; i++) {
            createDMSetUpPopUpAction action = new createDMSetUpPopUpAction(currentActionNumber+i, "DM");
            SendAction(action);
            LogMessage("Sent DMSendRequestAction #" + i);
        }
    }
    public FieldUnit getUnitFromName(String s) {
        for (FieldUnit unit : UnitList) {
            if (unit.getName().equals(s)) return unit;
        }
        LogMessage("No Units Found with Name " + s);
        return null;
    }

    public FieldUnit createFieldUnit(String name, Unit unit, int x, int y, int team) {
        FieldUnit newUnit = new FieldUnit(name, unit, x, y);
        UnitList.add(newUnit);
        refreshUnitPositions();
        addUnitToTeam(newUnit, team);
        updateDMUIScreens();
        return newUnit;
    }

    public void removeFieldUnit(FieldUnit unit) {
        unit.setExists(false);
        refreshUnitPositions();
        updateDMUIScreens();
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

    private void initializeGameplay(boolean sn) {
        queue = new Queue();
        QueuePosition initial = new QueuePosition("DM", false);
        initial.setActionNumber(-1);
        queue.addPosition(initial, 1);
        LogMessage("adding queue position for "+initial.getUnitID()+" at initialization");
        setActivePlayer("DM");
        if (sn) {
            LogMessage("Creating new gamestate at creation");
            gamestate = new GameState();
            gamestate.setGameMode(gameMode);
            saveGameState();
        }
        else {
            LogMessage("Loading state at creation");
            loadInitialGameState();
        }
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

            confirmAction = new MoveAction(currentActionNumber, selectedUnit.getName(),
                    x - selectedUnit.getX(), y - selectedUnit.getY());

            createConfirmButton(confirmAction, Color.DARKORANGE,"Move to ("+x+","+y+")");
            LocalMessage("Move pending. Click Confirm to send.");
        } else {
            LocalMessage("No unit selected. Click on a unit to select it.");
        }
    }



    // ============================================================
//   CONFIRM BUTTON AND END TURN BUTTON ON TOP BAR (with confirmation pop-up)
// ============================================================
    private HBox turnEndContainer;   // holds the "Turn End" button




    private void createConfirmButton(Action action, Color color, String description) {
        confirmContainer.getChildren().clear();

        BetterButton confirmBtn = new BetterButton("Confirm");
        confirmBtn.setPrefHeight(10);
        // Top-bar button uses the same color so the user can associate them
        confirmBtn.setSuccessStyle();

        confirmBtn.setOnAction(e -> showConfirmPopup(action, color, description));

        confirmContainer.getChildren().addAll(confirmBtn);
    }

    private void showConfirmPopup(Action action, Color color, String description) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Confirm Action");
        popupStage.initStyle(StageStyle.TRANSPARENT);

        // Root container with the colored border
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + toHex(color) + ";" +
                        "-fx-border-width: 4;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4);"
        );

        // Scene with transparent fill so the rounded corners show properly
        Scene scene = new Scene(content, 460, 220);
        scene.setFill(Color.TRANSPARENT);

        Label titleLabel = new Label("Confirm Action");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(420);
        descLabel.setAlignment(Pos.CENTER);

        BetterButton proceedBtn = new BetterButton("Proceed");
        proceedBtn.setSuccessStyle();
        proceedBtn.setOnAction(e -> {
            SendAction(action);
            confirmAction = null;
            confirmContainer.getChildren().clear();
            popupStage.close();
        });

        BetterButton cancelBtn = new BetterButton("Cancel");
        cancelBtn.setDangerStyle();
        cancelBtn.setOnAction(e -> popupStage.close());

        HBox btnBox = new HBox(10, cancelBtn, proceedBtn);
        btnBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(titleLabel, descLabel, btnBox);

        popupStage.setScene(scene);

        // Allow dragging the borderless window by holding anywhere on it
        final double[] dragOffset = new double[2];
        content.setOnMousePressed(e -> {
            dragOffset[0] = e.getSceneX();
            dragOffset[1] = e.getSceneY();
        });
        content.setOnMouseDragged(e -> {
            popupStage.setX(e.getScreenX() - dragOffset[0]);
            popupStage.setY(e.getScreenY() - dragOffset[1]);
        });

        popupStage.show();
    }

    private void showTurnEndPopup() {
        FieldUnit unit = getUnitFromName(currentPlayer);
        if (unit == null || !unit.isExists()) {
            LogMessage("TurnEndAction failed: unit '" + currentPlayer + "' not found.");
            return;
        }

        // ----- Collect what the unit still has available -----
        List<String> leftovers = new ArrayList<>();

        if (unit.hasTactical()) {
            leftovers.add("Tactical action left");
        }
        if (unit.getATP() > 0) {
            leftovers.add("ATP left: " + unit.getATP() + " / " + unit.getMaxATP());
        }
        if (unit.getStamina() > 0) {
            leftovers.add("Stamina left: " + unit.getStamina() + " / " + unit.getMaxStamina());
        }

        String description;
        if (leftovers.isEmpty()) {
            description = "Are you sure you want to end turn?";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Are you sure you want to end turn, you still have:\n");
            for (String s : leftovers) {
                sb.append("  • ").append(s).append("\n");
            }
            description = sb.toString();
        }

        // ----- Build and confirm the action -----
        EndTurnAction action = new EndTurnAction(currentActionNumber, currentPlayer);
        showConfirmPopup(action, Color.GRAY, description);
    }


    /** Converts a JavaFX Color to a CSS hex string (#RRGGBB). */
    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed()   * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue()  * 255));
    }

    // ============================================================
    //   CONSOLE COMMANDS (DM / Debug)
    // ============================================================

    private void processDMCommand(String command, TextArea output) {
        if (command == null || command.trim().isEmpty()) return;
        String[] parts = command.trim().split(" ");
        String action = parts[0].toLowerCase();
        if (action.startsWith("/")) action = action.substring(1);   // NEW: tolerate "/showAllUnits"
        switch (action) {
            case "create":
                if (parts.length < 4) {
                    output.appendText("Error: Usage: create <name> <x> <y> [team]\n");
                    return;
                }
                StringBuilder nameBuilder = new StringBuilder();
                int x = -1, y = -1, team = 0;
                try {
                    int end = parts.length;
                    boolean teamPresent = parts.length >= 5
                            && isValidInt(parts[end - 1])
                            && isValidInt(parts[end - 2])
                            && isValidInt(parts[end - 3]);
                    if (teamPresent) {
                        team = Integer.parseInt(parts[end - 1]);
                        end--;
                    }
                    y = Integer.parseInt(parts[end - 1]);
                    x = Integer.parseInt(parts[end - 2]);
                    for (int j = 1; j < end - 2; j++) {
                        if (j > 1) nameBuilder.append(" ");
                        nameBuilder.append(parts[j]);
                    }
                } catch (NumberFormatException ex) {
                    output.appendText("Error: x, y (and team) must be integers.\n");
                    return;
                }
                String name = nameBuilder.toString();
                if (name.isEmpty()) { output.appendText("Error: Name cannot be empty.\n"); return; }
                if (x < 0 || x >= battlefield.sizeX || y < 0 || y >= battlefield.sizeY) {
                    output.appendText("Error: Coordinates out of bounds.\n"); return;
                }
                for (FieldUnit u : UnitList) {
                    if (u.isExists() && u.getName().equals(name)) {
                        output.appendText("Error: Unit '" + name + "' already exists.\n"); return;
                    }
                }
                DMCreateUnitAction createAction = new DMCreateUnitAction(
                        currentActionNumber, "DM", name, x, y, team);
                SendAction(createAction);
                output.appendText("Sent DMCreateUnitAction for '" + name + "' at ("
                        + x + ", " + y + ") team=" + team + ".\n");
                break;
            case "round":
                EndRoundAction action1 = new EndRoundAction(
                        currentActionNumber, "DM");
                SendAction(action1);
                output.appendText("Sent Round end action");
                break;
            case "team":
                // team <name> <teamNumber>
                if (parts.length < 3) {
                    output.appendText("Error: Usage: team <unitName> <newTeamNumber>\n");
                    return;
                }
                StringBuilder targetNameBuilder = new StringBuilder();
                int newTeam;
                try {
                    newTeam = Integer.parseInt(parts[parts.length - 1]);
                    for (int j = 1; j < parts.length - 1; j++) {
                        if (j > 1) targetNameBuilder.append(" ");
                        targetNameBuilder.append(parts[j]);
                    }
                } catch (NumberFormatException ex) {
                    output.appendText("Error: team number must be an integer.\n");
                    return;
                }
                String targetName = targetNameBuilder.toString().trim();
                if (targetName.isEmpty()) {
                    output.appendText("Error: unit name cannot be empty.\n");
                    return;
                }
                SwitchTeamAction switchAction = new SwitchTeamAction(
                        currentActionNumber, "DM", targetName, newTeam);
                SendAction(switchAction);
                output.appendText("Sent SwitchTeamAction: " + targetName + " -> team " + newTeam + "\n");
                break;
            case "showallunits":
                showAllUnits = !showAllUnits;
                output.appendText("showAllUnits = " + showAllUnits + "\n");
                updateDMUIScreens();
                break;
            case "actions":
                    output.appendText(outputActions());
                    break;
            case "queue":
                output.appendText(outputQueue());
                break;
            case "delete":
                // Form 1: delete <x> <y>
                if (parts.length == 3 && isValidInt(parts[1]) && isValidInt(parts[2])) {
                    int dx = Integer.parseInt(parts[1]);
                    int dy = Integer.parseInt(parts[2]);
                    if (dx < 0 || dx >= battlefield.sizeX || dy < 0 || dy >= battlefield.sizeY) {
                        output.appendText("Error: Coordinates out of bounds.\n");
                        return;
                    }
                    DMDeleteUnitAction deleteAction = new DMDeleteUnitAction(
                            currentActionNumber, "DM", dx, dy);
                    SendAction(deleteAction);
                    output.appendText("Sent DMDeleteUnitAction at (" + dx + ", " + dy + ").\n");
                    return;
                }

                // Form 2: delete <name>  (existing behaviour)
                if (parts.length < 2) {
                    output.appendText("Error: Usage: delete <name> OR delete <x> <y>\n");
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
                    if (u.isExists() && u.getName().equals(delName)) { found = u; break; }
                }
                if (found == null) {
                    output.appendText("Error: Unit '" + delName + "' not found.\n");
                    return;
                }
                DMDeleteUnitAction deleteAction = new DMDeleteUnitAction(
                        currentActionNumber, "DM", found.getX(), found.getY());
                SendAction(deleteAction);
                output.appendText("Sent DMDeleteUnitAction for '" + delName +
                        "' at (" + found.getX() + ", " + found.getY() + ").\n");
                break;
            case "current":
                if (parts.length < 2) {
                    output.appendText("Error: Usage: active <playerName>\n");
                    return;
                }
                StringBuilder currentNameBuilder = new StringBuilder();
                for (int j = 1; j < parts.length; j++) {
                    if (j > 1) currentNameBuilder.append(" ");
                    currentNameBuilder.append(parts[j]);
                }
                String currentName = currentNameBuilder.toString();
                if (currentName.isEmpty()) {
                    output.appendText("Error: Player name cannot be empty.\n");
                    return;
                }
                setCurrentPlayer(currentName);
                return;
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
                output.appendText("  create <name> <x> <y> [team]  – creates a new unit (optional team)\n");
                output.appendText("  delete <name>          – deletes a unit (and any items on their slot) by name\n");
                output.appendText("  delete <x> <y>         – deletes unit + items at a sector\n");
                output.appendText("  active <playerName>    – DM chooses next player\n");
                output.appendText("  progress               – reloads GameState and processes new actions\n");
                output.appendText("  queue                  – shows current queue positions\n");
                output.appendText("  actions                – shows all actions and their numbers in gamestate\n");
                output.appendText("  help                   – shows this help\n");
                output.appendText("  team <name> <teamNumber> – switches a unit to another team\n");
                output.appendText("  /showAllUnits          – toggle showing finished units in the next-turn list\n");
                output.appendText("  round                  – Ends the round forcefully\n");
                break;

            default:
                output.appendText("Unknown command: " + action + ". Type 'help' for a list.\n");
                break;
        }
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
        checkShowPopUp();
        LogMessage("Current player set to: " + name);
        rebuildInventoryTab();
    }

    private void checkShowPopUp() {

        if (!pendingActions.isEmpty()) {
            LogMessage("Check to show pop -> fail, has pending actions");
            return;
        }
        QueuePosition current = getCurrentPosition();
        if (current != null) {
            if (current.Reaction && current.getActionNumber() == -1 &&
                    (currentPlayer.equals(activePlayer)) && !popupShowing) {
                LogMessage("Check to show pop -> Showing pop up!");
                showReactionPopup();
            } else LogMessage("Check to show pop -> No pop up to show");
        } else {
            LogMessage("Check to show pop -> Queue is empty after processing.");
            setActivePlayer(null);
        }
    }


    private void showReactionPopup() {
        QueuePosition current = getCurrentPosition();
        if (current == null || !current.Reaction) return;

        popupShowing = true;

        Stage popupStage = new Stage();
        popupStage.initModality(Modality.NONE);
        popupStage.setTitle("Reaction Turn");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label descLabel = new Label(current.getDescription());
        descLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        content.getChildren().add(descLabel);

        Node specificContent = null;
        BetterButton confirmBtn = new BetterButton("Confirm");
        BetterButton cancelBtn = null;

        Action currentAction = gamestate.getActionfromNumber(getCurrentPosition().getReactionTo());
        if (currentAction == null) {
            LogMessage("ERROR: REACTION POP UP SHOWING WITHOUT PROPER ACTION TO REACT TO");
            return;
        }

        switch (current.getReactionType()) {
            case DM_SETUP_PLAYER:
                specificContent = buildDMSetupContent(confirmBtn, popupStage, currentAction);
                break;
            case PLAYER_SETUP:
                specificContent = buildPlayerSetupContent(confirmBtn, popupStage, currentAction);
                break;
            default:
                // For other types, just show a message and confirm
                specificContent = new Label("No specific actions for this reaction type.");
                break;
        }

        if (specificContent != null) {
            content.getChildren().add(specificContent);
        }

        // Confirm button logic: it will be enabled/disabled by the content builders
        confirmBtn.setPrimaryStyle();
        confirmBtn.setDisable(true); // initially disabled, enabled when valid

        // TODO SKIPPABLE
     /*   if (current.isSkippable()) {
            cancelBtn = new BetterButton("Cancel");
            cancelBtn.setDangerStyle();
            cancelBtn.setOnAction(e -> {
                // Skip this reaction: remove it from queue and update
                removeCurrentPosition();
                popupStage.close();
                popupShowing = false;
                updateActivePlayerAndPopup();
            });
        } */

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER);
        if (cancelBtn != null) btnBox.getChildren().add(cancelBtn);
        btnBox.getChildren().add(confirmBtn);
        content.getChildren().add(btnBox);

        Scene scene = new Scene(content, 400, 300);
        popupStage.setScene(scene);
        popupStage.setOnCloseRequest(e -> {
            if (!current.isSkippable()) {
                e.consume();
            } else {
                //TODO skippable
                popupShowing = false;
                checkShowPopUp();
            }
        });
        popupStage.show();
        popupShowing = false;
    }

    private boolean nameIsPicked(String name, GameState gamestate1) {
        boolean picked = false;
        for (Action action : gamestate1.getActions()) {
            if (action instanceof AddPlayerAction action1 && action1.getUnitName().equals(name)) {picked = true;
            break;}
        }
        if (!UnitList.isEmpty()) {
        for (FieldUnit unit : UnitList) {
            if (unit.getName().equals(name)) {picked = true; break;}
        }}
        return picked;
    }

    private Node buildDMSetupContent(Button confirmBtn, Stage popupStage, Action cause) {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(5));

        TextField nameField = new TextField();
        nameField.setPromptText("Player name");
        TextField xField = new TextField();
        xField.setPromptText("X (0-" + (battlefield.sizeX-1) + ")");
        TextField yField = new TextField();
        yField.setPromptText("Y (0-" + (battlefield.sizeY-1) + ")");

        // ---- Team input only shown in TEAM / CUSTOM ----
        TextField teamField = new TextField();
        teamField.setPromptText("Team #");
        boolean manualTeam = (gameMode == GameState.GAME_MODE.TEAM
                || gameMode == GameState.GAME_MODE.CUSTOM);

        Runnable validate = () -> {
            boolean valid = false;
            String name = nameField.getText().trim();
            if (!name.isEmpty() && !nameIsPicked(name, gamestate)) {
                try {
                    int x = Integer.parseInt(xField.getText().trim());
                    int y = Integer.parseInt(yField.getText().trim());
                    if (x >= 0 && x < battlefield.sizeX && y >= 0 && y < battlefield.sizeY) {
                        if (manualTeam) {
                            Integer.parseInt(teamField.getText().trim()); // must parse
                        }
                        valid = true;
                    }
                } catch (NumberFormatException ignored) { }
            }
            confirmBtn.setDisable(!valid);
        };

        nameField.textProperty().addListener((o, a, b) -> validate.run());
        xField.textProperty().addListener((o, a, b) -> validate.run());
        yField.textProperty().addListener((o, a, b) -> validate.run());
        teamField.textProperty().addListener((o, a, b) -> validate.run());

        confirmBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            int x = Integer.parseInt(xField.getText().trim());
            int y = Integer.parseInt(yField.getText().trim());

            int team;
            switch (gameMode) {
                case CLASSIC -> team = 0;
                case FFA -> {
                    long existing = gamestate.getActions().stream()
                            .filter(a -> a instanceof AddPlayerAction).count();
                    team = (int) existing + 1;   // first FFA player -> team 1
                }
                default -> team = Integer.parseInt(teamField.getText().trim());
            }

            int location = currentActionNumber + 1;
            LogMessage("Creating AddPlayerAction AN=" + location + " team=" + team);
            AddPlayerAction action = new AddPlayerAction(location, "DM", name, x, y, team);
            SendAction(action);
            popupStage.close();
        });

        vbox.getChildren().addAll(
                new Label("Enter player name and starting position:"),
                new HBox(10, new Label("Name:"), nameField),
                new HBox(10, new Label("X:"), xField),
                new HBox(10, new Label("Y:"), yField)
        );
        if (manualTeam) {
            vbox.getChildren().add(new HBox(10, new Label("Team:"), teamField));
        }
        return vbox;
    }

    private boolean isValidInt(String text) {
        try {
            int val = Integer.parseInt(text.trim());
            // Optionally check bounds
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private Node buildPlayerSetupContent(Button confirmBtn, Stage popupStage, Action cause) {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(5));


        if (!(cause instanceof AddPlayerAction))  {
            LogMessage("ERROR - POP UP FOR PLAYERSETUP NOT FROM ADD PLAYER BUTTON");
            return null;
        }

        int assignedTeam = ((AddPlayerAction) cause).getTeam();
        Label teamLabel = new Label("Team: " + assignedTeam);

        int spawnX = ((AddPlayerAction) cause).getX();
        int spawnY = ((AddPlayerAction) cause).getY();

        QueuePosition current = getCurrentPosition();
        String playerName = current != null ? current.getUnitID() : "Player";

        Label playerLabel = new Label("Player: " + playerName);
        playerLabel.setStyle("-fx-font-weight: bold;");

        // Load saved Evangelion types
        List<String> filenames = EvangelionIO.listFiles();
        List<EvangelionType> evaTypes = new ArrayList<>();
        if (filenames.isEmpty()) {
            Label noEvaLabel = new Label("No saved Evangelions found. Please create one first.");
            noEvaLabel.setStyle("-fx-text-fill: red;");
            vbox.getChildren().addAll(playerLabel, noEvaLabel);
            confirmBtn.setDisable(true);
            return vbox;
        }

        for (String fname : filenames) {
            try {
                EvangelionType eva = EvangelionIO.load(fname);
                evaTypes.add(eva);
            } catch (IOException | ClassNotFoundException e) {
                LogMessage("Failed to load Evangelion: " + fname + " - " + e.getMessage());
            }
        }

        if (evaTypes.isEmpty()) {
            Label noEvaLabel = new Label("No valid Evangelions could be loaded.");
            noEvaLabel.setStyle("-fx-text-fill: red;");
            vbox.getChildren().addAll(playerLabel, noEvaLabel);
            confirmBtn.setDisable(true);
            return vbox;
        }

        // ComboBox for EvangelionType
        ComboBox<EvangelionType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(evaTypes);
        typeCombo.setPromptText("Select Evangelion Type");

        // Also need X and Y fields? The action expects x and y. We'll ask for them.
        Label xField = new Label();
        xField.setText(""+spawnX);
        Label yField = new Label();
        yField.setText(""+spawnY);

        Runnable validate = () -> {
            boolean valid = typeCombo.getValue() != null &&
                    isValidInt(xField.getText()) &&
                    isValidInt(yField.getText());
            confirmBtn.setDisable(!valid);
        };

        typeCombo.valueProperty().addListener((obs, old, neu) -> validate.run());
        xField.textProperty().addListener((obs, old, neu) -> validate.run());
        yField.textProperty().addListener((obs, old, neu) -> validate.run());

        confirmBtn.setOnAction(e -> {
            EvangelionType type = typeCombo.getValue();
            int x = Integer.parseInt(xField.getText().trim());
            int y = Integer.parseInt(yField.getText().trim());
            Unit unit = new Evangelion(type);
            PLAYERCreateEvangelionAction action = new PLAYERCreateEvangelionAction(
                    currentActionNumber + 1, playerName, playerName, x, y, unit,
                    assignedTeam);          // <-- use the team from AddPlayerAction
            SendAction(action);
            popupStage.close();
        });

        vbox.getChildren().addAll(
                playerLabel,
                teamLabel,
                new HBox(10, new Label("Evangelion Type:"), typeCombo),
                new HBox(10, new Label("X:"), xField),
                new HBox(10, new Label("Y:"), yField)
        );
        return vbox;
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

        // --- Right-aligned area that contains both the Turn End and Confirm buttons ---
        HBox rightBox = new HBox(10);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(rightBox, Priority.ALWAYS);

        turnEndContainer = new HBox(10);
        turnEndContainer.setAlignment(Pos.CENTER_RIGHT);

        confirmContainer = new HBox(10);
        confirmContainer.setAlignment(Pos.CENTER_RIGHT);

        rightBox.getChildren().addAll(turnEndContainer, confirmContainer);

        topBar.getChildren().addAll(activeLabel, activePlayerLabel, currentPlayerLabel, rightBox);
    }

    private void updateTopBarColor() {
        if (topBar == null) return;

        boolean isMyTurn = activePlayer != null && activePlayer.equals(currentPlayer);

        if (isMyTurn) {
            topBar.setStyle("-fx-background-color: #2e7d32;");
        } else {
            topBar.setStyle("-fx-background-color: #444444;");
        }

        updateTurnEndButton(isMyTurn);
    }

    /** Adds or removes the grey "Turn End" button depending on whose turn it is. */
    private void updateTurnEndButton(boolean isMyTurn) {
        if (turnEndContainer == null) return;
        turnEndContainer.getChildren().clear();

        if (!isMyTurn) return;
        if (currentPlayer == null || currentPlayer.isEmpty()) return;
        if (currentPlayer.equalsIgnoreCase("DM")) return; // DM has its own tools

        BetterButton turnEndBtn = new BetterButton("Turn End");
        turnEndBtn.setPrefHeight(10);
        // Grey look
        turnEndBtn.setStyle(
                "-fx-background-color: #888888;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 4;" +
                        "-fx-padding: 4 14 4 14;"
        );
        turnEndBtn.setOnAction(e -> showTurnEndPopup());

        turnEndContainer.getChildren().add(turnEndBtn);
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

    // ---- DM command history ----
    private final List<String> dmCommandHistory = new ArrayList<>();
    private int dmHistoryIndex = 0;

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

// ---- Arrow-Up / Arrow-Down navigate the command history ----
        commandInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.UP) {
                if (!dmCommandHistory.isEmpty() && dmHistoryIndex > 0) {
                    dmHistoryIndex--;
                    commandInput.setText(dmCommandHistory.get(dmHistoryIndex));
                    commandInput.positionCaret(commandInput.getText().length());
                }
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN) {
                if (dmHistoryIndex < dmCommandHistory.size() - 1) {
                    dmHistoryIndex++;
                    commandInput.setText(dmCommandHistory.get(dmHistoryIndex));
                    commandInput.positionCaret(commandInput.getText().length());
                } else if (dmHistoryIndex == dmCommandHistory.size() - 1) {
                    // Stepped past the newest entry -> back to an empty prompt
                    dmHistoryIndex = dmCommandHistory.size();
                    commandInput.clear();
                }
                event.consume();
            }
        });

        BetterButton sendCmdBtn = new BetterButton("Send");
        sendCmdBtn.setPrimaryStyle();
        sendCmdBtn.setOnAction(e -> {
            String cmd = commandInput.getText().trim();
            if (!cmd.isEmpty()) {
                // Record in history (skip exact duplicates of the last command)
                if (dmCommandHistory.isEmpty()
                        || !dmCommandHistory.get(dmCommandHistory.size() - 1).equals(cmd)) {
                    dmCommandHistory.add(cmd);
                }
                dmHistoryIndex = dmCommandHistory.size();

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
            updateDMUIScreens();
        });

        return wrapper;
    }

    public void updateDMUIScreens(){
        if (namePanel != null) {
            updateNameSelectionUI(namePanel);
        }
        if (turnChoicePanel != null) {
            updateTurnChoiceUI(turnChoicePanel);
        }
    }


    // ---- Updated LogMessage to use the message log panel ----
    private static final int MAX_LOG_LINES = 200;

    public void LogMessage(String msg) {
        System.out.println("Log " + msg);
        if (msgLogOutput == null) return;

        msgLogOutput.appendText(msg + "\n");

        // Trim if we exceed the cap
        String text = msgLogOutput.getText();
        long lineCount = text.chars().filter(c -> c == '\n').count();
        if (lineCount > MAX_LOG_LINES) {
            int cut = (int) (lineCount - MAX_LOG_LINES);
            int idx = 0;
            for (int i = 0; i < cut; i++) {
                idx = text.indexOf('\n', idx) + 1;
                if (idx <= 0) { idx = 0; break; }
            }
            String trimmed = text.substring(idx);
            msgLogOutput.setText(trimmed);
            msgLogOutput.positionCaret(trimmed.length());
        } else {
            // Auto-scroll to bottom
            msgLogOutput.positionCaret(text.length());
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


    private void updateTurnChoiceUI(VBox panel) {
        panel.getChildren().clear();

        Label info = new Label("Click a unit to make them the next player:");
        info.setStyle("-fx-font-size: 12px;");
        panel.getChildren().add(info);

        // Group live units by team
        Map<Integer, List<FieldUnit>> byTeam = new TreeMap<>();
        for (FieldUnit unit : UnitList) {
            if (!unit.isExists()) continue;
            if (unit.isTurnDone() && !showAllUnits) continue;
            byTeam.computeIfAbsent(unit.getTeam(), k -> new ArrayList<>()).add(unit);
        }

        for (Map.Entry<Integer, List<FieldUnit>> e : byTeam.entrySet()) {
            Label teamHeader = new Label("Team " + e.getKey());
            teamHeader.setStyle("-fx-font-weight: bold; -fx-underline: true; -fx-padding: 4 0 0 0;");
            panel.getChildren().add(teamHeader);

            VBox buttonList = new VBox(5);
            buttonList.setAlignment(Pos.CENTER_LEFT);
            for (FieldUnit unit : e.getValue()) {
                String label = unit.getName() + (unit.isTurnDone() ? "  (done)" : "");
                BetterButton btn = new BetterButton(label);
                btn.setPrimaryStyle();
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setOnAction(ev -> {
                    DMChoosePlayerAction action = new DMChoosePlayerAction(
                            currentActionNumber, "DM", unit.getName());
                    SendAction(action);
                    LogMessage("DM chose " + unit.getName() + " as the next player.");
                });
                buttonList.getChildren().add(btn);
            }
            panel.getChildren().add(buttonList);
        }

        if (byTeam.isEmpty()) {
            Label empty = new Label(showAllUnits
                    ? "No units on the field."
                    : "All units have finished their turn. Type /showAllUnits to view them.");
            empty.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            panel.getChildren().add(empty);
        }
    }

    // ---- Other Tabs (unchanged) ----
    private VBox buildInventoryTab() {
        inventoryTab = new VBox(10);
        inventoryTab.setPadding(new Insets(10));
        inventoryTab.setAlignment(Pos.TOP_LEFT);
        rebuildInventoryTab();
        return inventoryTab;

    }
    private Node createMiniBattlefieldGrid(Evangelion eva) {
        int reach = eva.getItemReach()*2;
        int size = 2 * reach + 1;
        int cellSize = 40;

        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(0);

        FieldUnit fu = getUnitFromName(currentPlayer);
        int centerX = fu.getX();
        int centerY = fu.getY();

        // ---- Create cells as StackPanes so we can layer rectangle + circle + item icon ----
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                int absX = centerX + dx;
                int absY = centerY + dy;

                StackPane cellPane = new StackPane();
                cellPane.setPrefSize(cellSize, cellSize);
                cellPane.setMinSize(cellSize, cellSize);
                cellPane.setMaxSize(cellSize, cellSize);

                Rectangle bg = new Rectangle(cellSize, cellSize);
                bg.setStroke(Color.BLACK);
                bg.setStrokeWidth(0.5);
                if (absX < 0 || absX >= battlefield.sizeX || absY < 0 || absY >= battlefield.sizeY) {
                    bg.setFill(Color.DARKGRAY);
                } else if (gameBoard != null) {
                    Sector sector = gameBoard.getSector(absX, absY);
                    bg.setFill(sector != null ? sector.getType().getColor() : Color.LIGHTGRAY);
                } else {
                    bg.setFill(Color.LIGHTGRAY);
                }
                cellPane.getChildren().add(bg);

                // --- Unit circle on the mini-grid ---
                FieldUnit u = getUnitAt(absX, absY);
                if (u != null && u.isExists()) {
                    Color fill = Color.CORNFLOWERBLUE;
                    Color stroke = Color.CORAL;
                    Node orig = u.getUnitCircle();
                    if (orig instanceof Circle) {
                        Paint p = ((Circle) orig).getFill();
                        if (p instanceof Color) fill = (Color) p;
                        Paint e = ((Circle) orig).getStroke();
                        if (p instanceof Color) stroke = (Color) e;
                    }
                    Circle mini = new Circle(cellSize * 0.28, fill);
                    mini.setStroke(stroke);
                    mini.setMouseTransparent(true);
                    cellPane.getChildren().add(mini);
                }

                // --- Field item icon on the mini-grid ---
                FieldItem fi = getFieldItemAt(absX, absY);
                if (fi != null) {
                    ImageView icon = getItemIcon(fi.getItem());
                    icon.setFitWidth(cellSize * 0.7);
                    icon.setFitHeight(cellSize * 0.7);
                    icon.setPreserveRatio(true);
                    icon.setUserData(fi);
                    setupDragFromFieldItem(icon, fi);
                    cellPane.getChildren().add(icon);
                }

                grid.add(cellPane, dx + reach, dy + reach);
            }
        }

        // ---- Panning (unchanged) ----
        Pane clipPane = new Pane(grid);
        clipPane.setPrefSize(cellSize * size, cellSize * size);
        clipPane.setClip(new Rectangle(cellSize * size, cellSize * size));

        final double[] dragStart = new double[2];
        clipPane.setOnMousePressed(e -> {
            if (e.isSecondaryButtonDown()) {
                dragStart[0] = e.getSceneX() - clipPane.getLayoutX();
                dragStart[1] = e.getSceneY() - clipPane.getLayoutY();
            }
        });
        clipPane.setOnMouseDragged(e -> {
            if (e.isSecondaryButtonDown()) {
                clipPane.setLayoutX(e.getSceneX() - dragStart[0]);
                clipPane.setLayoutY(e.getSceneY() - dragStart[1]);
            }
        });

        // ---- Drop targets: now on the StackPane, not the Rectangle ----
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                final int targetX = centerX + dx;
                final int targetY = centerY + dy;
                Node cellNode = grid.getChildren().get((dx + reach) * size + (dy + reach));

                cellNode.setOnDragOver(event -> {
                    if (draggedItem != null) event.acceptTransferModes(TransferMode.MOVE);
                    event.consume();
                });
                cellNode.setOnDragDropped(event -> {
                    if (draggedItem != null) {
                        onDraggingItemToBattlefield(draggedItem, draggedFromSlot, targetX, targetY, cellNode);
                        event.setDropCompleted(true);
                        draggedItem      = null;
                        draggedFromSlot  = null;
                        draggedFromNode  = null;
                        draggedFromField = null;
                    }
                    event.consume();
                });
            }
        }

        return clipPane;
    }

    private void onDraggingItemToSlot(Item item, Slot fromSlot, Slot toSlot,
                                      boolean targetHadItem, Node fromNode, Node toNode) {
        String srcName = (fromSlot != null) ? fromSlot.name : "FIELD";
        LogMessage("DRAG ITEM: " + item.getName() + " from " + srcName +
                " to " + toSlot.name + (targetHadItem ? " (replacing existing item)" : " (empty)"));

        clearPreviewArrows();
        String s = "";
        drawUIArrowBetween(fromNode, toNode, Color.ORANGE);
        FieldUnit evaUnit = getUnitFromName(currentPlayer);
        if (draggedFromField != null) {

            if (evaUnit != null && boardContainer != null) {
                double sx = draggedFromField.getX() * 20 + 10;
                double sy = draggedFromField.getY() * 20 + 10;
                double ex = evaUnit.getX() * 20 + 10;
                double ey = evaUnit.getY() * 20 + 10;
                Arrow arrow = new Arrow(boardContainer, Color.CORNFLOWERBLUE, sx, sy, ex, ey,
                        Arrow.ArrowType.PREVIEW);
                arrows.add(arrow);
            }
            confirmAction = InventoryItemTransferAction.pickUpAction(currentActionNumber, currentPlayer,
                    ((Evangelion) evaUnit.getUnit()).getSlots().indexOf(toSlot), draggedFromField.getX()- evaUnit.getX(),
                    draggedFromField.getY() - evaUnit.getY());
            s = "Picking up "+item.getName()+" from ("+draggedFromField.getX()+","+draggedFromField.getY()+") with "+toSlot.getName();
        } else {
            confirmAction = InventoryItemTransferAction.InventorySwitch(currentActionNumber, currentPlayer,
                    ((Evangelion) evaUnit.getUnit()).getSlots().indexOf(fromSlot), ((Evangelion) evaUnit.getUnit()).getSlots().indexOf(toSlot));
            s = "Putting "+item.getName()+" from "+fromSlot.getName()+" to "+toSlot.getName();
        }
        createConfirmButton(confirmAction, Color.BLACK, s);
    }

    private void onDraggingItemToBattlefield(Item item, Slot fromSlot, int x, int y, Node targetCell) {
        LogMessage("DRAG ITEM TO BATTLEFIELD: " + item.getName() + " from " + fromSlot.name +
                " to sector (" + x + "," + y + ")");

        clearPreviewArrows();
        drawUIArrowBetween(draggedFromNode, targetCell, Color.ORANGE);
        FieldUnit evaUnit = getUnitFromName(currentPlayer);
        if (evaUnit != null && boardContainer != null) {
            double sx = evaUnit.getX() * 20 + 10;
            double sy = evaUnit.getY() * 20 + 10;
            double ex = x * 20 + 10;
            double ey = y * 20 + 10;
            Arrow arrow = new Arrow(boardContainer, Color.ORANGE, sx, sy, ex, ey,
                    Arrow.ArrowType.PREVIEW);
            arrows.add(arrow);
        }
        confirmAction = InventoryItemTransferAction.dropAction(currentActionNumber, currentPlayer,
                ((Evangelion) evaUnit.getUnit()).getSlots().indexOf(fromSlot), x- evaUnit.getX(), y- evaUnit.getY());

        createConfirmButton(confirmAction, Color.BLACK, "Dropping "+item.getName()+" from "+fromSlot.getName()+" to "+CordsToText(x, y));
    }

    public String CordsToText(int x, int y) {
        return "("+x+","+y+")";
    }
    public void createFieldItemAt(Item item, int x, int y){
        FieldItem newitem = new FieldItem(item, x, y);
        FieldItemList.add(newitem);
        refreshFieldItems();
    }


    private FieldItem getFieldItemAt(int x, int y) {
        for (FieldItem fi : FieldItemList) {
            if (fi.getX() == x && fi.getY() == y) return fi;
        }
        return null;
    }

    /** Removes every field item at (x, y) and their main-grid icons. */
    private void deleteFieldItemsAt(int x, int y) {
        List<FieldItem> toRemove = new ArrayList<>();
        for (FieldItem fi : FieldItemList) {
            if (fi.getX() == x && fi.getY() == y) toRemove.add(fi);
        }
        for (FieldItem fi : toRemove) {
            FieldItemList.remove(fi);
            Node n = fieldItemNodes.remove(fi);
            if (n != null && grid != null) grid.getChildren().remove(n);
        }
        if (!toRemove.isEmpty())
            LogMessage("Deleted " + toRemove.size() + " field item(s) at (" + x + "," + y + ")");
    }

    /** Removes a specific field item and its main-grid icon. */
    public void removeFieldItem(FieldItem fi) {
        FieldItemList.remove(fi);
        Node n = fieldItemNodes.remove(fi);
        if (n != null && grid != null) grid.getChildren().remove(n);
    }

    /** Re-draws field-item icons on the main battlefield GridPane. */
    public void refreshFieldItems() {
        if (grid == null) return;
        for (Node n : fieldItemNodes.values()) grid.getChildren().remove(n);
        fieldItemNodes.clear();

        for (FieldItem fi : FieldItemList) {
            int x = fi.getX(), y = fi.getY();
            if (x < 0 || x >= battlefield.sizeX || y < 0 || y >= battlefield.sizeY) continue;
            ImageView icon = getItemIcon(fi.getItem());
            icon.setFitWidth(14);
            icon.setFitHeight(14);
            icon.setPreserveRatio(true);
            icon.setMouseTransparent(true);   // don't block clicks on the sector
            GridPane.setRowIndex(icon, y);
            GridPane.setColumnIndex(icon, x);
            GridPane.setHalignment(icon, HPos.CENTER);
            GridPane.setValignment(icon, VPos.CENTER);
            fieldItemNodes.put(fi, icon);
            grid.getChildren().add(icon);
        }
    }

    /** Attaches drag-detection to a field item icon on the mini-battlefield. */
    private void setupDragFromFieldItem(ImageView icon, FieldItem fi) {
        icon.setOnDragDetected(event -> {
            Dragboard db = icon.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString("field-item-drag");
            db.setContent(content);
            draggedItem      = fi.getItem();
            draggedFromSlot  = null;
            draggedFromNode  = icon;
            draggedFromField = fi;
            event.consume();
        });
    }



    public void clearPreviewArrows(){
        for (Arrow arrow : arrows) {
            if (arrow.getArrowType().equals(Arrow.ArrowType.PREVIEW)) arrow.delete();
        }
    }


    private void rebuildInventoryTab() {
        LogMessage("Attempting to rebuild inventory tab");
        inventoryTab.getChildren().clear();
        FieldUnit unit = getUnitFromName(currentPlayer);
        if (unit == null || !(unit.getUnit() instanceof Evangelion)) {
            Label placeholder = new Label("Select an Evangelion to view inventory.");
            inventoryTab.getChildren().add(placeholder);
            return;
        }
        Evangelion eva = (Evangelion) unit.getUnit();
        BuildEvangelionInventory(eva);
    }

    private ScrollableContainer createContainer(double left, double width, double top, double height) {
        ScrollableContainer container = new ScrollableContainer(left, width, top, height);
        container.setStyle("-fx-background-color: #f4f4f4;");
        return container;
    }
    private Node createSlotBox(int slotnum, Evangelion eva) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(5));
        box.setAlignment(Pos.CENTER);
        box.setMinSize(60, 60);
        box.setPrefSize(60, 60);
        box.setStyle("-fx-border-color: #888; -fx-border-width: 1; -fx-background-color: #eee;");

        // Slot name label (small)
        Slot slot = eva.getSlots().get(slotnum);
        Label nameLabel = new Label(slot.name);
        nameLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #333;");
        box.getChildren().add(nameLabel);

        // Item display area
        StackPane itemPane = new StackPane();
        itemPane.setMinSize(40, 40);
        itemPane.setPrefSize(40, 40);
        Item currentItem = eva.getItemFromSlot(slotnum);
        LogMessage("In slot "+slot.getName()+"item is "+ (currentItem != null ? currentItem.getName() : "NULL"));
        if (currentItem != null) {
            ImageView icon = getItemIcon(currentItem);
            icon.setUserData(currentItem); // store item for drag
            itemPane.getChildren().add(icon);
            setupDragFromItem(icon, slot);
        }
        box.getChildren().add(itemPane);

        // Click on box shows description
        box.setOnMouseClicked(e -> {
            if (currentItem != null) {
                descriptionLabel.setText(currentItem.getDescription());
            } else {
                descriptionLabel.setText("Empty slot.");
            }
        });

        // Drop target for items
        setupDropTarget(box, slot, eva);

        return box;
    }

    public Pane invPane;
    public BorderPane battlePane;
    public Pane chatPane;
    public Pane dmPane;

    private void BuildEvangelionInventory(Evangelion eva) {
        // Root pane
        invPane = new Pane();
        invPane.setPrefSize(900, 800);
        invPane.setStyle("-fx-background-color: #f4f4f4;");

        // Relative layout constants (unchanged)
        final double MAIN_LEFT = 0.2, MAIN_WIDTH = 0.4, MAIN_TOP = 0.1, MAIN_HEIGHT = 0.6;
        final double EXTRA_LEFT = 0.2, EXTRA_WIDTH = 0.4, EXTRA_TOP = 0.72, EXTRA_HEIGHT = 0.1;
        final double DESC_LEFT = 0.62, DESC_WIDTH = 0.2, DESC_TOP = 0.1, DESC_HEIGHT = 0.4;
        final double GRID_LEFT = 0.62, GRID_WIDTH = 0.2, GRID_TOP = 0.52, GRID_HEIGHT = 0.2;

        // ============================================================
        //  MAIN CONTAINER — picture with 4 slot boxes on the eva
        // ============================================================
        final Image backgroundImage = new Image(
                Objects.requireNonNull(
                        getClass().getResourceAsStream("/eva/evapicture.png"))
        );
        final double IMG_ASPECT = backgroundImage.getHeight() / backgroundImage.getWidth();

        ScrollableContainer mainContainer =
                createContainer(MAIN_LEFT, MAIN_WIDTH, MAIN_TOP, MAIN_HEIGHT);
        mainContainer.setContainerPadding(new Insets(0));
        mainContainer.setSpacing(0);
        // mainContainer.setBorderStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

        // No scrollbars; the picture fills the viewport exactly.
        mainContainer.setFitToHeight(true);
        mainContainer.setVerticalScrollBarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainContainer.setHorizontalScrollBarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Put the background on the VBox, not on the ScrollPane.
        // contain=true -> whole picture visible, no cropping, aspect preserved.
        mainContainer.getContentBox().setBackground(new Background(new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1.0, 1.0, true, true, /* contain */ true, /* cover */ false)
        )));

        // The four slot boxes
        Node leftArmSlot   = createSlotBox(0, eva);   // character's left arm  -> viewer's right
        Node rightArmSlot  = createSlotBox(1, eva);   // character's right arm -> viewer's left
        Node leftBaseSlot  = createSlotBox(2, eva);   // character's left shoulder  -> viewer's right
        Node rightBaseSlot = createSlotBox(3, eva);   // character's right shoulder -> viewer's left

        // Overlay pane that fills the container; slot positions are bound
        // below to fractions of the *displayed-picture* rectangle.
        Pane slotsPane = new Pane();
        VBox.setVgrow(slotsPane, Priority.ALWAYS);
        slotsPane.setMinSize(0, 0);

        // ---- Bindings used to locate the picture inside the container ----
        DoubleBinding viewportW = Bindings.createDoubleBinding(() -> {
            Bounds b = mainContainer.getViewportBounds();
            return b == null ? 0 : b.getWidth();
        }, mainContainer.viewportBoundsProperty());

        DoubleBinding viewportH = Bindings.createDoubleBinding(() -> {
            Bounds b = mainContainer.getViewportBounds();
            return b == null ? 0 : b.getHeight();
        }, mainContainer.viewportBoundsProperty());

        // Rectangle where the picture is actually drawn
        // (because BackgroundSize contain=true preserves aspect and centers).
        DoubleBinding dispW = Bindings.createDoubleBinding(() -> {
            double w = viewportW.get(), h = viewportH.get();
            return Math.min(w, h / IMG_ASPECT);
        }, viewportW, viewportH);

        DoubleBinding dispH = Bindings.createDoubleBinding(() -> {
            double w = viewportW.get(), h = viewportH.get();
            return Math.min(h, w * IMG_ASPECT);
        }, viewportW, viewportH);

        DoubleBinding dispX = viewportW.subtract(dispW).divide(2.0);
        DoubleBinding dispY = viewportH.subtract(dispH).divide(2.0);

        // Place the slots at fractions of the picture rectangle.
        // (fx, fy) are where the slot *center* should land.
        bindSlotToPicture(leftArmSlot,   dispX, dispY, dispW, dispH, 0.74, 0.54, 60);
        bindSlotToPicture(rightArmSlot,  dispX, dispY, dispW, dispH, 0.26, 0.54, 60);
        bindSlotToPicture(leftBaseSlot,  dispX, dispY, dispW, dispH, 0.70, 0.22, 60);
        bindSlotToPicture(rightBaseSlot, dispX, dispY, dispW, dispH, 0.30, 0.22, 60);

        slotsPane.getChildren().addAll(
                leftArmSlot, rightArmSlot, leftBaseSlot, rightBaseSlot);

        mainContainer.addNode(slotsPane);
        invPane.getChildren().add(mainContainer);

        // ============================================================
        //  EXTRA SLOTS CONTAINER (unchanged)
        // ============================================================
        List<Slot> extraSlots = new ArrayList<>(eva.getSlots());
        extraSlots.remove(eva.getSlots().get(0));
        extraSlots.remove(eva.getSlots().get(1));
        extraSlots.remove(eva.getSlots().get(2));
        extraSlots.remove(eva.getSlots().get(3));

        if (!extraSlots.isEmpty()) {
            ScrollableContainer extraContainer =
                    createContainer(EXTRA_LEFT, EXTRA_WIDTH, EXTRA_TOP, EXTRA_HEIGHT);
            extraContainer.setContainerPadding(new Insets(5));
            extraContainer.setSpacing(5);
            extraContainer.setBackgroundColor(Color.rgb(255, 255, 255, 0.95));
            extraContainer.setBorderStyle(
                    "-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

            HBox extraSlotsBox = new HBox(5);
            for (Slot slot : extraSlots) {
                extraSlotsBox.getChildren().add(
                        createSlotBox(eva.getSlots().indexOf(slot), eva));
            }
            extraContainer.addNode(extraSlotsBox);
            invPane.getChildren().add(extraContainer);
        }

        // ============================================================
        //  DESCRIPTION CONTAINER (unchanged)
        // ============================================================
        ScrollableContainer descContainer =
                createContainer(DESC_LEFT, DESC_WIDTH, DESC_TOP, DESC_HEIGHT);
        descContainer.setContainerPadding(new Insets(10));
        descContainer.setSpacing(5);
        descContainer.setBackgroundColor(Color.rgb(255, 255, 255, 0.95));
        descContainer.setBorderStyle(
                "-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

        descriptionLabel = new Label("No item selected.");
        descriptionLabel.setWrapText(true);
        descContainer.addNode(descriptionLabel);
        invPane.getChildren().add(descContainer);

        // ============================================================
        //  MINI BATTLEFIELD GRID CONTAINER (unchanged)
        // ============================================================
        ScrollableContainer gridContainer =
                createContainer(GRID_LEFT, GRID_WIDTH, GRID_TOP, GRID_HEIGHT);
        gridContainer.setContainerPadding(new Insets(5));
        gridContainer.setSpacing(0);
        gridContainer.setBackgroundColor(Color.rgb(255, 255, 255, 0.95));
        gridContainer.setBorderStyle(
                "-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-border-radius: 8;");

        Node miniGrid = createMiniBattlefieldGrid(eva);
        gridContainer.addNode(miniGrid);
        invPane.getChildren().add(gridContainer);

        inventoryTab.getChildren().add(invPane);
    }

    //HELPER METHODS FOR EVANGELION INVENTORY //

    private Node draggedFromNode;   // NEW: the icon the user started dragging
    /**
     * Walks up the parent chain from {@code node} and returns the "owning" pane.
     * For inventory nodes, this is invPane. As a fallback it returns the
     * top-most Pane it encounters.
     */
    private Pane findOwningPane(Node node) {
        if (node == null) return null;
        Node current = node;
        Pane topPane = null;
        while (current != null) {
            if (current == invPane) return invPane;
            if (current instanceof Pane) topPane = (Pane) current;
            current = current.getParent();
        }
        return topPane;
    }

    /**
     * Draws a UI-style arrow between two nodes (uses Arrow.canvasUI, so the
     * triangle is placed by explicit polygon points — good for arbitrary
     * node positions). The arrow is added to the owning pane of {@code from}.
     * Type is PREVIEW.
     */
    private Arrow drawUIArrowBetween(Node from, Node to, Color color) {
        if (from == null || to == null) return null;
        Pane parent = findOwningPane(from);
        if (parent == null) return null;

        Bounds fromScene   = from.localToScene(from.getBoundsInLocal());
        Bounds toScene     = to.localToScene(to.getBoundsInLocal());
        Bounds parentScene = parent.localToScene(parent.getBoundsInLocal());
        if (fromScene == null || toScene == null || parentScene == null) return null;

        double sx = fromScene.getCenterX() - parentScene.getMinX();
        double sy = fromScene.getCenterY() - parentScene.getMinY();
        double ex = toScene.getCenterX()   - parentScene.getMinX();
        double ey = toScene.getCenterY()   - parentScene.getMinY();

        Arrow arrow = new Arrow(parent, color, sx, sy, ex, ey, Arrow.ArrowType.PREVIEW);
        arrow.DrawArrowUI(sx, sy, ex, ey);   // explicit polygon points, no rotate/layout
        arrows.add(arrow);
        return arrow;
    }


    /**
     * Binds the *center* of {@code slot} to the point (fx, fy) expressed as a
     * fraction of the displayed-picture rectangle. {@code slotSize} is the
     * fixed size (in px) of the slot box (createSlotBox uses 60).
     */
    private void bindSlotToPicture(Node slot,
                                   DoubleBinding dispX, DoubleBinding dispY,
                                   DoubleBinding dispW, DoubleBinding dispH,
                                   double fx, double fy, double slotSize) {
        double half = slotSize / 2.0;
        slot.layoutXProperty().bind(dispX.add(dispW.multiply(fx)).subtract(half));
        slot.layoutYProperty().bind(dispH.multiply(fy).add(dispY).subtract(half));
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

    // ---- Bottom panel ----
    private Pane bottomPanel;
    public double bottomPanelHeight = 150;
    private ScrollableContainer buttonScrollContainer;
    private ScrollableContainer moveScrollContainer;
    private ScrollableContainer attackScrollContainer;
    private ScrollableContainer atPowerScrollContainer;
    private ScrollableContainer otherScrollContainer;

    // ---- Battlefield tab ----
    private VBox buildBattlefieldTab() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(10));
        wrapper.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Battlefield");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        wrapper.getChildren().add(title);

        GameBoard board = createGameBoardFromBattlefield(battlefield);
        this.gameBoard = board;
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

        // ============================================================
        //   BOTTOM PANEL — Pane holding ScrollableContainers
        //   Ratios (relative to the Pane):
        //     Buttons  : left = 0.01, width = 0.20
        //     Contents : left = 0.21, width = 0.78
        //   Top / height of the Pane changes with the Options slider, which
        //   raises the top edge up while growing the height by the same
        //   amount (the bottom edge is pinned by the BorderPane).
        // ============================================================
        bottomPanel = new Pane();
        bottomPanel.setStyle("-fx-background-color: #3a3a3a; " +
                "-fx-border-color: #555; -fx-border-width: 1 0 0 0;");
        bottomPanel.setMinHeight(0);
        bottomPanel.setPrefHeight(bottomPanelHeight);
        bottomPanel.setMaxHeight(bottomPanelHeight);

        // ----- Buttons container (left = 0.01, width = 0.2) -----
        buttonScrollContainer = new ScrollableContainer(
                0.01, 0.20, 0.02, 0.96, false, false);
        buttonScrollContainer.setContainerPadding(new Insets(5));
        buttonScrollContainer.setSpacing(8);
        buttonScrollContainer.setBackgroundColor(Color.rgb(40, 40, 40, 0.95), true); // dark charcoal
        buttonScrollContainer.setBorderStyle(
                "-fx-border-color: #888; -fx-border-width: 1; -fx-border-radius: 4;");
        buttonScrollContainer.setFitToHeight(true);
        buttonScrollContainer.setStyle(
                "-fx-background: rgba(40,40,40,0.950000);" +
                        "-fx-background-color: rgba(40,40,40,0.950000);" +
                        "-fx-background-insets: 0;");

// ----- Move container — warm yellow -----
        moveScrollContainer = new ScrollableContainer(
                0.21, 0.78, 0.02, 0.96, false, false);
        moveScrollContainer.setContainerPadding(new Insets(10));
        moveScrollContainer.setSpacing(8);
        moveScrollContainer.setBackgroundColor(Color.rgb(255, 244, 200, 0.95), true); // pale goldenrod
        moveScrollContainer.setBorderStyle(
                "-fx-border-color: #d4a017; -fx-border-width: 2; -fx-border-radius: 4;");
        moveScrollContainer.setFitToHeight(true);
        moveScrollContainer.setStyle(
                "-fx-background: rgba(255,244,200,0.950000);" +
                        "-fx-background-color: rgba(255,244,200,0.950000);" +
                        "-fx-background-insets: 0;");

// ----- Attack container — warm red -----
        attackScrollContainer = new ScrollableContainer(
                0.21, 0.78, 0.02, 0.96, false, false);
        attackScrollContainer.setContainerPadding(new Insets(10));
        attackScrollContainer.setSpacing(8);
        attackScrollContainer.setBackgroundColor(Color.rgb(255, 220, 220, 0.95), true); // pale red
        attackScrollContainer.setBorderStyle(
                "-fx-border-color: #8b0000; -fx-border-width: 2; -fx-border-radius: 4;");
        attackScrollContainer.setFitToHeight(true);
        attackScrollContainer.setStyle(
                "-fx-background: rgba(255,220,220,0.950000);" +
                        "-fx-background-color: rgba(255,220,220,0.950000);" +
                        "-fx-background-insets: 0;");

// ----- ATPower container — cool blue -----
        atPowerScrollContainer = new ScrollableContainer(
                0.21, 0.78, 0.02, 0.96, false, false);
        atPowerScrollContainer.setContainerPadding(new Insets(10));
        atPowerScrollContainer.setSpacing(8);
        atPowerScrollContainer.setBackgroundColor(Color.rgb(215, 235, 255, 0.95), true); // pale steel blue
        atPowerScrollContainer.setBorderStyle(
                "-fx-border-color: #1e5f8f; -fx-border-width: 2; -fx-border-radius: 4;");
        atPowerScrollContainer.setFitToHeight(true);
        atPowerScrollContainer.setStyle(
                "-fx-background: rgba(215,235,255,0.950000);" +
                        "-fx-background-color: rgba(215,235,255,0.950000);" +
                        "-fx-background-insets: 0;");

// ----- Other container — neutral gray -----
        otherScrollContainer = new ScrollableContainer(
                0.21, 0.78, 0.02, 0.96, false, false);
        otherScrollContainer.setContainerPadding(new Insets(10));
        otherScrollContainer.setSpacing(8);
        otherScrollContainer.setBackgroundColor(Color.rgb(235, 235, 235, 0.95), true); // light gray
        otherScrollContainer.setBorderStyle(
                "-fx-border-color: #555555; -fx-border-width: 2; -fx-border-radius: 4;");
        otherScrollContainer.setFitToHeight(true);
        otherScrollContainer.setStyle(
                "-fx-background: rgba(235,235,235,0.950000);" +
                        "-fx-background-color: rgba(235,235,235,0.950000);" +
                        "-fx-background-insets: 0;");

        // ----- Add to the Pane and bind each to the Pane's size -----
        bottomPanel.getChildren().addAll(
                buttonScrollContainer,
                moveScrollContainer, attackScrollContainer,
                atPowerScrollContainer, otherScrollContainer);

        buttonScrollContainer.bindToRegion(bottomPanel);
        moveScrollContainer.bindToRegion(bottomPanel);
        attackScrollContainer.bindToRegion(bottomPanel);
        atPowerScrollContainer.bindToRegion(bottomPanel);
        otherScrollContainer.bindToRegion(bottomPanel);

        // ----- Populate the buttons container -----
        BetterButton moveBtn = new BetterButton("Move");
        moveBtn.setPrimaryStyle();
        moveBtn.setMaxWidth(Double.MAX_VALUE);

        BetterButton attackBtn = new BetterButton("Attack");
        attackBtn.setPrimaryStyle();
        attackBtn.setMaxWidth(Double.MAX_VALUE);

        BetterButton atPowerBtn = new BetterButton("ATPowers");
        atPowerBtn.setPrimaryStyle();
        atPowerBtn.setMaxWidth(Double.MAX_VALUE);

        BetterButton otherBtn = new BetterButton("Other");
        otherBtn.setPrimaryStyle();
        otherBtn.setMaxWidth(Double.MAX_VALUE);

        buttonScrollContainer.addNode(moveBtn);
        buttonScrollContainer.addNode(attackBtn);
        buttonScrollContainer.addNode(atPowerBtn);
        buttonScrollContainer.addNode(otherBtn);

        // ----- Populate Move content -----
        Label moveLabel = new Label("Move Actions");
        moveLabel.setStyle(
                "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: #8a6800;" +            // dark goldenrod
                        "-fx-padding: 0 0 4 0;"
        );
        moveScrollContainer.addNode(moveLabel);

        BetterButton runBtn = new BetterButton("Run");
        runBtn.setPrimaryStyle();
        runBtn.setMaxWidth(Double.MAX_VALUE);
        moveScrollContainer.addNode(runBtn);

        BetterButton maneuverBtn = new BetterButton("Maneuver");
        maneuverBtn.setPrimaryStyle();
        maneuverBtn.setMaxWidth(Double.MAX_VALUE);
        moveScrollContainer.addNode(maneuverBtn);

        BetterButton coverBtn = new BetterButton("Cover");
        coverBtn.setPrimaryStyle();
        coverBtn.setMaxWidth(Double.MAX_VALUE);
        moveScrollContainer.addNode(coverBtn);

        BetterButton repositionBtn = new BetterButton("Reposition");
        repositionBtn.setPrimaryStyle();
        repositionBtn.setMaxWidth(Double.MAX_VALUE);
        moveScrollContainer.addNode(repositionBtn);

        // ----- Populate Attack content -----
        Label attackLabel = new Label("Attack Actions");
        attackLabel.setStyle(
                "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: #8b0000;" +            // dark red
                        "-fx-padding: 0 0 4 0;"
        );
        attackScrollContainer.addNode(attackLabel);

        // ----- Populate AT Power content -----
        Label atPowerLabel = new Label("AT Powers");
        atPowerLabel.setStyle(
                "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: #1e5f8f;" +            // steel blue
                        "-fx-padding: 0 0 4 0;"
        );
        atPowerScrollContainer.addNode(atPowerLabel);

        // ----- Populate Other content -----
        Label otherLabel = new Label("Other Actions");
        otherLabel.setStyle(
                "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: #333333;" +            // dark gray
                        "-fx-padding: 0 0 4 0;"
        );
        otherScrollContainer.addNode(otherLabel);

        // ----- Only one content container visible at a time -----
        moveScrollContainer.setVisible(true);
        attackScrollContainer.setVisible(false);
        atPowerScrollContainer.setVisible(false);
        otherScrollContainer.setVisible(false);

        // ----- Toggle buttons switch the visible content container -----
        moveBtn.setOnAction(e -> {
            moveScrollContainer.setVisible(true);
            attackScrollContainer.setVisible(false);
            atPowerScrollContainer.setVisible(false);
            otherScrollContainer.setVisible(false);
        });
        attackBtn.setOnAction(e -> {
            moveScrollContainer.setVisible(false);
            attackScrollContainer.setVisible(true);
            atPowerScrollContainer.setVisible(false);
            otherScrollContainer.setVisible(false);
        });
        atPowerBtn.setOnAction(e -> {
            moveScrollContainer.setVisible(false);
            attackScrollContainer.setVisible(false);
            atPowerScrollContainer.setVisible(true);
            otherScrollContainer.setVisible(false);
        });
        otherBtn.setOnAction(e -> {
            moveScrollContainer.setVisible(false);
            attackScrollContainer.setVisible(false);
            atPowerScrollContainer.setVisible(false);
            otherScrollContainer.setVisible(true);
        });

        // ----- Main layout -----
        battlePane = new BorderPane();
        battlePane.setCenter(scrollContainer);
        battlePane.setBottom(bottomPanel);
        VBox.setVgrow(battlePane, Priority.ALWAYS);

        wrapper.getChildren().add(battlePane);
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

        BetterButton weaponCreatorBtn = new BetterButton("Weapon");
        weaponCreatorBtn.setPrimaryStyle();
        weaponCreatorBtn.setOnAction(e -> showTab(weaponCreatorTab));

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



        bar.getChildren().addAll(bfBtn, invBtn, chatBtn, weaponCreatorBtn, dmBtn, optionsBtn, exitBtn);
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

    private boolean popupShowing = false;

    private QueuePosition getCurrentPosition() {
        return queue.currentPosition();
    }

    private void addPositionAfterCurrent(QueuePosition pos) {
        QueuePosition current = getCurrentPosition();
        if (current == null) {
            // No unprocessed turn – append at end
            queue.getQueue().add(pos);
            return;
        }
        queue.addPosition(pos, 1);
    }

    private void addPositionEnd(QueuePosition pos) {
        queue.getQueue().add(pos);
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
        // Keep field-item icons in sync with the battlefield
        refreshFieldItems();
    }



    public void processMoveVisuals(FieldUnit unit, int Sx, int Sy, int Ex, int Ey, double duration) {
        if (duration <= 0) {
            DrawArrow(Color.BLACK, Sx, Sy, Ex, Ey, Arrow.ArrowType.ACTION);
            Node circle = unitCircles.get(unit);
            if (circle != null) {
                GridPane.setRowIndex(circle, Ey);
                GridPane.setColumnIndex(circle, Ex);
                circle.setTranslateX(0);
                circle.setTranslateY(0);
            }
            return;
        }

        drawAnimatedArrow(Color.BLACK, Sx, Sy, Ex, Ey, duration, Arrow.ArrowType.ACTION);
        animateUnitMovement(unit, Sx, Sy, Ex, Ey, duration);
    }

    public void processDropVisuals(int Sx, int Sy, int Ex, int Ey) {
            DrawArrow(Color.ORANGE, Sx, Sy, Ex, Ey, Arrow.ArrowType.ACTION);
    }
    public void processPickUpVisuals(int Sx, int Sy, int Ex, int Ey) {
        DrawArrow(Color.LIGHTBLUE, Sx, Sy, Ex, Ey, Arrow.ArrowType.ACTION);
    }

    private void drawAnimatedArrow(Color color, int oldX, int oldY, int newX, int newY, double duration, Arrow.ArrowType type) {
        double oldCx = oldX * 20 + 10;
        double oldCy = oldY * 20 + 10;
        double newCx = newX * 20 + 10;
        double newCy = newY * 20 + 10;

        Arrow arrow = new Arrow(boardContainer, color, oldCx, oldCy, oldCx, oldCy, type);
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

    public Arrow DrawArrow(Color color, int Sx, int Sy, int Ex, int Ey, Arrow.ArrowType type) {
        double oldCx = Sx * 20 + 10;
        double oldCy = Sy * 20 + 10;
        double newCx = Ex * 20 + 10;
        double newCy = Ey * 20 + 10;

        Arrow arrow = new Arrow(boardContainer, color, oldCx, oldCy,newCx, newCy, type);
        arrows.add(arrow);
        return arrow;
    }

    private ImageView getItemIcon(Item item) {
        String iconName = "weapon_0.png"; // default
        iconName = item.getDisplayIcon();

        try {
            Image img = new Image(getClass().getResourceAsStream("/weapon_icons/" + iconName));
            ImageView iv = new ImageView(img);
            iv.setFitWidth(40);
            iv.setFitHeight(40);
            iv.setPreserveRatio(true);
            return iv;
        } catch (Exception e) {
            // return empty pane if icon missing
            return new ImageView();
        }
    }
    private void setupDragFromItem(ImageView icon, Slot sourceSlot) {
        icon.setOnDragDetected(event -> {
            Dragboard db = icon.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString("item-drag");
            db.setContent(content);
            draggedItem     = (Item) icon.getUserData();
            draggedFromSlot = sourceSlot;
            draggedFromNode = icon;
            draggedFromField = null;
            event.consume();
        });
    }

    private void setupDropTarget(Node target, Slot targetSlot, Evangelion eva) {
        target.setOnDragOver(event -> {
            if (event.getGestureSource() != target && draggedItem != null) {
                if (targetSlot.isDynamic() && targetSlot.isIntact()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
            }
            event.consume();
        });

        target.setOnDragDropped(event -> {
            if (draggedItem != null) {
                onDraggingItemToSlot(
                        draggedItem, draggedFromSlot, targetSlot,
                        eva.getItemFromSlot(eva.getSlots().indexOf(targetSlot)) != null,
                        draggedFromNode, target);
                event.setDropCompleted(true);
                draggedItem      = null;
                draggedFromSlot  = null;
                draggedFromNode  = null;
                draggedFromField = null;
            }
            event.consume();
        });
    }
    // ============================================================
    //   LAUNCH METHODS
    // ============================================================


    public static void startGame(Battlefield field, double speed, boolean fast, int playernumber) {
        startGame(field, speed, fast, playernumber, "DM", true, GameState.GAME_MODE.CLASSIC);
    }

    public static void startGame(Battlefield field, double speed, boolean fast,
                                 int playernumber, String playerName, boolean startNew) {
        startGame(field, speed, fast, playernumber, playerName, startNew,
                GameState.GAME_MODE.CLASSIC);
    }

    public static void startGame(Battlefield field, double speed, boolean fast,
                                 int playernumber, String playerName, boolean startNew,
                                 GameState.GAME_MODE gameMode) {
        System.out.println("Starting game with speed " + speed + " fast " + fast
                + " playernumber " + playernumber + " gamemode " + gameMode);
        if (startNew) {
            new Game(field, "DM", speed, fast, playernumber, true, gameMode);
        } else {
            System.out.println("Connecting as " + playerName);
            new Game(field, playerName, speed, fast, -1, false, gameMode);
        }
    }


}