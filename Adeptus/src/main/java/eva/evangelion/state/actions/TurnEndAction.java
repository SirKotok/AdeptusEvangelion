package eva.evangelion.state.actions;

public class TurnEndAction extends Action{
    public  TurnEndAction(int actionNumber, String actor) {
        super(actionNumber, actor);
        setTime(0.01);
    }

}