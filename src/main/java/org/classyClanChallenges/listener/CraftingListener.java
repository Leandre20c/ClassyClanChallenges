package org.classyClanChallenges.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.ChallengeManager;
import org.classyClanChallenges.challenges.WeeklyChallenge;
import org.classyClanChallenges.contribution.ContributionManager;

public class CraftingListener implements Listener {

    private final ChallengeManager challengeManager;
    private final ContributionManager contributionManager;

    public CraftingListener(ChallengeManager challengeManager, ContributionManager contributionManager) {
        this.challengeManager = challengeManager;
        this.contributionManager = contributionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraft(CraftItemEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getRecipe().getResult();
        String craftedItem = result.getType().name();

        // Vérifier si c'est un défi actif
        WeeklyChallenge weekly = challengeManager.getActiveChallenges();
        ChallengeEntry challenge = weekly.getChallenge(ChallengeCategory.CRAFT);
        if (challenge == null || !challenge.getTarget().equalsIgnoreCase(craftedItem)) {
            return;
        }

        // SOLUTION : Vérifier si l'inventaire peut accueillir l'item AVANT de donner les points
        int amountThatWillBeCrafted = calculateActualCraftAmount(event, player);

        // Si rien ne sera réellement crafté, ne pas donner de points
        if (amountThatWillBeCrafted <= 0) {
            return;
        }

        // Donner les points seulement pour ce qui sera réellement crafté
        ClassyClanChallenges plugin = ClassyClanChallenges.getInstance();
        plugin.getContributionManager().addGenericContribution(
                player.getUniqueId(),
                ChallengeCategory.CRAFT,
                craftedItem,
                amountThatWillBeCrafted
        );
    }

    /**
     * Calcule combien d'items seront RÉELLEMENT craftés en tenant compte de l'inventaire
     */
    private int calculateActualCraftAmount(CraftItemEvent event, Player player) {
        ItemStack result = event.getRecipe().getResult();

        if (event.getAction() == InventoryAction.PICKUP_ALL) {
            // Clic normal (pas shift) - craft 1 fois seulement
            if (canAddToInventory(player, result)) {
                return result.getAmount();
            } else {
                return 0; // Inventaire plein
            }
        }
        else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // Shift + clic - craft autant que possible
            return calculateMaxShiftCraft(event, player, result);
        }

        return 0;
    }

    /**
     * Calcule combien d'items peuvent être craftés avec shift+clic
     */
    private int calculateMaxShiftCraft(CraftItemEvent event, Player player, ItemStack result) {
        // 1. Calculer combien de fois on peut crafter selon les ingrédients
        int maxCraftFromIngredients = calculateMaxCraftsFromIngredients(event);

        // 2. Calculer combien d'items l'inventaire peut accueillir
        int maxItemsInventoryCanHold = calculateMaxItemsInventoryCanHold(player, result);

        // 3. Le minimum des deux
        int maxCrafts = Math.min(maxCraftFromIngredients, maxItemsInventoryCanHold / result.getAmount());

        return Math.max(0, maxCrafts * result.getAmount());
    }

    /**
     * Calcule combien de fois on peut crafter selon les ingrédients disponibles
     */
    private int calculateMaxCraftsFromIngredients(CraftItemEvent event) {
        int minStack = Integer.MAX_VALUE;

        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ingredient != null && !ingredient.getType().isAir()) {
                minStack = Math.min(minStack, ingredient.getAmount());
            }
        }

        return minStack == Integer.MAX_VALUE ? 0 : minStack;
    }

    /**
     * Calcule combien d'items l'inventaire peut encore accueillir
     */
    private int calculateMaxItemsInventoryCanHold(Player player, ItemStack itemToAdd) {
        int canHold = 0;
        Material material = itemToAdd.getType();
        int maxStackSize = material.getMaxStackSize();

        // Vérifier tous les slots de l'inventaire principal (0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack slot = player.getInventory().getItem(i);

            if (slot == null || slot.getType().isAir()) {
                // Slot vide - peut tenir un stack complet
                canHold += maxStackSize;
            } else if (slot.getType() == material && slot.getAmount() < maxStackSize) {
                // Slot avec le même item - peut en tenir plus
                canHold += (maxStackSize - slot.getAmount());
            }
        }

        return canHold;
    }

    /**
     * Vérifie si un item peut être ajouté à l'inventaire
     */
    private boolean canAddToInventory(Player player, ItemStack item) {
        return calculateMaxItemsInventoryCanHold(player, item) >= item.getAmount();
    }
}