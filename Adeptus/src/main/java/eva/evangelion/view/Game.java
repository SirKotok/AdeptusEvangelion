package eva.evangelion.view;

import eva.evangelion.gameboard.Battlefield;
import eva.evangelion.gameboard.GameBoard;
import eva.evangelion.gameboard.Sector;
import eva.evangelion.items.Weapon.AttackProfile;
import eva.evangelion.items.Weapon.Weapon;
import eva.evangelion.state.GameStateStore;
import eva.evangelion.units.battle.*;
import eva.evangelion.view.options.GameOptions;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.*;

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

import java.nio.file.*;
import java.util.*;

public class Game {

    // ============================================================
    //   FIELDS
    // ============================================================

    private GameState.GAME_MODE gameMode = GameState.GAME_MODE.CLASSIC;
    private boolean showAllUnits = false;

    private volatile boolean closed = false;
    private final List<Stage> childStages = new ArrayList<>();

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
    private VBox turnChoicePanel;
    /** Non-null only while the DM "setup player" popup is open: receives board clicks. */
    private java.util.function.BiConsumer<Integer, Integer> spawnPickHandler = null;

    // ---- Player & Active Player ----
    private String currentPlayer;              // Current assigned name to this client, local, not stored in GameState
    private String activePlayer;               // Player who's turn it currently is local, not stored in GameState
    private int currentActionNumber = 0;

    // ---- Action processing speed ----
    private double actionSpeed = 1.0;           // default 1x speed
    private boolean fastActions = false;        // if true, force instant processing
    // ---- Attack selection state ----
    private AttackProfile activeAttackProfile = null;   // currently visualized profile
    private Weapon selectedAttackWeapon = null;  // null = neutral / unarmed
    private Slot          selectedAttackWeaponSlot = null;
    private boolean       attackModeActive = false;
    private FieldUnit     attackUnit = null;
    private ScrollableContainer attackWeaponChooserContainer;
    // ---- Turn Actions retaining ---- //
    private String pendingConfirmDescription = null;
    private Color  pendingConfirmColor = Color.GRAY;
    private boolean couldActLastCheck = false;      // detects "your turn just started"
    private int lastMoveClickX = -1, lastMoveClickY = -1;

    /** True only when the queue's current position is a normal (non-reaction) turn of this client's player. */
    private boolean isMyActionTurn() {
        if (queue == null || currentPlayer == null) return false;
        QueuePosition cur = queue.currentPosition();
        return cur != null && !cur.isReaction() && currentPlayer.equals(cur.getUnitID());
    }


    // ---- UI top bar ----
    private HBox topBar;
    private HBox confirmContainer;
    private Label activePlayerLabel;
    private Label currentPlayerLabel;

    // ---- GameState persistence ----
    private static final String GAME_STATE_DIR = "Active";
    private static final String GAME_STATE_FILE = "gamestate.ser";
    private Path stateDir;   // activegame or DMgame
    private boolean dmSandbox;                   // true when working in DMgame
    private String knownSessionId;                       // session of the state this client is running
    private boolean sessionKnown = false;
    private volatile boolean restarting = false;

    private Path stateFile() { return GameStateStore.file(stateDir); }
    // ---- Units ----
    private final ObservableList<FieldUnit> UnitList = FXCollections.observableArrayList();
    private final Map<FieldUnit, Node> unitCircles = new HashMap<>();
    // ---- Field items ----
    private final ObservableList<FieldItem> FieldItemList = FXCollections.observableArrayList();
    private final Map<FieldItem, Node> fieldItemNodes = new HashMap<>();
    private FieldItem draggedFromField = null;   // non-null when dragging from the mini-battlefield

    // ---- Movement visualization ----
    private MoveAction.MOVEMENTTYPE activeMoveMode = null;   // null = not in move mode
    private FieldUnit movementUnit = null;

    private final Map<String, Color> previewVisualisationLayer = new LinkedHashMap<>();
    private final Map<String, Color> attackVisualisationLayer  = new LinkedHashMap<>();

    // Attack helper
    private boolean attackUnarmedChosen = false;   // "Neutral (Unarmed)" explicitly picked


    private final GameOptions  options = GameOptions.load();
    
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

    /** True only while the initial batch of actions is being loaded. Forces fast processing. */
    private boolean initialReload;
    private boolean initialLoad = true;


    /** Fast if the user chose fast actions, or if we are loading a game. */
    private boolean isFast() {
        return fastActions || initialReload;
    }

    // ============================================================
    //   CONSTRUCTORS
    // ============================================================
    private int startingplayernumber;

    public Game(Battlefield battlefield, String playerName, double speed, boolean fast,
                int playernumber, boolean startnew, GameState.GAME_MODE gameMode, boolean initialReloading) throws IOException {
        this(battlefield, playerName, speed, fast, playernumber, startnew, gameMode,
                GameStateStore.activeDir(), false, initialReloading);
    }

    /** round -> number of turns that happened in all earlier rounds. Round 0 has offset 0. */
    private final Map<Integer, Integer> roundStartOffset = new HashMap<>();


    public Game(Battlefield battlefield, String playerName, double speed, boolean fast,
                int playernumber, boolean startnew, GameState.GAME_MODE gameMode,
                Path stateDir, boolean sandbox, boolean initialReloading) throws IOException {
        this.initialReload = initialReloading;
        this.stateDir = stateDir;
        this.dmSandbox = sandbox;
        this.currentPlayer = (playerName != null && !playerName.isEmpty()) ? playerName : "Player";

        this.gameMode = (gameMode != null) ? gameMode : GameState.GAME_MODE.CLASSIC;
        GameStateStore.ensureDirs();
        GameStateStore.migrateLegacy(Paths.get(GAME_STATE_DIR, GAME_STATE_FILE));

        System.out.println("STARTING GAME WITH "+playernumber+" PLAYERNUMBER AND STARTNEW = "+startnew);
        this.battlefield = battlefield;
        this.actionSpeed = speed;
        this.fastActions = fast;
        bottomPanelHeight = options.bottomPanelHeight;
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
        stage.setTitle(dmSandbox ? "[DM SANDBOX - not shared with players]" : "Adeptus Evangelion");
        stage.show();


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

        stage.setOnHidden(e -> shutdown());

    }


    // ============================================================
    //   GAME STATE PERSISTENCE
    // ============================================================

    private void startGameStateWatcher() {
        Path dir = stateDir;
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
        Path file = stateFile();
        if (Files.exists(file)) {
            markInitialBatch = true;
            try {
                reloadGameState();
            } finally {
                markInitialBatch = false;
                initialReload = false;
                initialLoad = false;
            }
        } else {
            gamestate = new GameState();
            knownSessionId = gamestate.getSessionId();
            sessionKnown = true;
            saveGameState();
        }
    }

    /** Actions up to this number were already in the file when the game was opened: no info popups for them. */
    private int suppressPopupsUpTo = 0;
    private boolean markInitialBatch = false;
    /** Number of the action that processAction is working on right now. */
    private int processingActionNumber = Integer.MAX_VALUE;

    private boolean isReplay() {
        return processingActionNumber <= suppressPopupsUpTo;
    }

