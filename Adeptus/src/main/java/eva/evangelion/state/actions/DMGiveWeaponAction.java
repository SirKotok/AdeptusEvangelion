package eva.evangelion.state.actions;

import eva.evangelion.items.Weapon.Weapon;

public class DMGiveWeaponAction extends Action {

    private final String targetUnitName;
    private final int    slotNumber;
    private final Weapon weapon;

    public DMGiveWeaponAction(int actionNumber, String actor,
                              String targetUnitName, int slotNumber, Weapon weapon) {
        super(actionNumber, actor);
        this.targetUnitName = targetUnitName;
        this.slotNumber    = slotNumber;
        this.weapon        = weapon;
    }

    public String getTargetUnitName() { return targetUnitName; }
    public int    getSlotNumber()     { return slotNumber; }
    public Weapon getWeapon()         { return weapon; }

    @Override
    public String toString() {
        return "DMGiveWeaponAction{target=" + targetUnitName
                + ", slot=" + slotNumber
                + ", weapon=" + (weapon == null ? "null" : weapon.getName()) + "}";
    }
}