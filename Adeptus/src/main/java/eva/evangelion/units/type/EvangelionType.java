package eva.evangelion.units.type;

import eva.evangelion.items.Weapon.Weapon;
import eva.evangelion.units.active.Slot;
import eva.evangelion.units.customisation.Upgrade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EvangelionType implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String gameClass;
    private List<Upgrade> upgrades; // only Feature upgrades
    private String primaryColor = "#ffffff";    // new
    private String secondaryColor = "#000000";  // new

    public EvangelionType() {
        this.upgrades = new ArrayList<>();
        this.primaryColor = "#ffffff";
        this.secondaryColor = "#000000";
    }

    private static final List<String> CLASSES = Arrays.asList(
            "Frontliner", "Operator", "Pointman", "Ranger", "Zoner"
    );

    public static List<String> getGameClasses() {
        return CLASSES;
    }


    public List<Slot> Slots = new ArrayList<>();

    public EvangelionType(String name, String gameClass, List<Upgrade> upgrades,
                          String primaryColor, String secondaryColor) {
        this.name = name;
        this.gameClass = gameClass;
        this.upgrades = new ArrayList<>(upgrades);
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;

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
            Weapon test = new Weapon("Weapon"+i, "Knife", Weapon.Hand.ONE_HANDED);
            test.WeaponProperties.add(Weapon.WeaponProperty.SMALL);
            slot.setItem(test);
        }

    }

    public EvangelionType(String name, String gameClass, List<Upgrade> upgrades) {
        this.name = name;
        this.gameClass = gameClass;
        this.upgrades = new ArrayList<>(upgrades);
        this.primaryColor = "#ffffff";
        this.secondaryColor = "#000000";
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGameClass() { return gameClass; }
    public void setGameClass(String gameClass) { this.gameClass = gameClass; }
    public List<Upgrade> getUpgrades() { return upgrades; }
    public void setUpgrades(List<Upgrade> upgrades) { this.upgrades = new ArrayList<>(upgrades); }
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }

    @Override
    public String toString() {
        return "EvangelionType{" +
                "name='" + name + '\'' +
                ", gameClass='" + gameClass + '\'' +
                ", upgrades=" + upgrades.size() +
                ", primaryColor='" + primaryColor + '\'' +
                ", secondaryColor='" + secondaryColor + '\'' +
                '}';
    }
}