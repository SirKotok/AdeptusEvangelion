package eva.evangelion.state.actions;

import eva.evangelion.items.Weapon.Weapon;

/** DM puts a weapon on the field or into an empty slot of a unit. */
public class DMAddWeaponAction extends Action {
    private static final long serialVersionUID = 1L;

    public enum Placement { FIELD, SLOT }

    private final Placement placement;
    private final Weapon weapon;
    private final int x;
    private final int y;
    private final String targetUnitName;
    private final int slotNumber;

    private DMAddWeaponAction(int actionNumber, String actor, Placement placement, Weapon weapon,
                              int x, int y, String targetUnitName, int slotNumber) {
        super(actionNumber, actor);
        this.placement = placement;
        this.weapon = weapon;
        this.x = x;
        this.y = y;
        this.targetUnitName = targetUnitName;
        this.slotNumber = slotNumber;
    }

    public static DMAddWeaponAction toField(int actionNumber, String actor, Weapon weapon, int x, int y) {
        return new DMAddWeaponAction(actionNumber, actor, Placement.FIELD, weapon, x, y, null, -1);
    }

    public static DMAddWeaponAction toSlot(int actionNumber, String actor, Weapon weapon,
                                           String targetUnitName, int slotNumber) {
        return new DMAddWeaponAction(actionNumber, actor, Placement.SLOT, weapon, -1, -1, targetUnitName, slotNumber);
    }

    public Placement getPlacement() { return placement; }
    public Weapon getWeapon() { return weapon; }
    public int getX() { return x; }
    public int getY() { return y; }
    public String getTargetUnitName() { return targetUnitName; }
    public int getSlotNumber() { return slotNumber; }

    @Override
    public String toString() {
        String w = weapon != null ? weapon.getName() : "null";
        return placement == Placement.FIELD
                ? "DMAddWeaponAction{" + w + " -> field (" + x + "," + y + ")}"
                : "DMAddWeaponAction{" + w + " -> " + targetUnitName + " slot " + slotNumber + "}";
    }
}