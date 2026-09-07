package eva.evangelion.state;

public class QueuePosition {
    public enum ReactionType {
        DM_SETUP_PLAYER,
        PLAYER_SETUP,
        ATTACK_OF_OPPORTUNITY,
        DM_DRAMA_MOVEMENT
    }

    public int Round = -1;
    public int Turn = -1;
    public String UnitID;
    public final boolean Reaction;
    int ActionNumber = -1;
    int ReactionTo = -1;
    private ReactionType reactionType;
    private boolean skippable = false;
    private String description = "";

    public QueuePosition(String unitID, boolean reaction) {
        UnitID = unitID;
        Reaction = reaction;
    }

    public boolean isReaction() {
        return Reaction;
    }

    public int getReactionTo() {
        return ReactionTo;
    }

    public void setReactionTo(int reactionTo) {
        ReactionTo = reactionTo;
    }

    // Factory method for reaction turns
    public static QueuePosition createReactionTurn(String unitID, ReactionType type, String description, int ReactionTo) {
        QueuePosition pos = new QueuePosition(unitID, true);
        pos.setReactionType(type);
        pos.setDescription(description);
        pos.setSkippable(false); // default
        pos.setReactionTo(ReactionTo);
        return pos;
    }

    // Getters and setters for new fields
    public ReactionType getReactionType() { return reactionType; }
    public void setReactionType(ReactionType reactionType) { this.reactionType = reactionType; }
    public boolean isSkippable() { return skippable; }
    public void setSkippable(boolean skippable) { this.skippable = skippable; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Existing getters/setters
    public void setUnitID(String unitID) { UnitID = unitID; }
    public void setActionNumber(int actionNumber) { ActionNumber = actionNumber; }
    public void setRound(int round) { Round = round; }
    public void setTurn(int turn) { Turn = turn; }
    public int getActionNumber() { return ActionNumber; }
    public int getRound() { return Round; }
    public int getTurn() { return Turn; }
    public String getUnitID() { return UnitID; }
}