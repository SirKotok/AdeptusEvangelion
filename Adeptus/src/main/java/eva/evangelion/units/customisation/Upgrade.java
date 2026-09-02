package eva.evangelion.units.customisation;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class Upgrade implements Serializable {

    public enum Type {
        GENERIC,
        FEATURE,
        CLASS
    }

    public enum Tag {
        NO_EFFECT,
        COMPLEX,
        WING,
        WING_REPLACEMENT,
        UPGRADE,
        STATS,
        POST_BATTLE_EFFECT,
        REQUISITION,
        POWER,
        FORCED_ACTION,
        WEAPON,
        PICK,
        ADD_DOOM,
        NERV_PERSONAL
    }

    public enum Stat {
        TOUGHNESS,
        ARMOR,
        REFLEXES,
        ACCURACY,
        SPEED,
        STRENGTH
    }

    private final String name;
    private final String description;
    private final Type type;
    private final EnumSet<Tag> tags;
    private final Map<Stat, Integer> statModifiers;
    private final int fateModifier;
    private final int doomModifier;
    private final int requisitionBonus;
    private final int nervPersonalBonus;
    private final int powerLevel;

    // Protected constructor used by Builder
    protected Upgrade(Builder<?> builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.type = builder.type;
        this.tags = builder.tags.clone();
        this.statModifiers = new HashMap<>(builder.statModifiers);
        this.fateModifier = builder.fateModifier;
        this.doomModifier = builder.doomModifier;
        this.requisitionBonus = builder.requisitionBonus;
        this.nervPersonalBonus = builder.nervPersonalBonus;
        this.powerLevel = builder.powerLevel;
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Type getType() { return type; }
    public EnumSet<Tag> getTags() { return EnumSet.copyOf(tags); }
    public Map<Stat, Integer> getStatModifiers() { return new HashMap<>(statModifiers); }
    public int getFateModifier() { return fateModifier; }
    public int getDoomModifier() { return doomModifier; }
    public int getRequisitionBonus() { return requisitionBonus; }
    public int getNervPersonalBonus() { return nervPersonalBonus; }
    public int getPowerLevel() { return powerLevel; }

    // ---------- Builder ----------
    public static class Builder<T extends Builder<T>> {
        private final String name;
        private final String description;
        private final Type type;
        private final EnumSet<Tag> tags = EnumSet.noneOf(Tag.class);
        private final Map<Stat, Integer> statModifiers = new HashMap<>();
        private int fateModifier = 0;
        private int doomModifier = 0;
        private int requisitionBonus = 0;
        private int nervPersonalBonus = 0;
        private int powerLevel = 0;

        public Builder(String name, String description, Type type) {
            this.name = name;
            this.description = description;
            this.type = type;
        }

        // Self-type for chaining in subclasses
        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        public T addTag(Tag tag) {
            tags.add(tag);
            return self();
        }

        public T addTags(Tag... tags) {
            for (Tag t : tags) this.tags.add(t);
            return self();
        }

        public T stat(Stat stat, int value) {
            statModifiers.put(stat, value);
            return self();
        }

        public T fate(int value) {
            this.fateModifier = value;
            return self();
        }

        public T doom(int value) {
            this.doomModifier = value;
            return self();
        }

        public T requisition(int value) {
            this.requisitionBonus = value;
            return self();
        }

        public T nervPersonal(int value) {
            this.nervPersonalBonus = value;
            return self();
        }

        public T powerLevel(int level) {
            this.powerLevel = level;
            return self();
        }

        // build() returns Upgrade – subclasses can override with covariant return
        public Upgrade build() {
            return new Upgrade(this);
        }
    }
}