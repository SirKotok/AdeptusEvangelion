package eva.evangelion.state.actions;

public class DMDeleteUnitAction extends Action {
    private final int x;
    private final int y;

    public DMDeleteUnitAction(int actionNumber, String actor, int x, int y) {
        super(actionNumber, actor);
        this.x = x;
        this.y = y;
    }


    public int getX() { return x; }
    public int getY() { return y; }
}