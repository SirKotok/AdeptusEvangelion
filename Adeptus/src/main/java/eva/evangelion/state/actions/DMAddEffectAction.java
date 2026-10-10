package eva.evangelion.state.actions;

import eva.evangelion.units.battle.Effect;

/** DM adds a custom effect to a unit. */
public class DMAddEffectAction extends Action {
    private static final long serialVersionUID = 1L;

    private final String targetUnitName;
    private final Effect effect;

    public DMAddEffectAction(int actionNumber, String actor, String targetUnitName, Effect effect) {
        super(actionNumber, actor);
        this.targetUnitName = targetUnitName;
        this.effect = effect;
    }

    public String getTargetUnitName() { return targetUnitName; }
    public Effect getEffect() { return effect; }

    @Override
    public String toString() {
        return "DMAddEffectAction{" + (effect != null ? effect.getName() : "null") + " -> " + targetUnitName + "}";
    }
}