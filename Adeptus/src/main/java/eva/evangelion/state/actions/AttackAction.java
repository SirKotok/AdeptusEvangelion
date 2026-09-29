package eva.evangelion.state.actions;

import eva.evangelion.items.Weapon.AttackProfile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Attack action contains only the roll result, locations hit and additional strain/penetration. If the attack missed or not is recalculated
 * on the client. It also contains the fact that attack can be unable to miss if its Area != =1 */

public class AttackAction extends Action {

    /** A single sector hit by the attack. */
    public static class Hit implements Serializable {
        public final int deltax;
        public final int deltay;
        public Hit(int deltax, int deltay) { this.deltax = deltax; this.deltay = deltay; }
        @Override public String toString() { return "(" + deltax + "," + deltay + ")"; }
    }

    /** Every sector hit by this attack. */
    public List<Hit> hitPositions = new ArrayList<>();

    public boolean isAttackOfOpportunity = false;


    public int slotNumber = -1;
    public AttackProfile actionCombatProfile;

    public AttackProfile getActionCombatProfile() {
        return actionCombatProfile;
    }

    public void setActionCombatProfile(AttackProfile actionCombatProfile) {
        this.actionCombatProfile = actionCombatProfile;
    }

    public int rolledHitValue;   // raw d100 attack roll
    public int rolledDamage;  // raw damage: dice + Power + bonus damage// Halving on a miss in case of canFullyMiss = false is applied on client side processing attack action. Halved BEFORE armor is taken into account.
    public int accuracyTN;    // attacker's TN — It is compared to the Attacker's client side TN, if they are not the same should output an error.

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