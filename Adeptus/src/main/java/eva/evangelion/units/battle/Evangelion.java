package eva.evangelion.units.battle;

import eva.evangelion.items.Weapon.Item;
import eva.evangelion.items.Weapon.Weapon;
import eva.evangelion.units.active.Slot;
import eva.evangelion.units.type.EvangelionType;

import java.util.ArrayList;
import java.util.List;

public class Evangelion extends Unit{
    public final EvangelionType type;
    public List<Slot> Slots = new ArrayList<>();



    public Evangelion(EvangelionType type) {
        this.type = type;
        addSlotsBasedOnType(this.type);
    }

       // Sectors moved per Run


    public void addSlotsBasedOnType(EvangelionType type){
        Slots.add(Slot.ArmSlot("Left Arm"));
        Slots.add(Slot.ArmSlot("Right Arm"));
        Slots.add(Slot.StorageSlot("Left Storage"));
        Slots.add(Slot.StorageSlot("Right Storage"));
        Slots.add(Slot.StorageSlot("A"));
        Slots.add(Slot.StorageSlot("B"));
        Slots.add(Slot.ArmSlot("Third Arm"));
        int i = 0;
        for (Slot slot : Slots) {
            i++;
            Weapon test = Weapon.createBasicRangedWeapon("weapon #"+i, "Knife", Weapon.Tech.NONE, new ArrayList<>(), Weapon.Hand.ONE_HANDED, 4, 2 ,5);
            test.WeaponProperties.add(Weapon.WeaponProperty.SMALL);
            slot.setItem(test);
        }
    }







    public EvangelionType getType() {
        return type;
    }
    public List<Slot> getSlots(){
        return Slots;
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




    @Override
    public String toString() {
        return "Evangelion{" +
                "type=" + (type != null ? type.getName() : "null") +
                ", accuracy=" + accuracy +
                ", S=" + attackStrength +
                ", toughness=" + toughness+"/"+maxtoughness+
                ", armor=" + armor +
                ", reflexes=" + reflexes +
                ", speed=" + speed;
    }



}
