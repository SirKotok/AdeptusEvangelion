package eva.evangelion.items.Weapon;

public class FieldItem {

    public final Item item;
    public int X;
    public int Y;

    public FieldItem(Item item, int x, int y) {
        this.item = item;
        this.X = x;
        this.Y = y;
    }

    public void setY(int y) {
        Y = y;
    }

    public Item getItem() {
        return item;
    }

    public void setX(int x) {
        X = x;
    }
    public void setPosition(int x, int y){
        setX(x);
        setY(y);
    }

    public int getY() {
        return Y;
    }

    public int getX() {
        return X;
    }
}
