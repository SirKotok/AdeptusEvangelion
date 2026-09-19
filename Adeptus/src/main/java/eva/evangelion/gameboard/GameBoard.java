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
    public void SetBoardColorsToSectoryTypes(){
        for (Sector sector : sectors) {
            ChangeSectorColorToType(sector);
        }
    }



    public void ChangeSectorColorToType(Sector sector) {
        if (sector.getType() != null) {
            sector.setBackground(new Background(new BackgroundFill(
                    sector.getType().getColor(),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));
            sector.setBorder(new Border(new BorderStroke(
                    Color.BLACK,
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(0.5)
            )));
        }
    }


    public boolean isCover(Sector sector) {
        return false;
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



    public void setType(Sector sector, SectorType type){
        sector.setType(type);
        ChangeSectorColorToType(sector);
    }




}