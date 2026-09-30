package eva.evangelion.items.Weapon;

import eva.evangelion.units.battle.Effect;

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

    /** Effects currently on this weapon (e.g. the Overheat penalty). Applied to profiles when they are created. */
    public List<Effect> effects = new ArrayList<>();

    public List<Effect> getEffects() {
        if (effects == null) effects = new ArrayList<>();   // weapons saved before this field existed
        return effects;
    }

    public void addEffect(Effect e) {
        if (e == null) return;
        if (!e.isStacks() && hasEffect(e.getName())) return;
        getEffects().add(e);
    }

    public boolean hasEffect(String name) {
        for (Effect e : getEffects()) if (e.getName().equals(name)) return true;
        return false;
    }

    public void clearEffects(Effect.EffectEnd end) {
        getEffects().removeIf(e -> e.getEffectEnd() == end);
    }

    /** Weapon effects change the profile's Power (Attack Strength). Called exactly once per freshly built profile. */
    private void applyWeaponEffects(AttackProfile profile) {
        for (Effect e : getEffects()) profile.Power += e.getDeltaAttackStrength();
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

    public boolean hasActivatableTech(){
        return this.getCurrentTech() == Weapon.Tech.N2SHELL
                || this.getCurrentTech() == Weapon.Tech.MASER;
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

      if (!doesNormalProfiles) {
          List<AttackProfile> specials = new ArrayList<>();
          for (AttackProfile p : getSpecialProfiles()) {
              AttackProfile c = p.copy();
              applyWeaponEffects(c);
              specials.add(c);
          }
          return specials;
      }
      List<AttackProfile> profiles = new ArrayList<>();
      profiles.addAll(getNormalProfiles());
      List<AttackProfile> profilesToRemove = new ArrayList<>();
      for (AttackProfile profile : profiles)  {




          if (getBaseArea() != -1 && profile.AreaType == -1) profile.AreaType = getBaseArea(); //set profile to correct area
          profile.Penetration+=getBasePenetration(); //Add basic penetration to profile;

          switch (tech) {
              case POLYTHERMIC, SUPERCONDUCTIVE -> profile.Penetration++;
              case PROGRESSIVE, POSITRON -> profile.Penetration+=2;
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

      for (AttackProfile p : profiles) applyWeaponEffects(p);   // normal profiles, once
      // ADDING ADDITIONAL PROFILES:
      for (Customisation custom : Customisations) {
          switch(custom) {
              case BAYONET -> {}
              case THROWING -> {}
          }
      }
      for (AttackProfile p : getSpecialProfiles()) {            // special profiles, once
          AttackProfile c = p.copy();
          applyWeaponEffects(c);
          profiles.add(c);
      }

      return profiles;
  }

}



