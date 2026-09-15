package eva.evangelion.units.battle;

import java.io.Serializable;

public class Unit implements Serializable {

    public static final int BASE_ACCURACY         = 75;
    public static final int BASE_ATTACK_STRENGTH  = 4;
    public static final int BASE_TOUGHNESS        = 15;
    public static final int BASE_MAX_TOUGHNESS    = 15;
    public static final int BASE_ARMOR            = 3;
    public static final int BASE_REFLEXES         = 25;
    public static final int BASE_SPEED            = 3;

    protected int accuracy        = BASE_ACCURACY;         // Used for Attack Tests
    protected int attackStrength  = BASE_ATTACK_STRENGTH;  // Damage bonus ("S")
    protected int toughness       = BASE_TOUGHNESS;        // Damage Pool
    protected int maxtoughness    = BASE_MAX_TOUGHNESS;    // Damage Pool maximum
    protected int armor           = BASE_ARMOR;            // Damage reduction per hit
    protected int reflexes        = BASE_REFLEXES;         // Used for Guard Tests
    protected int speed           = BASE_SPEED;




    int ItemReach = 1;

    public int getItemReach() {
        return ItemReach;
    }

    public Unit(){
    }

    // ---- Accuracy ----
    public int getAccuracy() {
        return accuracy;
    }
    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }
    public void addAccuracy(int delta) {
        this.accuracy += delta;
    }

    // ---- Attack Strength (S) ----
    public int getAttackStrength() {
        return attackStrength;
    }
    public void setAttackStrength(int attackStrength) {
        this.attackStrength = attackStrength;
    }
    public void addAttackStrength(int delta) {
        this.attackStrength += delta;
    }

    // ---- Toughness ----
    public int getToughness() {
        return toughness;
    }
    public void setToughness(int toughness) {
        this.toughness = toughness;
    }

    public void addToughness(int delta) {
        this.toughness += delta;
    }

    public void addMaxToughness(int delta) {
        this.maxtoughness += delta;
    }
    public int getMaxToughness() {
        return maxtoughness;
    }
    public void setMaxToughness(int toughness) {
        this.maxtoughness = toughness;
    }

    // ---- Armor ----
    public int getArmor() {
        return armor;
    }
    public void setArmor(int armor) {
        this.armor = armor;
    }
    public void addArmor(int delta) {
        this.armor += delta;
    }

    // ---- Reflexes ----
    public int getReflexes() {
        return reflexes;
    }
    public void setReflexes(int reflexes) {
        this.reflexes = reflexes;
    }
    public void addReflexes(int delta) {
        this.reflexes += delta;
    }

    // ---- Speed ----
    public int getSpeed() {
        return speed;
    }
    public void setSpeed(int speed) {
        this.speed = speed;
    }
    public void addSpeed(int delta) {
        this.speed += delta;
    }



}
