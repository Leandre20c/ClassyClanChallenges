package org.classyClanChallenges.util;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.classyClanChallenges.listener.MenuListener;

public class GUIBuilder {
    private final Inventory inventory;
    private boolean cancelClicks = false;

    public GUIBuilder(String title, int rows) {
        this.inventory = Bukkit.createInventory(null, rows * 9, title);
    }

    public void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    public Inventory build() {
        if (cancelClicks) {
            MenuListener.registerLockedInventory(inventory);
        }
        return inventory;
    }


    public boolean shouldCancelClicks() {
        return cancelClicks;
    }

    public void setCancelClicks(boolean cancelClicks) {
        this.cancelClicks = cancelClicks;
    }
}
