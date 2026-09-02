package eva.evangelion.state;

public class QueuePosition {
    public int Round = -1;
    public int Turn = -1;
    public String UnitID;
    public final boolean Reaction;
    int ActionNumber = -1;
    public QueuePosition(String unitID, boolean reaction){
        UnitID = unitID;
        Reaction = reaction;
    }


    public void setUnitID(String unitID) {
        UnitID = unitID;
    }

    public void setActionNumber(int actionNumber) {
        ActionNumber = actionNumber;
    }

    public void setRound(int round) {
        Round = round;
    }

    public void setTurn(int turn) {
        Turn = turn;
    }

    public int getActionNumber() {
        return ActionNumber;
    }

    public int getRound() {
        return Round;
    }

    public int getTurn() {
        return Turn;
    }

    public String getUnitID() {
        return UnitID;
    }
}
