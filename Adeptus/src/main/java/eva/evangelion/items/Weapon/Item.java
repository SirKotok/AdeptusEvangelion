package eva.evangelion.items.Weapon;

import java.io.Serializable;

public class Item implements Serializable {
    public String Name;
    public boolean small = false;

    public Item(String name) {
        Name = name;
    }

    public boolean isSmall() {
        return small;
    }
}
