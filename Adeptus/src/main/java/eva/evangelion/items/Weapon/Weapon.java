package eva.evangelion.items.Weapon;

import java.util.ArrayList;
import java.util.List;

public class Weapon extends Item {

    public boolean ActiveTech = false;
    public void SetActivateTech(boolean activate) {ActiveTech = activate;}
    public boolean isActiveTech() {
        return ActiveTech;
    }
    private int minRange = 0;
    private int maxRange = 1;

    public int getMinRange() { return minRange; }
    public int getMaxRange() { return maxRange; }
    public void setMinRange(int minRange) { this.minRange = Math.max(0, minRange); }
    public void setMaxRange(int maxRange) { this.maxRange = Math.max(1, maxRange); }

    public enum Tech {
        NONE, CHAIN, PROGRESSIVE, POLYTHERMIC, SUPERCONDUCTIVE, GAUSS, N2SHELL, MASER, POSITRON;

        /*
        Chain
Progressive

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
   public boolean doesNormalProfiles;
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


    public Tech getCurrentTech() {
        return Technology.get(0);
    }


   public static Weapon createBasicMeleeWeapon(String name, String type, Tech t, List<Customisation> Customisation, Hand h) {
       Weapon w = new Weapon(name, type, h);
       w.Technology.add(t);
       w.Customisations = Customisation;
       w.Ranged = false;
       w.doesNormalProfiles = true;
       return w;
   }
    public static Weapon createBasicRangedWeapon(String name,
                                                 String type,
                                                 Tech t,
                                                 List<Customisation> Customisation,
                                                 Hand h, int ammo, int minrange, int maxrange) {
        Weapon w = new Weapon(name, type, h);
        w.Technology.add(t);
        w.Customisations = Customisation;
        w.Ranged = true;
        w.doesNormalProfiles = true;
        w.setAmmo(ammo);
        w.setMinRange(minrange);
        w.setMaxRange(maxrange);
        w.setMaxAmmo(ammo);
        return w;
    }

   public List<AttackProfile> getNormalProfiles(){
       if (!doesNormalProfiles) return null;
       List<AttackProfile> profiles = new ArrayList<>();
       profiles.add(AttackProfile.createBasicAttack(this));
       profiles.add(AttackProfile.createBlitzAttack(this));
       if (this.isRanged()) profiles.add(AttackProfile.createFullAutoAttack(this));



       return profiles;
   }

    public List<AttackProfile> getSpecialProfiles(){
        return SpecialProfiles;
    }


  public List<AttackProfile> getWeaponProfiles(Tech tech){

      if (!doesNormalProfiles) return getSpecialProfiles();
      List<AttackProfile> profiles = new ArrayList<>();
      profiles.addAll(getNormalProfiles());
      List<AttackProfile> profilesToRemove = new ArrayList<>();
      for (AttackProfile profile : profiles)  {




          if (getBaseArea() != -1 && profile.AreaType == -1) profile.AreaType = getBaseArea(); //set profile to correct area
          profile.Penetration+=getBasePenetration(); //Add basic penetration to profile;

          switch (tech) { // TODO CHAIN, GAUSS
              case POLYTHERMIC -> {
                  profile.Penetration++;
                 //TODO Polythermic handling {if (profile.Dice*profile.Dicepower < 7) {profile.Dice = 3;} else {profile.Dice = 4; profile.Power++;} profile.Dicepower = 3;}
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
                      if (profile.AreaType > -1) profilesToRemove.add(profile);
                      profile.AreaType = -2;
                      profile.AmmoCost++;
                      profile.Penetration++;
                  }
              }
          }

          for (WeaponProperty property : WeaponProperties) {
              switch (property) { //TODO PENETRATION / LINE / AREA ??? DEFENSIVE // ABLATIVE
                  //TODO remake profile effects
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
                //TODO  case THROWING -> {profile.AttackProperties.add(AttackProfile.AttackProperty.THROWING);} // NEW THROWING PROFILE

                  case ARMORPIERCING -> { profile.AttackProperties.add(AttackProfile.AttackProperty.ARMORPIERCING); // DURING ATTACK
                  }
              }
          }

          profile.MinRange = this.minRange;
          profile.MaxRange = this.maxRange;

          // Reach extends melee reach to 3 sectors
          boolean hasReachProperty = this.WeaponProperties != null
                  && this.WeaponProperties.contains(WeaponProperty.REACH);
          boolean profileHasReach = profile.AttackProperties != null
                  && profile.AttackProperties.contains(AttackProfile.AttackProperty.REACH);

          if (!this.isRanged() && (hasReachProperty || profileHasReach)) {
              profile.MinRange = 1;
              profile.MaxRange = 3;
          }


      }

      profiles.removeAll(profilesToRemove);

      for (AttackProfile profile : profiles)  {

          for (Customisation custom : Customisations) {
              switch(custom) { //TODO BALANCED, EXPLOSIVE, Reinforced, THROWING, Bayonet, Telescopic Sight, Autoloader
                  case ANTI_ARMOR -> profile.Penetration++;
              }
          }
          }
      // ADDING ADDITIONAL PROFILES:
      for (Customisation custom : Customisations) {
          switch(custom) {
              case BAYONET -> {}
              case THROWING -> {}
          }
      }
      for (AttackProfile p : getSpecialProfiles()) profiles.add(p.copy()); //ADDING SPECIAL CUSTOM PROFILES IF THEY EXIST


      return profiles;
  }

}



