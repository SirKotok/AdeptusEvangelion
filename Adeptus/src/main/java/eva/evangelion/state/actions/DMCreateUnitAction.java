package eva.evangelion.state.actions;

import eva.evangelion.units.battle.Unit;

public class DMCreateUnitAction extends Action {
    private final String unitName;
    private final int team;
    private final Unit unit = new Unit();
    private final int x;
    private final int y;

    public DMCreateUnitAction(int actionNumber, String actor, String unitName, int x, int y) {
        super(actionNumber, actor);
        this.unitName = unitName;
        this.x = x;
        this.y = y;
        setTime(0.01);
        team = 0;
    }

    public DMCreateUnitAction(int actionNumber, String actor, String unitName, int x, int y, int team) {
        super(actionNumber, actor);
        this.unitName = unitName;
        this.x = x;
        this.y = y;
        setTime(0.01);
        this.team = team;
    }

    public Unit getUnit() {
        return unit;
    }

    public String getUnitName() { return unitName; }
    public int getX() { return x; }
    public int getY() { return y; }
}