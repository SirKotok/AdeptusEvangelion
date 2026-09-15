package eva.evangelion.state.actions;

public class EndTurnAction extends Action{
    public EndTurnAction(int actionNumber, String actor) {
        super(actionNumber, actor);
        setTime(0.01);
    }

}