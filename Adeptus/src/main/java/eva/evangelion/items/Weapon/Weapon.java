package eva.evangelion.items.Weapon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Weapon extends Item {

    public boolean ActiveTech = false;
    public void SetActivateTech(boolean activate) {ActiveTech = activate;}
    public boolean isActiveTech() {
        return ActiveTech;
    }


    public enum Tech {
        NONE, CHAIN, PROGRESSIVE, POLYTHERMIC, SUPERCONDUCTIVE, GAUSS, N2SHELL, MASER, POSITRON;

        /*
        Chain
Chain weapons use thousands of blades to rip angels apart.  You deal 1 Strain per DoS on your Attack Test with a Chain Weapon (Maximum 3 Strain).  This is applied even if your target successfully Guards.
Progressive
Progressive weapons use sonic vibrations to enhance their cutting power or impact.  You have +2 Penetration with a Progressive Weapon.
Polythermic
Polythermic weapons use superheated ceramic to burn flesh.  You have +1 Penetration with a Polythermic Weapon.  When you roll damage with a Polythermic Weapon, you can choose to Overheat it, changing your weapon’s damage dice for this attack according to the chart below.  After using this ability, you take a -2 penalty to damage on your next attack with this weapon.  Angels instead take the Damage Penalty for 1 round
Base Damage
New Damage
1d6 or 2d3
3d3
1d10 or 2d6
4d3+1


Superconductive
Superconductive weapons electrify the target, momentarily stunning them.  You have +1 Penetration with a
Superconductive weapon.  If you deal damage with a Superconductive weapon, you can choose to apply a -10 Penalty to the target’s next Attack Test or a -10 Penalty to the target’s next Guard attempt.  Multiple applications of the same penalty do not stack.
Ranged Weapon Technologies
Gauss
Gauss Weapons use electromagnetism to launch powerful rounds at hypersonic velocity.  You deal an additional +1 damage per DoS on your attack test (maximum +4 Damage).
N2 Shell
N2 Shell Weapons detonate with an anti-matter explosion.  Attacks you make with a N2 Weapon have the Area (0) Property.  If the weapon or attack is already Area, you instead gain +1 damage.  You can spend 1 extra Ammo when attacking with an N2 Weapon to increase the Area rating of this weapon by 1.
Maser
Maser Weapons fire concentrated beams of microwave energy.  Maser Weapons have +1 Penetration.  By spending 1 extra Ammo, your attack gains the Line property and an additional +1 Penetration.
Positron
Positron Weapons pierce all manner of protections with ease.  Positron Weapons have +2 Penetration.


        *  */

        public Tech nextTech() {
            Tech[] values = Tech.values();
            int nextIndex = (this.ordinal() + 1) % values.length;
            return values[nextIndex];
        }

        public Tech nextTechNoNone() {
            Tech[] values = Tech.values();
            int shifted = (this.ordinal() - 1); // Shift to exclude NONE
            int nextShifted = (shifted + 1) % 8; // 8 non-NONE values
            int nextIndex = nextShifted + 1;
            return values[nextIndex];
        }

        private static final Tech[] MELEE_CYCLE = {
                NONE, CHAIN, PROGRESSIVE, POLYTHERMIC, SUPERCONDUCTIVE
        };

        private static final Tech[] MELEE_NO_NONE_CYCLE = {
                CHAIN, PROGRESSIVE, POLYTHERMIC, SUPERCONDUCTIVE
        };

        private static final Tech[] RANGED_CYCLE = {
                NONE, GAUSS, N2SHELL, MASER, POSITRON
        };

        private static final Tech[] RANGED_NO_NONE_CYCLE = {
                GAUSS, N2SHELL, MASER, POSITRON
        };



        // MELEE METHODS
        public Tech nextTechMelee() {
            return nextInCycle(MELEE_CYCLE);
        }

        public Tech nextTechMeleeNoNone() {
            return nextInCycle(MELEE_NO_NONE_CYCLE);
        }

        // RANGED METHODS
        public Tech nextTechRanged() {
            return nextInCycle(RANGED_CYCLE);
        }

        public Tech nextTechRangedNoNone() {
            return nextInCycle(RANGED_NO_NONE_CYCLE);
        }

        // Helper method to handle cycle logic
        private Tech nextInCycle(Tech[] cycle) {
            for (int i = 0; i < cycle.length; i++) {
                if (cycle[i] == this) {
                    return cycle[(i + 1) % cycle.length];
                }
            }
            return cycle[0]; // Default to first element if not found
        }

    }
    public boolean isSmall() {
        return WeaponProperties.contains(WeaponProperty.SMALL);
    }

    public String ProfileType;
    public enum Customisation {
        ANTI_ARMOR, BALANCED, DOUBLE_EDGED, EXPLOSIVE, EXTRA_AMMO, REINFORCED, THROWING, BAYONET, ENHANCED_BAYONET, TELESCOPIC_SIGHT, AUTO_LOADER
    }
    // Anti armor increased penetration by 1
    // Balanced increased defensive by 10
    // double edged allows a second technology to be determined at turn start
    // explosive - MELEE ONLY - gives a special attack
    // Extra ammo - RANGED ONLY gives more ammo based on original ammo
    // Reinforced
    // Throwing - MELEE ONLY - gives a special attack
    // Bayonet - RANGED ONLY - gives a special attack
    // Enhanced Bayonet - RANGED ONLY - gives a special attack  (but you may apply a melee weapon technology to the Knife profile), cost 2 requisition
    // Telescopic Sight - RANGED ONLY (modification)
    // Autoloader - RANGED ONLY (modification)
    /*


   Anti-Armor
Increase your Penetration by +1
Balanced
Melee Weapons only.  This weapon gains Defensive 10.
Double Edged
Melee Weapons only, and you must first apply a Weapon Technology at its normal cost for you in order to gain any benefit from this Customization.  You gain a second Technology which you can apply to your Weapon.  If your weapon is Progressive or Chain, you may choose from the Polythermic or Superconductive Technologies.  Alternatively, if your weapon is Polythermic or Superconductive, you can instead choose either the Progressive or Chain Technologies.  At the start of your turn, you decide which Technology to use for each Double Edged Weapon you are wielding—this choice remains in place until the start of your next turn.
Explosive
Melee Weapons only.  This Weapon gains 1 Ammo.  When you deal damage with this weapon, you can choose to expend Ammo to forgo rolling and instead deal maximum Damage.  Explosive Weapons cannot be reloaded.
Extra Ammo
Ranged Weapons only.  If the weapon has 5 or more Ammo capacity, it gains +2 Ammo, otherwise it gains +1 Ammo.
Reinforced
This Customization can only be applied to Shields.  Increase the Ablative Value of the Shield by 1.  This may be purchased twice, for a total Ablative Value of 3.
Throwing
Melee Weapons only.  The Weapon gains the Throwing property.  If your weapon is already Throwing, you extend its Range by 1 and the weapon returns to you on a Miss or the target’s successful Guard.
Bayonet
Ranged Weapons only.  This weapon incorporates a Knife that can make melee attacks.  The Knife cannot be independently upgraded nor have a Technology applied.  This Customization does not change how many hands it takes to wield the weapon.
Enhanced Bayonet
Ranged Weapons only.  As Bayonet, but you may apply a melee weapon technology to the Knife profile.  This Customization costs 2 Requisition.
Telescopic Sight
Ranged Weapons only.  When attacking an enemy at Range 3 or greater, your weapon gains Precise if it wasn’t Precise already.  If your weapon is already Precise, you instead gain +1 Damage on all Attack Actions you make at Range 3 or greater with this weapon, except for Blitz and Full Auto.
Autoloader
Ranged Weapons only.  Your weapon gains Spray if it did not have Spray already when making attacks at Range 3 or closer.  If your weapon already has Spray, once per Battle you can make a Blitz or Full Auto Attack with this weapon without expending Ammo.  This cannot be applied to weapons which have the Area Property.


     */


    public enum Hand {
        ONE_HANDED,
        TWO_HANDED,
        NONE
    }
    public enum WeaponProperty {
        GRAPPLE, INTRINSIC, PRECISE, PROVEN,
        REACH, CQB, SWIFT, SMALL, SPRAY, THROWING, ARMORPIERCING, AREA, LINE
    }

   public List<Customisation> Customisations = new ArrayList<>();
   public List<WeaponProperty> WeaponProperties = new ArrayList<>();
   public List<AttackProfile> SpecialProfiles = new ArrayList<>();
   public boolean doesBasicProfiles;
   public List<Tech> Technology = new ArrayList<>();

    public String getProfileType() {
        return ProfileType;
    }
    private int basePenetration = 0;
    private int baseArea = -1;   // -1 = none, -2 = line, >=0 area value

    public int getBasePenetration() {
        return basePenetration;
    }

    public void setBasePenetration(int basePenetration) {
        this.basePenetration = basePenetration;
    }

    public int getBaseArea() {
        return baseArea;
    }

    public void setBaseArea(int baseArea) {
        this.baseArea = baseArea;
    }
   public Hand hands;
   public boolean Ranged;
   public boolean isRanged() {
        return Ranged;
    }
   public void setRanged(boolean ranged) {
        Ranged = ranged;
    }


   public int defensive;

    public int getDefensive() {
        return defensive;
    }

    public void setDefensive(int defensive) {
        this.defensive = defensive;
    }

    public int Ammo;
   public int maxAmmo;

   public int getAmmo() {
       return Ammo;
   }

   public int getMaxAmmo() {
       return maxAmmo;
   }

   public void setAmmo(int ammo) {
        Ammo = ammo;
   }

   public void setMaxAmmo(int maxAmmo) {
       this.maxAmmo = maxAmmo;
   }


   public Weapon(String name, String type, Hand h){
       super(name);
       ProfileType = type;
       hands = h;
   }

    public Hand getHands() {
        return hands;
    }

    public Tech CurrentTech = Tech.NONE;

   public Weapon createBasicMeleeWeapon(String name, String type, Tech t, List<Customisation> Customisation, Hand h) {
       Weapon w = new Weapon(name, type, h);
       w.Technology.add(t);
       w.Customisations = Customisation;
       w.Ranged = false;
       doesBasicProfiles = true;
       return w;
   }
    public Weapon createBasicRangedWeapon(String name, String type, Tech t, List<Customisation> Customisation, int ammo, Hand h) {
        Weapon w = new Weapon(name, type, h);
        w.Technology.add(t);
        w.Customisations = Customisation;
        w.Ranged = true;
        doesBasicProfiles = true;
        setAmmo(ammo);
        setMaxAmmo(ammo);
        return w;
    }

   public List<AttackProfile> getBasicProfiles(){
       if (!doesBasicProfiles) return null;
       List<AttackProfile> profiles = new ArrayList<>();
       profiles.add(AttackProfile.createBasicAttack(this));
       profiles.add(AttackProfile.createBlitzAttack(this));
       if (this.isRanged()) profiles.add(AttackProfile.createFullAutoAttack(this));
       return profiles;
   }


  public List<AttackProfile> getSpecialProfiles(){
       return SpecialProfiles;
  }

  public List<AttackProfile> getWeaponProfiles(){
      if (!doesBasicProfiles) return getSpecialProfiles();
      List<AttackProfile> profiles = getSpecialProfiles();
      profiles.addAll(getBasicProfiles());
      for (AttackProfile profile : profiles)  {
          if (getBaseArea() != -1 && profile.AreaType == -1) profile.AreaType = getBaseArea(); //set profile to correct area
          profile.Penetration+=getBasePenetration(); //Add basic penetration to profile;

          switch (CurrentTech) { // TODO CHAIN, GAUSS
              case POLYTHERMIC -> {
                  profile.Penetration++;
                  if (isActiveTech()) {if (profile.Dice*profile.Dicepower < 7) {profile.Dice = 3;} else {profile.Dice = 4; profile.Power++;} profile.Dicepower = 3;}
              }
              case PROGRESSIVE, POSITRON -> profile.Penetration+=2;
              case SUPERCONDUCTIVE -> profile.Penetration+=1;
              case N2SHELL -> {
                  if (profile.AreaType > -1) {
                   profile.Power++;
                  } else profile.AreaType = 0;
                  if (isActiveTech()) {
                    profile.AreaType++;
                    profile.AmmoCost++;
                  }
              }
              case MASER -> {
                  profile.Penetration++;
                  if (isActiveTech()) {
                      profile.AreaType = -2;
                      profile.AmmoCost++;
                      profile.Penetration++;
                  }
              }
          }

          for (WeaponProperty property : WeaponProperties) {
              switch (property) { //TODO PENETRATION / LINE / AREA ??? DEFENSIVE // ABLATIVE
                  case GRAPPLE -> { profile.AttackProperties.add(AttackProfile.AttackProperty.GRAPPLE); // COMPLICATED
                  }
                  case INTRINSIC -> {profile.AttackProperties.add(AttackProfile.AttackProperty.INTRINSIC); // NONE
                  }
                  case PRECISE -> { profile.AttackProperties.add(AttackProfile.AttackProperty.PRECISE); //DURING ATTACK
                  }
                  case PROVEN -> {profile.AttackProperties.add(AttackProfile.AttackProperty.PROVEN); //DURING ATTACK
                  }
                  case REACH -> {profile.AttackProperties.add(AttackProfile.AttackProperty.REACH); //CHANGES ATTACK PROFILE
                  }
                  case CQB -> {profile.AttackProperties.add(AttackProfile.AttackProperty.CQB); //PREDICATE CHANGE
                  }
                  case SWIFT -> {profile.AttackProperties.add(AttackProfile.AttackProperty.SWIFT); // CHANGES BLITZ
                  }
                  case SMALL -> {profile.AttackProperties.add(AttackProfile.AttackProperty.SMALL); // NONE
                  }
                  case SPRAY -> {profile.AttackProperties.add(AttackProfile.AttackProperty.SPRAY); // CHANGES BLITZ / FO
                  }
                  case THROWING -> {profile.AttackProperties.add(AttackProfile.AttackProperty.THROWING); // NEW THROWING PROFILE
                  }
                  case ARMORPIERCING -> { profile.AttackProperties.add(AttackProfile.AttackProperty.ARMORPIERCING); // DURING ATTACK
                  }
              }
          }
      }

      for (AttackProfile profile : profiles)  {

          for (Customisation custom : Customisations) {
              switch(custom) { //TODO BALANCED, EXPLOSIVE, Reinforced, THROWING, Bayonet, Telescopic Sight, Autoloader
                  case ANTI_ARMOR -> profile.Penetration++;
              }
          }
          }




      return profiles;
  }

}



