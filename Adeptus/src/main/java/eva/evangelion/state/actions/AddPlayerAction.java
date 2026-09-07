package eva.evangelion.state.actions;

public class AddPlayerAction extends Action {
    private final String unitName;
    private final int x;
    private final int y;

    public AddPlayerAction(int actionNumber, String actor, String unitName, int x, int y) {
        super(actionNumber, actor);
        this.unitName = unitName;
        this.x = x;
        this.y = y;
    }

    public String getUnitName() { return unitName; }
    public int getX() { return x; }
    public int getY() { return y; }
}