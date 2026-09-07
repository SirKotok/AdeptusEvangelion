package eva.evangelion.state.actions;

import eva.evangelion.units.battle.Unit;

public class PLAYERCreateEvangelionAction extends Action {
    private final String unitName;
    private final int x;
    private final int y;
    private final Unit unit;

    public PLAYERCreateEvangelionAction(int actionNumber, String actor, String unitName, int x, int y, Unit unit) {
        super(actionNumber, actor);
        this.unitName = unitName;
        this.x = x;
        this.y = y;
        this.unit = unit;
    }

    public String getUnitName() { return unitName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public Unit getUnit() { return unit; }
}