    private void reloadGameState() {
        Path file = stateFile();
        if (!Files.exists(file)) return;
        try {
            GameState newState = GameState.loadFromFile(file);
            if (newState == null) return;                 // loadFromFile can return null

            if (isSessionReplaced(newState)) {
                onSessionReplacedByDM();
                return;
            }

            if (!sessionKnown) {                          // first successful load: remember it
                knownSessionId = newState.getSessionId();
                sessionKnown = true;
            }
            if (markInitialBatch) {
                int max = 0;
                for (Action a : newState.getActions()) max = Math.max(max, a.getActionNumber());
                suppressPopupsUpTo = max;
                markInitialBatch = false;
            }

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

                if (hasDuplicateActionNumbers(newState)) {
                    newState = CorruptedStateAttemptedFix(newState);
                    gamestate = newState;
                    saveGameState();          // overwrite the file with the repaired state
                }



                processNewState(newState);
            }
        } catch (IOException | ClassNotFoundException e) {
            LogMessage("Failed to load GameState: " + e.getMessage());
        }
    }

    private void saveGameState() {
        try {
            GameStateStore.write(gamestate, stateDir);
        } catch (IOException e) {
            LogMessage("Failed to save GameState: " + e.getMessage());
        }
    }

    /** Reads the file on disk, or null on any failure. */
    private GameState loadStateQuietly() {
        try {
            return GameState.loadFromFile(stateFile());
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    /** Freshest version of the game: the disk copy if readable, else what we hold in memory. */
    private GameState currentStateSnapshot() {
        GameState onDisk = loadStateQuietly();
        return onDisk != null ? onDisk : gamestate;
    }


    // ---- processNewState: collects new actions and starts sequential processing ----
    private void processNewState(GameState state) {
        if (closed) return;
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
        if (closed || isProcessing || pendingActions.isEmpty()) return;
        isProcessing = true;

        if (isFast()) {
            List<Action> actionsCopy;
            synchronized (pendingActions) {
                actionsCopy = new ArrayList<>(pendingActions);
                pendingActions.clear();
            }
            for (Action action : actionsCopy) {
                processAction(action);
                LogMessage("Processed action (fast): " + action);
            }
            pruneActionArrows();
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
                if (closed) return;
                processAction(action);
                LogMessage("Processed action: " + action);
                pruneActionArrows();
            });

            processingTimeline.getKeyFrames().add(kf);
        }

        processingTimeline.setOnFinished(e -> {
            if (closed) return;
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

    // ---- processAction: handles all action processing
    private void processAction(Action action) {
        processingActionNumber = action.getActionNumber();
        processActionCost(action);
        if (action instanceof MoveAction) {
            processMoveAction((MoveAction) action);
        } else if (action instanceof AttackAction) {
            processAttackAction((AttackAction) action);
        } else if (action instanceof DMChoosePlayerAction) {
            processDMChoosePlayerAction((DMChoosePlayerAction) action);
        } else if (action instanceof DMCreateUnitAction) {
            processDMCreateUnitAction((DMCreateUnitAction) action);
        } else if (action instanceof DMGiveWeaponAction) {
            processDMGiveWeaponAction((DMGiveWeaponAction) action);
        }  else if (action instanceof EndRoundAction) {
            processForcedRoundEnd((EndRoundAction) action);
        } else if (action instanceof DMDeleteUnitAction) {
            processDMDeleteUnitAction((DMDeleteUnitAction) action);
        } else if (action instanceof DefenceAction) {
            processDefenceAction((DefenceAction) action);
        } else if (action instanceof DealWoundAction) {
            processDealWoundAction((DealWoundAction) action);
        } else if (action instanceof createDMSetUpPopUpAction) {
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
        queueWoundsIfNeeded(action);
        LogMessage(outputQueue());
        LogMessage(outputActions());
        rebuildInventoryTab();
        QueuePosition next = queue.currentPosition();
        if (next != null) {
            LogMessage("Setting active player to "+next.getUnitID()+" at process action end, current position in queue is "
                    +queue.getQueue().indexOf(next));
            setActivePlayer(next.getUnitID());
        }
        refreshStatsContainers();
        updateAttackButtonState();
        renderConfirmButton();

    }



    // ============================================================
//   ATTACK PROCESSING
// ============================================================

    /** Area attacks (and their overlap) also hit the attacker if he stands inside the zone. */
    //TODO make part of profile?
    private static final boolean AREA_CAN_HIT_ATTACKER = true;
    private static final int LAYERED_FIELD_ARMOR = 3;

    private record Cell(int x, int y) {}


    /** After every action: any unit at 0 Toughness or less gets an unskippable WOUND reaction turn, right away. */
    private void queueWoundsIfNeeded(Action cause) {
        int insertAt = 0;
        for (FieldUnit u : UnitList) {
            if (!u.isExists() || u.getToughness() > 0) continue;
            if (hasPendingWound(u.getName())) continue;

            int sourceNumber = lastDamagedBy.getOrDefault(u.getName(), cause.getActionNumber());
            Action source = gamestate.getActionfromNumber(sourceNumber);
            String by = (source instanceof AttackAction a) ? a.getActor() : "an unknown source";

            QueuePosition wound = QueuePosition.createReactionTurn(u.getName(), ReactionType.WOUND,
                    "Your Toughness has reached 0. You have received a Wound from " + by + ".", sourceNumber);
            wound.setSkippable(false);
            queue.addPosition(wound, insertAt++);
            LogMessage("Added WOUND reaction for " + u.getName() + " (caused by action " + sourceNumber + ")");
        }
    }

    private boolean hasPendingWound(String unitName) {
        for (QueuePosition p : queue.getQueue()) {
            if (p.isReaction() && p.getReactionType() == ReactionType.WOUND
                    && p.getActionNumber() < 0 && unitName.equals(p.getUnitID())) return true;
        }
        return false;
    }
    /** Your turn is running and this unit has already used its Attack Action. */
    private boolean alreadyAttackedThisTurn(FieldUnit u) {
        return u != null && isMyActionTurn() && !u.canAttack();
    }

    /** For now only restores Toughness. Wound effects come later. */
    private void processDealWoundAction(DealWoundAction wound) {
        FieldUnit unit = getUnitFromName(wound.getActor());
        QueuePosition pos = queue.currentPosition();       // grab BEFORE activateQueue moves on

        if (unit == null || !unit.isExists() || pos == null
                || pos.getReactionType() != ReactionType.WOUND
                || !wound.getActor().equals(pos.getUnitID())) {
            LogError("ERROR - DealWoundAction " + wound.getActionNumber() + " has no matching wound turn\n");
            activateQueue(wound);
            return;
        }

        unit.setToughness(unit.getMaxToughness());         // TODO Wound effects / Wound Level / Hit Location
        LogMessage(unit.getName() + " received a Wound. Toughness restored to " + unit.getMaxToughness());
        activateQueue(wound);
    }


    private void processAttackAction(AttackAction attack) {
        FieldUnit attacker = getUnitFromName(attack.getActor());
        AttackProfile profile = attack.getActionCombatProfile();
        if (attacker == null || !attacker.isExists() || profile == null) {
            LogError("ERROR - attacker or attack profile missing at action " + attack.getActionNumber() + "\n");
            activateQueue(attack);     // don't stall the queue
            return;
        }

        // ---- 1. TN sanity check (must be done BEFORE ATTACK effects are cleared) ----
        int expectedTN = AttackAction.clampTN(attacker.getAccuracy());
        if (expectedTN != attack.accuracyTN) {
            LogError("ERROR - accuracy TN mismatch at action " + attack.getActionNumber()
                    + ": action says " + attack.accuracyTN + ", attacker has " + expectedTN + "\n");
        }
        // ---- 1b. One Attack Action per Turn / Interval ----
        if (!attack.isAttackOfOpportunity) {
            if (!attacker.canAttack()) {
                LogError("ERROR - " + attacker.getName() + " already attacked this turn, action "
                        + attack.getActionNumber() + "\n");
            }
            attacker.setHasAttacked(true);
        }


        // ---- 2. Ammo ----
        spendAmmo(attacker, attack, profile);

        // ---- 3. Every affected sector, each exactly once ----
        Set<Cell> cells = collectAffectedCells(attacker, attack, profile);

        // ---- 4. Sector replacement (Area and Line only) ----
        if (profile.AreaType != -1) replaceSectors(cells, profile);

        // ---- 5. Who is hit, who is missed ----
        boolean canMiss = profile.AreaType == -1;
        boolean rollOk = attack.isRollSuccess();
        List<FieldUnit> hitUnits = new ArrayList<>();
        List<FieldUnit> missedUnits = new ArrayList<>();
        for (Cell c : cells) {
            FieldUnit u = getUnitAt(c.x(), c.y());
            if (u == null || !u.isExists()) continue;
            if (u == attacker && (canMiss || !AREA_CAN_HIT_ATTACKER)) continue;
            if (canMiss && !rollOk) missedUnits.add(u);
            else hitUnits.add(u);
        }

        for (FieldUnit missed : missedUnits) { //TODO show only when attack action is the last action in the processing queue, no need to show if it moved past it (initial load doesnt fully help here, need replacement)
            LogMessage(attacker.getName() + " missed " + missed.getName());
            if (!initialReload && missed.getName().equals(currentPlayer)) {
                showAttackMissedPopup(attacker.getName());
            }
        }

        // ---- 6. Visuals ----
        animateAttack(attacker, attack);

        // ---- 7. Queue: attacker(processed) -> defences -> attacker ----
        activateQueue(attack);

        boolean halved = isReducedByMiss(attack);
        int insertAt = 0;
        for (FieldUnit target : hitUnits) {
            // Guard is disabled against a missed Area/Line, so then only Layered Field can help.
            boolean canReact = target.canDefend() && (target.getATP() > 0 || !halved);
            if (!canReact) {
                processHit(attack, attacker, target, 0);
                continue;
            }
            String desc = "You have been "+(attack.actionCombatProfile.isRanged() ? "shot at by " : "hit in melee by ") + attacker.getName() + " and they"
                    + (halved ? " missed, but the " + (profile.AreaType == -2 ? "line" : "area")
                    + " attack still deals half damage" : " hit")
                    + ", choose your defence option.";
            QueuePosition reaction = QueuePosition.createReactionTurn(
                    target.getName(), ReactionType.DEFENCE, desc, attack.getActionNumber());
            reaction.setSkippable(false);
            queue.addPosition(reaction, insertAt++);
            LogMessage("Added DEFENCE reaction for " + target.getName());
        }

        Weapon usedWeapon = getUsedWeapon(attacker, attack);
        attacker.ClearEffects(Effect.EffectEnd.ATTACK, usedWeapon);   // also clears ATTACK effects on the weapon used
        applySelfAttackEffectsToUnitorWeapon(attacker, attack, usedWeapon);               // new penalties go on AFTER the clearing

        QueuePosition attackerTurn = new QueuePosition(attacker.getName(), false);
        attackerTurn.setActionNumber(-1);
        queue.addPosition(attackerTurn, insertAt);
    }


    private void processDefenceAction(DefenceAction def) {
        FieldUnit defender = getUnitFromName(def.getActor());
        QueuePosition pos = queue.currentPosition();   // grab BEFORE activateQueue moves on

        if (defender == null || !defender.isExists() || pos == null
                || pos.getReactionType() != ReactionType.DEFENCE) {
            LogError("ERROR - DefenceAction " + def.getActionNumber() + " has no matching defence turn\n");
            activateQueue(def);
            return;
        }

        if (def.guardRolled) {
            if (AttackAction.clampTN(defender.getReflexes()) != def.guardTN) {
                LogError("ERROR - guard TN mismatch at action " + def.getActionNumber() + "\n");
            }
            defender.setUsedGuard(true);
            defender.ClearEffects(Effect.EffectEnd.GUARD);   // e.g. Superconductive (Guard)
        }

        Action reactedTo = gamestate.getActionfromNumber(pos.getReactionTo());
        if (reactedTo instanceof AttackAction atk) {
            FieldUnit attacker = getUnitFromName(atk.getActor());
            int bonusArmor = def.layeredField ? LAYERED_FIELD_ARMOR : 0;
            boolean guardSuccess = def.isGuardSuccess()
                    && !isReducedByMiss(atk)
                    && canBeDefendedAgainst(atk.getActionCombatProfile());
            int roll = def.guardRolled ? def.guardRoll : 0;
            processHit(atk, attacker, defender, bonusArmor, guardSuccess, roll, def.guardTN);   // Strain goes through either way
        } else {
            LogError("ERROR - DefenceAction is not reacting to an AttackAction\n");
        }

        activateQueue(def);    // just move on, the queue is not changed
    }


    /**
     * Damage -> (halved if Area/Line missed) -> Armor (+ bonus) reduced by Penetration -> Toughness.
     * Then every on-hit effect of the profile is copied onto the defender if damage got through.
     * The attacker is currently unused.
     */
    /** Everything that happened to one defender in one hit. Used for logs and info popups. */
    private record HitResult(String attackerName, boolean areaOrLine, boolean halved, boolean guarded,
                             int rawDamage, int damageAfterHalving,
                             int baseArmor, int bonusArmor, int penetration, int armorAfterPen,
                             int blockedByArmor, int wouldHaveDealt, int damageDealt, int strain,
                             int toughnessBefore, int toughnessAfter, int maxToughness,
                             int guardRoll, int guardTN, List<String> effectsGained) {}

    /** Defender name -> number of the attack that last took Toughness from it (used for Wound reactions). */
    private final Map<String, Integer> lastDamagedBy = new HashMap<>();

    private HitResult processHit(AttackAction attack, FieldUnit attacker, FieldUnit defender, int bonusArmor) {
        return processHit(attack, attacker, defender, bonusArmor, false, 0, 0);
    }

    /**
     * Damage -> (halved if Area/Line missed) -> Armor (+bonus) reduced by Penetration -> Toughness.
     * Strain is added on top: it ignores Armor, is not halved and is applied even if the defender guarded.
     * On-hit effects are applied only if real damage got through (Strain alone does not count).
     * If guarded, the damage is still calculated (for the info popup) but not dealt.
     */
    private HitResult processHit(AttackAction attack, FieldUnit attacker, FieldUnit defender,
                                 int bonusArmor, boolean guarded, int guardRoll, int guardTN) {
        AttackProfile profile = attack.getActionCombatProfile();
        if (profile == null || defender == null || !defender.isExists()) return null;

        boolean areaOrLine = profile.AreaType != -1;
        boolean halved = isReducedByMiss(attack) || (areaOrLine && guarded);   // never halved twice
        boolean negated = guarded && !areaOrLine;                              // only normal attacks are fully stopped
        int raw = attack.rolledDamage;
        int afterHalf = halved ? raw / 2 : raw;

        int baseArmor = defender.getArmor();
        int armorAfterPen = Math.max(0, baseArmor + bonusArmor - profile.Penetration);
        int blocked = Math.min(afterHalf, armorAfterPen);
        int wouldHave = afterHalf - blocked;
        int damage = negated ? 0 : wouldHave;
        int strain = Math.max(0, profile.Strain);

        int before = defender.getToughness();
        int totalLoss = damage + strain;
        if (totalLoss > 0) {
            defender.DealToughnessDamage(totalLoss);
            lastDamagedBy.put(defender.getName(), attack.getActionNumber());
        }
        int after = defender.getToughness();

        List<String> gained = new ArrayList<>();
        if (damage > 0) {
            for (Effect e : profile.onHitEffects) {
                defender.addEffect(e.copy());         // FieldUnit.addEffect skips non-stacking duplicates
                gained.add(e.getName());
            }
        }

        String attackerName = attacker != null ? attacker.getName() : attack.getActor();
        LogMessage(defender.getName() + (negated ? " guarded" : " is hit") + ": " + raw + " raw"
                + (halved ? " (halved to " + afterHalf + ")" : "") + ", armor after penetration " + armorAfterPen
                + ", " + damage + " damage + " + strain + " strain. Toughness " + before + " -> " + after);
        if (totalLoss > before) LogMessage(defender.getName() + " has " + (totalLoss - before) + " overflow damage");
        for (String g : gained) LogMessage(defender.getName() + " gains " + g);

        HitResult result = new HitResult(attackerName, profile.AreaType != -1, halved, guarded,
                raw, afterHalf, baseArmor, bonusArmor, profile.Penetration, armorAfterPen,
                blocked, wouldHave, damage, strain,
                before, after, defender.getMaxToughness(), guardRoll, guardTN, gained);

        if (shouldShowInfoFor(defender)) {
            if (negated) showGuardSuccessPopup(result);
            else showHitPopup(result);
        }
        return result;
    }






    /** True for Area/Line attacks whose roll failed: they still hit, but for half damage. */
    private boolean isReducedByMiss(AttackAction attack) {
        AttackProfile p = attack.getActionCombatProfile();
        return p != null && p.AreaType != -1 && !attack.isRollSuccess();
    }

    /** TODO: hook for future profile properties that make an attack impossible to Guard against. */
    private boolean canBeDefendedAgainst(AttackProfile profile) {
        return true;
    }

    private void spendAmmo(FieldUnit attacker, AttackAction attack, AttackProfile profile) {
        if (profile.AmmoCost <= 0) {
            return;
        }
        List<Slot> slots = attacker.getSlots();
        if (slots == null || attack.slotNumber < 0 || attack.slotNumber >= slots.size()
                || !(slots.get(attack.slotNumber).getItem() instanceof Weapon w)) {
            LogError("ERROR - attack at " + attack.getActionNumber() + " costs ammo but has no weapon\n");
            return;
        }
        int left = w.getAmmo() - profile.AmmoCost;
        if (left < 0) LogError("ERROR = AMMO LESS THAN 0 AT " + attack.getActionNumber() + "\n");
        w.setAmmo(Math.max(0, left));
    }

    private Weapon getUsedWeapon(FieldUnit attacker, AttackAction attack) {
        List<Slot> slots = attacker.getSlots();
        if (slots == null || attack.slotNumber < 0 || attack.slotNumber >= slots.size()) return null;
        return slots.get(attack.slotNumber).getItem() instanceof Weapon w ? w : null;
    }

    /** Self effects go on the weapon that was used, or on the unit if it attacked without a weapon. */
    private void applySelfAttackEffectsToUnitorWeapon(FieldUnit attacker, AttackAction attack, Weapon usedWeapon) {
        for (Effect e : attack.getSelfEffects()) {
            if (usedWeapon != null) {
                usedWeapon.addEffect(e.copy());
                LogMessage(usedWeapon.getName() + " (" + attacker.getName() + ") gains " + e.getName());
            } else {
                attacker.addEffect(e.copy());
                LogMessage(attacker.getName() + " gains " + e.getName());
            }
        }
    }

    /**
     * All sectors touched by the attack, deduplicated (overlapping areas / lines / multihit
     * on the same sector count once). The hit locations themselves are always included.
     * For a line the stored hit is the END of the line, so the whole line is rebuilt from it.
     */
    private Set<Cell> collectAffectedCells(FieldUnit attacker, AttackAction attack, AttackProfile profile) {
        Set<Cell> cells = new LinkedHashSet<>();
        int ax = attacker.getX(), ay = attacker.getY();

        for (AttackAction.Hit h : attack.getHitPositions()) {
            int hx = ax + h.deltax, hy = ay + h.deltay;

            if (profile.AreaType == -2) {
                int len = Math.max(Math.abs(h.deltax), Math.abs(h.deltay));
                boolean straight = h.deltax == 0 || h.deltay == 0 || Math.abs(h.deltax) == Math.abs(h.deltay);
                if (len == 0 || !straight) {
                    LogError("ERROR - line hit " + h + " is not a straight line\n");
                    if (isOnBoard(hx, hy)) cells.add(new Cell(hx, hy));
                    continue;
                }
                for (int[] s : getLineSectors(ax, ay, Integer.signum(h.deltax), Integer.signum(h.deltay), len)) {
                    cells.add(new Cell(s[0], s[1]));
                }
                if (isOnBoard(hx, hy)) cells.add(new Cell(hx, hy));    // no-op if already in the line
                continue;
            }

            if (isOnBoard(hx, hy)) cells.add(new Cell(hx, hy));
            else LogMessage("hit outside of board " + h);

            if (profile.AreaType >= 0) {
                int r = profile.AreaType;
                for (int ox = -r; ox <= r; ox++) {
                    for (int oy = -r; oy <= r; oy++) {
                        if (isOnBoard(hx + ox, hy + oy)) cells.add(new Cell(hx + ox, hy + oy));
                    }
                }
            }
        }
        return cells;
    }

    private void replaceSectors(Set<Cell> cells, AttackProfile profile) {
        SectorType replacement = profile.getReplace();
        if (replacement == null || gameBoard == null) return;
        int changed = 0;
        for (Cell c : cells) {
            Sector s = gameBoard.getSector(c.x(), c.y());
            if (s == null || !s.getType().Replacable) continue;   // e.g. mines
            s.setType(replacement);
            changed++;
        }
        if (changed > 0) {
            LogMessage("Replaced " + changed + " sector(s) with " + replacement.Name);
            applyVisualisation();     // resets colours from the new types and re-applies the layers
        }
    }

    /** Red arrows from the attacker to every stored hit location (line: to the end of the line). */
    private void animateAttack(FieldUnit attacker, AttackAction attack) {
        double duration = isFast() ? 0 : attack.getTime() / actionSpeed;
        for (AttackAction.Hit h : attack.getHitPositions()) {
            int sx = attacker.getX(), sy = attacker.getY();
            int ex = sx + h.deltax, ey = sy + h.deltay;
            if (duration <= 0) DrawArrow(Color.RED, sx, sy, ex, ey, Arrow.ArrowType.ACTION);
            else drawAnimatedArrow(Color.RED, sx, sy, ex, ey, duration, Arrow.ArrowType.ACTION);
        }
    }

    /** Information only: not a turn, does not block processing. */
    private void showAttackMissedPopup(String attackerName) {
        if (closed || isReplay()) return;
        Stage s = newChildStage();
        s.initModality(Modality.NONE);
        s.setTitle("Attack missed");

        Label msg = new Label("You have been attacked by " + attackerName + ", but they missed.");
        msg.setWrapText(true);
        msg.setMaxWidth(320);
        msg.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        BetterButton ok = new BetterButton("OK");
        ok.setPrimaryStyle();
        ok.setOnAction(e -> s.close());

        VBox box = new VBox(15, msg, ok);
        box.setPadding(new Insets(20));
        box.setAlignment(Pos.CENTER);
        s.setScene(new Scene(box, 360, 150));
        s.show();
    }

    /** Only this player's own client shows it, and never while replaying an old game. */
    private boolean shouldShowInfoFor(FieldUnit unit) {
        return !closed && !initialReload && unit != null && unit.getName().equals(currentPlayer);
    }

    private void showHitPopup(HitResult r) {
        if (closed || isReplay()) return;
        String head;
        if (r.areaOrLine() && r.guarded()) {
            head = "You guarded against " + r.attackerName() + "'s area/line attack, but it still hit you for half damage.";
        } else if (r.areaOrLine() && r.halved()) {
            head = "You were caught by " + r.attackerName() + "'s attack. It missed, but still hit you for half damage.";
        } else {
            head = "You were hit by " + r.attackerName() + ".";
        }

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Damage rolled", String.valueOf(r.rawDamage())});
        if (r.halved()) {
            rows.add(new String[]{r.guarded() ? "Reduced by your successful Guard (half damage)"
                    : "Reduced by the miss (half damage)",
                    r.rawDamage() + " → " + r.damageAfterHalving()
                            + "   (-" + (r.rawDamage() - r.damageAfterHalving()) + ")"});
        }
        rows.add(new String[]{"Your Armor", r.baseArmor() + (r.bonusArmor() > 0 ? " + " + r.bonusArmor() + " Layered Field" : "")});
        rows.add(new String[]{"Penetration", r.penetration() + "   (Armor after penetration: " + r.armorAfterPen() + ")"});
        rows.add(new String[]{"Blocked by Armor", String.valueOf(r.blockedByArmor())});

        if (r.guardRoll() > 0) {
            rows.add(new String[]{"Guard", (r.guarded() ? "success" : "failed")
                    + " (rolled " + r.guardRoll() + " vs " + r.guardTN() + ")"});
        }
        rows.add(new String[]{"Damage dealt to Toughness", String.valueOf(r.damageDealt())});
        rows.add(new String[]{"Strain (ignores Armor)", String.valueOf(r.strain())});
        rows.add(new String[]{"Toughness", r.toughnessBefore() + " → " + Math.max(0, r.toughnessAfter()) + " / " + r.maxToughness()});
        if (!r.effectsGained().isEmpty()) {
            rows.add(new String[]{"Effects gained", String.join(", ", r.effectsGained())});
        }

        List<String> notes = new ArrayList<>();
        if (r.toughnessAfter() <= 0) notes.add("Your Toughness reached 0. You will receive a Wound.");
        showInfoPopup("You have been hit", head, "#8b0000", rows, notes);
    }

    private void showGuardSuccessPopup(HitResult r) {
        if (closed || isReplay()) return;
        String head = "You guarded against " + r.attackerName() + "'s attack!";

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Guard roll", r.guardRoll() + " vs " + r.guardTN() + "  (success)"});
        rows.add(new String[]{"Damage without Guard", r.wouldHaveDealt() + "   (" + r.damageAfterHalving() + " damage, "
                + r.blockedByArmor() + " blocked by Armor " + r.baseArmor()
                + (r.bonusArmor() > 0 ? " + " + r.bonusArmor() + " Layered Field" : "")
                + ", Penetration " + r.penetration() + ")"});
        rows.add(new String[]{"Blocked by Guard", String.valueOf(r.wouldHaveDealt())});
        rows.add(new String[]{"Strain that still went through", String.valueOf(r.strain())});
        rows.add(new String[]{"Toughness", r.toughnessBefore() + " → " + Math.max(0, r.toughnessAfter()) + " / " + r.maxToughness()});

        List<String> notes = new ArrayList<>();
        notes.add("Guard does not stop Strain.");
        if (r.toughnessAfter() <= 0) notes.add("Your Toughness reached 0. You will receive a Wound.");
        showInfoPopup("Attack guarded", head, "#2e7d32", rows, notes);
    }

    /** Information only: not a turn, does not block processing. */
    private void showInfoPopup(String title, String headline, String accentHex,
                               List<String[]> rows, List<String> notes) {
        if (closed || isReplay()) return;
        Stage s = newChildStage();
        s.initModality(Modality.NONE);
        s.setTitle(title);

        Label head = new Label(headline);
        head.setWrapText(true);
        head.setMaxWidth(400);
        head.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + accentHex + ";");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(5);
        grid.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 10; -fx-border-color: #cccccc; -fx-border-width: 1;");
        int r = 0;
        for (String[] row : rows) {
            Label k = new Label(row[0]);
            k.setStyle("-fx-text-fill: #555;");
            Label v = new Label(row[1]);
            v.setWrapText(true);
            v.setMaxWidth(250);
            v.setStyle("-fx-font-weight: bold;");
            grid.add(k, 0, r);
            grid.add(v, 1, r);
            r++;
        }

        VBox box = new VBox(12, head, grid);
        for (String n : notes) {
            Label nl = new Label(n);
            nl.setWrapText(true);
            nl.setMaxWidth(400);
            nl.setStyle("-fx-font-style: italic; -fx-text-fill: #333;");
            box.getChildren().add(nl);
        }
        BetterButton ok = new BetterButton("OK");
        ok.setPrimaryStyle();
        ok.setOnAction(e -> s.close());
        box.getChildren().add(ok);
        box.setPadding(new Insets(16));
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-border-color: " + accentHex + "; -fx-border-width: 3;");

        s.setScene(new Scene(box, 460, Math.min(680, 190 + rows.size() * 30 + notes.size() * 36)));
        s.show();
    }



    /**
     * Degrees of Success for a given roll vs the attacker's Accuracy.
     * 1 DoS = roll beats the TN by 10 or more.
     * Returns 0 when there was no roll.
     */
    private int computeDos(int roll, int accuracy) {
        return AttackAction.dosFor(roll, accuracy);
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
        double visualDuration = isFast() ? 0.01 : movement.getTime() / actionSpeed;
        processMoveVisuals(actor, oldX, oldY, newX, newY, visualDuration);
        LogMessage("Moved " + actor.getName() + " from (" + oldX + "," + oldY + ") to (" + newX + "," + newY + ")");
        activateQueue(movement);
        setActorToNext(movement);
    }

    public void processActionCost(Action action) {
        FieldUnit actor = getUnitFromName(action.getActor());
        if (actor != null && actor.isExists()) {
        actor.useATP(action.ATPCost);
        if (actor.getATP() < 0) LogError("ERROR = ATP LESS THEN 0 AT "+action.getActionNumber());
        actor.useStamina(action.staminaCost);
        if (actor.getStamina() < 0) LogError("ERROR = STAMINA LESS THEN 0 AT "+action.getActionNumber());
        if (actor.usedTactical() && action.isTactical()) LogError("ERROR = TACTICAL ACTION USED WHEN UNIT HAS NO TACTICAL LEFT AT "+action.getActionNumber());
        actor.useTactical(action.isTactical());
    } else LogMessage("Action has no actor at "+action.getActionNumber());
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

        actor.setHasAttacked(false);

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
        unit.setUsedTactical(false);
    }

    private void processEndRound() {
        for (FieldUnit unit : UnitList) {
            unit.ClearEffects(Effect.EffectEnd.ROUND_END);
            unit.setUsedGuard(false);

        }
        roundStartOffset.put(CurrentRound + 1,                                   // NEW
                roundStartOffset.getOrDefault(CurrentRound, 0) + CurrentTurn);
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
            LogError("ItemTransferActionFailed since Actor Doesnt Exist" + action.getActor());
            return;
        }

        if (action.getTransferType().equals(InventoryItemTransferAction.TRANSFER_TYPE.INVENTORY)) {
            List<Slot> evaslots = ((Evangelion) actor.getUnit()).getSlots();

            int from = action.getSlotFrom();
            int to = action.getSlotTo();
            if (evaslots.size() < from || evaslots.size() < to) {
                LogError("ItemTransferActionFailed - Impossible Slots Error");
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
                } else LogError("ItemTransferAction Failed since SLOT CANT FIT ITEM");
            } else LogError("ItemTransferAction Failed since Item being transferred doesn't exist");

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
            case INVENTORY -> LogError("ERROR, INVENTORY ACTION SPOTTED AT IMPOSSIBLE PLACE");
            case UNIT2BOARD -> {
                List<Slot> evaslots = ((Evangelion) actor.getUnit()).getSlots();
                int from = action.getSlotFrom();
                if (evaslots.get(from).getItem() == null) {
                    LogError("ERROR, PICKING UP ITEM TO OCCUPIED SLOT");
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
                    LogError("ERROR, PICKING UP ITEM TO OCCUPIED SLOT");
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

        double visualDuration = isFast() ? 0.01 : action.getTime() / actionSpeed;
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

        double visualDuration = isFast() ? 0.01 : action.getTime() / actionSpeed;
        animateDestroyUnit(unitToDelete, visualDuration, () -> {
            removeFieldUnit(unitToDelete);
            LogMessage("DMDeleteUnitAction: Deleted unit '" + unitToDelete.getName() + "' at (" + x + "," + y + ").");
        });
        // Whatever is at that cell, wipe field items there too.
        deleteFieldItemsAt(x, y);
    }

    private void processDMChoosePlayerAction(DMChoosePlayerAction action) {
        QueuePosition current = queue.currentPosition();
        if (current == null || current.isReaction()) {
            LogError("DMChoosePlayerAction ignored: current position is a reaction or missing.");
            return;
        }
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


    /**
     * Replaces whatever item is currently in the given slot of the target
     * Evangelion with the {@link Weapon} carried inside the action.
     *
     * The DM client loads the weapon from disk when the command is issued;
     * the object then travels through the GameState serialization like any
     * other action payload, so the receiving side never needs file access.
     */
    private void processDMGiveWeaponAction(DMGiveWeaponAction action) {
        DMQueueInsertion(action, "DM_GIVE_WEAPON");

        FieldUnit target = getUnitFromName(action.getTargetUnitName());
        if (target == null || !target.isExists()) {
            LogMessage("DMGiveWeaponAction failed: unit '" + action.getTargetUnitName() + "' not found.");
            return;
        }
        if (!(target.getUnit() instanceof Evangelion eva)) {
            LogMessage("DMGiveWeaponAction failed: '" + target.getName() + "' is not an Evangelion.");
            return;
        }

        Weapon weapon = action.getWeapon();
        if (weapon == null) {
            LogMessage("DMGiveWeaponAction failed: action carries no weapon.");
            return;
        }

        List<Slot> slots = eva.getSlots();
        int slotNum = action.getSlotNumber();
        if (slotNum < 0 || slotNum >= slots.size()) {
            LogMessage("DMGiveWeaponAction failed: slot " + slotNum
                    + " out of range (0-" + (slots.size() - 1) + ").");
            return;
        }

        Slot slot = slots.get(slotNum);
        Item old = slot.getItem();
        if (old != null) {
            LogMessage("DMGiveWeaponAction: replacing '" + old.getName()
                    + "' in slot " + slot.getName());
        }
        slot.setItem(weapon);
        LogMessage("DMGiveWeaponAction: gave '" + weapon.getName()
                + "' to " + target.getName()
                + " in slot " + slot.getName() + " (#" + slotNum + ").");

        if (target.getName().equals(currentPlayer)) {
            rebuildInventoryTab();
        }
    }

    /**
     * Loads a serialized {@link Weapon} from {@code Active/weapons/}.
     * Accepts the raw name (with spaces / punctuation), the already sanitized
     * name, or a name ending in {@code .ser}.
     *
     * Only invoked by the DM client when issuing the giveWeapon command —
     * the loaded weapon is then embedded in a {@link DMGiveWeaponAction} and
     * shipped through the GameState.
     *
     * @return the weapon, or {@code null} if it could not be found / read.
     */
    private Weapon loadWeaponByName(String name) {
        if (name == null || name.isBlank()) return null;

        File dir = new File("Active/weapons");
        if (!dir.isDirectory()) {
            LogMessage("Weapon directory not found: " + dir.getAbsolutePath());
            return null;
        }

        String trimmed = name.trim();
        String sanitized = trimmed.replaceAll("[^a-zA-Z0-9]", "_");
        List<String> candidates = new ArrayList<>();
        candidates.add(trimmed);
        if (!trimmed.toLowerCase().endsWith(".ser")) {
            candidates.add(trimmed + ".ser");
        }
        candidates.add(sanitized);
        candidates.add(sanitized + ".ser");

        File file = null;
        for (String candidate : candidates) {
            File f = new File(dir, candidate);
            if (f.isFile()) { file = f; break; }
        }
        if (file == null) return null;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof Weapon w) return w;
            LogMessage("File '" + file.getName() + "' is not a Weapon ("
                    + obj.getClass().getSimpleName() + ").");
        } catch (IOException | ClassNotFoundException e) {
            LogMessage("Failed to load weapon '" + file.getName() + "': " + e.getMessage());
        }
        return null;
    }

    /** Sanitized names (without .ser) of every weapon in Active/weapons/. */
    private List<String> listAvailableWeaponFiles() {
        File dir = new File("Active/weapons");
        List<String> names = new ArrayList<>();
        if (!dir.isDirectory()) return names;
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".ser"));
        if (files == null) return names;
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File f : files) {
            names.add(f.getName().substring(0, f.getName().length() - 4));
        }
        return names;
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
        // A client that hasn't noticed a replace/revert yet must not overwrite the new game with its old state.
        GameState onDisk = loadStateQuietly();
        if (isSessionReplaced(onDisk)) {
            onSessionReplacedByDM();
            return;
        }

        int next = gamestate.getActions().size() + 1;
        if (action.getActionNumber() != next) {
            setActionNumber(action, next);       // reflection helper from before
        }
        gamestate.addAction(action);
        saveGameState();
        LogMessage("Action sent: " + action + " as #" + next);
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


    /**
     * Top-level click dispatcher. Decides which interaction mode is active and
     * forwards to the matching handler. Kept deliberately thin so the individual
     * modes can be reasoned about in isolation.
     */
    private void handleSectorClick(int x, int y) {

        // 0. DM is choosing a spawn point for a player
        if (spawnPickHandler != null) {
            spawnPickHandler.accept(x, y);
            return;
        }

        // 1. Attack visualization mode wins
        if (attackModeActive && activeAttackProfile != null && attackUnit != null) {
            handleAttackSectorClick(x, y);
            return;
        }

        // 2. Movement visualization mode
        if (activeMoveMode != null && movementUnit != null) {
            handleMovementSectorClick(x, y);
            return;
        }

        // 3. Default: unit selection / basic move
        handleDefaultSectorClick(x, y);
    }

// ------------------------------------------------------------------
//   ATTACK MODE
// ------------------------------------------------------------------

    /**
     * Handles a click while an attack profile is being visualized.
     *
     * Unlike the movement handler, this permits targeting <em>empty</em> sectors —
     * i.e. the attack can be aimed at a spot on the board with no unit on it.
     * The only hard requirement is that the sector is inside the profile's
     * MaxRange. Sectors below MinRange still produce an attack, but log a
     * "too close" warning so the player knows a penalty will apply.
     */

    /** One aimed hit. x/y is the resolved target (for lines: the end of the line). dirX/dirY are only used by lines. */
    private record PendingHit(int x, int y, int dirX, int dirY, int clickX, int clickY) {}

    /** Oldest first. Its size never exceeds the profile's multihit. */
    private final LinkedList<PendingHit> pendingAttackHits = new LinkedList<>();

    private static final Color ATTACK_CENTER_TINT = Color.rgb(255, 0, 0);
    private static final Color ATTACK_AREA_TINT   = Color.rgb(255, 140, 0);
    private static final Color ATTACK_LINE_TINT   = Color.rgb(200, 0, 200);

    private void handleAttackSectorClick(int x, int y) {
        // Only sectors in the highlighted zone are clickable (for lines the zone is only straight lines).
        if (!isInPreviewVisualisationLayer(x, y)) return;

        FieldUnit target = getUnitAt(x, y);
        if (target == attackUnit) {
            LocalMessage("You cannot target yourself.");
            return;
        }

        int dx = x - attackUnit.getX();
        int dy = y - attackUnit.getY();
        int dist = Math.max(Math.abs(dx), Math.abs(dy));

        AttackProfile combatProfile = copyProfileAndApplyEffects(activeAttackProfile);
        boolean isLine = combatProfile.AreaType == -2;

        if (dist > combatProfile.MaxRange) {
            LocalMessage("Sector out of range (" + dist + " > max " + combatProfile.MaxRange + ").");
            return;
        }

        // Lines always travel to max range, so "too close" only matters for other attacks.
        if (!isLine && isAttackBelowMinRange(combatProfile, dist)) {
            LocalMessage("Warning: sector is inside min range (" + dist + " < " + combatProfile.MinRange
                    + "). Attack will suffer the too-close penalty.");
        }

        // ---- Resolve what this click actually targets ----
        PendingHit hit;
        if (isLine) {
            int dirX = Integer.signum(dx);
            int dirY = Integer.signum(dy);
            List<int[]> line = getLineSectors(attackUnit.getX(), attackUnit.getY(), dirX, dirY, combatProfile.MaxRange);
            if (line.isEmpty()) return;
            int[] end = line.get(line.size() - 1);
            hit = new PendingHit(end[0], end[1], dirX, dirY, x, y);   // line
        } else {
            hit = new PendingHit(x, y, 0, 0, x, y);                   // everything else
        }

        // ---- Multihit: the newest click replaces the oldest ----
        int maxHits = Math.max(1, combatProfile.multihit);
        pendingAttackHits.addLast(hit);
        while (pendingAttackHits.size() > maxHits) {
            pendingAttackHits.removeFirst();
        }

        // ---- Redraw arrows + highlights from scratch so they always match the hit list ----
        redrawAttackPreview(combatProfile);

        // ---- Build the pending AttackAction ----
        AttackAction atk = new AttackAction(currentActionNumber+1, attackUnit.getName());
        for (PendingHit h : pendingAttackHits) {
            atk.addHit(h.x() - attackUnit.getX(), h.y() - attackUnit.getY());
        }
        atk.slotNumber = (selectedAttackWeaponSlot != null)
                ? attackUnit.getSlots().indexOf(selectedAttackWeaponSlot) : -1;
        atk.actionCombatProfile = combatProfile;
        atk.ATPCost = combatProfile.ATP;
        atk.staminaCost = combatProfile.Stamina;
        confirmAction = atk;

        StringBuilder desc = new StringBuilder(attackUnit.getName()).append(" → ");
        int i = 0;
        for (PendingHit h : pendingAttackHits) {
            if (i++ > 0) desc.append(", ");
            desc.append(CordsToText(h.x(), h.y()));
            FieldUnit u = getUnitAt(h.x(), h.y());
            if (u != null) desc.append(" (").append(u.getName()).append(")");
        }
        if (isLine) desc.append(" [Line]");
        createConfirmButton(atk, Color.DARKRED, desc.toString());

        LogMessage("Attack pending: " + pendingAttackHits.size() + "/" + maxHits + " hit(s). "
                + (isMyActionTurn() ? "Click Confirm to finalise" : "It will be confirmable on your turn")
                + (pendingAttackHits.size() < maxHits ? ", or click more sectors." : "."));
    }

    /** Rebuilds arrows (attack layer) from pendingAttackHits. */
    private void redrawAttackPreview(AttackProfile profile) {
        // Arrows
        clearPreviewArrows();
        double sx = attackUnit.getX() * 20 + 10;
        double sy = attackUnit.getY() * 20 + 10;
        for (PendingHit h : pendingAttackHits) {
            double ex = h.x() * 20 + 10;
            double ey = h.y() * 20 + 10;
            registerArrow(new Arrow(boardContainer, Color.RED, sx, sy, ex, ey, Arrow.ArrowType.PREVIEW));
        }

        // Highlights (attack layer sits on top of the range zone in the preview layer)
        attackVisualisationLayer.clear();

        if (profile.AreaType == -2) {
            // Line: every sector along the line, end of the line in the strong color
            for (PendingHit h : pendingAttackHits) {
                for (int[] s : getLineSectors(attackUnit.getX(), attackUnit.getY(),
                        h.dirX(), h.dirY(), profile.MaxRange)) {
                    addToAttackVisualisationLayer(s[0], s[1], ATTACK_LINE_TINT);
                }
            }
            for (PendingHit h : pendingAttackHits) {
                addToAttackVisualisationLayer(h.x(), h.y(), ATTACK_CENTER_TINT);
            }
        } else if (profile.AreaType >= 0) {
            // Area: (2*area+1)^2 square around every hit. Area tint first, all centers last,
            // so an overlapping area from another hit can't paint over a center.
            int r = profile.AreaType;
            for (PendingHit h : pendingAttackHits) {
                for (int ax = -r; ax <= r; ax++) {
                    for (int ay = -r; ay <= r; ay++) {
                        int tx = h.x() + ax;
                        int ty = h.y() + ay;
                        if (!isOnBoard(tx, ty)) continue;
                        addToAttackVisualisationLayer(tx, ty, ATTACK_AREA_TINT);
                    }
                }
            }
            for (PendingHit h : pendingAttackHits) {
                addToAttackVisualisationLayer(h.x(), h.y(), ATTACK_CENTER_TINT);
            }
        }
        // AreaType == -1 (normal attack): the arrow is enough, no extra highlight.

        applyVisualisation();
    }

    private boolean isOnBoard(int x, int y) {
        return x >= 0 && x < battlefield.sizeX && y >= 0 && y < battlefield.sizeY;
    }

    /**
     * Sectors of a line starting next to (ax, ay) and going in (dirX, dirY) for up to maxRange sectors.
     * Stops at the board edge, so the last element is the real end of the line.
     */
    private List<int[]> getLineSectors(int ax, int ay, int dirX, int dirY, int maxRange) {
        List<int[]> result = new ArrayList<>();
        if (dirX == 0 && dirY == 0) return result;
        for (int i = 1; i <= maxRange; i++) {
            int tx = ax + dirX * i;
            int ty = ay + dirY * i;
            if (!isOnBoard(tx, ty)) break;
            result.add(new int[]{tx, ty});
        }


        return result;
    }


    private AttackProfile copyProfileAndApplyEffects(AttackProfile weaponProfile) {
        AttackProfile c = weaponProfile.copy();
        if (!c.strengthApplied && attackUnit != null) {
            c.Power += attackUnit.getAttackStrength();
            c.strengthApplied = true;
        }
        return c;
    }

    private AttackProfile turnIntoCombatProfile(AttackProfile weaponProfile, FieldUnit unit) {
        if (!weaponProfile.strengthApplied) {
            weaponProfile.Power += unit.getAttackStrength();
            weaponProfile.strengthApplied = true;
        }
        return weaponProfile;
    }
// ------------------------------------------------------------------
//   MOVEMENT MODE
// ------------------------------------------------------------------

    /**
     * Handles a click while a movement subtype is being visualized.
     * Clicking outside the highlighted zone is a no-op.
     */
    private void handleMovementSectorClick(int x, int y) {
        if (!isInPreviewVisualisationLayer(x, y)) return;   // ignore non-highlighted sectors

        int dx = x - movementUnit.getX();
        int dy = y - movementUnit.getY();

        clearPreviewArrows();

        // Preview arrow on the main board
        double sx = movementUnit.getX() * 20 + 10;
        double sy = movementUnit.getY() * 20 + 10;
        double ex = x * 20 + 10;
        double ey = y * 20 + 10;
        Arrow preview = new Arrow(boardContainer, Color.ORANGE, sx, sy, ex, ey,
                Arrow.ArrowType.PREVIEW);
        registerArrow(preview);

        lastMoveClickX = x;
        lastMoveClickY = y;

        MoveAction mv = new MoveAction(currentActionNumber+1, movementUnit.getName(), dx, dy);
        createConfirmButton(mv, Color.DARKORANGE,
                movementUnit.getName() + " → " + CordsToText(x, y));
        LogMessage(isMyActionTurn()
                ? "Move pending. Click Confirm to choose movement type."
                : "Move prepared. You can confirm it when it's your turn.");
    }

// ------------------------------------------------------------------
//   DEFAULT MODE (no visualization active)
// ------------------------------------------------------------------

    /**
     * Handles a click when neither attack nor movement visualization is active.
     * Clicking a unit selects it and shows its stats; clicking an empty sector
     * with a selected unit sets up a basic MoveAction.
     */
    private void handleDefaultSectorClick(int x, int y) {

        FieldUnit unitAtLocation = getUnitAt(x, y);
        if (unitAtLocation != null) {
            selectedUnit = unitAtLocation;
            LocalMessage("Selected unit: " + selectedUnit.getName());
            showStatsForUnit(selectedUnit);                 // secondary container
            return;
        }

        clearSecondaryStats();

        if (selectedUnit == null) {
            LogMessage("No unit selected. Click on a unit to select it.");
            return;
        }

    }



    // ============================================================
//   CONFIRM BUTTON AND END TURN BUTTON ON TOP BAR (with confirmation pop-up)
// ============================================================
    private HBox turnEndContainer;   // holds the "Turn End" button




    private void createConfirmButton(Action action, Color color, String description) {
        confirmAction = action;
        pendingConfirmColor = color;
        pendingConfirmDescription = description;
        renderConfirmButton();
    }

    /** Real button on your turn, grey "pending" text otherwise. Safe to call any time. */
    private void renderConfirmButton() {
        if (confirmContainer == null) return;
        confirmContainer.getChildren().clear();
        if (confirmAction == null) return;

        // Not your turn: show what is prepared
        if (!isMyActionTurn()) {
            Label waiting = new Label("Pending: " + pendingConfirmDescription + "  (waits for your turn)");
            waiting.setStyle("-fx-text-fill: #dddddd; -fx-font-style: italic;");
            confirmContainer.getChildren().add(waiting);
            return;
        }

        // Your turn, but this unit already used its Attack Action: show the action + the reason
        if (confirmAction instanceof AttackAction pending
                && alreadyAttackedThisTurn(getUnitFromName(pending.getActor()))) {
            Label shown = new Label("Pending: " + pendingConfirmDescription);
            shown.setStyle("-fx-text-fill: #dddddd; -fx-font-style: italic;");
            Label why = new Label("You can't attack: you already attacked this turn.");
            why.setStyle("-fx-text-fill: #ff9999; -fx-font-weight: bold;");
            VBox box = new VBox(2, shown, why);
            confirmContainer.getChildren().add(box);
            return;
        }

        final Action action = confirmAction;
        BetterButton confirmBtn = new BetterButton("Confirm");
        confirmBtn.setPrefHeight(10);
        confirmBtn.setSuccessStyle();
        confirmBtn.setOnAction(e -> {
            if (!isMyActionTurn()) { renderConfirmButton(); return; }   // turn changed under us
            if (action instanceof AttackAction a && alreadyAttackedThisTurn(getUnitFromName(a.getActor()))) {
                renderConfirmButton();                                   // flag changed under us
                return;
            }
            showConfirmPopup(action, pendingConfirmColor, pendingConfirmDescription);
        });
        confirmContainer.getChildren().add(confirmBtn);
    }

    private void showConfirmPopup(Action action, Color color, String description) {

        if (action instanceof MoveAction) {
            MoveAction mv = (MoveAction) action;
            FieldUnit u = getUnitFromName(mv.getActor());
            if (u != null) {
                int tx = u.getX() + mv.getDeltaX();
                int ty = u.getY() + mv.getDeltaY();
                showMoveConfirmPopup(mv, tx, ty);
                return;
            }
        }
        // ---- NEW: Attack branch ----
        if (action instanceof AttackAction atk) {
            FieldUnit u = getUnitFromName(atk.getActor());
            Weapon w = null;
            Slot s = null;
            if (u != null && atk.slotNumber >= 0 && atk.slotNumber < u.getSlots().size()) {
                s = u.getSlots().get(atk.slotNumber);
                if (s.getItem() instanceof Weapon ww) w = ww;
            }
            showAttackConfirmPopup(atk, atk.getActionCombatProfile(), w, s);
            return;
        }



        Stage popupStage = newChildStage();
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
            if (action instanceof InventoryItemTransferAction) clearPreviewArrows();
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



    private static final String SUPERCONDUCTIVE_SRC = "Superconductive";

    private void showAttackConfirmPopup(AttackAction action, AttackProfile profile,
                                        Weapon weapon, Slot slot) {
        if (profile == null) { LogMessage("No attack profile selected."); return; }
        FieldUnit attacker = getUnitFromName(action.getActor());
        if (attacker == null || !attacker.isExists()) return;

        final Weapon.Tech tech = (weapon != null && weapon.getCurrentTech() != null)
                ? weapon.getCurrentTech() : Weapon.Tech.NONE;
        final int tn = AttackAction.clampTN(attacker.getAccuracy());

        // Costs always come from the profile
        action.staminaCost = profile.Stamina;
        action.ATPCost     = profile.ATP;

        final boolean showAmmo = weapon != null && (weapon.isRanged() || profile.AmmoCost > 0);
        final boolean canAfford =
                attacker.getStamina() >= profile.Stamina
                        && attacker.getATP() >= profile.ATP
                        && (!showAmmo || weapon.getAmmo() >= profile.AmmoCost);

        // ---------- Window ----------
        Stage popupStage = newChildStage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Confirm Attack");
        popupStage.initStyle(StageStyle.TRANSPARENT);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + toHex(Color.DARKRED) + ";" +
                        "-fx-border-width: 4;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4);");

        Scene scene = new Scene(content, 560, 700);
        scene.setFill(Color.TRANSPARENT);

        Label titleLabel = new Label("Confirm Attack");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // ---------- Targets ----------
        StringBuilder targetText = new StringBuilder();
        for (int i = 0; i < action.hitPositions.size(); i++) {
            AttackAction.Hit h = action.hitPositions.get(i);
            int hx = attacker.getX() + h.deltax;
            int hy = attacker.getY() + h.deltay;
            if (i > 0) targetText.append(", ");
            FieldUnit hU = getUnitAt(hx, hy);
            if (hU != null) targetText.append(hU.getName()).append("  ").append(CordsToText(hx, hy));
            else            targetText.append(CordsToText(hx, hy));
        }
        Label targetLabel = new Label("Target: " + targetText);
        targetLabel.setWrapText(true);
        targetLabel.setMaxWidth(500);
        targetLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        Label profileLabel = new Label(describeProfileForPopup(profile, weapon));
        profileLabel.setWrapText(true);
        profileLabel.setMaxWidth(500);
        profileLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a4d;");

        // ---------- Costs ----------
        HBox costRow = new HBox(20);
        costRow.setAlignment(Pos.CENTER);
        costRow.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 10; " +
                "-fx-border-color: #cccccc; -fx-border-width: 1;");

        Label staminaCost = new Label("Stamina: " + profile.Stamina + " (have " + attacker.getStamina() + ")");
        staminaCost.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " +
                (attacker.getStamina() < profile.Stamina ? "#b00020" : "#2e7d32") + ";");
        Label atpCost = new Label("ATP: " + profile.ATP + " (have " + attacker.getATP() + ")");
        atpCost.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " +
                (attacker.getATP() < profile.ATP ? "#b00020" : "#2e7d32") + ";");
        costRow.getChildren().addAll(staminaCost, atpCost);

        if (showAmmo) {
            int remaining = weapon.getAmmo() - profile.AmmoCost;
            Label ammoCost = new Label("Ammo: " + profile.AmmoCost
                    + "  (" + weapon.getAmmo() + "/" + weapon.getMaxAmmo() + ")");
            ammoCost.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " +
                    (remaining < 0 ? "#b00020" : "#2e7d32") + ";");
            costRow.getChildren().add(ammoCost);
        }

        // ---------- Superconductive choice (only tech with an on-hit effect) ----------
        final String OPT_ATTACK = "-10 to the target's next Attack Test";
        final String OPT_GUARD  = "-10 to the target's next Guard attempt";
        final String OPT_NONE   = "No effect";

        ComboBox<String> onHitCombo = new ComboBox<>();
        onHitCombo.getItems().addAll(OPT_ATTACK, OPT_GUARD, OPT_NONE);
        onHitCombo.setValue(OPT_ATTACK);
        onHitCombo.setPrefWidth(300);

        // Writes the current choice into the profile (replaces any earlier Superconductive entry).
        Runnable syncOnHit = () -> {
            profile.onHitEffects.removeIf(fx -> fx.getName().startsWith(SUPERCONDUCTIVE_SRC));
            if (tech != Weapon.Tech.SUPERCONDUCTIVE) {
                return;
            }
            String v = onHitCombo.getValue();
            if (OPT_ATTACK.equals(v))     profile.onHitEffects.add(Effect.superconductiveAttack());
            else if (OPT_GUARD.equals(v)) profile.onHitEffects.add(Effect.superconductiveGuard());
        };

        onHitCombo.valueProperty().addListener((o, a, b) -> syncOnHit.run());
        syncOnHit.run();

        HBox onHitRow = new HBox(10, new Label("Superconductive (on damage):"), onHitCombo);
        onHitRow.setAlignment(Pos.CENTER);

        // ---------- Polythermic Overheat (chosen BEFORE the damage is rolled) ----------
        final boolean polythermic = tech == Weapon.Tech.POLYTHERMIC;
        final String overheatPenaltyText = weapon != null
                ? "-2 damage on your next attack with this weapon"
                : "-2 damage until the end of the round";
        final CheckBox overheatCb = new CheckBox("Overheat: " + profile.Dice + "d" + profile.Dicepower
                + " → " + profile.overheatPreview() + "   (afterwards " + overheatPenaltyText + ")");
        overheatCb.setWrapText(true);
        overheatCb.setMaxWidth(500);
        overheatCb.setStyle("-fx-font-weight: bold; -fx-text-fill: #b45f00;");

        // ---------- Results ----------
        VBox resultsBox = new VBox(6);
        resultsBox.setAlignment(Pos.CENTER);
        resultsBox.setStyle("-fx-background-color: #fafafa; -fx-padding: 14; " +
                "-fx-border-color: #dddddd; -fx-border-width: 1;");
        resultsBox.setMinHeight(200);

        Label accuracyHeader = new Label("Accuracy");
        accuracyHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        Label accuracyResult = new Label("Target: " + tn);
        accuracyResult.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");
        Label dosLabel = new Label("");
        dosLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        Label damageHeader = new Label("Damage");
        damageHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #666;");
        Label damageResult = new Label("—");
        damageResult.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");
        Label damageBreakdown = new Label("");
        damageBreakdown.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        Label techNote = new Label("");
        techNote.setWrapText(true);
        techNote.setMaxWidth(480);
        techNote.setStyle("-fx-font-size: 12px; -fx-text-fill: #1a1a4d; -fx-font-weight: bold;");

        Label areaNote = new Label("");
        areaNote.setStyle("-fx-font-size: 12px; -fx-text-fill: #b00020; -fx-font-style: italic;");
        areaNote.setWrapText(true);
        areaNote.setMaxWidth(480);
        if (profile.AreaType == -2)
            areaNote.setText("Line attack: hits everything along the line. Half damage if it misses or is guarded.");
        else if (profile.AreaType >= 0)
            areaNote.setText("Area " + profile.AreaType + " attack: Half damage if it misses or is guarded.");

        resultsBox.getChildren().addAll(
                accuracyHeader, accuracyResult, dosLabel,
                new Separator(),
                damageHeader, damageResult, damageBreakdown,
                techNote, areaNote);

        // ---------- Buttons ----------
        BetterButton rollBtn = new BetterButton("ROLL ATTACK");
        rollBtn.setPrimaryStyle();
        rollBtn.setPrefWidth(220);
        rollBtn.setPrefHeight(40);
        rollBtn.setStyle(rollBtn.getStyle() +
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-color: #8b0000; -fx-text-fill: white;");

        BetterButton proceedBtn = new BetterButton("Proceed");
        proceedBtn.setSuccessStyle();
        proceedBtn.setDisable(true);

        BetterButton cancelBtn = new BetterButton("Cancel");
        cancelBtn.setDangerStyle();
        cancelBtn.setOnAction(e -> popupStage.close());

        // Shows whatever is stored in the action right now (also used when the popup is reopened).
        Runnable refresh = () -> {
            if (action.rolledHitValue <= 0) return;
            boolean hit = action.isRollSuccess();
            int dos = action.getDos();

            accuracyResult.setText("Rolled " + action.rolledHitValue + "  vs  " + action.accuracyTN
                    + (hit ? "   HIT" : "   MISS"));
            accuracyResult.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: "
                    + (hit ? "#2e7d32" : "#b00020") + ";");
            dosLabel.setText(hit ? dos + " Degrees of Success" : "No Degrees of Success");

            // Tech bonuses are derived from the DoS, not stored anywhere
            int gaussBonus = (tech == Weapon.Tech.GAUSS)  ? Math.min(dos, 4) : 0;
            int chainStrain = (tech == Weapon.Tech.CHAIN) ? Math.min(dos, 3) : 0;

            int basePower = profile.Power - gaussBonus;                 // Power without the Gauss bonus
            int diceTotal = action.rolledDamage - profile.Power;        // just the dice

            damageResult.setText(String.valueOf(action.rolledDamage));
            StringBuilder bd = new StringBuilder();
            bd.append(profile.Dice).append("d").append(profile.Dicepower).append(" = ").append(diceTotal);
            if (basePower != 0) bd.append("  ").append(basePower > 0 ? "+ " : "- ")
                    .append(Math.abs(basePower)).append(" power");
            if (gaussBonus > 0) bd.append("  + ").append(gaussBonus).append(" Gauss");
            damageBreakdown.setText(bd.toString());

            StringBuilder tn2 = new StringBuilder();
            if (tech == Weapon.Tech.GAUSS)
                tn2.append("Gauss: +").append(gaussBonus).append(" damage (1 per DoS, max +4).");
            if (tech == Weapon.Tech.CHAIN)
                tn2.append("Chain: +").append(chainStrain)
                        .append(" Strain (1 per DoS, max 3), total Strain ").append(profile.Strain)
                        .append(", applied even if the target guards.");
            if (tech == Weapon.Tech.SUPERCONDUCTIVE)
                tn2.append("Superconductive: the chosen effect applies to each target that takes damage.");
            if (polythermic) {
                overheatCb.setSelected(profile.overheated);
                overheatCb.setDisable(true);                      // the choice is locked after the roll
                if (profile.overheated) {
                    overheatCb.setText("Overheated  (afterwards " + overheatPenaltyText + ")");
                    tn2.append("Overheated: damage rolled with ").append(profile.Dice).append("d")
                            .append(profile.Dicepower).append(". Afterwards ").append(overheatPenaltyText).append(".");
                }
            }
            profileLabel.setText(describeProfileForPopup(profile, weapon));   // shows the changed dice
            techNote.setText(tn2.toString());

            rollBtn.setDisable(true);
            cancelBtn.setDisable(true);
            onHitCombo.setDisable(false);
            proceedBtn.setDisable(!canAfford);
        };

        rollBtn.setOnAction(e -> {
            if (action.rolledHitValue > 0) return;        // one roll per action
            java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();

            action.accuracyTN     = tn;
            action.rolledHitValue = rng.nextInt(1, 101);

            if (polythermic && overheatCb.isSelected()) {
                profile.applyOverheat();                          // changes Dice / Dicepower / Power BEFORE the dice are rolled
                action.getSelfEffects().removeIf(fx -> fx.getName().equals(Effect.OVERHEAT_PENALTY));
                action.addSelfEffect(Effect.overheatPenalty(weapon != null));
            }
            overheatCb.setDisable(true);

            int diceTotal = 0;
            for (int i = 0; i < profile.Dice; i++) diceTotal += rng.nextInt(1, profile.Dicepower + 1);

            profile.resolveTechnology(tech, action.getDos());
            action.rolledDamage = diceTotal + profile.Power;

            refresh.run();
        });

        proceedBtn.setOnAction(e -> {
            if (action.rolledHitValue <= 0 || !canAfford) return;
            syncOnHit.run();
            proceedBtn.setDisable(true);
            SendAction(action);
            clearAttackVisualization();
            confirmAction = null;
            confirmContainer.getChildren().clear();
            popupStage.close();
        });

        if (!canAfford) {
            rollBtn.setDisable(true);
            overheatCb.setDisable(true);
            areaNote.setText("Not enough resources for this attack.");
        }
        if (action.rolledHitValue > 0) refresh.run();     // popup reopened after a roll

        HBox btnBox = new HBox(10, cancelBtn, proceedBtn);
        btnBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(titleLabel, targetLabel, profileLabel, costRow);
        if (tech == Weapon.Tech.SUPERCONDUCTIVE) content.getChildren().add(onHitRow);
        if (polythermic) content.getChildren().add(overheatCb);
        content.getChildren().addAll(rollBtn, resultsBox, btnBox);

        final double[] dragOffset = new double[2];
        content.setOnMousePressed(e -> { dragOffset[0] = e.getSceneX(); dragOffset[1] = e.getSceneY(); });
        content.setOnMouseDragged(e -> {
            popupStage.setX(e.getScreenX() - dragOffset[0]);
            popupStage.setY(e.getScreenY() - dragOffset[1]);
        });

        popupStage.setScene(scene);
        popupStage.show();
    }
    /**
     * Compact one-line profile summary for the attack popup.
     * Shows dice, power, penetration and a tech marker where relevant.
     * Example: "2d6+2  |  Pen 3  |  Rng 2-5  |  Gauss +DoS"
     */
    private String describeProfileForPopup(AttackProfile p, Weapon weapon) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.Dice).append("d").append(p.Dicepower);
        if (p.Power != 0) sb.append(p.Power > 0 ? "+" : "").append(p.Power);
        sb.append(" | Penetration: ").append(p.Penetration);

        if (p.AreaType == -2)     sb.append(" | Line");
        else if (p.AreaType >= 0) sb.append(" | Area ").append(p.AreaType);

        if (p.Ranged) sb.append(" | Range ").append(p.MinRange).append("-").append(p.MaxRange);
        else if (p.MaxRange > 1) sb.append(" | Reach ").append(p.MaxRange);



        if (weapon != null) {
            switch (weapon.getCurrentTech()) {
                case GAUSS          -> sb.append(" | Gauss (+1 dmg/DoS, max +4)");
                case CHAIN          -> sb.append(" | Chain (1 Strain/DoS, max 3)");
                case SUPERCONDUCTIVE-> sb.append(" | Superconductive");
                case POLYTHERMIC -> sb.append(p.overheated ? " | OVERHEATED" : " | Polythermic");
                case N2SHELL  -> { if (weapon.isActiveTech()) sb.append("   |   N2 Shell Active"); }
                case MASER    -> { if (weapon.isActiveTech()) sb.append("   |   Maser Active"); }
                default -> { }
            }
        }

        return sb.toString();
    }






    // ============================================================
