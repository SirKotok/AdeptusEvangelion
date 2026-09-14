package eva.evangelion.state.actions;

public class InventoryItemTransferAction extends Action{
    public InventoryItemTransferAction(int actionNumber, String actor) {
        super(actionNumber, actor);
        setTime(0.01);
    }

    public enum TRANSFER_TYPE {
        INVENTORY,
        UNIT2BOARD,
        BOARD2UNIT
    }

    public TRANSFER_TYPE transferType;

    public void setTransferType(TRANSFER_TYPE transferType) {
        this.transferType = transferType;
    }

    public TRANSFER_TYPE getTransferType() {
        return transferType;
    }

    int slotTo = -1;
    int slotFrom = -1;
    int DeltaX;
    int DeltaY;


    public void setSlotTo(int slotTo) {
        this.slotTo = slotTo;
    }

    public void setSlotFrom(int slotFrom) {
        this.slotFrom = slotFrom;
    }


    public void setDelta(int deltaX, int deltaY){
        DeltaX = deltaX;
        DeltaY = deltaY;
    }

    public int getDeltaY() {
        return DeltaY;
    }

    public int getDeltaX() {
        return DeltaX;
    }

    public int getSlotFrom() {
        return slotFrom;
    }

    public int getSlotTo() {
        return slotTo;
    }

    public static InventoryItemTransferAction InventorySwitch(int actionNumber, String actor, int slotFrom, int slotTo) {
        InventoryItemTransferAction action = new InventoryItemTransferAction(actionNumber, actor);
        action.setSlotFrom(slotFrom);
        action.setSlotTo(slotTo);
        action.setTransferType(TRANSFER_TYPE.INVENTORY);
        return action;
    }
    public static InventoryItemTransferAction dropAction(int actionNumber, String actor, int slotFrom, int deltaX, int deltaY) {
        InventoryItemTransferAction action = new InventoryItemTransferAction(actionNumber, actor);
        action.setSlotFrom(slotFrom);
        action.setDelta(deltaX, deltaY);
        action.setTransferType(TRANSFER_TYPE.UNIT2BOARD);
        return action;
    }
    public static InventoryItemTransferAction pickUpAction(int actionNumber, String actor, int slotTo, int deltaX, int deltaY) {
        InventoryItemTransferAction action = new InventoryItemTransferAction(actionNumber, actor);
        action.setSlotTo(slotTo);
        action.setDelta(deltaX, deltaY);
        action.setTransferType(TRANSFER_TYPE.BOARD2UNIT);
        return action;
    }


}
