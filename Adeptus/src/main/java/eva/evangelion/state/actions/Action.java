package eva.evangelion.state.actions;

import eva.evangelion.units.battle.FieldUnit;

import java.io.Serializable;

public class Action implements Serializable {
    public double time = 0.5d;
    public final int ActionNumber;
    public final String actor;

    public Action(int actionNumber, String actor) {
        ActionNumber = actionNumber;
        this.actor = actor;
    }

    public String getActor() {
        return actor;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public int getActionNumber() {
        return ActionNumber;
    }

}
