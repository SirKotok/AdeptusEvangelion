package eva.evangelion.units.battle;

/**
 * Represents a single active modifier on a {@link FieldUnit}.
 * Every stat except Toughness (the damage pool) is expressed as a delta.
 * Toughness itself is handled specially on FieldUnit, since it has its own
 * setter and damage method.
 */
public class Effect {

    /** When the effect is automatically cleared. */
    public enum EffectEnd {
        NEVER,      // lasts until manually removed (e.g. permanent upgrades)
        TURN_END,   // cleared at the end of the current Turn
        ROUND_END,  // cleared at the end of the current Round
        COMMAND     // cleared only when explicitly commanded (e.g. end of battle)
    }

    private String name;
    private EffectEnd effectEnd;

    // ---- Stat deltas (all default to 0 = no change) ----
    private int deltaAccuracy       = 0;
    private int deltaAttackStrength = 0;
    private int deltaArmor          = 0;
    private int deltaReflexes       = 0;
    private int deltaSpeed          = 0;
    private int deltaMaxToughness   = 0;

    public Effect(String name, EffectEnd effectEnd) {
        this.name = name;
        this.effectEnd = effectEnd;
    }

    // ---- Name / duration ----
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public EffectEnd getEffectEnd() { return effectEnd; }
    public void setEffectEnd(EffectEnd effectEnd) { this.effectEnd = effectEnd; }

    // ---- Accuracy ----
    public int getDeltaAccuracy() { return deltaAccuracy; }
    public void setDeltaAccuracy(int deltaAccuracy) { this.deltaAccuracy = deltaAccuracy; }

    // ---- Attack Strength ----
    public int getDeltaAttackStrength() { return deltaAttackStrength; }
    public void setDeltaAttackStrength(int deltaAttackStrength) { this.deltaAttackStrength = deltaAttackStrength; }

    // ---- Armor ----
    public int getDeltaArmor() { return deltaArmor; }
    public void setDeltaArmor(int deltaArmor) { this.deltaArmor = deltaArmor; }

    // ---- Reflexes ----
    public int getDeltaReflexes() { return deltaReflexes; }
    public void setDeltaReflexes(int deltaReflexes) { this.deltaReflexes = deltaReflexes; }

    // ---- Speed ----
    public int getDeltaSpeed() { return deltaSpeed; }
    public void setDeltaSpeed(int deltaSpeed) { this.deltaSpeed = deltaSpeed; }

    // ---- Max Toughness ----
    public int getDeltaMaxToughness() { return deltaMaxToughness; }
    public void setDeltaMaxToughness(int deltaMaxToughness) { this.deltaMaxToughness = deltaMaxToughness; }

    // ---- Fluent helpers, so you can build effects one-liner style ----
    public Effect withAccuracy(int d)       { this.deltaAccuracy = d;       return this; }
    public Effect withAttackStrength(int d) { this.deltaAttackStrength = d; return this; }
    public Effect withArmor(int d)          { this.deltaArmor = d;          return this; }
    public Effect withReflexes(int d)       { this.deltaReflexes = d;       return this; }
    public Effect withSpeed(int d)          { this.deltaSpeed = d;          return this; }
    public Effect withMaxToughness(int d)   { this.deltaMaxToughness = d;   return this; }

    @Override
    public String toString() {
        return "Effect{" + name +
                ", end=" + effectEnd +
                ", acc=" + deltaAccuracy +
                ", S=" + deltaAttackStrength +
                ", arm=" + deltaArmor +
                ", ref=" + deltaReflexes +
                ", spd=" + deltaSpeed +
                ", maxT=" + deltaMaxToughness +
                '}';
    }
}