package eva.evangelion.state.actions;

public class DropAction extends Action{

    public DropAction(int actionNumber, String actor, int slotNum, int endingSlotNum, int dropDeltaX, int dropDeltaY) {
        super(actionNumber, actor);
        SlotNum = slotNum;
        this.endingSlotNum = endingSlotNum;
        this.dropDeltaX = dropDeltaX;
        this.dropDeltaY = dropDeltaY;
    }

    public final int SlotNum;
    public final int endingSlotNum;
    public final int dropDeltaX;
    public final int dropDeltaY;




    public int getSlotNum() {
        return SlotNum;
    }

    public int getDropDeltaX() {
        return dropDeltaX;
    }

    public int getDropDeltaY() {
        return dropDeltaY;
    }
}
