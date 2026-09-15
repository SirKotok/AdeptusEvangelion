package eva.evangelion.state.actions;

public class SwitchTeamAction extends Action {
    private final String targetName;
    private final int newTeam;

    public SwitchTeamAction(int actionNumber, String actor, String targetName, int newTeam) {
        super(actionNumber, actor);
        this.targetName = targetName;
        this.newTeam = newTeam;
        setTime(0.01);
    }

    public String getTargetName() { return targetName; }
    public int getNewTeam()       { return newTeam; }
}