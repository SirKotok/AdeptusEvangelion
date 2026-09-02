package eva.evangelion.units.battle;

import eva.evangelion.units.type.EvangelionType;

public class Evangelion extends Unit{
    public final EvangelionType type;
    public Evangelion(EvangelionType type) {
        this.type = type;
    }
    public EvangelionType getType() {
        return type;
    }
}
