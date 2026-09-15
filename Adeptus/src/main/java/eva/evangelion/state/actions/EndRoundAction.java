package eva.evangelion.state.actions;

public class EndRoundAction extends Action{
    public EndRoundAction(int actionNumber, String actor) {
        super(actionNumber, actor);
        setTime(0.01);
    }
}
