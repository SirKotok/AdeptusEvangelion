package eva.evangelion.state.actions;

/** Sent by a unit that has just received a Wound. Processing restores its Toughness. */
public class DealWoundAction extends Action {

    public DealWoundAction(int actionNumber, String actor) {
        super(actionNumber, actor);
    }

    @Override
    public String toString() {
        return "DealWoundAction{actor=" + getActor() + "}";
    }
}