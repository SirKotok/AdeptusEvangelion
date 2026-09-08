package eva.evangelion.units.battle;

import eva.evangelion.items.Weapon.Item;
import eva.evangelion.units.active.Slot;
import eva.evangelion.units.type.EvangelionType;

import java.util.List;

public class Evangelion extends Unit{
    public final EvangelionType type;
    public Evangelion(EvangelionType type) {
        this.type = type;
    }
    public EvangelionType getType() {
        return type;
    }
    public List<Slot> getSlots(){
        return type.Slots;
    }
    protected Item itemFromSlot(Slot slot) {
        return slot.getItem();
    }

    protected void setSlotToItem(Item item, Slot slot) {
        slot.setItem(item);
    }

    public Item getRightHandItem(){
       return rightArm().getItem();
    }
    public Item getLightHandItem(){
        return lefttArm().getItem();
    }

    public Item getItemFromSlot(int i){
        return itemFromSlot(getSlots().get(i));
    }

    public void setItemToSlot(Item item, int i){
        setSlotToItem(item, getSlots().get(i));
    }


    public Slot rightBaseSlot(){
        return getSlots().get(3);
    }
    public Slot leftBaseSlot(){
        return getSlots().get(2);
    }
    public Slot lefttArm(){
        return getSlots().get(0);
    }
    public Slot rightArm(){
        return getSlots().get(1);
    }

}
