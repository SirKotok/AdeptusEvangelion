package eva.evangelion.state.actions;

public class MoveAction extends Action{

    public boolean canTriggerAtkofOp = true;
    public int deltaX;
    public int deltaY;

    public MoveAction(int actionNumber, String actor, int deltax, int deltay) {
        super(actionNumber, actor);
        deltaX = deltax;
        deltaY = deltay;
    }

    public int getDeltaX() {
        return deltaX;
    }

    public int getDeltaY() {
        return deltaY;
    }

    public boolean canTriggerAtkofOp() {
        return canTriggerAtkofOp;
    }

    public void setCanTriggerAtkofOp(boolean canTriggerAtkofOp) {
        this.canTriggerAtkofOp = canTriggerAtkofOp;
    }

    public void setDeltaX(int deltaX) {
        this.deltaX = deltaX;
    }

    public void setDeltaY(int deltaY) {
        this.deltaY = deltaY;
    }
}
