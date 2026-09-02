package eva.evangelion.state.actions;

public class DMChoosePlayerAction extends Action {

    private final String chosenPlayer;

    @Override
    public double getTime() {
        return 0.01;
    }

    public DMChoosePlayerAction(int actionNumber, String actor, String chosenPlayer) {
        super(actionNumber, actor);
        this.chosenPlayer = chosenPlayer;
    }

    public String getChosenPlayer() {
        return chosenPlayer;
    }
}