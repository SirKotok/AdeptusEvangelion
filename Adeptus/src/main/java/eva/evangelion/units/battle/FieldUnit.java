package eva.evangelion.units.battle;

import eva.evangelion.items.Weapon.AttackProfile;
import eva.evangelion.units.active.Slot;
import eva.evangelion.units.type.EvangelionType;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;

public class FieldUnit {

    public final Unit unit;
    public int X;
    public int Y;
    public final String Name;
    public Circle UnitCircle;
    public boolean Exists = true;
    private int team;
    private boolean usedGuard = false;


    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }
    public boolean isAlly(FieldUnit unit) {
        return unit.getTeam() == this.getTeam();
    }

    public boolean turnDone = false;

    public void setTurnDone(boolean turnDone) {
        this.turnDone = turnDone;
    }

    public boolean isTurnDone() {
        return turnDone;
    }

    public int Stamina;
    public int MaxStamina = 2;
    public int ATP;
    public int maxATP = 1;
    public boolean usedTactical = false;
    /** All currently-active modifiers on this unit. */
    private final List<Effect> CurrentEffects = new ArrayList<>();

    public void setExists(boolean exists) {
        Exists = exists;
    }

    public boolean isExists() {
        return Exists;
    }

    public FieldUnit(String name, Unit unit, int x, int y) {
        this.unit = unit;
        X = x;
        Y = y;
        Stamina = MaxStamina;
        ATP = maxATP;
        this.Name = name;
        UnitCircle = new Circle(9.5);
        if (this.isEva()) {
            Evangelion eva = (Evangelion) unit;
            EvangelionType evatype = eva.getType();
            UnitCircle.setStroke(Paint.valueOf(evatype.getSecondaryColor()));
            UnitCircle.setFill(Paint.valueOf(evatype.getPrimaryColor()));
        }
    }

    public int getATP() {
        return ATP;
    }

    public int getMaxATP() {
        return maxATP;
    }

    public int getMaxStamina() {
        return MaxStamina;
    }

    public int getStamina() {
        return Stamina;
    }

    public void setATP(int ATP) {
        this.ATP = ATP;
    }
    public void useATP(int ATP) {
        this.ATP -= ATP;
    }
    public void useStamina(int Stamina) {
        this.Stamina -= Stamina;
    }
    public void useTactical(boolean yes){
        if (yes) setUsedTactical(true);
    }

    public List<AttackProfile> getUnitAttackProfiles(){
        List<AttackProfile> profiles = new ArrayList<>();
        profiles.add(new AttackProfile("Test 1", 2, 5, 0, AttackProfile.ProfileTypes.BASIC, false));
        profiles.add(new AttackProfile("Test 2", 4, 2, 2, AttackProfile.ProfileTypes.BASIC, false));
        return profiles;   // <-- was: return null;
    }
    public List<Slot> getSlots(){
        if (this.unit instanceof Evangelion eva)
        {return  eva.getSlots();}
        else return null;
    }

    public void setMaxATP(int maxATP) {
        this.maxATP = maxATP;
    }

    public void setMaxStamina(int maxStamina) {
        MaxStamina = maxStamina;
    }

    public void setStamina(int stamina) {
        Stamina = stamina;
    }

    public void setUsedTactical(boolean usedTactical) {
        this.usedTactical = usedTactical;
    }

    public boolean usedTactical() {
        if (!(getUnit() instanceof Evangelion)) return true;
        return usedTactical;
    }
    public boolean hasTactical() {
        return !usedTactical;
    }
    public String getName() {
        return Name;
    }

    public Circle getUnitCircle() {
        return UnitCircle;
    }

    public Unit getUnit() {
        return unit;
    }

    public boolean isEva(){
        return unit instanceof Evangelion;
    }

    public int getX() { return X; }
    public int getY() { return Y; }
    public void setX(int x) { X = x; }
    public void setY(int y) { Y = y; }

    // ============================================================
    //   EFFECTS MANAGEMENT
    // ============================================================

    public List<Effect> getCurrentEffects() {
        return CurrentEffects;
    }

    public void addEffect(Effect effect) {
        if (effect != null) CurrentEffects.add(effect);
    }

    public void removeEffect(Effect effect) {
        CurrentEffects.remove(effect);
    }

    /**
     * Removes every effect whose {@link Effect.EffectEnd} matches the given
     * category. Call this at Turn end, Round end, or on command.
     */
    public void ClearEffects(Effect.EffectEnd end) {
        CurrentEffects.removeIf(e -> e.getEffectEnd() == end);
    }



    // ============================================================
    //   EFFECTIVE STAT GETTERS (base + all active effects)
    // ============================================================

    public int getAccuracy() {
        int total = unit.getAccuracy();
        for (Effect e : CurrentEffects) total += e.getDeltaAccuracy();
        return total;
    }

    public int getAttackStrength() {
        int total = unit.getAttackStrength();
        for (Effect e : CurrentEffects) total += e.getDeltaAttackStrength();
        return total;
    }

    public int getArmor() {
        int total = unit.getArmor();
        for (Effect e : CurrentEffects) total += e.getDeltaArmor();
        return total;
    }

    public int getReflexes() {
        int total = unit.getReflexes();
        for (Effect e : CurrentEffects) total += e.getDeltaReflexes();
        return total;
    }

    public int getSpeed() {
        int total = unit.getSpeed();
        for (Effect e : CurrentEffects) total += e.getDeltaSpeed();
        return total;
    }

    public int getMaxToughness() {
        int total = unit.getMaxToughness();
        for (Effect e : CurrentEffects) total += e.getDeltaMaxToughness();
        return total;
    }

    // ============================================================
    //   TOUGHNESS (special: has its own setter & damage method)
    // ============================================================

    public int getToughness() {
        return unit.getToughness();
    }

    public void setToughness(int value) {
        unit.setToughness(value);
    }

    /**
     * Applies damage to the unit's Toughness. The value is clamped at 0;
     * overflow handling (Wound trigger) is the caller's responsibility.
     */
    public void DealToughnessDamage(int amount) {
        if (amount <= 0) return;
        int newValue = unit.getToughness() - amount;
        if (newValue < 0) newValue = 0;
        unit.setToughness(newValue);
    }

    @Override
    public String toString() {
        return "FieldUnit{" + Name +
                ", acc=" + getAccuracy() +
                ", S=" + getAttackStrength() +
                ", arm=" + getArmor() +
                ", ref=" + getReflexes() +
                ", spd=" + getSpeed() +
                ", tough=" + getToughness() + "/" + getMaxToughness() +
                ", effects=" + CurrentEffects.size() +
                '}';
    }


    public boolean usedGuard() {
        return usedGuard;
    }

    public void setUsedGuard(boolean usedGuard) {
        this.usedGuard = usedGuard;
    }
}