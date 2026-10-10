package eva.evangelion.items.Weapon;

import java.io.Serializable;
import java.util.ArrayList;

/** A user-made weapon type. Saved to Custom/weapon_types. */
public class CustomWeaponType implements Serializable {
    private static final long serialVersionUID = 1L;

    public String displayName;
    public boolean ranged;
    public Weapon.Hand hands = Weapon.Hand.ONE_HANDED;
    public int baseCost;
    public int dice = 1;
    public int dicePower = 6;
    public int power;              // flat damage bonus of the basic attack
    public int ammo;               // magazine capacity
    public int ammoCost = 1;       // ammo spent by the basic attack
    public int minRange;
    public int maxRange = 1;
    public int basePenetration;
    public int baseArea = -1;      // -1 none, -2 line, 0+ area
    public ArrayList<Weapon.WeaponProperty> baseProperties = new ArrayList<>();

    /**
     * The basic attack for this type. Penetration and Area stay at their defaults here,
     * because Weapon.getWeaponProfiles() adds basePenetration/baseArea itself.
     */
    public AttackProfile toBasicProfile() {
        AttackProfile p = new AttackProfile("Basic Attack", dice, dicePower, power,
                AttackProfile.ProfileTypes.BASIC, ranged);
        p.AmmoCost = ranged ? ammoCost : 0;
        p.MinRange = minRange;
        p.MaxRange = maxRange;
        return p;
    }
}