//   ATTACK CHOOSER
// ============================================================

    private void validateWeaponSelection() {
        FieldUnit unit = findUnitSilent(currentPlayer);
        if (attackUnit != null && attackUnit != unit) {
            LogMessage("Validate Weapon - The player now controls a different unit: nothing of the old selection is valid.");
            clearAttackVisualization();
            selectedAttackWeapon = null;
            selectedAttackWeaponSlot = null;
            attackUnarmedChosen = false;
            return;
        }
        if (selectedAttackWeapon == null) { selectedAttackWeaponSlot = null; return; }

        if (unit != null && unit.getSlots() != null) {
            if (selectedAttackWeaponSlot != null
                    && selectedAttackWeaponSlot.isActive()
                    && selectedAttackWeaponSlot.getItem() == selectedAttackWeapon) return;
            for (Slot s : unit.getSlots()) {
                if (s.isActive() && s.isIntact() && s.getItem() == selectedAttackWeapon) {
                    selectedAttackWeaponSlot = s;
                    return;
                }
            }
        }
        LogMessage("Validate Weapon - The Weapon was moved or removed");
        selectedAttackWeapon = null;
        selectedAttackWeaponSlot = null;
        clearAttackVisualization();
    }
    /**
     * True when the weapon has a technology that can be toggled on/off via
     * {@link Weapon#SetActivateTech(boolean)}. Currently only N2 Shell and Maser
     * consume the activation flag inside {@link Weapon#getWeaponProfiles(eva.evangelion.items.Weapon.Weapon.Tech)}.
     */
    private boolean hasActivatableTech(Weapon weapon) {
        if (weapon == null) return false;
        return weapon.hasActivatableTech();
    }

    private void rebuildWeaponChooser() {
        if (attackWeaponChooserContainer == null) return;
        attackWeaponChooserContainer.clearNodes();
        validateWeaponSelection();

        Label title = new Label("Choose Weapon");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #8b0000;");
        attackWeaponChooserContainer.addNode(title);

        FieldUnit unit = findUnitSilent(currentPlayer);
        if (unit == null || !unit.isExists()) {
            Label empty = new Label("No unit selected.");
            empty.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            attackWeaponChooserContainer.addNode(empty);
            return;
        }

        BetterButton neutralBtn = new BetterButton("Neutral (Unarmed)");
        neutralBtn.setPrimaryStyle();
        neutralBtn.setMaxWidth(Double.MAX_VALUE);
        if (attackUnarmedChosen && selectedAttackWeapon == null) {
            neutralBtn.setStyle("-fx-border-color: #8b0000; -fx-border-width: 3; -fx-border-radius: 4;");
        }
        neutralBtn.setOnAction(e -> {
            if (attackUnarmedChosen && selectedAttackWeapon == null) return;   // already chosen
            selectedAttackWeapon = null;
            selectedAttackWeaponSlot = null;
            attackUnarmedChosen = true;
            switchAttackSource();
        });
        attackWeaponChooserContainer.addNode(neutralBtn);

        if (unit.getSlots() != null) {
            for (Slot slot : unit.getSlots()) {
                if (!slot.isActive() || !slot.isIntact()) continue;
                Item it = slot.getItem();
                if (!(it instanceof Weapon weapon)) continue;

                String label = slot.getName() + ": " + weapon.getName();
                if (weapon.isRanged())
                    label += "   [" + weapon.getAmmo() + "/" + weapon.getMaxAmmo() + "]";

                BetterButton slotBtn = new BetterButton(label);
                slotBtn.setPrimaryStyle();
                slotBtn.setMaxWidth(Double.MAX_VALUE);
                if (selectedAttackWeaponSlot == slot) {
                    slotBtn.setStyle("-fx-border-color: #8b0000; -fx-border-width: 3; -fx-border-radius: 4;");
                }
                slotBtn.setOnAction(e -> {
                    if (selectedAttackWeaponSlot == slot) return;              // already chosen: keep the profile
                    selectedAttackWeapon = weapon;
                    selectedAttackWeaponSlot = slot;
                    attackUnarmedChosen = false;
                    switchAttackSource();
                });
                attackWeaponChooserContainer.addNode(slotBtn);
            }

            // ---- Tech-activation checkbox (only for N2 Shell / Maser) ----
            if (selectedAttackWeapon != null && hasActivatableTech(selectedAttackWeapon)) {
                Weapon w = selectedAttackWeapon;
                String techName = (w.getCurrentTech() == Weapon.Tech.N2SHELL) ? "N2 Shell" : "Maser";

                CheckBox activateTechCb = new CheckBox("Activate " + techName);
                activateTechCb.setSelected(w.isActiveTech());
                activateTechCb.setStyle("-fx-font-weight: bold; -fx-text-fill: #8b0000; -fx-padding: 8 0 0 4;");
                activateTechCb.setTooltip(new Tooltip(
                        w.getCurrentTech() == Weapon.Tech.N2SHELL
                                ? "N2 Shell: spend 1 extra Ammo to increase the Area rating by 1."
                                : "Maser: spend 1 extra Ammo to gain Line and +1 additional Penetration."));
                activateTechCb.setOnAction(e -> {
                    boolean on = activateTechCb.isSelected();
                    w.SetActivateTech(on);
                    if (!refreshActiveProfile()) {          // new version unaffordable (e.g. the extra Ammo)
                        w.SetActivateTech(!on);
                        activateTechCb.setSelected(!on);
                        return;
                    }
                    LogMessage("Tech " + techName + " " + (on ? "ACTIVATED" : "deactivated")
                            + " for " + w.getName() + ".");
                });
                attackWeaponChooserContainer.addNode(activateTechCb);
            }
        }
    }

    private void rebuildAttackMenu() {
        if (attackScrollContainer == null) return;
        attackScrollContainer.clearNodes();

        Label header = new Label("Attack Actions");
        header.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #8b0000; -fx-padding: 0 0 4 0;");
        attackScrollContainer.addNode(header);

        FieldUnit unit = findUnitSilent(currentPlayer);
        if (unit == null || !unit.isExists()) {
            Label empty = new Label("No unit to attack with.");
            empty.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            attackScrollContainer.addNode(empty);
            return;
        }

        if (!hasAttackSourceChosen()) {
            Label hint = new Label("Choose a weapon (or Neutral) to see its attack profiles.");
            hint.setWrapText(true);
            hint.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            attackScrollContainer.addNode(hint);
            return;
        }

        String weaponName = (selectedAttackWeapon != null) ? selectedAttackWeapon.getName() : "Neutral (Unarmed)";
        Label context = new Label("Using: " + weaponName);
        context.setStyle("-fx-font-style: italic; -fx-text-fill: #555; -fx-padding: 0 0 6 0;");
        attackScrollContainer.addNode(context);

        List<AttackProfile> profiles = getCurrentAttackProfiles(unit);
        if (profiles.isEmpty()) {
            Label empty = new Label("No attack profiles available.");
            empty.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            attackScrollContainer.addNode(empty);
            return;
        }

        for (AttackProfile profile : profiles) {
            String reason = unusableReason(unit, profile);
            boolean active = attackModeActive && isSameProfile(activeAttackProfile, profile);

            VBox profileBox = new VBox(3);
            profileBox.setStyle(active
                    ? "-fx-border-color: #8b0000; -fx-border-width: 3; -fx-padding: 4;"
                    : "-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 4;");

            BetterButton btn = new BetterButton(profile.name);
            btn.setPrimaryStyle();
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setDisable(reason != null);
            btn.setOnAction(e -> startAttackVisualization(profile));

            Label descLabel = new Label(describeProfileDetailed(profile));
            descLabel.setWrapText(true);
            descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #222;");

            profileBox.getChildren().addAll(btn, descLabel);
            if (reason != null) {
                Label why = new Label("Unavailable: " + reason);
                why.setWrapText(true);
                why.setStyle("-fx-font-size: 11px; -fx-text-fill: #b00020; -fx-font-weight: bold;");
                profileBox.getChildren().add(why);
            }
            attackScrollContainer.addNode(profileBox);
        }
    }

    /** Shows the first profile the unit can actually use. Only called after a weapon / Neutral was picked. */
    private void autoSelectFirstProfile() {
        FieldUnit unit = findUnitSilent(currentPlayer);
        if (unit == null || !unit.isExists()) return;
        List<AttackProfile> profiles = getCurrentAttackProfiles(unit);
        for (AttackProfile p : profiles) {
            if (unusableReason(unit, p) == null) {
                startAttackVisualization(p);
                return;
            }
        }
        if (!profiles.isEmpty()) LocalMessage("None of this weapon's profiles can be used right now.");
    }

    private static String fmtNum(double d) {
        return d == Math.rint(d) ? String.valueOf((int) d) : String.format(Locale.US, "%.1f", d);
    }

    /** One-line description under a profile button: dice, penetration, tech, area/line, range, costs. */
    private String describeProfileDetailed(AttackProfile p) {
        List<String> parts = new ArrayList<>();

        // Damage: xdn+s
        StringBuilder dmg = new StringBuilder();
        dmg.append(p.Dice).append("d").append(p.Dicepower);
        if (p.Power != 0) dmg.append(p.Power > 0 ? "+" : "-").append(Math.abs(p.Power));
        parts.add(dmg.toString());

        // Penetration
        parts.add("Penetration " + p.Penetration);

        // Technology
        Weapon w = selectedAttackWeapon;
        if (w != null && w.getCurrentTech() != null && w.getCurrentTech() != Weapon.Tech.NONE) {
            String tech = prettifyEnum(w.getCurrentTech().name());
            if (hasActivatableTech(w)) tech += w.isActiveTech() ? " (on)" : " (off)";
            parts.add(tech);
        }

        // Line / Area (nothing for a single target)
        if (p.AreaType == -2) parts.add("Line");
        else if (p.AreaType >= 0) parts.add("Area " + p.AreaType);

        // Range
        if (p.Ranged) parts.add("Range " + p.MinRange + "-" + p.MaxRange);
        else if (p.MaxRange > 1) parts.add("Reach " + p.MaxRange);
        else parts.add("Melee");

        // Costs
        if (p.Stamina > 0)  parts.add("Stamina " + p.Stamina);
        if (p.ATP > 0)      parts.add("ATP " + p.ATP);
        if (p.AmmoCost > 0) parts.add("Ammo " + p.AmmoCost);

        return String.join(" | ", parts);
    }



    private void startAttackVisualization(AttackProfile profile) {
        FieldUnit unit = findUnitSilent(currentPlayer);
        if (unit == null || !unit.isExists()) { LocalMessage("No unit to attack with."); return; }


        // Already showing this profile: do nothing (no toggling off).
        if (attackModeActive && attackUnit == unit && isSameProfile(activeAttackProfile, profile)) return;

        String reason = unusableReason(unit, profile);
        if (reason != null) { LocalMessage(reason + "."); return; }

        activeMoveMode = null;
        movementUnit = null;
        discardPendingAttackConfirm();
        clearPreviewVisualisationLayer();
        clearPreviewArrows();
        pendingAttackHits.clear();
        attackVisualisationLayer.clear();

        attackModeActive    = true;
        activeAttackProfile = profile;
        attackUnit          = unit;

        // Range is entirely determined by the profile.
        int minRange = profile.MinRange;
        int maxRange = profile.MaxRange;

        // Attackable zone – red
        Color attackTint   = Color.rgb(255, 40, 40);
        // Too-close zone – pink, visually distinct from the attack zone
        Color tooCloseTint = Color.rgb(255, 159, 159);

        for (int dx = -maxRange; dx <= maxRange; dx++) {
            for (int dy = -maxRange; dy <= maxRange; dy++) {
                if (dx == 0 && dy == 0) continue;
                int dist = Math.max(Math.abs(dx), Math.abs(dy));
                if (dist > maxRange) continue;

                boolean isLine = profile.AreaType == -2;
                if (isLine && !(dx == 0 || dy == 0 || Math.abs(dx) == Math.abs(dy))) continue;

                int tx = unit.getX() + dx;
                int ty = unit.getY() + dy;
                if (!isOnBoard(tx, ty)) continue;

                Color tint = isAttackBelowMinRange(profile, dist) ? tooCloseTint : attackTint;

                addToPreviewVisualisationLayer(tx, ty, tint);
            }
        }

        applyVisualisation();
        rebuildAttackMenu();

        LogMessage("Visualizing attack profile '" + profile.name + "' for "
                + unit.getName() + " (range " + minRange + "-" + maxRange + ").");
    }

    /**
     * Returns true when an attack made at {@code distance} (Chebyshev) would fall
     * inside the profile's minimum range, incurring the "too close" debuff.
     * Range itself is always taken from the profile — the weapon no longer
     * contributes to it at this stage (that happens in Weapon.getWeaponProfiles()).
     */
    private boolean isAttackBelowMinRange(AttackProfile profile, int distance) {
        if (profile == null) return false;
        return distance < profile.MinRange;
    }

    private void clearAttackVisualization() {
        attackModeActive = false;
        activeAttackProfile = null;
        attackUnit = null;
        pendingAttackHits.clear();
        attackVisualisationLayer.clear();
        discardPendingAttackConfirm();
        clearPreviewArrows();
        clearPreviewVisualisationLayer();
    }



    /**
     * Highlights every sector the current unit can legally move to for the
     * given movement subtype. Toggles off if the same mode is already active.
     *
     * For Run / Cover, further stamina-tiers of range are tinted progressively
     * darker so the player can see how much stamina each sector will cost.
     */
    private void startMoveVisualization(MoveAction.MOVEMENTTYPE mode) {
        // Toggle off if pressing the same mode again
        if (activeMoveMode == mode) {
            return;
        }

        FieldUnit unit = getUnitFromName(currentPlayer);
        if (unit == null || !unit.isExists()) {
            LocalMessage("No unit to move.");
            return;
        }



        // Requirement checks per subtype
        int stamina = unit.getStamina();
        int atp     = unit.getATP();
        int speed   = Math.max(1, unit.getSpeed());

        if ((mode == MoveAction.MOVEMENTTYPE.RUN || mode == MoveAction.MOVEMENTTYPE.COVER)
                && stamina < 1) {
            LocalMessage("Not enough Stamina.");
            return;
        }
        if (mode == MoveAction.MOVEMENTTYPE.TACTICAL && unit.usedTactical()) {
            LocalMessage("Tactical action already used this turn.");
            return;
        }
        if (mode == MoveAction.MOVEMENTTYPE.REPOSITION && atp < 1) {
            LocalMessage("Not enough ATP for Reposition.");
            return;
        }

        // Fresh layer
        clearPreviewVisualisationLayer();
        activeMoveMode = mode;
        movementUnit = unit;

        int maxDist = maxDistanceFor(mode, unit);
        Color baseTint = movementTint(mode);

        for (int dx = -maxDist; dx <= maxDist; dx++) {
            for (int dy = -maxDist; dy <= maxDist; dy++) {
                if (dx == 0 && dy == 0) continue;

                int dist = Math.max(Math.abs(dx), Math.abs(dy));   // Chebyshev
                if (dist > maxDist) continue;

                int tx = unit.getX() + dx;
                int ty = unit.getY() + dy;
                if (tx < 0 || tx >= battlefield.sizeX || ty < 0 || ty >= battlefield.sizeY) continue;

                FieldUnit occupying = getUnitAt(tx, ty);
                if (occupying != null && occupying != unit) continue;

                if (mode == MoveAction.MOVEMENTTYPE.COVER) {
                    Sector s = gameBoard.getSector(tx, ty);
                    if (s == null || !gameBoard.isCover(s)) continue;
                }

                // --- Tier for the tint ---
                // Run / Cover: tier = how many stamina this distance costs, minus 1
                // Tactical / Maneuver: single tier
                // Reposition: single tier
                int tier;
                switch (mode) {
                    case RUN, COVER -> tier = Math.max(0,
                            (int) Math.ceil((double) dist / speed) - 1);
                    default         -> tier = 0;
                }

                Color tint = darkenForTier(baseTint, tier);
                addToPreviewVisualisationLayer(tx, ty, tint);
            }
        }

        // Push the layer to the board
        applyVisualisation();

        LogMessage("Visualizing " + mode + " movement for " + unit.getName()
                + " (" + previewVisualisationLayer.size() + " sectors, maxDist=" + maxDist + ").");
    }

    private void clearMovementVisualization() {
        activeMoveMode = null;
        movementUnit = null;
        lastMoveClickX = -1;
        lastMoveClickY = -1;
        clearPreviewArrows();
        clearPreviewVisualisationLayer();
    }
    /**
     * Returns a progressively darker version of {@code base}.
     * tier 0 = original, tier 1 = ~25% darker, tier 2 = ~44% darker, ...
     */
    private static Color darkenForTier(Color base, int tier) {
        if (tier <= 0) return base;
        double factor = Math.pow(0.75, tier);          // 1.0, 0.75, 0.5625, ...
        return new Color(
                base.getRed()   * factor,
                base.getGreen() * factor,
                base.getBlue()  * factor,
                1.0
        );
    }

    /** Range (in sectors, Manhattan) that a single stamina buys. */
    private static int runTierRange(FieldUnit unit) {
        return Math.max(1, unit.getSpeed());
    }

    /**
     * How much Stamina (0..n) a movement of {@code distance} costs for
     * the given subtype. Returns 0 if the subtype uses no stamina.
     */
    private static int staminaCostFor(MoveAction.MOVEMENTTYPE type, int distance, int speed) {
        switch (type) {
            case RUN, COVER -> {
                int per = Math.max(1, speed);
                int tiers = (int) Math.ceil((double) distance / per);
                return Math.max(1, tiers);
            }
            case TACTICAL, MANEUVER, REPOSITION -> { return 0; }
        }
        return 0;
    }

    /** ATP cost of the given subtype. */
    private static int atpCostFor(MoveAction.MOVEMENTTYPE type) {
        return (type == MoveAction.MOVEMENTTYPE.REPOSITION) ? 1 : 0;
    }

    /** Max reachable distance for a subtype given unit stats. */
    private static int maxDistanceFor(MoveAction.MOVEMENTTYPE type, FieldUnit unit) {
        int speed = Math.max(1, unit.getSpeed());
        switch (type) {
            case RUN, COVER    -> { return speed * Math.max(1, unit.getStamina()); }
            case TACTICAL      -> { return Math.max(2, (int) Math.ceil(speed / 2.0)); }
            case MANEUVER      -> { return 1; }
            case REPOSITION    -> { return 3; }
        }
        return 0;
    }


