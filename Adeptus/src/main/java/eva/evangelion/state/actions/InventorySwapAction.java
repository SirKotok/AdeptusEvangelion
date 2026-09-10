package eva.evangelion.state.actions;



public class InventorySwapAction extends Action{
    public InventorySwapAction(int actionNumber, String actor, int startingSlotNum, int endingSlotNum) {
        super(actionNumber, actor);

        this.startingSlotNum = startingSlotNum;
        this.endingSlotNum = endingSlotNum;
    }


   public final int startingSlotNum;
   public final int endingSlotNum;



    public int getStartingSlotNum() {
        return startingSlotNum;
    }

    public int getEndingSlotNum() {
        return endingSlotNum;
    }

}
