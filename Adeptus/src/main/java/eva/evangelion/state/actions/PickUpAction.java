package eva.evangelion.state.actions;

public class PickUpAction extends Action{

    public PickUpAction(int actionNumber, String actor, int slotNum, int endingSlotNum, int pickDeltaX, int pickDeltaY) {
        super(actionNumber, actor);
        SlotNum = slotNum;
        this.endingSlotNum = endingSlotNum;
        this.pickDeltaX = pickDeltaX;
        this.pickDeltaY = pickDeltaY;
    }


    public final int SlotNum;
    public final int endingSlotNum;
    public final int pickDeltaX;
    public final int pickDeltaY;



    public int getSlotNum() {
        return SlotNum;
    }

    public int getPickDeltaX() {
        return pickDeltaX;
    }

    public int getPickDeltaY() {
        return pickDeltaY;
    }
}