// ============================================================
//   SPECIAL MOVE CONFIRM POPUP
// ============================================================

    private void showMoveConfirmPopup(MoveAction action, int targetX, int targetY) {
        FieldUnit unit = getUnitFromName(action.getActor());
        if (unit == null || !unit.isExists()) return;

        final int startX   = unit.getX();
        final int startY   = unit.getY();
        final int distance = Math.max(Math.abs(action.getDeltaX()), Math.abs(action.getDeltaY()));   // Chebyshev
        final int speed    = Math.max(1, unit.getSpeed());
        final int atp      = unit.getATP();
        final int stamina  = unit.getStamina();

        Sector destSector   = gameBoard.getSector(targetX, targetY);
        boolean destIsCover = destSector != null && gameBoard.isCover(destSector);
        int tacticalRange   = Math.max(2, (int) Math.ceil(speed / 2.0));
        int maxRun          = speed * Math.max(1, stamina);

        // Build the list of currently valid subtypes for this destination
        List<MoveAction.MOVEMENTTYPE> available = new ArrayList<>();
        if (stamina >= 1 && distance <= maxRun)
            available.add(MoveAction.MOVEMENTTYPE.RUN);
        if (!unit.usedTactical() && distance <= tacticalRange)
            available.add(MoveAction.MOVEMENTTYPE.TACTICAL);
        if (atp >= 1 && distance <= 3)
            available.add(MoveAction.MOVEMENTTYPE.REPOSITION);
        if (distance == 1 && stamina >= 1)
            available.add(MoveAction.MOVEMENTTYPE.MANEUVER);
        if (destIsCover && stamina >= 1 && distance <= maxRun)
            available.add(MoveAction.MOVEMENTTYPE.COVER);

        if (available.isEmpty()) {
            LocalMessage("No valid movement types for this destination.");
            return;
        }

        // ---- Window ----
        Stage popupStage = newChildStage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Confirm Movement");
        popupStage.initStyle(StageStyle.TRANSPARENT);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + toHex(Color.DARKORANGE) + ";" +
                        "-fx-border-width: 4;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4);"
        );

        Scene scene = new Scene(content, 580, 620);
        scene.setFill(Color.TRANSPARENT);

        Label titleLabel = new Label("Confirm Movement");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // ---- Position line: start → end + distance ----
        Label posLabel = new Label(
                unit.getName() + ": " +
                        CordsToText(startX, startY) + "  →  " + CordsToText(targetX, targetY) +
                        "     (distance " + distance + ")");
        posLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        // ---- Unit status box ----
        GridPane statusGrid = new GridPane();
        statusGrid.setHgap(15);
        statusGrid.setVgap(4);
        statusGrid.setStyle(
                "-fx-background-color: #f4f4f4; -fx-padding: 8;" +
                        "-fx-border-color: #cccccc; -fx-border-width: 1;"
        );

        statusGrid.add(new Label("Stamina:"),  0, 0);
        statusGrid.add(new Label(stamina + " / " + unit.getMaxStamina()), 1, 0);
        statusGrid.add(new Label("ATP:"),      0, 1);
        statusGrid.add(new Label(atp + " / " + unit.getMaxATP()), 1, 1);
        statusGrid.add(new Label("Tactical:"), 0, 2);
        Label tactLabel = new Label(unit.hasTactical() ? "Available" : "Used");
        tactLabel.setStyle(unit.hasTactical()
                ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                : "-fx-text-fill: #b00020; -fx-font-weight: bold;");
        statusGrid.add(tactLabel, 1, 2);

        // ---- Subtype selector ----
        MoveAction.MOVEMENTTYPE defaultType =
                (activeMoveMode != null && available.contains(activeMoveMode))
                        ? activeMoveMode
                        : available.get(0);

        ComboBox<MoveAction.MOVEMENTTYPE> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(available);
        typeCombo.setValue(defaultType);
        typeCombo.setPrefWidth(220);

        HBox comboRow = new HBox(10, new Label("Movement Type:"), typeCombo);
        comboRow.setAlignment(Pos.CENTER);

        // ---- Cost line (recomputes on subtype change) ----
        Label costLabel = new Label();
        costLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        Runnable updateCost = () -> {
            MoveAction.MOVEMENTTYPE t = typeCombo.getValue();
            int sc = staminaCostFor(t, distance, speed);
            int ac = atpCostFor(t);
            costLabel.setText("Cost:  " + sc + " Stamina,  " + ac + " ATP");
            costLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; "
                    + "-fx-text-fill: " + ((sc > stamina || ac > atp)
                    ? "#b00020" : "#2e7d32") + ";");
        };
        updateCost.run();

        // ---- Description box ----
        TextArea descArea = new TextArea();
        descArea.setEditable(false);
        descArea.setWrapText(true);
        descArea.setPrefRowCount(9);
        descArea.setPrefWidth(520);
        descArea.setText(getMovementTypeDescription(defaultType));

        typeCombo.valueProperty().addListener((o, oldV, newV) -> {
            if (newV != null) {
                descArea.setText(getMovementTypeDescription(newV));
                updateCost.run();
            }
        });

        // ---- Buttons ----
        BetterButton proceedBtn = new BetterButton("Proceed");
        proceedBtn.setSuccessStyle();
        proceedBtn.setOnAction(e -> {
            MoveAction.MOVEMENTTYPE chosen = typeCombo.getValue();
            applyMovementTypeToAction(action, chosen);
            SendAction(action);
            clearMovementVisualization();
            confirmAction = null;
            confirmContainer.getChildren().clear();
            popupStage.close();
        });

        BetterButton cancelBtn = new BetterButton("Cancel");
        cancelBtn.setDangerStyle();
        cancelBtn.setOnAction(e -> {
            popupStage.close();
        });

        HBox btnBox = new HBox(10, cancelBtn, proceedBtn);
        btnBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(
                titleLabel,
                posLabel,
                statusGrid,
                comboRow,
                costLabel,
                descArea,
                btnBox);

        // Draggable borderless window
        final double[] dragOffset = new double[2];
        content.setOnMousePressed(e -> {
            dragOffset[0] = e.getSceneX();
            dragOffset[1] = e.getSceneY();
        });
        content.setOnMouseDragged(e -> {
            popupStage.setX(e.getScreenX() - dragOffset[0]);
            popupStage.setY(e.getScreenY() - dragOffset[1]);
        });

        popupStage.setScene(scene);
        popupStage.show();
    }

    private String getMovementTypeDescription(MoveAction.MOVEMENTTYPE type) {
        switch (type) {
            case RUN:
                return "The Eva dashes across the battlefield, achieving a new position.  " +
                        "Move up to your Speed. \n\n" +
                        "When you Run at least half your speed (minimum 2 Sectors), you also gain " +
                        "a +15 bonus to Reflexes until the start of your next Turn or Interval.  \n\n" +
                        "This bonus does not stack with itself nor with the Defend Action.";
            case COVER:
                return "You move up to your Speed, ending adjacent to a nearby sturdy object.  " +
                        "Until the start of your next Turn or Interval, you gain +1 Armor.";
            case TACTICAL:
                return "Once per Turn or Interval, you may take one of the following Actions " +
                        "for 0 Stamina\n" +
                        "Move up to half your Speed, minimum 2 Sectors.";
            case MANEUVER:
                return "You carefully move around the Battlefield.  You move one Sector, and this " +
                        "movement cannot trigger Attacks of Opportunity";
            case REPOSITION:
                return "You may expend your ATP to move";
        }
        return "";
    }

    private void applyMovementTypeToAction(MoveAction action, MoveAction.MOVEMENTTYPE type) {
        action.setMovementType(type);

        FieldUnit actor = getUnitFromName(action.getActor());
        int speed = (actor != null) ? Math.max(1, actor.getSpeed()) : 1;
        int distance = Math.max(Math.abs(action.getDeltaX()),
                Math.abs(action.getDeltaY()));   // Chebyshev

        switch (type) {
            case RUN, COVER -> {
                action.staminaCost = staminaCostFor(type, distance, speed);
                action.ATPCost     = 0;
                action.setTactical(false);
                action.setCanTriggerAtkofOp(true);
            }
            case TACTICAL -> {
                action.staminaCost = 0;
                action.ATPCost     = 0;
                action.setTactical(true);
                action.setCanTriggerAtkofOp(true);
            }
            case MANEUVER -> {
                action.staminaCost = 1;
                action.ATPCost     = 0;
                action.setCanTriggerAtkofOp(false);
                action.setTactical(false);
            }
            case REPOSITION -> {
                action.staminaCost = 0;
                action.ATPCost     = 1;
                action.setTactical(false);
                action.setCanTriggerAtkofOp(true);
            }
        }
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
        EndTurnAction action = new EndTurnAction(currentActionNumber+1, currentPlayer);
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

    private void LogError(String error) {
        TextArea output = dmConsoleOutput;
        if (error == null || error.trim().isEmpty()) return;
        LogMessage(error);
        output.appendText(error);
    }


    private void processDMCommand(String command, TextArea output) {
        if (command == null || command.trim().isEmpty()) return;
        String[] parts = command.trim().split(" ");
        String action = parts[0].toLowerCase();
        if (action.startsWith("/")) action = action.substring(1);
        switch (action) {
            case "savegame":   output.appendText(cmdSaveGame(parts));   break;
            case "saves":      output.appendText(cmdListSaves());       break;
            case "opengame":   output.appendText(cmdOpenGame(parts));   break;
            case "livegame":   output.appendText(cmdLiveGame());        break;
            case "replacegame":output.appendText(cmdReplaceGame(parts));break;
            case "revert":     output.appendText(cmdRevert(parts));     break;
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
                        currentActionNumber+1, "DM", name, x, y, team);
                SendAction(createAction);
                output.appendText("Sent DMCreateUnitAction for '" + name + "' at ("
                        + x + ", " + y + ") team=" + team + ".\n");
                break;
            case "giveweapon":
                // Form 1: giveWeapon <slotNum> <weaponName>               → current player's unit
                // Form 2: giveWeapon <unitName> <slotNum> <weaponName>    → named unit
                if (parts.length < 3) {
                    output.appendText("Error: Usage: giveWeapon <slotNum> <weaponName>\n");
                    output.appendText("       giveWeapon <unitName> <slotNum> <weaponName>\n");
                    output.appendText("Available weapons: " + String.join(", ",
                            listAvailableWeaponFiles()) + "\n");
                    return;
                }

                String gwUnitName;
                int    gwSlotNum;
                String gwWeaponName;

                if (parts.length == 3) {
                    gwUnitName = currentPlayer;
                    try {
                        gwSlotNum = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ex) {
                        output.appendText("Error: slot number must be an integer.\n");
                        return;
                    }
                    gwWeaponName = parts[2];
                } else {
                    gwUnitName = parts[1];
                    try {
                        gwSlotNum = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException ex) {
                        output.appendText("Error: slot number must be an integer.\n");
                        return;
                    }
                    StringBuilder wn = new StringBuilder();
                    for (int j = 3; j < parts.length; j++) {
                        if (j > 3) wn.append(" ");
                        wn.append(parts[j]);
                    }
                    gwWeaponName = wn.toString();
                }

                if (gwWeaponName == null || gwWeaponName.isBlank()) {
                    output.appendText("Error: weapon name cannot be empty.\n");
                    return;
                }

                // ---- DM-side validation against the live world ----
                FieldUnit gwTarget = getUnitFromName(gwUnitName);
                if (gwTarget == null || !(gwTarget.getUnit() instanceof Evangelion)) {
                    output.appendText("Error: no Evangelion named '" + gwUnitName + "'.\n");
                    return;
                }
                int slotCount = ((Evangelion) gwTarget.getUnit()).getSlots().size();
                if (gwSlotNum < 0 || gwSlotNum >= slotCount) {
                    output.appendText("Error: slot " + gwSlotNum
                            + " out of range (0-" + (slotCount - 1) + ").\n");
                    return;
                }

                // ---- DM-side disk load; the object rides inside the action ----
                Weapon gwWeapon = loadWeaponByName(gwWeaponName);
                if (gwWeapon == null) {
                    output.appendText("Error: could not load weapon '" + gwWeaponName
                            + "' from Active/weapons/.\n");
                    output.appendText("Available: " + String.join(", ",
                            listAvailableWeaponFiles()) + "\n");
                    return;
                }

                DMGiveWeaponAction gwAction = new DMGiveWeaponAction(
                        currentActionNumber+1, "DM", gwUnitName, gwSlotNum, gwWeapon);
                SendAction(gwAction);
                output.appendText("Sent DMGiveWeaponAction: '" + gwWeapon.getName()
                        + "' → " + gwUnitName + " slot #" + gwSlotNum + ".\n");
                break;
            case "round":
                EndRoundAction action1 = new EndRoundAction(
                        currentActionNumber+1, "DM");
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
                        currentActionNumber+1, "DM", targetName, newTeam);
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
                            currentActionNumber+1, "DM", dx, dy);
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
                        currentActionNumber+1, "DM", found.getX(), found.getY());
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
                DMChoosePlayerAction dmAction = new DMChoosePlayerAction(currentActionNumber+1, "DM", activeName);
                SendAction(dmAction);
                output.appendText("DM chose next player: " + activeName + " and created dmAction "+dmAction.getActionNumber());
                break;

            case "progress":
                output.appendText("Manually reloading GameState and processing new actions...\n");
                reloadGameState();
                break;
            case "weapons":
                // List every weapon file in Active/weapons/, together with a
                // short summary of its key stats so the DM can pick one to
                // give out via the giveWeapon command.
                output.appendText("Saved weapons in Active/weapons/:\n");
                File wdir = new File("Active/weapons");
                if (!wdir.isDirectory()) {
                    output.appendText("  (directory does not exist)\n");
                    break;
                }
                File[] weaponFiles = wdir.listFiles((d, n) -> n.toLowerCase().endsWith(".ser"));
                if (weaponFiles == null || weaponFiles.length == 0) {
                    output.appendText("  (no weapons saved)\n");
                    break;
                }
                Arrays.sort(weaponFiles, Comparator.comparing(File::getName));
                for (File wf : weaponFiles) {
                    String baseName = wf.getName().substring(0, wf.getName().length() - 4);
                    Weapon preview = loadWeaponForPreview(wf);
                    if (preview == null) {
                        output.appendText("  " + baseName + "   (unreadable)\n");
                        continue;
                    }
                    StringBuilder line = new StringBuilder();
                    line.append("  ").append(baseName);
                    line.append("   [").append(preview.getProfileType()).append("]");
                    if (preview.isRanged()) {
                        line.append("  Rng ").append(preview.getMinRange())
                                .append("-").append(preview.getMaxRange())
                                .append("  Ammo ").append(preview.getMaxAmmo());
                    } else if (preview.getMaxRange() > 1) {
                        line.append("  Reach ").append(preview.getMaxRange());
                    }
                    if (!preview.Technology.isEmpty()
                            && preview.getCurrentTech() != Weapon.Tech.NONE) {
                        line.append("  Tech ").append(preview.getCurrentTech());
                    }
                    if (!preview.WeaponProperties.isEmpty()) {
                        line.append("  ").append(preview.WeaponProperties);
                    }
                    line.append("\n");
                    output.appendText(line.toString());
                }
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
                output.appendText("  showAllUnits          – toggle showing finished units in the next-turn list\n");
                output.appendText("  round                  – Ends the round forcefully\n");
                output.appendText("  giveWeapon <slot> <weapon>            – replace current player's slot with a saved weapon\n");
                output.appendText("  giveWeapon <unit> <slot> <weapon>     – same, but for a named unit\n");
                output.appendText("  weapons                – list all saved weapons in Active/weapons/\n");
                output.appendText("  savegame <name> [overwrite]   – saves the current game to the saved folder\n");
                output.appendText("  saves                         – lists saved games\n");
                output.appendText("  opengame <name>               – opens a save in your private DMgame (players unaffected)\n");
                output.appendText("  livegame                      – returns you to the shared activegame\n");
                output.appendText("  replacegame <name> confirm    – REPLACES activegame with a save; every client restarts\n");
                output.appendText("  revert <actionNumber> confirm – deletes all actions after that number, loads result into activegame\n");
                break;

            default:
                output.appendText("Unknown command: " + action + ". Type 'help' for a list.\n");
                break;
        }
    }

    /**
     * Deserializes a single weapon file for display purposes only.
     * Never throws; returns {@code null} on any problem so the listing
     * command can print "(unreadable)" and carry on.
     */
    private Weapon loadWeaponForPreview(File file) {
        if (file == null || !file.isFile()) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            return (obj instanceof Weapon w) ? w : null;
        } catch (IOException | ClassNotFoundException e) {
            LogMessage("Preview load failed for '" + file.getName() + "': " + e.getMessage());
            return null;
        }
    }
    /**
     * Builds the text shown in the inventory description pane when an item is
     * clicked. For weapons this assembles a formatted block from the weapon's
     * own fields (type, hands, range, tech, properties, customisations, …);
     * for any other item it falls back to the item's own getDescription(),
     * or to the name if that comes back blank.
     */
    private String describeItemForInventory(Item item) {
        if (item == null) return "Empty slot.";
        if (item instanceof Weapon w) return describeWeaponForInventory(w);
        String desc = item.getDescription();
        return (desc == null || desc.isBlank()) ? item.getName() : desc;
    }

    /** Formatted multi-line description of a single weapon. */
    private String describeWeaponForInventory(Weapon w) {
        StringBuilder sb = new StringBuilder();

        // ---- Header: name + type + hands + ranged/melee ----
        sb.append(w.getName()).append("\n");
        sb.append(w.getProfileType());

        String hands = switch (w.getHands()) {
            case ONE_HANDED -> "  (One-Handed)";
            case TWO_HANDED -> "  (Two-Handed)";
            case NONE       -> "";
        };
        sb.append(hands);
        sb.append(w.isRanged() ? "  •  Ranged" : "  •  Melee");
        sb.append("\n");
        sb.append("────────────────────────────\n");

        // ---- Range / ammo ----
        if (w.isRanged()) {
            sb.append("Range:  ").append(w.getMinRange())
                    .append(" – ").append(w.getMaxRange()).append("\n");
            sb.append("Ammo:   ").append(w.getAmmo())
                    .append(" / ").append(w.getMaxAmmo()).append("\n");
        } else if (w.getMaxRange() > 1) {
            sb.append("Reach:  ").append(w.getMaxRange()).append("\n");
        }

        // ---- Tech ----
        if (w.Technology != null && !w.Technology.isEmpty()
                && w.getCurrentTech() != Weapon.Tech.NONE) {
            sb.append("Tech:   ").append(w.getCurrentTech());
            if (w.isActiveTech()) sb.append("  [ACTIVE]");
            sb.append("\n");
        }

        // ---- Base combat stats ----
        if (w.getBasePenetration() > 0)
            sb.append("Penetration:  +").append(w.getBasePenetration()).append("\n");

        if (w.getBaseArea() == -2) {
            sb.append("Area:   Line\n");
        } else if (w.getBaseArea() >= 0) {
            sb.append("Area:   ").append(w.getBaseArea()).append("\n");
        }

        if (w.getDefensive() > 0)
            sb.append("Defensive:  ").append(w.getDefensive()).append("\n");

        // ---- Properties ----
        if (w.WeaponProperties != null && !w.WeaponProperties.isEmpty()) {
            sb.append("\nProperties:\n");
            for (Weapon.WeaponProperty p : w.WeaponProperties) {
                sb.append("  • ").append(prettifyEnum(p.name())).append("\n");
            }
        }

        // ---- Customisations ----
        if (w.Customisations != null && !w.Customisations.isEmpty()) {
            sb.append("\nCustomisations:\n");
            for (Weapon.Customisation c : w.Customisations) {
                sb.append("  • ").append(prettifyEnum(c.name())).append("\n");
            }
        }

        if (!w.getEffects().isEmpty()) {
            sb.append("\nEffects:\n");
            for (Effect e : w.getEffects()) sb.append("  • ").append(e.getName()).append("\n");
        }

        // ---- Optional flavour text from the item itself ----
        String flavour = w.getDescription();
        if (flavour != null && !flavour.isBlank()) {
            sb.append("\n").append(flavour);
        }

        return sb.toString();
    }

    /** Turns "ONE_HANDED" / "TELESCOPIC_SIGHT" into "One Handed" / "Telescopic Sight". */
    private static String prettifyEnum(String enumName) {
        if (enumName == null) return "";
        String[] words = enumName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (words[i].isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(words[i].charAt(0)))
                    .append(words[i].substring(1));
        }
        return sb.toString();
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
        refreshStatsContainers();
    }

    private void checkShowPopUp() {
        if (closed) return;
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
        if (closed) return;
        QueuePosition current = getCurrentPosition();
        if (current == null || !current.Reaction) return;

        Action currentAction = gamestate.getActionfromNumber(current.getReactionTo());
        if (currentAction == null) {
            LogError("ERROR: REACTION POP UP SHOWING WITHOUT PROPER ACTION TO REACT TO\n");
            return;
        }
        popupShowing = true;

        Stage popupStage = newChildStage();
        popupStage.initModality(Modality.NONE);
        popupStage.setTitle("Reaction Turn");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label descLabel = new Label(current.getDescription());
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(440);
        descLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        content.getChildren().add(descLabel);

        Node specificContent = null;
        BetterButton confirmBtn = new BetterButton("Confirm");
        BetterButton cancelBtn = null;

        switch (current.getReactionType()) {
            case DM_SETUP_PLAYER:
                specificContent = buildDMSetupContent(confirmBtn, popupStage, currentAction);
                break;
            case PLAYER_SETUP:
                specificContent = buildPlayerSetupContent(confirmBtn, popupStage, currentAction);
                break;
            case DEFENCE:
                specificContent = buildDefenceContent(confirmBtn, popupStage, currentAction, current);
                break;
            case WOUND:
                specificContent = buildWoundContent(confirmBtn, popupStage, currentAction, current);
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
        ReactionType rt = current.getReactionType();
        confirmBtn.setDisable(rt != ReactionType.DEFENCE && rt != ReactionType.WOUND);


        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER);
        if (cancelBtn != null) btnBox.getChildren().add(cancelBtn);
        btnBox.getChildren().add(confirmBtn);
        content.getChildren().add(btnBox);

        boolean defenceUi = current.getReactionType() == ReactionType.DEFENCE;
        Scene scene = new Scene(content, defenceUi ? 480 : 400, defenceUi ? 560 : 300);
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


    private Node buildWoundContent(Button proceedBtn, Stage popupStage, Action cause, QueuePosition reactionPos) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(5));

        FieldUnit unit = getUnitFromName(reactionPos.getUnitID());
        if (unit == null || !unit.isExists()) {
            LogError("ERROR - wound popup without a valid unit\n");
            root.getChildren().add(new Label("Nothing to do."));
            return root;
        }

        String by = (cause instanceof AttackAction a) ? a.getActor() : "an unknown source";
        Label from = new Label("Wound dealt by: " + by);
        from.setStyle("-fx-font-weight: bold; -fx-text-fill: #8b0000;");
        Label tough = new Label("Toughness: " + Math.max(0, unit.getToughness()) + " / " + unit.getMaxToughness());
        Label note = new Label("When you proceed, your Toughness is restored to " + unit.getMaxToughness() + ".");
        note.setWrapText(true);
        note.setMaxWidth(340);
        VBox box = new VBox(4, from, tough, note);
        box.setStyle("-fx-background-color: #fff0f0; -fx-padding: 8; -fx-border-color: #d99; -fx-border-width: 1;");

        proceedBtn.setText("Proceed");
        proceedBtn.setDisable(false);
        proceedBtn.setOnAction(e -> {
            if (queue.currentPosition() != reactionPos) { popupStage.close(); return; }   // stale popup
            proceedBtn.setDisable(true);
            SendAction(new DealWoundAction(currentActionNumber + 1, unit.getName()));
            popupStage.close();
        });

        root.getChildren().add(box);
        return root;
    }

    private Node buildDefenceContent(Button proceedBtn, Stage popupStage, Action cause, QueuePosition reactionPos) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(5));

        FieldUnit defender = getUnitFromName(reactionPos.getUnitID());
        if (!(cause instanceof AttackAction atk) || atk.getActionCombatProfile() == null
                || defender == null || !defender.isExists()) {
            LogError("ERROR - defence popup without a valid attack\n");
            root.getChildren().add(new Label("Nothing to defend against."));
            return root;
        }
        final AttackProfile profile = atk.getActionCombatProfile();
        final boolean reduced = isReducedByMiss(atk);
        final int incoming = (reduced ? atk.rolledDamage / 2 : atk.rolledDamage);

        // ---------- Incoming attack ----------
        Label inHeader = new Label("Incoming attack");
        inHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #8b0000;");
        Label dmg = new Label("Damage: " + incoming + (reduced ? "  (halved, the attack missed)" : ""));
        Label pen = new Label("Penetration: " + profile.Penetration);
        Label strain = new Label("Strain: " + profile.Strain + "  (ignores Armor, applies even if you guard)");
        VBox incomingBox = new VBox(3, inHeader, dmg, pen, strain);
        incomingBox.setStyle("-fx-background-color: #fff0f0; -fx-padding: 8; -fx-border-color: #d99; -fx-border-width: 1;");
        if (profile.AreaType != -1 && !reduced) {
            Label areaNote = new Label("Area/Line: a successful Guard only halves the damage.");
            areaNote.setWrapText(true);
            areaNote.setStyle("-fx-font-style: italic; -fx-text-fill: #b00020;");
            incomingBox.getChildren().add(areaNote);
        }
        // ---------- Your stats ----------
        Label youHeader = new Label("Your defences: " + defender.getName());
        youHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a4d;");
        Label reflexes = new Label("Reflexes: " + defender.getReflexes());
        Label guardState = new Label("Guard: " + (defender.usedGuard() ? "already used this round" : "available"));
        Label atp = new Label("ATP: " + defender.getATP() + " / " + defender.getMaxATP());
        Label toughness = new Label("Toughness: " + defender.getToughness() + " / " + defender.getMaxToughness());
        Label armorLabel = new Label();
        Label expected = new Label();
        VBox statsBox = new VBox(3, youHeader, reflexes, guardState, atp, toughness, armorLabel, expected);
        statsBox.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 8; -fx-border-color: #ccc; -fx-border-width: 1;");

        // ---------- Layered Field ----------
        CheckBox layered = new CheckBox("Use Layered Field  (costs 1 ATP, +" + LAYERED_FIELD_ARMOR + " Armor)");
        layered.setDisable(defender.getATP() < 1);

        Runnable updateArmor = () -> {
            int base = defender.getArmor();
            boolean on = layered.isSelected();
            int total = base + (on ? LAYERED_FIELD_ARMOR : 0);
            armorLabel.setText(on ? "Armor: " + base + " + " + LAYERED_FIELD_ARMOR + " = " + total
                    : "Armor: " + base);
            armorLabel.setStyle(on ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;" : "");
            int through = Math.max(0, incoming - Math.max(0, total - profile.Penetration));
            expected.setText("Damage you would take if unguarded: " + through+(profile.Strain > 0 ? " + "+profile.Strain+" strain" : ""));
        };
        layered.selectedProperty().addListener((o, a, b) -> updateArmor.run());
        updateArmor.run();

        // ---------- Guard ----------
        // TODO: Guard is disabled for a missed Area/Line because it cannot reduce the half damage any
        // further. If the rules change (e.g. Guard should still negate it), remove `reduced` here AND in

        final boolean guardBlocked = defender.usedGuard() || reduced;
        final int guardTN = AttackAction.clampTN(defender.getReflexes());
        final boolean[] rolled = {false};
        final int[] roll = {0};

        BetterButton guardBtn = new BetterButton("Roll Guard (1d100 vs " + guardTN + ")");
        guardBtn.setPrimaryStyle();
        guardBtn.setDisable(guardBlocked);
        Label guardResult = new Label(reduced ? "Guard cannot reduce the damage of a missed area/line attack."
                : defender.usedGuard() ? "Guard already used." : "");
        guardResult.setWrapText(true);

        guardBtn.setOnAction(e -> {
            if (rolled[0]) return;
            roll[0] = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 101);
            rolled[0] = true;
            boolean ok = AttackAction.rollSucceeds(roll[0], guardTN);
            guardResult.setText("Rolled " + roll[0] + " vs " + guardTN + (ok
                    ? (profile.AreaType != -1 ? "  -  GUARD SUCCESS, half damage" : "  -  GUARD SUCCESS, no damage")
                    : "  -  guard failed"));
            guardResult.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (ok ? "#2e7d32" : "#b00020") + ";");
            guardBtn.setDisable(true);
        });

        // ---------- Proceed ----------
        proceedBtn.setText("Proceed");
        proceedBtn.setDisable(false);
        proceedBtn.setOnAction(e -> {
            if (queue.currentPosition() != reactionPos) { popupStage.close(); return; }   // stale popup
            proceedBtn.setDisable(true);
            DefenceAction d = new DefenceAction(currentActionNumber + 1, defender.getName());
            d.layeredField = layered.isSelected() && defender.getATP() > 0;
            d.ATPCost = d.layeredField ? 1 : 0;
            d.guardRolled = rolled[0];
            d.guardRoll = roll[0];
            d.guardTN = guardTN;
            SendAction(d);
            popupStage.close();
        });

        root.getChildren().addAll(incomingBox, statsBox, layered, new Separator(), guardBtn, guardResult);
        return root;
    }

    private boolean nameIsPicked(String name, GameState gamestate1) {
        if (name.equalsIgnoreCase("DM")) return true;
        if (gamestate1 != null) {
            for (Action action : gamestate1.getActions()) {
                if (action instanceof AddPlayerAction ap && ap.getUnitName().equalsIgnoreCase(name)) return true;
            }
        }
        for (FieldUnit unit : UnitList) {
            if (unit.getName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /** Spawn points of players the DM already added but who have no unit yet. */
    private Set<Cell> reservedSpawnCells() {
        Set<Cell> reserved = new HashSet<>();
        if (gamestate == null) return reserved;
        for (Action a : gamestate.getActions()) {
            if (!(a instanceof AddPlayerAction ap)) continue;
            boolean hasUnit = false;
            for (FieldUnit u : UnitList) {
                if (u.getName().equals(ap.getUnitName())) { hasUnit = true; break; }
            }
            if (!hasUnit) reserved.add(new Cell(ap.getX(), ap.getY()));
        }
        return reserved;
    }

    /** True if a living unit stands on (x,y) or a pending player is going to spawn there. */
    private boolean isSpawnCellTaken(int x, int y) {
        if (getUnitAt(x, y) != null) return true;
        return reservedSpawnCells().contains(new Cell(x, y));
    }

    private Integer parseIntOrNull(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    /** Grey = reserved by a pending player, green/red = the sector currently chosen. */
    private void refreshSpawnMarkers(Integer selX, Integer selY) {
        previewVisualisationLayer.clear();
        for (Cell c : reservedSpawnCells()) {
            addToPreviewVisualisationLayer(c.x(), c.y(), Color.rgb(150, 150, 150));
        }
        if (selX != null && selY != null && isOnBoard(selX, selY)) {
            addToPreviewVisualisationLayer(selX, selY,
                    isSpawnCellTaken(selX, selY) ? Color.RED : Color.LIMEGREEN);
        }
        applyVisualisation();
    }

    private Node buildDMSetupContent(Button confirmBtn, Stage popupStage, Action cause) {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(5));

        TextField nameField = new TextField();
        nameField.setPromptText("Player name");
        TextField xField = new TextField();
        xField.setPromptText("X (0-" + (battlefield.sizeX - 1) + ")");
        TextField yField = new TextField();
        yField.setPromptText("Y (0-" + (battlefield.sizeY - 1) + ")");

        TextField teamField = new TextField();
        teamField.setPromptText("Team #");
        boolean manualTeam = (gameMode == GameState.GAME_MODE.TEAM
                || gameMode == GameState.GAME_MODE.CUSTOM);

        Label hint = new Label("Tip: click a sector on the Battlefield tab to fill in X and Y.");
        hint.setWrapText(true);
        hint.setMaxWidth(340);
        hint.setStyle("-fx-font-style: italic; -fx-text-fill: #555;");

        Label problemLabel = new Label();
        problemLabel.setWrapText(true);
        problemLabel.setMaxWidth(340);
        problemLabel.setStyle("-fx-text-fill: #b00020; -fx-font-weight: bold;");

        Runnable validate = () -> {
            String name = nameField.getText().trim();
            Integer x = parseIntOrNull(xField.getText());
            Integer y = parseIntOrNull(yField.getText());
            String problem = null;

            if (name.isEmpty()) {
                problem = "Enter a player name.";
            } else if (nameIsPicked(name, gamestate)) {
                problem = "The name '" + name + "' is already used by another player or unit.";
            } else if (x == null || y == null) {
                problem = "Click a sector on the battlefield, or enter X and Y.";
            } else if (!isOnBoard(x, y)) {
                problem = "Coordinates are outside the board.";
            } else if (isSpawnCellTaken(x, y)) {
                problem = "Sector " + CordsToText(x, y) + " is occupied or reserved for another player.";
            } else if (manualTeam && parseIntOrNull(teamField.getText()) == null) {
                problem = "Enter a team number.";
            }

            problemLabel.setText(problem == null ? "" : problem);
            confirmBtn.setDisable(problem != null);
            refreshSpawnMarkers(x, y);
        };

        nameField.textProperty().addListener((o, a, b) -> validate.run());
        xField.textProperty().addListener((o, a, b) -> validate.run());
        yField.textProperty().addListener((o, a, b) -> validate.run());
        teamField.textProperty().addListener((o, a, b) -> validate.run());

        // Board clicks fill X/Y while this popup is open
        spawnPickHandler = (cx, cy) -> {
            xField.setText(String.valueOf(cx));
            yField.setText(String.valueOf(cy));
        };
        popupStage.setOnHidden(e -> {
            spawnPickHandler = null;
            clearPreviewVisualisationLayer();
        });

        confirmBtn.setOnAction(e -> {
            validate.run();                       // the state may have changed since the last keystroke
            if (confirmBtn.isDisable()) return;

            String name = nameField.getText().trim();
            int x = Integer.parseInt(xField.getText().trim());
            int y = Integer.parseInt(yField.getText().trim());

            int team;
            switch (gameMode) {
                case CLASSIC -> team = 0;
                case FFA -> {
                    long existing = gamestate.getActions().stream()
                            .filter(a -> a instanceof AddPlayerAction).count();
                    team = (int) existing + 1;
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
                hint,
                new HBox(10, new Label("Name:"), nameField),
                new HBox(10, new Label("X:"), xField),
                new HBox(10, new Label("Y:"), yField)
        );
        if (manualTeam) {
            vbox.getChildren().add(new HBox(10, new Label("Team:"), teamField));
        }
        vbox.getChildren().add(problemLabel);

        validate.run();   // sets the initial state and shows the grey reserved sectors
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
        boolean canAct = isMyActionTurn();

        topBar.setStyle(isMyTurn ? (canAct ? "-fx-background-color: #2e7d32;" : "-fx-background-color: #d1cc47;") : "-fx-background-color: #444444;");

        boolean justBecameAvailable = canAct && !couldActLastCheck;
        couldActLastCheck = canAct;
        updateTurnEndButton(canAct);

        if (justBecameAvailable && confirmAction != null) {
            revalidatePendingActionForTurn();
        }
        renderConfirmButton();
        updateAttackButtonState();
    }
    /**
     * Your turn just started and an action was prepared earlier. Positions, stamina, ATP and
     * ammo may have changed, so rebuild the preview from scratch and re-click the same sectors.
     */
    private void revalidatePendingActionForTurn() {
        if (confirmAction instanceof AttackAction) {
            if (!refreshActiveProfile()) {              // existing method: re-creates zone + clicks, rebuilds Confirm
                clearAttackVisualization();
                LocalMessage("Your prepared attack is no longer possible.");
            }
            return;
        }

        if (confirmAction instanceof MoveAction) {
            if (activeMoveMode == null || movementUnit == null || lastMoveClickX < 0) {
                confirmAction = null;                   // stale leftover from a cleared visualization
                return;
            }
            MoveAction.MOVEMENTTYPE mode = activeMoveMode;
            int cx = lastMoveClickX, cy = lastMoveClickY;

            clearMovementVisualization();
            confirmAction = null;
            startMoveVisualization(mode);

            if (activeMoveMode != null && isInPreviewVisualisationLayer(cx, cy)) {
                handleMovementSectorClick(cx, cy);      // builds the new pending MoveAction
            } else {
                clearMovementVisualization();
                LocalMessage("Your prepared move is no longer possible.");
            }
        }
        // Other pending actions (inventory drags) are kept as they are.
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
                            currentActionNumber+1, "DM", unit.getName());
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
                registerArrow(arrow);
            }
            confirmAction = InventoryItemTransferAction.pickUpAction(currentActionNumber+1, currentPlayer,
                    ((Evangelion) evaUnit.getUnit()).getSlots().indexOf(toSlot), draggedFromField.getX()- evaUnit.getX(),
                    draggedFromField.getY() - evaUnit.getY());
            s = "Picking up "+item.getName()+" from ("+draggedFromField.getX()+","+draggedFromField.getY()+") with "+toSlot.getName();
        } else {
            confirmAction = InventoryItemTransferAction.InventorySwitch(currentActionNumber+1, currentPlayer,
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
            registerArrow(arrow);
        }
        confirmAction = InventoryItemTransferAction.dropAction(currentActionNumber+1, currentPlayer,
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



    public void clearPreviewArrows() {
        Iterator<Arrow> it = arrows.iterator();
        while (it.hasNext()) {
            Arrow a = it.next();
            if (a.getArrowType() == Arrow.ArrowType.PREVIEW) {
                a.delete();
                it.remove();
            }
        }
    }


    private int absoluteTurn(int round, int turn) {
        return roundStartOffset.getOrDefault(round, 0) + turn;
    }

    /** Every ACTION arrow gets the Round / Turn it was created in. */
    private void registerArrow(Arrow a) {
        a.setCreatedAt(CurrentRound, CurrentTurn);
        LogMessage("Arrow " + a.getArrowType() + " stamped R" + CurrentRound + " T" + CurrentTurn);
        arrows.add(a);
    }

    /** Removes ACTION arrows that are at least `arrowLifetimeTurns` turns old. 0 = keep forever. */
    private void pruneActionArrows() {
        int limit = options.arrowLifetimeTurns;
        if (limit <= 0) return;
        int now = absoluteTurn(CurrentRound, CurrentTurn);
        Iterator<Arrow> it = arrows.iterator();
        while (it.hasNext()) {
            Arrow a = it.next();
            if (a.getArrowType() != Arrow.ArrowType.ACTION) continue;
            if (a.getRound() < 0) {
                a.setCreatedAt(CurrentRound, CurrentTurn);
                continue;
            }
            if (now - absoluteTurn(a.getRound(), a.getTurn()) >= limit) {
                a.delete();
                it.remove();
            }
        }
    }

    private void rebuildInventoryTab() {
        LogMessage("Attempting to rebuild inventory tab");
        inventoryTab.getChildren().clear();
        FieldUnit unit = getUnitFromName(currentPlayer);
        if (unit == null || !(unit.getUnit() instanceof Evangelion)) {
            Label placeholder = new Label("Select an Evangelion to view inventory.");
            inventoryTab.getChildren().add(placeholder);
            rebuildWeaponChooser();
            rebuildAttackMenu();
            return;
        }
        Evangelion eva = (Evangelion) unit.getUnit();
        BuildEvangelionInventory(eva);
        rebuildWeaponChooser();
        rebuildAttackMenu();
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
            descriptionLabel.setText(describeItemForInventory(currentItem));
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
        registerArrow(arrow);
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
    // ---- Current-stats containers (right side of the battlefield bottom pane) ----
// ---- Current-stats containers (right side of the battlefield bottom pane) ----
    private ScrollableContainer currentStatsContainer;      // ALWAYS shows the current player's unit
    private ScrollableContainer secondaryStatsContainer;    // shows the clicked/inspected unit
    private FieldUnit statsUnit2 = null;                    // inspected unit (null = hidden)
    /**
     * Selector for {@link #getStatsForFieldUnit(FieldUnit, int)}.
     *   0 = BASIC    – stamina / ATP / tactical / armor / toughness
     *   1 = COMBAT   – BASIC + accuracy / attack strength / reflexes / speed
     *   2 = FULL     – COMBAT + team / position / effects / turn-done
     */
    private int statsMode = 1;
    private BetterButton attackBtn;
    private String attackBtnDefaultStyle = "";
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


        // ----- CurrentStats container — occupies the remaining right area -----
        currentStatsContainer = new ScrollableContainer(
                0.50, 0.49, 0.02, 0.96, false, false);
        currentStatsContainer.setContainerPadding(new Insets(10));
        currentStatsContainer.setSpacing(6);
        currentStatsContainer.setBackgroundColor(Color.rgb(235, 240, 255, 0.95), true);
        currentStatsContainer.setBorderStyle(
                "-fx-border-color: #333366; -fx-border-width: 2; -fx-border-radius: 4;");
        currentStatsContainer.setFitToHeight(true);
        currentStatsContainer.setStyle(
                "-fx-background: rgba(235,240,255,0.950000);" +
                        "-fx-background-color: rgba(235,240,255,0.950000);" +
                        "-fx-background-insets: 0;");
        currentStatsContainer.addNode(new Label("Click a unit on the battlefield to see its stats."));

// ----- Secondary stats container (for a 2nd selected unit), hidden by default -----
        secondaryStatsContainer = new ScrollableContainer(
                0.75, 0.24, 0.02, 0.96, false, false);
        secondaryStatsContainer.setContainerPadding(new Insets(10));
        secondaryStatsContainer.setSpacing(6);
        secondaryStatsContainer.setBackgroundColor(Color.rgb(255, 245, 235, 0.95), true);
        secondaryStatsContainer.setBorderStyle(
                "-fx-border-color: #663333; -fx-border-width: 2; -fx-border-radius: 4;");
        secondaryStatsContainer.setFitToHeight(true);
        secondaryStatsContainer.setStyle(
                "-fx-background: rgba(255,245,235,0.950000);" +
                        "-fx-background-color: rgba(255,245,235,0.950000);" +
                        "-fx-background-insets: 0;");
        secondaryStatsContainer.setVisible(false);

// ----- Move container — warm yellow -----
        moveScrollContainer = new ScrollableContainer(
                0.21, 0.28, 0.02, 0.96, false, false);
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
                0.21, 0.28, 0.02, 0.96, false, false);
        attackScrollContainer.setContainerPadding(new Insets(10));
        attackScrollContainer.setSpacing(8);
        attackScrollContainer.setBackgroundColor(Color.rgb(255, 220, 220, 0.95), true);
        attackScrollContainer.setBorderStyle(
                "-fx-border-color: #8b0000; -fx-border-width: 2; -fx-border-radius: 4;");
        attackScrollContainer.setFitToHeight(true);
        attackScrollContainer.setStyle(
                "-fx-background: rgba(255,220,220,0.950000);" +
                        "-fx-background-color: rgba(255,220,220,0.950000);" +
                        "-fx-background-insets: 0;");

// ----- ATPower container — cool blue -----
        atPowerScrollContainer = new ScrollableContainer(
                0.21, 0.28, 0.02, 0.96, false, false);
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
                0.21, 0.28, 0.02, 0.96, false, false);
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
                atPowerScrollContainer, otherScrollContainer,
                currentStatsContainer, secondaryStatsContainer);


        buttonScrollContainer.bindToRegion(bottomPanel);
        moveScrollContainer.bindToRegion(bottomPanel);
        attackScrollContainer.bindToRegion(bottomPanel);
        atPowerScrollContainer.bindToRegion(bottomPanel);
        otherScrollContainer.bindToRegion(bottomPanel);
        layoutStatsContainers(false);
        // ----- Populate the buttons container -----
        BetterButton moveBtn = new BetterButton("Move");
        moveBtn.setPrimaryStyle();
        moveBtn.setMaxWidth(Double.MAX_VALUE);

        attackBtn = new BetterButton("Attack");
        attackBtn.setPrimaryStyle();
        attackBtnDefaultStyle = attackBtn.getStyle();
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
                        "-fx-text-fill: #8a6800;" +
                        "-fx-padding: 0 0 4 0;"
        );
        moveScrollContainer.addNode(moveLabel);

        BetterButton runBtn = new BetterButton("Run");
        runBtn.setPrimaryStyle();
        runBtn.setMaxWidth(Double.MAX_VALUE);
        runBtn.setOnAction(e -> startMoveVisualization(MoveAction.MOVEMENTTYPE.RUN));
        moveScrollContainer.addNode(runBtn);

        BetterButton tacticalBtn = new BetterButton("Tactical");
        tacticalBtn.setPrimaryStyle();
        tacticalBtn.setMaxWidth(Double.MAX_VALUE);
        tacticalBtn.setOnAction(e -> startMoveVisualization(MoveAction.MOVEMENTTYPE.TACTICAL));
        moveScrollContainer.addNode(tacticalBtn);

        BetterButton maneuverBtn = new BetterButton("Maneuver");
        maneuverBtn.setPrimaryStyle();
        maneuverBtn.setMaxWidth(Double.MAX_VALUE);
        maneuverBtn.setOnAction(e -> startMoveVisualization(MoveAction.MOVEMENTTYPE.MANEUVER));
        moveScrollContainer.addNode(maneuverBtn);

        BetterButton coverBtn = new BetterButton("Cover");
        coverBtn.setPrimaryStyle();
        coverBtn.setMaxWidth(Double.MAX_VALUE);
        coverBtn.setOnAction(e -> startMoveVisualization(MoveAction.MOVEMENTTYPE.COVER));
        moveScrollContainer.addNode(coverBtn);

        BetterButton repositionBtn = new BetterButton("Reposition");
        repositionBtn.setPrimaryStyle();
        repositionBtn.setMaxWidth(Double.MAX_VALUE);
        repositionBtn.setOnAction(e -> startMoveVisualization(MoveAction.MOVEMENTTYPE.REPOSITION));
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
        startMoveVisualization(MoveAction.MOVEMENTTYPE.RUN);
        attackScrollContainer.setVisible(false);
        atPowerScrollContainer.setVisible(false);
        otherScrollContainer.setVisible(false);

        // ----- Toggle buttons switch the visible content container -----
        // ----- Toggle buttons switch the visible content container -----
        moveBtn.setOnAction(e -> {
            clearAttackVisualization();
            moveScrollContainer.setVisible(true);
            attackScrollContainer.setVisible(false);
            atPowerScrollContainer.setVisible(false);
            otherScrollContainer.setVisible(false);
            attackWeaponChooserContainer.setVisible(false);
            startMoveVisualization(MoveAction.MOVEMENTTYPE.RUN);
        });
        attackBtn.setOnAction(e -> {
            // Already open: do nothing
            if (attackScrollContainer.isVisible() && attackWeaponChooserContainer.isVisible()) return;

            clearMovementVisualization();
            moveScrollContainer.setVisible(false);
            attackScrollContainer.setVisible(true);
            atPowerScrollContainer.setVisible(false);
            otherScrollContainer.setVisible(false);
            attackWeaponChooserContainer.setVisible(true);

            // Fresh start: nothing is selected, the player has to pick a weapon / Neutral
            clearAttackVisualization();
            selectedAttackWeapon = null;
            selectedAttackWeaponSlot = null;
            attackUnarmedChosen = false;
            rebuildWeaponChooser();
            rebuildAttackMenu();
        });

        atPowerBtn.setOnAction(e -> {
            clearAttackVisualization();
            moveScrollContainer.setVisible(false);
            attackScrollContainer.setVisible(false);
            atPowerScrollContainer.setVisible(true);
            otherScrollContainer.setVisible(false);
            attackWeaponChooserContainer.setVisible(false);
        });
        otherBtn.setOnAction(e -> {
            clearAttackVisualization();
            moveScrollContainer.setVisible(false);
            attackScrollContainer.setVisible(false);
            atPowerScrollContainer.setVisible(false);
            otherScrollContainer.setVisible(true);
            attackWeaponChooserContainer.setVisible(false);
        });

        // ----- Main layout -----
        battlePane = new BorderPane();
        battlePane.setCenter(scrollContainer);
        battlePane.setBottom(bottomPanel);
        VBox.setVgrow(battlePane, Priority.ALWAYS);

        // ============================================================
        //  Attack weapon chooser — added directly to battlePane.
        //  Hidden until Attack mode is toggled on.
        // ============================================================
        attackWeaponChooserContainer = new ScrollableContainer(
                0.01, 0.2, 0.02, 0.3, false, false);
        attackWeaponChooserContainer.setContainerPadding(new Insets(10));
        attackWeaponChooserContainer.setSpacing(6);

        attackWeaponChooserContainer.setBackgroundColor(Color.rgb(255, 235, 235, 0.95), true);
        attackWeaponChooserContainer.setBorderStyle(
                "-fx-border-color: #8b0000; -fx-border-width: 2; -fx-border-radius: 4;");
        attackWeaponChooserContainer.setFitToHeight(true);
        attackWeaponChooserContainer.setStyle(
                "-fx-background: rgba(255,235,235,0.950000);" +
                        "-fx-background-color: rgba(255,235,235,0.950000);" +
                        "-fx-background-insets: 0;");
        attackWeaponChooserContainer.setVisible(false);
        attackWeaponChooserContainer.bindToRegion(battlePane);

// 1. Actually put it in the scene graph.
        battlePane.getChildren().add(attackWeaponChooserContainer);

// 2. BorderPane ignores children that aren't in a named slot, so
//    nothing will ever autosize the chooser for us. Drive its size
//    ourselves whenever battlePane changes size.
        Runnable syncChooserSize = attackWeaponChooserContainer::autosize;
        battlePane.widthProperty() .addListener((o, a, b) -> syncChooserSize.run());
        battlePane.heightProperty().addListener((o, a, b) -> syncChooserSize.run());

// First time it becomes sized (before the initial layout pass, width = 0)
        Platform.runLater(syncChooserSize);

        wrapper.getChildren().add(battlePane);
        return wrapper;

    }

    /** Grey while it's your turn and the unit already used its Attack Action. */
    private void updateAttackButtonState() {
        if (attackBtn == null) return;
        FieldUnit u = findUnitSilent(currentPlayer);
        boolean blocked = alreadyAttackedThisTurn(u);

        if (blocked) {
            attackBtn.setStyle("-fx-background-color: #9e9e9e; -fx-text-fill: #eeeeee; -fx-background-radius: 4;");
            attackBtn.setTooltip(new Tooltip("You already attacked this turn. You can still preview attacks."));
        } else {
            attackBtn.setStyle(attackBtnDefaultStyle);
            attackBtn.setTooltip(null);
        }
    }

    /** Profiles are rebuilt constantly, so identity (==) is useless. Name + type is stable within one weapon. */
    private boolean isSameProfile(AttackProfile a, AttackProfile b) {
        return a != null && b != null && a.ProfileType == b.ProfileType && Objects.equals(a.name, b.name);
    }

    private boolean hasAttackSourceChosen() {
        return selectedAttackWeapon != null || attackUnarmedChosen;
    }

    /** The ONE place that builds the profile list (with Attack Strength applied). Empty if nothing is chosen. */
    private List<AttackProfile> getCurrentAttackProfiles(FieldUnit unit) {
        List<AttackProfile> profiles = new ArrayList<>();
        if (selectedAttackWeapon != null) {
            List<AttackProfile> w = selectedAttackWeapon.getWeaponProfiles(selectedAttackWeapon.getCurrentTech());
            if (w != null) profiles.addAll(w);
        } else if (attackUnarmedChosen) {
            List<AttackProfile> u = unit.getUnitAttackProfiles();
            if (u != null) profiles.addAll(u);
        }
        for (AttackProfile p : profiles) turnIntoCombatProfile(p, unit);
        return profiles;
    }

    /** null = usable, otherwise the reason it can't be used right now. */
    private String unusableReason(FieldUnit unit, AttackProfile p) {
        if (unit.getStamina() < p.Stamina)
            return "Not enough Stamina (need " + p.Stamina + ", have " + unit.getStamina() + ")";
        if (unit.getATP() < p.ATP)
            return "Not enough ATP (need " + p.ATP + ", have " + unit.getATP() + ")";
        if (selectedAttackWeapon != null && p.AmmoCost > 0 && selectedAttackWeapon.getAmmo() < p.AmmoCost)
            return "Not enough Ammo (need " + p.AmmoCost + ", have " + selectedAttackWeapon.getAmmo() + ")";
        return null;
    }

    /** A pending attack confirm belongs to the visualization, so it goes away with it. */
    private void discardPendingAttackConfirm() {
        if (confirmAction instanceof AttackAction) {
            confirmAction = null;
            if (confirmContainer != null) confirmContainer.getChildren().clear();
        }
    }

    /** Weapon (or Neutral) was changed: drop the old visualization, rebuild, show the first usable profile. */
    private void switchAttackSource() {
        clearAttackVisualization();
        rebuildWeaponChooser();
        rebuildAttackMenu();
        autoSelectFirstProfile();
    }

    /** Same profile after its stats changed (tech toggled): clear and show the new version of it. */
    /**
     * The active profile's stats changed (N2 Shell / Maser toggled). Re-creates the pending attack
     * at the same clicked sectors with the new stats. Clicks that are no longer valid
     * (e.g. not on a straight line once the profile became a Line) are removed.
     * Returns false (and changes nothing) if the new version can't be used.
     */
    private boolean refreshActiveProfile() {
        FieldUnit unit = findUnitSilent(currentPlayer);
        if (unit == null || !attackModeActive || activeAttackProfile == null || attackUnit != unit) {
            rebuildAttackMenu();        // nothing shown yet, just refresh descriptions
            return true;
        }

        AttackProfile old = activeAttackProfile;
        List<AttackProfile> profiles = getCurrentAttackProfiles(unit);

        // Find the new version of the same profile (by name, otherwise by type if that's unambiguous)
        AttackProfile fresh = null;
        for (AttackProfile p : profiles) {
            if (isSameProfile(p, old)) { fresh = p; break; }
        }
        if (fresh == null) {
            int matches = 0;
            for (AttackProfile p : profiles) {
                if (p.ProfileType == old.ProfileType) { fresh = p; matches++; }
            }
            if (matches != 1) fresh = null;
        }
        if (fresh == null) {
            clearAttackVisualization();
            rebuildAttackMenu();
            LocalMessage("The profile changed and can't be matched any more. Choose a profile again.");
            return true;
        }

        String reason = unusableReason(unit, fresh);
        if (reason != null) {
            LocalMessage(reason + ".");
            return false;               // caller reverts the toggle
        }

        // Remember where the player clicked (not where the hits were resolved to)
        List<int[]> clicks = new ArrayList<>();
        for (PendingHit h : pendingAttackHits) clicks.add(new int[]{h.clickX(), h.clickY()});

        clearAttackVisualization();
        startAttackVisualization(fresh);        // new range zone (straight lines only for a Line)

        int removed = 0;
        for (int[] c : clicks) {
            if (!isInPreviewVisualisationLayer(c[0], c[1])) { removed++; continue; }
            handleAttackSectorClick(c[0], c[1]);   // rebuilds arrows, highlights and the Confirm button
        }
        if (removed > 0) {
            LocalMessage(removed + " hit location(s) can't be used with the changed attack and were removed.");
        }
        return true;
    }

    /**
     * If no weapon is currently selected, pick the first usable one from the
     * current player's slots. Does nothing if the player already made a choice.
     */
    private void autoSelectFirstWeapon() {
        if (selectedAttackWeapon != null) return;
        FieldUnit unit = findUnitSilent(currentPlayer);
        if (unit == null || !unit.isExists() || unit.getSlots() == null) return;
        for (Slot s : unit.getSlots()) {
            if (s.isActive() && s.isIntact() && s.getItem() instanceof Weapon w) {
                selectedAttackWeapon = w;
                selectedAttackWeaponSlot = s;
                return;
            }
        }
    }

    // ============================================================
//   CURRENT-STATS CONTAINERS
// ============================================================

    /**
     * Case-insensitive-safe lookup that does NOT log on failure. Used by the
     * stats containers so that an unassigned current player (e.g. "DM") doesn't
     * spam the log on every refresh.
     */
    private FieldUnit findUnitSilent(String name) {
        if (name == null) return null;
        for (FieldUnit u : UnitList) {
            if (u.getName().equals(name)) return u;
        }
        return null;
    }

    /**
     * (Re)binds the stats containers to the bottom panel.
     * When {@code split} is false, {@link #currentStatsContainer} uses the whole
     * remaining area. When {@code split} is true, it shrinks to half and
     * {@link #secondaryStatsContainer} occupies the other half.
     */
    private void layoutStatsContainers(boolean split) {
        // Clear previous bindings
        currentStatsContainer.layoutXProperty().unbind();
        currentStatsContainer.layoutYProperty().unbind();
        currentStatsContainer.prefWidthProperty().unbind();
        currentStatsContainer.prefHeightProperty().unbind();
        secondaryStatsContainer.layoutXProperty().unbind();
        secondaryStatsContainer.layoutYProperty().unbind();
        secondaryStatsContainer.prefWidthProperty().unbind();
        secondaryStatsContainer.prefHeightProperty().unbind();

        // Vertical placement is identical in both layouts
        currentStatsContainer.layoutYProperty()
                .bind(bottomPanel.heightProperty().multiply(0.02));
        currentStatsContainer.prefHeightProperty()
                .bind(bottomPanel.heightProperty().multiply(0.96));
        secondaryStatsContainer.layoutYProperty()
                .bind(bottomPanel.heightProperty().multiply(0.02));
        secondaryStatsContainer.prefHeightProperty()
                .bind(bottomPanel.heightProperty().multiply(0.96));

        if (split) {
            currentStatsContainer.layoutXProperty()
                    .bind(bottomPanel.widthProperty().multiply(0.50));
            currentStatsContainer.prefWidthProperty()
                    .bind(bottomPanel.widthProperty().multiply(0.24));
            secondaryStatsContainer.layoutXProperty()
                    .bind(bottomPanel.widthProperty().multiply(0.75));
            secondaryStatsContainer.prefWidthProperty()
                    .bind(bottomPanel.widthProperty().multiply(0.24));
        } else {
            currentStatsContainer.layoutXProperty()
                    .bind(bottomPanel.widthProperty().multiply(0.50));
            currentStatsContainer.prefWidthProperty()
                    .bind(bottomPanel.widthProperty().multiply(0.49));
        }
    }

    /**
     * Called when a unit is clicked on the battlefield. Puts that unit into the
     * secondary (right-hand) stats container.
     *
     * The primary container is always reserved for the current player's unit and
     * is unaffected by this call.
     */
    public void showStatsForUnit(FieldUnit unit) {
        if (unit == null || !unit.isExists()) return;
        statsUnit2 = unit;
        refreshStatsContainers();
    }

    /**
     * Hides the secondary stats container. Called when the user clicks an empty
     * sector, or when the previously inspected unit no longer exists.
     */
    public void clearSecondaryStats() {
        if (statsUnit2 == null) return;
        statsUnit2 = null;
        refreshStatsContainers();
    }

    /**
     * Rebuilds both stats containers.
     *
     *   • Primary   — always the unit whose name equals {@link #currentPlayer}.
     *                 If there is no such unit (e.g. current player is "DM"
     *                 or a player who hasn't spawned yet), a placeholder is shown.
     *   • Secondary — the most recently inspected unit, or hidden if none.
     */
    public void refreshStatsContainers() {
        if (currentStatsContainer == null || secondaryStatsContainer == null) return;

        currentStatsContainer.clearNodes();
        secondaryStatsContainer.clearNodes();

        // ---------- Primary: current player ----------
        FieldUnit primary = null;
        if (currentPlayer != null && !currentPlayer.isEmpty()
                && !currentPlayer.equalsIgnoreCase("DM")) {
            primary = findUnitSilent(currentPlayer);
        }

        if (primary == null || !primary.isExists()) {
            Label placeholder = new Label(
                    "No unit to show for current player '"
                            + (currentPlayer == null ? "" : currentPlayer) + "'.");
            placeholder.setWrapText(true);
            placeholder.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            currentStatsContainer.addNode(placeholder);
        } else {
            currentStatsContainer.addNode(getStatsForFieldUnit(primary, statsMode));
        }

        // ---------- Secondary: inspected unit ----------
        if (statsUnit2 == null || !statsUnit2.isExists()) {
            statsUnit2 = null;
            secondaryStatsContainer.setVisible(false);
            layoutStatsContainers(false);
        } else {
            secondaryStatsContainer.addNode(getStatsForFieldUnit(statsUnit2, statsMode));
            secondaryStatsContainer.setVisible(true);
            layoutStatsContainers(true);
        }
    }

    /**
     * Builds the stats panel for a single {@link FieldUnit}.
     *
     * @param unit the unit whose stats are shown (may be null → returns placeholder)
     * @param mode 0 = BASIC   (stamina / ATP / tactical / armor / toughness)
     *             1 = COMBAT  (BASIC + accuracy / attack strength / reflexes / speed)
     *             2 = FULL    (COMBAT + team / position / effects / turn-done)
     *             Negative values are treated as 0; values &gt; 2 are treated as 2.
     * @return a JavaFX {@link Node} ready to be added to a container.
     */
    public Node getStatsForFieldUnit(FieldUnit unit, int mode) {
        VBox box = new VBox(4);
        box.setFillWidth(true);

        if (unit == null || !unit.isExists()) {
            Label placeholder = new Label("No unit selected.");
            placeholder.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            box.getChildren().add(placeholder);
            return box;
        }

        // ---------------- Header ----------------
        Label nameLabel = new Label(unit.getName());
        nameLabel.setStyle(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a1a4d;");
        box.getChildren().add(nameLabel);

        if (unit.getUnit() != null) {
            Label typeLabel = new Label(unit.getUnit().getClass().getSimpleName()
                    + "   (Team " + unit.getTeam() + ")");
            typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
            box.getChildren().add(typeLabel);
        }

        box.getChildren().add(new Separator());

        // ---------------- BASIC ----------------
        Label basicHeader = new Label("Core");
        basicHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a4d;");
        box.getChildren().add(basicHeader);

        Label stamina = new Label(
                "Stamina:   " + unit.getStamina() + " / " + unit.getMaxStamina());
        Label atp = new Label(
                "ATP:       " + unit.getATP() + " / " + unit.getMaxATP());
        box.getChildren().addAll(stamina, atp);

        Label tactical = new Label("Tactical:  "
                + (unit.hasTactical() ? "Available" : "Used"));
        tactical.setStyle(unit.hasTactical()
                ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                : "-fx-text-fill: #b00020; -fx-font-weight: bold;");
        box.getChildren().add(tactical);

        Label armor = new Label("Armor:     " + unit.getArmor());
        Label toughness = new Label(
                "Toughness: " + unit.getToughness() + " / " + unit.getMaxToughness());
        box.getChildren().addAll(armor, toughness);

        if (mode < 1) return box;

        // ---------------- COMBAT ----------------
        box.getChildren().add(new Separator());
        Label combatHeader = new Label("Combat");
        combatHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a4d;");
        box.getChildren().add(combatHeader);

        box.getChildren().addAll(
                new Label("Accuracy:        " + unit.getAccuracy()),
                new Label("Attack Strength: " + unit.getAttackStrength()),
                new Label("Reflexes:        " + unit.getReflexes()),
                new Label("Speed:           " + unit.getSpeed())
        );

        if (mode < 2) return box;

        // ---------------- FULL ----------------
        box.getChildren().add(new Separator());
        Label fullHeader = new Label("Details");
        fullHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a4d;");
        box.getChildren().add(fullHeader);

        box.getChildren().addAll(
                new Label("Team:       " + unit.getTeam()),
                new Label("Position:   (" + unit.getX() + ", " + unit.getY() + ")"),
                new Label("Effects:    " + unit.getCurrentEffects().size()),
                new Label("Turn done:  " + (unit.isTurnDone() ? "yes" : "no")),
                new Label("Guarded:    " + (unit.usedGuard() ? "yes" : "no"))
        );

        return box;
    }

    /** Optional: change the stats depth at runtime. */
    public void setStatsMode(int mode) {
        this.statsMode = Math.max(0, Math.min(2, mode));
        refreshStatsContainers();
    }


    // ---- Bottom Button Bar (unchanged) ----
    // ---- Bottom Button Bar ----
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

        // ----- NEW: Toggle the battlefield bottom panel -----
        BetterButton toggleBottomBtn = new BetterButton("Hide Panel");
        toggleBottomBtn.setPrimaryStyle();
        toggleBottomBtn.setOnAction(e -> {
            if (bottomPanel == null) return;
            boolean nowVisible = !bottomPanel.isVisible();
            bottomPanel.setVisible(nowVisible);
            bottomPanel.setManaged(nowVisible);   // let the BorderPane reclaim the space
            toggleBottomBtn.setText(nowVisible ? "Hide Panel" : "Show Panel");
        });

        BetterButton optionsBtn = new BetterButton("Options");
        optionsBtn.setPrimaryStyle();
        optionsBtn.setOnAction(e -> showOptionsDialog());

        BetterButton exitBtn = new BetterButton("Exit");
        exitBtn.setDangerStyle();
        exitBtn.setOnAction(e -> stage.close());

        bar.getChildren().addAll(bfBtn, invBtn, chatBtn, weaponCreatorBtn,
                dmBtn, toggleBottomBtn, optionsBtn, exitBtn);
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
                options.viewportWidth = viewport.getPrefWidth();     // NEW (after clamping)
                options.viewportHeight = viewport.getPrefHeight();
                saveOptions();                                       // NEW
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




        Label arrowLabel = new Label("Action arrows disappear after (turns, 0 = never):");
        Spinner<Integer> arrowSpinner = new Spinner<>(0, GameOptions.MAX_ARROW_LIFETIME, options.arrowLifetimeTurns);
        arrowSpinner.setEditable(true);

        content.getChildren().addAll(
                speedLabel, speedSlider, fastCheck,
                new Separator(),
                viewportLabel,
                resizeGrid, resizeBtnBox,
                new Separator(),
                heightLabel, heightSlider,
                new Separator(),
                arrowLabel, arrowSpinner          // NEW
        );


        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                this.actionSpeed = speedSlider.getValue();
                this.fastActions = fastCheck.isSelected();
                try { arrowSpinner.increment(0); } catch (Exception ignored) { }   // commits typed text
                options.arrowLifetimeTurns = arrowSpinner.getValue();
                pruneActionArrows();
                LogMessage("Speed set to " + actionSpeed + "x, FastActions: " + fastActions);
            }
            return null;
        });

        dialog.showAndWait();
        saveOptions();

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

        board.SetBoardColorsToSectoryTypes();
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
        if (viewport == null) return;
        newWidth = Math.max(newWidth, 100);
        newHeight = Math.max(newHeight, 100);
        viewport.setPrefSize(newWidth, newHeight);
        viewport.setMaxSize(newWidth, newHeight);
        updateSliderRanges();
    }
    private void saveOptions() {
        options.bottomPanelHeight = bottomPanelHeight;
        options.save();
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
        registerArrow(arrow);
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
    //   GENERIC SECTOR VISUALISATION LAYER
    // ============================================================
    //
    // The layer is independent of what it's being used for: movement
    // previews, attack ranges, effect areas, etc. Add entries with
    // addToVisualisationLayer(), then call applyVisualisation().
    //
    // applyVisualisation() always resets the board to its base sector
    // colors first, then blends each tinted sector with its base color,
    // so a highlighted Grass sector will still look different from a
    // highlighted Concrete sector.

    /** Blends this much of the tint into the base color when drawing. */
    private static final double VIS_TINT_RATIO = 0.60;

    /** Border applied to tinted sectors so they pop. */
    private static final Color VIS_BORDER = Color.rgb(255, 200, 0);

    private static String sectorKey(int x, int y) { return x + "," + y; }

    /** Adds (or replaces) a tint on sector (x, y). DOES NOT REPAINT */
    private void addToPreviewVisualisationLayer(int x, int y, Color tint) {
        previewVisualisationLayer.put(sectorKey(x, y), tint);
    }
    private void addToPreviewVisualisationLayerRepaint(int x, int y, Color tint) {
        previewVisualisationLayer.put(sectorKey(x, y), tint);
        applyVisualisation();
    }
    /** Removes the preview tint from sector (x, y). Does NOT repaint. */
    private void removeFromPreviewVisualisationLayer(int x, int y) {
        previewVisualisationLayer.remove(sectorKey(x, y));
    }

    /** Clears the entire preview layer*/
    private void clearPreviewVisualisationLayer() {
        previewVisualisationLayer.clear();
        applyVisualisation();
    }

    /** True if sector (x, y) currently has a tint applied. */
    private boolean isInPreviewVisualisationLayer(int x, int y) {
        return previewVisualisationLayer.containsKey(sectorKey(x, y));
    }


    /** Border used for sectors that belong to the attack layer (drawn over the preview border). */
    private static final Color VIS_ATTACK_BORDER = Color.rgb(200, 0, 0);

    /** Adds (or replaces) a tint on sector (x, y) in the attack layer. DOES NOT REPAINT */
    private void addToAttackVisualisationLayer(int x, int y, Color tint) {
        attackVisualisationLayer.put(sectorKey(x, y), tint);
    }

    /** Adds (or replaces) a tint in the attack layer and repaints the board. */
    private void addToAttackVisualisationLayerRepaint(int x, int y, Color tint) {
        attackVisualisationLayer.put(sectorKey(x, y), tint);
        applyVisualisation();
    }

    /** Removes the attack tint from sector (x, y). Does NOT repaint. */
    private void removeFromAttackVisualisationLayer(int x, int y) {
        attackVisualisationLayer.remove(sectorKey(x, y));
    }

    /** Clears the entire attack layer and repaints. The preview layer is untouched. */
    private void clearAttackVisualisationLayer() {
        attackVisualisationLayer.clear();
        applyVisualisation();
    }

    /** True if sector (x, y) currently has a tint in the attack layer. */
    private boolean isInAttackVisualisationLayer(int x, int y) {
        return attackVisualisationLayer.containsKey(sectorKey(x, y));
    }

    /**
     * Repaints the entire board:
     *   1) Every sector is reset to its base SectorType color.
     *   2) Every sector that has a tint in the layer is repainted with
     *      a blend of its base color and the tint, plus a highlight border.
     *
     * Safe to call repeatedly. Call this after any change to the layer
     * or after any event that may have re-painted sectors (e.g. moves,
     * creations, deletions).
     */
    private void applyVisualisation() {
        if (gameBoard == null) return;

        // 1. Reset every sector to its base color.
        gameBoard.SetBoardColorsToSectoryTypes();

        // 2. Every sector touched by either layer
        Set<String> keys = new LinkedHashSet<>(previewVisualisationLayer.keySet());
        keys.addAll(attackVisualisationLayer.keySet());

        for (String key : keys) {
            String[] parts = key.split(",");
            int x, y;
            try {
                x = Integer.parseInt(parts[0]);
                y = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ex) {
                continue;
            }
            Sector s = gameBoard.getSector(x, y);
            if (s == null || s.getType() == null) continue;

            Color color = s.getType().getColor();

            // Preview layer first (bottom)...
            Color previewTint = previewVisualisationLayer.get(key);
            if (previewTint != null) color = blendColors(color, previewTint, VIS_TINT_RATIO);

            // ...attack layer on top
            Color attackTint = attackVisualisationLayer.get(key);
            if (attackTint != null) color = blendColors(color, attackTint, VIS_TINT_RATIO);

            s.setBackground(new Background(new BackgroundFill(
                    color, CornerRadii.EMPTY, Insets.EMPTY)));
            s.setBorder(new Border(new BorderStroke(
                    attackTint != null ? VIS_ATTACK_BORDER : VIS_BORDER,
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY, new BorderWidths(1.2))));
        }
    }

    /** ratio = 0 → base, ratio = 1 → tint. Returns an opaque color. */
    private static Color blendColors(Color base, Color tint, double ratio) {
        double r = base.getRed()   * (1 - ratio) + tint.getRed()   * ratio;
        double g = base.getGreen() * (1 - ratio) + tint.getGreen() * ratio;
        double b = base.getBlue()  * (1 - ratio) + tint.getBlue()  * ratio;
        return new Color(r, g, b, 1.0);
    }

    /** Tint color used for a given movement subtype. */
    private static Color movementTint(MoveAction.MOVEMENTTYPE mode) {
        switch (mode) {
            case RUN:        return Color.rgb(255, 235, 100);   // yellow
            case COVER:      return Color.rgb(120, 220, 120);   // green
            case TACTICAL:   return Color.rgb(120, 170, 255);   // blue
            case MANEUVER:   return Color.rgb(255, 170, 100);   // orange
            case REPOSITION: return Color.rgb(200, 120, 255);   // purple
        }
        return Color.rgb(180, 180, 180);
    }



    // ============================================================
    //   LAUNCH METHODS
    // ============================================================


    // Existing signature stays, so MainMenu keeps working unchanged.
    public static void startGame(Battlefield field, double speed, boolean fast, int playernumber,
                                 String playerName, boolean startNew, GameState.GAME_MODE gameMode) throws IOException {
        startGame(field, speed, fast, playernumber, playerName, startNew, gameMode,
                GameStateStore.activeDir(), false, false);
    }



    public static void startGame(Battlefield field, double speed, boolean fast, int playernumber,
                                 String playerName, boolean startNew, GameState.GAME_MODE gameMode,
                                 Path stateDir, boolean sandbox, boolean initialloading) throws IOException {
        System.out.println("Starting game with speed " + speed + " fast " + fast
                + " playernumber " + playernumber + " gamemode " + gameMode);
        if (startNew) {
            new Game(field, "DM", speed, fast, playernumber, true, gameMode, stateDir, sandbox, initialloading);
        } else {
            System.out.println("Connecting as " + playerName);
            new Game(field, playerName, speed, fast, -1, false, gameMode, stateDir, sandbox, initialloading);
        }
    }


    private Stage newChildStage() {
        Stage s = new Stage();
        s.initOwner(stage);   // must be called before show()
        childStages.add(s);
        // A listener doesn't overwrite any setOnHidden you set later.
        s.showingProperty().addListener((obs, was, is) -> {
            if (!is) childStages.remove(s);
        });
        return s;
    }


    private void shutdown() {
        if (closed) return;
        closed = true;

        // Stop the action-processing animation
        if (processingTimeline != null) {
            processingTimeline.stop();
            processingTimeline = null;
        }
        pendingActions.clear();
        isProcessing = false;
        popupShowing = false;


        // Close every popup that is still open
        for (Stage s : new ArrayList<>(childStages)) {
            s.close();
        }
        childStages.clear();
    }

    /** True if two or more actions in the state share the same action number. */
    private boolean hasDuplicateActionNumbers(GameState state) {
        Set<Integer> seen = new HashSet<>();
        for (Action a : state.getActions()) {
            if (!seen.add(a.getActionNumber())) return true;
        }
        return false;
    }

    /**
     * Attempts to repair a state whose actions share action numbers.
     * Builds a NEW GameState with copies of all actions in their original order,
     * numbered 1-N by their position in the list (the same scheme initializeUnits uses).
     * The original file is backed up first.
     */

    /** Short readable description of an action for logs. Never throws. */
    private String describeAction(Action a) {
        StringBuilder sb = new StringBuilder(a.getClass().getSimpleName())
                .append(" actor=").append(a.getActor());
        try {
            // Only use toString() if the action class actually overrides it
            if (a.getClass().getMethod("toString").getDeclaringClass() != Object.class) {
                sb.append(" | ").append(a);
            }
        } catch (Exception e) {
            sb.append(" | (details unavailable: ").append(e.getClass().getSimpleName()).append(")");
        }
        return sb.toString();
    }

    private GameState CorruptedStateAttemptedFix(GameState corrupted) {
        LogError("ERROR - CORRUPTED GameState: duplicate action numbers found. Attempting to fix by renumbering. Backup saved");


        // Group actions by number, remembering their position in the list
        Map<Integer, List<Integer>> indicesByNumber = new TreeMap<>();
        List<Action> allActions = corrupted.getActions();
        for (int i = 0; i < allActions.size(); i++) {
            indicesByNumber.computeIfAbsent(allActions.get(i).getActionNumber(), k -> new ArrayList<>()).add(i);
        }

        indicesByNumber.forEach((num, indices) -> {
            if (indices.size() > 1) {
                LogError("ERROR - action number " + num + " is used by " + indices.size() + " actions:");
                for (int idx : indices) {
                    LogError("    list position " + idx + ": " + describeAction(allActions.get(idx)));
                }
            }
        });

        // Keep the broken file so nothing is lost
        try {
            Path file = stateFile();
            if (Files.exists(file)) {
                Files.copy(file, stateDir.resolve(GAME_STATE_FILE + ".corrupted.bak"),
                        StandardCopyOption.REPLACE_EXISTING);
                LogMessage("Backed up corrupted GameState to " + GAME_STATE_FILE + ".corrupted.bak");
            }
        } catch (IOException e) {
            LogError("Could not back up corrupted GameState: " + e.getMessage());
        }

        GameState fixed = new GameState();
        fixed.setGameMode(corrupted.getGameMode());
        fixed.setInitialState(corrupted.getInitialState());
        fixed.setSessionId(corrupted.getSessionId());

        List<Action> actions = corrupted.getActions();
        for (int i = 0; i < actions.size(); i++) {
            Action copy = copyAction(actions.get(i));
            int oldNumber = copy.getActionNumber();
            setActionNumber(copy, i + 1);        // was: copy.setActionNumber(i + 1);
            fixed.addAction(copy);
            if (oldNumber != i + 1) {
                LogMessage("Renumbered " + copy.getClass().getSimpleName()
                        + " from " + oldNumber + " to " + (i + 1));
            }
        }

        LogMessage("CorruptedStateAttemptedFix finished: " + actions.size() + " actions renumbered.");
        return fixed;
    }
    /** Sets Action.ActionNumber (final) via reflection so Action's serialized form doesn't change. */
    private static void setActionNumber(Action action, int number) {
        try {
            java.lang.reflect.Field f = Action.class.getDeclaredField("ActionNumber");
            f.setAccessible(true);
            f.setInt(action, number);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not renumber action", e);
        }
    }
    /** Deep copy through serialization (actions are already Serializable). Falls back to the original. */
    private Action copyAction(Action original) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(original);
            oos.flush();
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
                return (Action) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            LogError("Could not copy action " + original.getActionNumber() + ", reusing the original: " + e.getMessage());
            return original;
        }
    }







    private boolean isSessionReplaced(GameState onDisk) {
        return !dmSandbox && sessionKnown && onDisk != null
                && !Objects.equals(knownSessionId, onDisk.getSessionId());
    }

    /** Called on players (and the DM's other windows) when the DM swapped the active game. */
    private void onSessionReplacedByDM() {
        if (restarting || closed) return;
        restarting = true;
        Platform.runLater(() -> {
            shutdown();   // stop processing, watcher and popups first
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "The DM has loaded a different game.\nYour session will restart with the new game.",
                    ButtonType.OK);
            a.setHeaderText("Game replaced");
            a.showAndWait();
            relaunch(GameStateStore.activeDir(), false);
        });
    }

    /** Closes this window and starts a new Game on the given folder. */
    private void relaunch(Path dir, boolean sandbox) {
        Platform.runLater(() -> {
            shutdown();
            stage.close();
            try {
                Game.startGame(new Battlefield(50, 50), actionSpeed, fastActions,
                        1, currentPlayer, false, GameState.GAME_MODE.CLASSIC, dir, sandbox, true);
            } catch (IOException e) {
                LogError("Could not restart the game: " + e.getMessage());
            }
        });
    }




    private String cmdSaveGame(String[] parts) {
        if (parts.length < 2) return "Usage: savegame <name> [overwrite]\n";
        boolean overwrite = parts.length > 2 && parts[2].equalsIgnoreCase("overwrite");
        try {
            GameState snap = currentStateSnapshot();
            GameStateStore.saveAs(snap, parts[1], overwrite);
            return "Saved " + snap.getActionCount() + " actions as '" + parts[1] + "'.\n";
        } catch (IllegalArgumentException e) {
            return e.getMessage() + "\n";
        } catch (FileAlreadyExistsException e) {
            return "A save named '" + parts[1] + "' already exists. Add 'overwrite' to replace it.\n";
        } catch (IOException e) {
            return "Save failed: " + e.getMessage() + "\n";
        }
    }

    private String cmdListSaves() {
        try {
            List<String> names = GameStateStore.listSaves();
            if (names.isEmpty()) return "  (no saves)\n";
            StringBuilder sb = new StringBuilder("Saved games:\n");
            for (String n : names) {
                sb.append("  ").append(n);
                try {
                    GameState s = GameStateStore.loadSave(n);
                    if (s != null) sb.append("   (").append(s.getActionCount()).append(" actions)");
                } catch (IOException e) {
                    sb.append("   (unreadable)");
                }
                sb.append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            return "Could not list saves: " + e.getMessage() + "\n";
        }
    }

    private String cmdOpenGame(String[] parts) {
        if (parts.length < 2) return "Usage: opengame <name>\n";
        try {
            GameState s = GameStateStore.loadSave(parts[1]);
            if (s == null) return "No save named '" + parts[1] + "'.\n";
            GameStateStore.write(s, GameStateStore.dmDir());   // overwrites the previous sandbox
            relaunch(GameStateStore.dmDir(), true);
            return "Opening '" + parts[1] + "' in DMgame...\n";
        } catch (IllegalArgumentException e) {
            return e.getMessage() + "\n";
        } catch (IOException e) {
            return "Open failed: " + e.getMessage() + "\n";
        }
    }

    private String cmdLiveGame() {
        if (!dmSandbox) return "You are already in the active game.\n";
        relaunch(GameStateStore.activeDir(), false);
        return "Returning to the active game...\n";
    }

    private String cmdReplaceGame(String[] parts) {
        if (parts.length < 2) return "Usage: replacegame <name> confirm\n";
        boolean confirm = parts.length > 2 && parts[2].equalsIgnoreCase("confirm");
        try {
            GameState s = GameStateStore.loadSave(parts[1]);
            if (s == null) return "No save named '" + parts[1] + "'.\n";
            if (!confirm) {
                return "This replaces the active game (" + s.getActionCount() + " actions in '" + parts[1]
                        + "') and forces ALL players to restart. The current active game is backed up first.\n"
                        + "Type: replacegame " + parts[1] + " confirm\n";
            }
            restarting = true;      // stops our own watcher from also reacting to the change
            try {
                String backup = GameStateStore.backupActive();
                GameStateStore.replaceActive(s);
                LogMessage("Active game replaced with '" + parts[1] + "'. Old one backed up as " + backup);
            } catch (IOException e) {
                restarting = false;
                return "Replace failed: " + e.getMessage() + "\n";
            }
            relaunch(GameStateStore.activeDir(), false);
            return "Active game replaced. Restarting...\n";
        } catch (IllegalArgumentException e) {
            return e.getMessage() + "\n";
        } catch (IOException e) {
            return "Replace failed: " + e.getMessage() + "\n";
        }
    }

    private String cmdRevert(String[] parts) {
        if (parts.length < 2) return "Usage: revert <actionNumber> confirm\n";
        boolean confirm = parts.length > 2 && parts[2].equalsIgnoreCase("confirm");
        int n;
        try {
            n = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return "Action number must be an integer.\n";
        }
        GameState source = currentStateSnapshot();
        GameState reverted;
        try {
            reverted = source.revertedTo(n);
        } catch (IllegalArgumentException e) {
            return e.getMessage() + "\n";
        }
        int removed = source.getActionCount() - reverted.getActionCount();
        if (!confirm) {
            return "Reverting to action " + n + " deletes " + removed + " action(s) and loads the result into the "
                    + "active game for ALL players. The current active game is backed up first.\n"
                    + "Type: revert " + n + " confirm\n";
        }
        restarting = true;
        try {
            GameStateStore.replaceActive(reverted);
        } catch (IOException e) {
            restarting = false;
            return "Revert failed: " + e.getMessage() + "\n";
        }
        relaunch(GameStateStore.activeDir(), false);
        return "Reverted to action " + n + " (" + removed + " removed). Restarting...\n";
    }


}