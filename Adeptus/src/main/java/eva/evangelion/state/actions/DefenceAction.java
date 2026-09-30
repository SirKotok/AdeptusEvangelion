package eva.evangelion.state.actions;

public class DefenceAction extends Action {

    /** Layered Field was activated (costs 1 ATP, +3 Armor against this hit only). */
    public boolean layeredField = false;

    /** Guard was rolled. */
    public boolean guardRolled = false;
    public int guardRoll = 0;
    /** Reflexes (clamped TN) the roll was made against. Re-checked on processing. */
    public int guardTN = 0;

    public DefenceAction(int actionNumber, String actor) {
        super(actionNumber, actor);
        setTime(0.01);
    }

    public boolean isGuardSuccess() {
        return guardRolled && AttackAction.rollSucceeds(guardRoll, guardTN);
    }

    @Override
    public String toString() {
        return "DefenceAction{actor=" + getActor() + ", layered=" + layeredField
                + ", guardRolled=" + guardRolled + ", roll=" + guardRoll + " vs " + guardTN + "}";
    }
}