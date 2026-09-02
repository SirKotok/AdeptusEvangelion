package eva.evangelion.units.customisation;

public class Feature extends Upgrade {

    public enum FeatureType {
        HISTORY,
        EXPERIMENTAL,
        CONSTRUCTION,
        MUTATION,
        COSMETIC
    }

    private final FeatureType featureType;
    private final int startNumber;
    private final int endNumber;

    // Constructor used by FeatureBuilder
    protected Feature(FeatureBuilder builder) {
        super(builder);                     // ✅ calls Upgrade's protected constructor
        this.featureType = builder.featureType;
        this.startNumber = builder.startNumber;
        this.endNumber = builder.endNumber;
    }

    // Getters
    public FeatureType getFeatureType() { return featureType; }
    public int getStartNumber() { return startNumber; }
    public int getEndNumber() { return endNumber; }

    // ---------- FeatureBuilder (extends Upgrade.Builder) ----------
    public static class FeatureBuilder extends Upgrade.Builder<FeatureBuilder> {
        private final FeatureType featureType;
        private final int startNumber;
        private final int endNumber;

        public FeatureBuilder(String name, String description, FeatureType featureType,
                              int startNumber, int endNumber) {
            super(name, description, Type.FEATURE);
            this.featureType = featureType;
            this.startNumber = startNumber;
            this.endNumber = endNumber;
        }

        @Override
        public Feature build() {            // ✅ returns Feature, not Upgrade
            return new Feature(this);       // ✅ calls Feature constructor
        }
    }

    @Override
    public String toString() {
        return "Feature{" +
                "featureType=" + featureType +
                ", range=" + startNumber + "-" + endNumber +
                ", name='" + getName() + '\'' +
                ", tags=" + getTags() +
                ", statModifiers=" + getStatModifiers() +
                ", fateModifier=" + getFateModifier() +
                ", doomModifier=" + getDoomModifier() +
                ", requisitionBonus=" + getRequisitionBonus() +
                ", nervPersonalBonus=" + getNervPersonalBonus() +
                ", powerLevel=" + getPowerLevel() +
                '}';
    }
}