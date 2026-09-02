package eva.evangelion.units.battle;

import eva.evangelion.units.type.EvangelionType;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import kotlin.Pair;

public class FieldUnit {

    public final Unit unit;
    public int X;
    public int Y;
    public final String Name;
    public Circle UnitCircle;
    public boolean Exists = true;

    public void setExists(boolean exists) {
        Exists = exists;
    }

    public boolean isExists() {
        return Exists;
    }

    public FieldUnit(String name, Unit unit, int x, int y) {
        this.unit = unit;
        X = x;
        Y = y;
        this.Name = name;
        UnitCircle = new Circle(9.5);
        if (this.isEva()) {
            Evangelion eva = (Evangelion) unit;
            EvangelionType evatype = eva.getType();
            UnitCircle.setStroke(Paint.valueOf(evatype.getSecondaryColor()));
            UnitCircle.setFill(Paint.valueOf(evatype.getPrimaryColor()));
        }
    }

    public String getName() {
        return Name;
    }

    public Circle getUnitCircle() {
        return UnitCircle;
    }

    public Unit getUnit() {
        return unit;
    }

    public boolean isEva(){
        return unit instanceof Evangelion;
    }

    public int getX() {
        return X;
    }

    public int getY() {
        return Y;
    }

    public void setX(int x) {
        X = x;
    }

    public void setY(int y) {
        Y = y;
    }
}
