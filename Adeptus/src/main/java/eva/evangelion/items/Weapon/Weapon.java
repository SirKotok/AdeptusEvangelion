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
        REACH, CQB, SWIFT, SMALL, SPRAY, THROWING, ARMORPIERCING
    }

   public List<Customisation> Customisations = new ArrayList<>();
   public List<WeaponProperty> WeaponProperties = new ArrayList<>();
   public List<AttackProfile> SpecialProfiles = new ArrayList<>();
   public boolean doesBasicProfiles;
   public List<Tech> Technology = new ArrayList<>();

    public String getProfileType() {
        return ProfileType;
    }

   public Hand hands;
   public boolean Ranged;
   public boolean isRanged() {
        return Ranged;
    }
   public void setRanged(boolean ranged) {
        Ranged = ranged;
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



