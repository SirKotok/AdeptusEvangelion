package eva.evangelion.units.active;

import eva.evangelion.items.Weapon.*; // adjust import if needed

import java.io.Serializable;

public class Slot implements Serializable {

    public final String name;
    // ---- Classifications ----
    private boolean active;   // true = ACTIVE, false = PASSIVE
    private boolean dynamic;  // true = DYNAMIC, false = STATIC
    private boolean intact;   // true = INTACT, false = DAMAGED

    public String getName() {
        return name;
    }


    // ---- Size ----
    public enum Size {SMALL, LARGE, ANY }
    private Size size;

    // ---- Item ----
    private Item item;

    // ---- Constructors ----

    public Slot(String name, boolean active, boolean dynamic, boolean intact, Size size) {
        this.name = name;
        this.active = active;
        this.dynamic = dynamic;
        this.intact = intact;
        this.size = size;
        this.item = null;
    }

    // ---- Getters for classifications (dual states) ----
    public boolean isActive() { return active; }
    public boolean isPassive() { return !active; }

    public boolean isDynamic() { return dynamic; }
    public boolean isStatic() { return !dynamic; }

    public boolean isIntact() { return intact; }
    public boolean isDamaged() { return !intact; }

    // ---- Size ----
    public Size getSize() { return size; }

    // ---- Item ----
    public Item getItem() { return item; }
    public void setItem(Item item) {
        if (canFit(item)) this.item = item; else System.out.println("ITEM DOESNT FIT WTF HOW DID YOU SET IT");
    }

    public boolean hasWeapon() {
        return item instanceof Weapon;
    }
    public boolean hasAmmo(){
        return item instanceof Ammo;
    }

    public boolean canFit(Item item){
        switch(getSize()){
            case ANY -> {
                return true;
            }
            case SMALL -> {
                return item.isSmall();
            }
            case LARGE -> {
                return !item.isSmall();
            }
        }
        return false;
    }

    // ---- Setters for classifications ----
    public void setActive(boolean active) { this.active = active; }
    public void setDynamic(boolean dynamic) { this.dynamic = dynamic; }
    public void setIntact(boolean intact) { this.intact = intact; }
    public void setSize(Size size) { this.size = size; }

    // ---- Convenience methods ----
    public void setPassive() { this.active = false; }
    public void setStatic() { this.dynamic = false; }
    public void setDamaged() { this.intact = false; }

    @Override
    public String toString() {
        return "Slot{" +
                "active=" + active +
                ", dynamic=" + dynamic +
                ", intact=" + intact +
                ", size=" + size +
                ", item=" + item +
                '}';
    }

    public static Slot StorageSlot(String S) {return new Slot(S,false, true, true, Size.SMALL);}
    public static Slot ArmSlot(String S) {return new Slot(S, true, true, true, Size.ANY);}


}

