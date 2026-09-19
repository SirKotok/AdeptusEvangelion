package eva.evangelion.state.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AttackAction extends Action {

    /** A single sector hit by the attack. */
    public static class Hit implements Serializable {
        public final int x;
        public final int y;
        public Hit(int x, int y) { this.x = x; this.y = y; }
        @Override public String toString() { return "(" + x + "," + y + ")"; }
    }

    /** Every sector hit by this attack. */
    public List<Hit> hitPositions = new ArrayList<>();

    // ---- Attack metadata (consumed by confirm popup + future resolution) ----
    public String weaponName;        // null = neutral / unarmed
    public String weaponSlotName;    // null = neutral / unarmed
    public String profileName;
    public int    ammoCost;
    public int    rolledValue;       // d100 result

    public AttackAction(int actionNumber, String actor) {
        super(actionNumber, actor);
    }

    public void addHit(int x, int y)  { hitPositions.add(new Hit(x, y)); }
    public List<Hit> getHitPositions(){ return hitPositions; }

    @Override
    public String toString() {
        return "AttackAction{actor=" + getActor() +
                ", profile=" + profileName +
                ", hits=" + hitPositions + "}";
    }
}