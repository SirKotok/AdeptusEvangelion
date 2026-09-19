package eva.evangelion.state.actions;

import eva.evangelion.items.Weapon.AttackProfile;

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



    public int slotNumber = -1;
    public AttackProfile actionCombatProfile;

    public AttackProfile getActionCombatProfile() {
        return actionCombatProfile;
    }

    public void setActionCombatProfile(AttackProfile actionCombatProfile) {
        this.actionCombatProfile = actionCombatProfile;
    }

    public int rolledValue;        // d100 attack roll        (existing)
    public int rolledDamage;       // raw damage before area halving
    public int finalDamage;        // damage after area halving (== rolledDamage if not area or hit)
    public int techDamageBonus;    // flat bonus applied from tech (e.g. Gauss DoS)

    public AttackAction(int actionNumber, String actor) {
        super(actionNumber, actor);
    }

    public void addHit(int x, int y)  { hitPositions.add(new Hit(x, y)); }
    public List<Hit> getHitPositions(){ return hitPositions; }

    @Override
    public String toString() {
        return "AttackAction{actor=" + getActor() +
                ", profile=" + actionCombatProfile.toString() +
                ", hits=" + hitPositions + "}";
    }
}