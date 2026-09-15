package eva.evangelion.state.actions;

import eva.evangelion.units.battle.Unit;

public class PLAYERCreateEvangelionAction extends Action {
    private final String unitName;
    private final int x;
    private final int y;
    private final Unit unit;
    private final int Team;

    public int getTeam() {
        return Team;
    }


    public PLAYERCreateEvangelionAction(int actionNumber, String actor, String unitName, int x, int y, Unit unit, int team) {
        super(actionNumber, actor);
        this.unitName = unitName;
        this.x = x;
        this.y = y;
        this.unit = unit;
        Team = team;
        setTime(0.01);
    }

    public String getUnitName() { return unitName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public Unit getUnit() { return unit; }
}