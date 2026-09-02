package eva.evangelion.gameboard;


import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameBoard {

    public GridPane Board;
    public BorderPane Container;
    public int boardwidth = 20;
    public int boardheight = 30;

    public ArrayList<Sector> sectors = new ArrayList<>();

    public GameBoard(GridPane Board,  BorderPane container, int width, int height){
        this.Board = Board;
        this.Container = container;
        boardwidth = width;
        boardheight = height;
        makeBoard(this.Board);
    }
    public GameBoard(GridPane Board){
        this.Board = Board;
     //   this.Container = container;
        makeBoard(this.Board);
    }

    public void setPosition(double x, double y){
        Container.setLayoutX(x);
        Container.setLayoutY(y);
    }



    public void makeBoard(GridPane battleboard){
        for(int i=0; i<boardwidth; i++){
            for(int j=0; j<boardheight; j++){
                Sector sector = new Sector(i,j);
                sector.setPrefHeight(20);
                sector.setPrefWidth(20);
                sector.setBorder(new Border(new BorderStroke(Color.BLACK,
                        BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
                battleboard.add(sector, i, j, 1, 1);
                sectors.add(sector);
            }
        }
    }


    public boolean OnBoard(int x, int y) {
        return x >= 0 && x < boardwidth && y >= 0 && y < boardheight;
    }
    public void UpdateBoardColors(){
        for (Sector sector : sectors) {
            ChangeColor(sector);
        }
    }



    // Helper method to set yellow border with default background
    private void setYellowBorder(Sector sector) {
        if (sector.getType() != null) {
            // Set the background to the type's color with a yellow border
            sector.setBackground(new Background(new BackgroundFill(
                    sector.getType().getColor(),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            // Add a yellow border
            sector.setBorder(new Border(new BorderStroke(
                    Color.YELLOW,
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(1.2) // Border thickness
            )));
        }
    }

    // Helper method to set orange border with default background
    private void setOrangeBorder(Sector sector) {
        if (sector.getType() != null) {
            // Set the background to the type's color with an orange border
            sector.setBackground(new Background(new BackgroundFill(
                    sector.getType().getColor(),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            // Add an orange border
            sector.setBorder(new Border(new BorderStroke(
                    Color.ORANGE,
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(1.2) // Border thickness
            )));
        }
    }

    public void ChangeColor(Sector sector) {
        if (sector.getType() != null) {
            // Reset to default: background only, no border
            sector.setBackground(new Background(new BackgroundFill(
                    sector.getType().getColor(),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));
            sector.setBorder(new Border(new BorderStroke(
                    Color.BLACK,
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(0.5) // Border thickness
            ))); // Remove any border
        }
    }



    public boolean SupportCheck(Sector sector) {
        return sector.getType().SupportStructure;
    }

    public List<Sector> DrawSquare(Sector sector, int size, Color color) {
        int startx = sector.x-size;
        int starty = sector.y-size;
        int endx = sector.x+size;
        int endy = sector.y+size;
        List<Sector> affected = new ArrayList<>();
        for (Sector sector1 : sectors) {
            if (sector1.x >= startx && sector1.x <= endx && sector1.y >= starty && sector1.y <= endy) {
                sector1.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
                affected.add(sector1); }
        }
        return affected;
    }
    public List<Sector> DrawSquare(int x, int y, int size, Color color) {
        int startx = x-size;
        int starty = y-size;
        int endx = x+size;
        int endy = y+size;
        List<Sector> affected = new ArrayList<>();
        for (Sector sector1 : sectors) {
            if (sector1.x >= startx && sector1.x <= endx && sector1.y >= starty && sector1.y <= endy) {
                sector1.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
                affected.add(sector1); }
        }
        return affected;
    }

    public List<Sector> DrawSquareReplacable(int x, int y, int size, Color color) {
        int startx = x-size;
        int starty = y-size;
        int endx = x+size;
        int endy = y+size;
        List<Sector> affected = new ArrayList<>();
        for (Sector sector1 : sectors) {
            if (sector1.x >= startx && sector1.x <= endx && sector1.y >= starty && sector1.y <= endy && sector1.type.CanMoveTo) {
                sector1.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
                affected.add(sector1); }
        }
        return affected;
    }



    public List<Sector> DrawLine(int startx, int starty, int endx, int endy, int stepx, int stepy, Color color) {
        int sx = startx;
        int sy = starty;
        int dx = Math.abs(endx - startx);
        int dy = Math.abs(endy - starty);
        int steps = Math.max(dx, dy);
        List<Sector> affected = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            Sector sector1 = getSector(sx, sy);
            if (sector1 != null && i != 0) {
                sector1.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
                affected.add(sector1); }
            if (sx != endx) sx += stepx;
            if (sy != endy) sy += stepy;
        }
        return affected;
    }

    public Sector getSector(int x, int y) {
        for (Sector sector1 : sectors) {
            if (sector1.x == x && sector1.y == y) return sector1;
        }
        return null;
    }



    public Sector getRandomSectorInRange(int CenterX, int CenterY, int range) {
        int boardWidth = boardwidth;
        int boardHeight = boardheight;

        // Calculate valid X range (clamped to board boundaries)
        int minX = Math.max(0, CenterX - range);
        int maxX = Math.min(boardWidth - 1, CenterX + range);

        // Calculate valid Y range (clamped to board boundaries)
        int minY = Math.max(0, CenterY - range);
        int maxY = Math.min(boardHeight - 1, CenterY + range);

        Random random = new Random();

        // Generate random coordinates within the valid range
        int x = random.nextInt(maxX - minX + 1) + minX;
        int y = random.nextInt(maxY - minY + 1) + minY;

        // Return the corresponding Sector from the gameboard
        return getSector(x, y); // See note below
    }



    public void setType(Sector sector, SectorType type){
        sector.setType(type);
        ChangeColor(sector);
    }




}