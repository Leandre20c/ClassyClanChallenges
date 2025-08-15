package org.classyClanChallenges.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
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

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getRecipe().getResult();
        String craftedItem = result.getType().name();

        int amountCrafted = result.getAmount(); // Par défaut : 1

        // ✅ Si shift + clique → calculer combien seront réellement ajoutés à l’inventaire
        if (event.getClick().isShiftClick()) {
            // Estimation du nombre maximal de stacks possibles
            int maxCraftable = calculateMaxCrafts(event);
            amountCrafted = maxCraftable * result.getAmount(); // Total estimé
        }

        // Ajoute les points si l’item est un défi actif
        ClassyClanChallenges plugin = ClassyClanChallenges.getInstance();
        plugin.getContributionManager().addGenericContribution(
                player.getUniqueId(),
                ChallengeCategory.CRAFT,
                craftedItem,
                amountCrafted
        );
    }

    private int calculateMaxCrafts(CraftItemEvent event) {
        // Compte combien de fois on peut crafter la recette en fonction des ingrédients dispos
        int min = Integer.MAX_VALUE;

        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item == null || item.getType().isAir()) continue;
            min = Math.min(min, item.getAmount());
        }

        return min == Integer.MAX_VALUE ? 1 : min;
    }


}
