package eva.evangelion.items.Weapon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AttackProfile implements Serializable {
    int Dice;
    int Dicepower;
    int Power;
    int Stamina;
    int AmmoCost = 0;
    int Penetration = 0;
    int AreaType = -1;
     // Area = -2 -> Line
     // Area = -1 -> Normal Attack
     // Area = 0+ -> Area Attack
    int ATP;
    String name;
    public boolean Ranged;
    public boolean isRanged() {
        return Ranged;
    }
    public void setRanged(boolean ranged) {
        Ranged = ranged;
    }

    public enum ProfileTypes {
        BASIC,
        BLITZ,
        FULLAUTO,
        THROW,
        SPECIAL
    }
    public enum AttackProperty {
        GRAPPLE, INTRINSIC, PRECISE, PROVEN,
        REACH, CQB, SWIFT, SMALL, SPRAY, THROWING, ARMORPIERCING
    }

    public List<AttackProperty> AttackProperties = new ArrayList<>();

    public ProfileTypes ProfileType;

    public AttackProfile(String Name, int dice, int dicepower, int power, int stamina, int atp, boolean range, ProfileTypes type){
        ProfileType = type;
        setRanged(range);
        name = Name;
        Dice = dice;
        Dicepower = dicepower;
        Power = power;
        Stamina = stamina;
        ATP = atp;
    }

    public AttackProfile(String Name, int dice, int dicepower, int power, ProfileTypes type, boolean range){
        ProfileType = type;
        setRanged(range);
        name = Name;
        Dice = dice;
        Dicepower = dicepower;
        Power = power;
        Stamina = type == AttackProfile.ProfileTypes.BASIC ? 1 : 2;
        ATP = 0;
    }

    public static AttackProfile createBasicAttack(Weapon w) {
        AttackProfile profile = null;
        switch (w.getProfileType()) {
            // General Melee Weapons
            case "Knife":
                profile = new AttackProfile("Basic Attack", 2, 3, 0, ProfileTypes.BASIC, false);
                break;
            case "Reach Weapon":
                profile = new AttackProfile("Basic Attack", 1, 10, 0, ProfileTypes.BASIC, false);
                break;
            case "Shield":
                profile = new AttackProfile("Basic Attack", 1, 6, 0, ProfileTypes.BASIC, false);
                break;
            case "Slashing Weapon":
                profile = new AttackProfile("Basic Attack", 1, 10, 0, ProfileTypes.BASIC, false);
                break;
            case "Crushing Weapon":
                profile = new AttackProfile("Basic Attack", 1, 10, 0, ProfileTypes.BASIC, false);
                break;
            case "Large Slashing Weapon":
                profile = new AttackProfile("Basic Attack", 2, 6, 0, ProfileTypes.BASIC, false);
                break;
            case "Large Crushing Weapon":
                profile = new AttackProfile("Basic Attack", 2, 6, 0, ProfileTypes.BASIC, false);
                break;
            case "Large Reach Weapon":
                profile = new AttackProfile("Basic Attack", 2, 6, 0, ProfileTypes.BASIC, false);
                break;

            // Intrinsic Weapons
            case "Unarmed":
                profile = new AttackProfile("Basic Attack", 1, 6, 0, ProfileTypes.BASIC, false);
                break;
            case "Natural Weapon":
                profile = new AttackProfile("Basic Attack", 1, 10, 0, ProfileTypes.BASIC, false);
                break;

            // General Ranged Weapons
            case "Pistol":
                profile = new AttackProfile("Basic Attack", 2, 3, 0, ProfileTypes.BASIC, true); profile.AmmoCost = 1;
                break;
            case "SMG":
                profile = new AttackProfile("Basic Attack", 1, 10, 0, ProfileTypes.BASIC, true); profile.AmmoCost = 1;
                break;
            case "Assault Rifle":
                profile = new AttackProfile("Basic Attack", 2, 6, 0, ProfileTypes.BASIC, true); profile.AmmoCost = 1;
                break;
            case "Shotgun":
                profile = new AttackProfile("Basic Attack", 2, 6, 0, ProfileTypes.BASIC, true); profile.AmmoCost = 1;
                break;
            case "Sniper Rifle":
                profile = new AttackProfile("Basic Attack", 1, 10, 0, ProfileTypes.BASIC, true); profile.AmmoCost = 1;
                break;
            case "Machine Gun":
                profile = new AttackProfile("Basic Attack", 2, 6, 0, ProfileTypes.BASIC, true); profile.AmmoCost = 1;
                break;
            case "Bazooka":
                profile = new AttackProfile("Basic Attack", 2, 6, 2, ProfileTypes.BASIC, true); profile.AmmoCost = 1;
                break;
            default:
                // Unrecognized weapon type – return null or handle gracefully
                break;
        }

        return profile;
    }

    public static AttackProfile createBlitzAttack(Weapon w) {
        AttackProfile profile = createBasicAttack(w);
        profile.name = "Blitz";
        profile.Power = profile.Power + (w.getHands().equals(Weapon.Hand.TWO_HANDED) ? 4 : 2);
        profile.ProfileType = ProfileTypes.BLITZ;
        profile.Stamina = 2;
        if (profile.Ranged) profile.AmmoCost = 2;
        return profile;
    }

    public static AttackProfile createFullAutoAttack(Weapon w) {
        AttackProfile profile = createBasicAttack(w);
        profile.name = "Full Auto";
        profile.AreaType = 1;
        profile.ProfileType = ProfileTypes.FULLAUTO;
        if (profile.Ranged) profile.AmmoCost = 2;
        return profile;
    }

}
