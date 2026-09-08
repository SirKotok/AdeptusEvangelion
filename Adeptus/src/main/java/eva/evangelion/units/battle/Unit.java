package eva.evangelion.units.battle;

import java.io.Serializable;

public class Unit implements Serializable {
    int Toughness = 25;
    int MaxToughness = 25;
    int Armor = 2;
    int Accuracy = 75;
    int Reflexes = 25;
    int Speed = 2;
    int MaxStamina = 2;
    int Strength = 2;
    int RangedStrength = 2;
    int ItemReach = 1;

    public int getItemReach() {
        return ItemReach;
    }

    public Unit(){

    }


}
