package eva.evangelion.state.actions;

public class AddPlayerAction extends Action {
    private final String unitName;
    private final int x;
    private final int y;
    private final int team;

    public AddPlayerAction(int actionNumber, String actor, String unitName, int x, int y) {
        this(actionNumber, actor, unitName, x, y, 0);
    }

    public AddPlayerAction(int actionNumber, String actor, String unitName, int x, int y, int team) {
        super(actionNumber, actor);
        this.unitName = unitName;
        this.x = x;
        this.y = y;
        this.team = team;
        setTime(0.01);
    }

    public String getUnitName() { return unitName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getTeam() { return team; }
}