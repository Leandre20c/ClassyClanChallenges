package org.classyClanChallenges.listener;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.classyClanChallenges.ClassyClanChallenges;

import java.util.HashSet;
import java.util.Set;

public class MenuListener implements Listener {

    private static final Set<Inventory> lockedInventories = new HashSet<>();

    public static void registerLockedInventory(Inventory inventory) {
        lockedInventories.add(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        ItemStack item = event.getCurrentItem();

        if (inv == null || item == null || !item.hasItemMeta()) return;

        if (lockedInventories.contains(inv)) {
            event.setCancelled(true);

            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            // Clic sur le bouton classement
            if (meta.getPersistentDataContainer().has(
                    new NamespacedKey(ClassyClanChallenges.getInstance(), "jdc_classement"),
                    PersistentDataType.INTEGER)) {
                if (event.getWhoClicked() instanceof Player player) {
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(ClassyClanChallenges.getInstance(), () ->
                            player.performCommand("jdc classement"), 2L);
                }
            }

            // Tu peux ajouter d'autres boutons ici si besoin
        }
    }
}
