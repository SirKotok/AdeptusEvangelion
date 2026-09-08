package eva.evangelion.items.Weapon;

import java.io.Serializable;

public class Item implements Serializable {
    public String Name;
    public boolean small = false;
    public String displayIcon = "weapon_0.png";
    public String descriptionDisplay = "DESCRIPTION";
    public Item(String name) {
        Name = name;
    }

    public String getDescription() {
        return descriptionDisplay;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }
    public void setDisplayIcon(String displayIcon) {
        this.displayIcon = displayIcon;
    }

    public String getDisplayIcon() {
        return displayIcon;
    }
    public boolean isSmall() {
        return small;
    }
}
