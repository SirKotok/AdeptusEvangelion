package eva.evangelion.view;

import eva.evangelion.gameboard.Battlefield;
import eva.evangelion.gameboard.GameBoard;
import eva.evangelion.gameboard.Sector;
import eva.evangelion.gameboard.SectorType;
import eva.evangelion.view.UIElements.BetterButton;
import javafx.event.EventTarget;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import kotlin.Triple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BattlefieldCreator {

    private Battlefield Battlefield;
    private GameBoard Board;
    private AnchorPane creatorPane;
    private Scene creatorScene;
    private Stage creatorStage;
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 900;

    private TextField WidthText = new TextField("20");
    private TextField HeightText = new TextField("30");
    private TextField NameText = new TextField("Name");
    private Label CurrentSectorLabel = new Label("Current Sector: Blank | Brush: 1x1");
    private final SectorType Blank = SectorType.Blank;
    private final SectorType Acid = SectorType.Acid;
    private final SectorType Wall = SectorType.Wall;
    private final SectorType Energy = SectorType.EnergyField;
    private SectorType CurrentSectorType = Blank;
    private Stage menuStage;
    private Pane viewport = new Pane();
    private List<BetterButton> SectorTypesButtons;
    private List<BetterButton> BrushSizeButtons;
    private List<SectorType> SectorTypesList;

    private int brushSize = 1;
    private boolean isDragging = false;
    private Sector lastPaintedSector = null;

    private final int[] brushSizes = {1, 2, 3, 4, 5, 6, 7, 8};
    private final String[] brushSizeNames = {"1x1", "2x2", "3x3", "4x4", "5x5", "6x6", "7x7", "8x8"};

    public enum BrushAnchor {
        CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
    private BrushAnchor currentBrushAnchor = BrushAnchor.CENTER;

    public BattlefieldCreator() throws IOException {
        initializeStage();
    }

    public void createNewMaker(Stage menuStage, Battlefield field) throws IOException {
        SectorTypesButtons = new ArrayList<>();
        BrushSizeButtons = new ArrayList<>();
        this.Battlefield = field;

        SectorTypesList = new ArrayList<>();
        SectorTypesList.add(Blank);
        SectorTypesList.add(Acid);
        SectorTypesList.add(Wall);
        SectorTypesList.add(Energy);

        CurrentSectorLabel.setLayoutX(10);
        CurrentSectorLabel.setLayoutY(5);
        creatorPane.getChildren().add(CurrentSectorLabel);

        int y = 10;
        int x = 160;
        WidthText.setPrefSize(50, 30);
        WidthText.setLayoutX(x);
        WidthText.setLayoutY(y);
        HeightText.setPrefSize(50, 30);
        HeightText.setLayoutX(x + 60);
        HeightText.setLayoutY(y);
        NameText.setPrefSize(50, 30);
        NameText.setLayoutX(x + 260);
        NameText.setLayoutY(y);
        creatorPane.getChildren().addAll(HeightText, WidthText, NameText);

        // Sector type buttons
        for (SectorType sectorType : SectorTypesList) {
            createSectorSelectorButton(sectorType.Name, SectorTypesButtons, sectorType);
        }
        SetUpMenuList(creatorPane, SectorTypesButtons, 40, 40);

        // Brush size buttons
        createBrushSizeButtons();
        SetUpBrushSizeList(creatorPane, BrushSizeButtons, 800, 40);

        createGameBoard(20, 30);

        BetterButton change = createChangeSizeButton("Size");
        change.setPrimaryStyle();
        change.setLayoutX(x + 120);
        change.setLayoutY(y);
        creatorPane.getChildren().add(change);

        this.menuStage = menuStage;
        this.menuStage.hide();
        creatorStage.show();
    }

    private BetterButton createChangeSizeButton(String name) {
        BetterButton button = new BetterButton(name);
        button.setPrefHeight(30);
        button.setOnAction(event -> {
            fromBoardtoBattlefield();
            try {
                viewport.getChildren().remove(Board.Board);
                creatorPane.getChildren().remove(Board.Container);

                int width = Integer.parseInt(WidthText.getText());
                int height = Integer.parseInt(HeightText.getText());
                createGameBoard(width, height);
                fromBattlefieldToBoardSectors();
            } catch (NumberFormatException ignore) { /* ignore */ }
        });
        return button;
    }

    private void fromBoardtoBattlefield() {
        Battlefield.sizeX = Board.boardwidth;
        Battlefield.sizeY = Board.boardheight;
        if (Board.sectors == null) return;
        List<Triple<Integer, Integer, SectorType>> newlist = new ArrayList<>();
        for (Sector sector : Board.sectors) {
            if (!sector.getType().Name.equals("Blank")) {
                Triple<Integer, Integer, SectorType> triple = new Triple<>(sector.x, sector.y, sector.getType());
                if (!newlist.contains(triple)) newlist.add(triple);
            }
        }
        Battlefield.SpecialTiles = newlist;
    }

    private void fromBattlefieldToBoardSectors() {
        if (Battlefield.SpecialTiles == null) return;
        for (Triple<Integer, Integer, SectorType> triple : Battlefield.SpecialTiles) {
            Sector sector = Board.getSector(triple.getFirst(), triple.getSecond());
            if (sector != null) sector.setType(triple.getThird());
        }
        Board.UpdateBoardColors();
    }

    private void createGameBoard(int x, int y) {
        GridPane gridPane = new GridPane();
        BorderPane scrollContainer = new BorderPane();
        Board = new GameBoard(gridPane, scrollContainer, x, y);

        viewport.setPrefSize(Math.min(x * 20, 600), Math.min(y * 20, 800));
        viewport.setStyle("-fx-background-color: lightgray;");
        viewport.getChildren().add(gridPane);

        Slider hSlider = new Slider();
        Slider vSlider = new Slider();
        hSlider.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        vSlider.setOrientation(javafx.geometry.Orientation.VERTICAL);

        scrollContainer.setCenter(viewport);
        scrollContainer.setBottom(hSlider);
        scrollContainer.setRight(vSlider);
        scrollContainer.setLayoutX(140);
        scrollContainer.setLayoutY(40);

        creatorPane.getChildren().add(scrollContainer);

        for (Sector sector : Board.sectors) {
            sector.setType(Blank);
        }
        Board.UpdateBoardColors();

        setupMousePainting(gridPane);
        setUpSliders();
    }

    private void setupMousePainting(GridPane gridPane) {
        gridPane.setOnMousePressed((MouseEvent event) -> {
            isDragging = true;
            EventTarget target = event.getTarget();
            if (target instanceof Sector) {
                Sector sector = (Sector) target;
                paintWithBrush(sector);
                lastPaintedSector = sector;
            }
        });

        gridPane.setOnMouseDragged(event -> {
            if (!isDragging) return;
            EventTarget target = event.getTarget();
            if (target instanceof Sector) {
                Sector sector = (Sector) target;
                if (sector != lastPaintedSector) {
                    paintWithBrush(sector);
                    lastPaintedSector = sector;
                }
            }
        });

        gridPane.setOnMouseReleased(event -> {
            isDragging = false;
            lastPaintedSector = null;
        });
    }

    private void paintWithBrush(Sector centerSector) {
        if (CurrentSectorType == null) return;

        int centerX = centerSector.x;
        int centerY = centerSector.y;
        int startX, startY, endX, endY;

        if (brushSize == 1) {
            startX = endX = centerX;
            startY = endY = centerY;
        } else if (brushSize % 2 == 1) {
            int offset = (brushSize - 1) / 2;
            startX = centerX - offset;
            startY = centerY - offset;
            endX = centerX + offset;
            endY = centerY + offset;
        } else {
            int half = brushSize / 2;
            switch (currentBrushAnchor) {
                case CENTER:
                    startX = centerX - half + 1;
                    startY = centerY - half + 1;
                    endX = centerX + half;
                    endY = centerY + half;
                    break;
                case TOP_LEFT:
                    startX = centerX;
                    startY = centerY;
                    endX = centerX + brushSize - 1;
                    endY = centerY + brushSize - 1;
                    break;
                case TOP_RIGHT:
                    startX = centerX - brushSize + 1;
                    startY = centerY;
                    endX = centerX;
                    endY = centerY + brushSize - 1;
                    break;
                case BOTTOM_LEFT:
                    startX = centerX;
                    startY = centerY - brushSize + 1;
                    endX = centerX + brushSize - 1;
                    endY = centerY;
                    break;
                case BOTTOM_RIGHT:
                    startX = centerX - brushSize + 1;
                    startY = centerY - brushSize + 1;
                    endX = centerX;
                    endY = centerY;
                    break;
                default:
                    startX = centerX - half + 1;
                    startY = centerY - half + 1;
                    endX = centerX + half;
                    endY = centerY + half;
            }
        }

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                if (x >= 0 && x < Board.boardwidth && y >= 0 && y < Board.boardheight) {
                    Sector target = Board.getSector(x, y);
                    if (target != null) Board.setType(target, CurrentSectorType);
                }
            }
        }
    }

    private void createBrushSizeButtons() {
        for (int i = 0; i < brushSizes.length; i++) {
            final int size = brushSizes[i];
            final String name = brushSizeNames[i];
            BetterButton button = new BetterButton(name);
            button.setPrefHeight(30);
            button.setSecondaryStyle(); // default grey

            if (size == 1) {
                button.setSelectedStyle(); // grey stays selected
            }

            button.setOnAction(event -> {
                brushSize = size;
                updateLabel();
                // Reset all buttons to secondary style
                for (BetterButton btn : BrushSizeButtons) {
                    btn.setSecondaryStyle();
                }
                // Set selected style on the clicked one
                button.setSelectedStyle();
            });
            BrushSizeButtons.add(button);
        }
    }

    private void setUpSliders() {
        Slider hSlider = (Slider) Board.Container.getBottom();
        Slider vSlider = (Slider) Board.Container.getRight();

        double boardPixelWidth = Board.boardwidth * 20;
        double boardPixelHeight = Board.boardheight * 20;

        double hMax = Math.max(0, boardPixelWidth - viewport.getPrefWidth());
        hSlider.setMin(0);
        hSlider.setMax(hMax);

        double vMax = Math.max(0, boardPixelHeight - viewport.getPrefHeight());
        vSlider.setMin(-vMax);
        vSlider.setMax(0);

        hSlider.setDisable(hMax == 0);
        vSlider.setDisable(vMax == 0);
        hSlider.setVisible(hMax > 0);
        vSlider.setVisible(vMax > 0);

        Board.Board.layoutXProperty().bind(hSlider.valueProperty().multiply(-1));
        Board.Board.layoutYProperty().bind(vSlider.valueProperty());

        Rectangle clip = new Rectangle(viewport.getPrefWidth(), viewport.getPrefHeight());
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);
    }

    private void updateLabel() {
        CurrentSectorLabel.setText("Current Sector: " + CurrentSectorType.Name + " | Brush: " + brushSize + "x" + brushSize);
    }

    private void createSectorSelectorButton(String name, List<BetterButton> menu, SectorType sector) {
        BetterButton button = new BetterButton(name);
        button.setPrefHeight(30);
        button.setPrimaryStyle();
        menu.add(button);
        button.setOnAction(event -> {
            if (CurrentSectorType != sector) CurrentSectorType = sector;
            updateLabel();
        });
    }

    private void SetUpBrushSizeList(AnchorPane pane, List<BetterButton> list, float startx, float starty) {
        float x = startx;
        float y = starty;

        Label brushLabel = new Label("Brush Size:");
        brushLabel.setLayoutX(x);
        brushLabel.setLayoutY(y - 25);
        pane.getChildren().add(brushLabel);

        for (BetterButton button : list) {
            button.setLayoutX(x);
            button.setLayoutY(y);
            pane.getChildren().add(button);
            y += button.getPrefHeight() + 10;
        }
    }

    private void SetUpMenuList(AnchorPane pane, List<BetterButton> list, float startx, float starty) {
        float x = startx;
        float y = starty;
        for (BetterButton button : list) {
            button.setLayoutX(x);
            button.setLayoutY(y);
            pane.getChildren().add(button);
            y += button.getPrefHeight() + 10;
        }
    }

    private void initializeStage() {
        creatorPane = new AnchorPane();
        creatorScene = new Scene(creatorPane, WIDTH, HEIGHT);
        creatorStage = new Stage();
        creatorStage.setScene(creatorScene);
    }
}