package org.classyClanChallenges.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.ChallengeManager;
import org.classyClanChallenges.contribution.ContributionManager;

public class MiningListener implements Listener {

    private final ChallengeManager challengeManager;
    private final ContributionManager contributionManager;

    public MiningListener(ChallengeManager challengeManager, ContributionManager contributionManager) {
        this.challengeManager = challengeManager;
        this.contributionManager = contributionManager;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Vérifie que le bloc n'a pas été placé par un joueur
        if (block.hasMetadata("placed_by_player")) return;

        ChallengeEntry challenge = challengeManager.getActiveChallenges().getChallenge(ChallengeCategory.MINE);
        if (challenge == null) return;

        Material target = Material.matchMaterial(challenge.getTarget());
        if (target == null) return;

        // Si c'est le bon bloc du challenge
        if (block.getType() == target) {
            // Vérifier si c'est dans une zone où on peut donner la progression
            if (!isProtectedArea(player, block)) {
                contributionManager.addContribution(player.getUniqueId(), ChallengeCategory.MINE, 1);
            }
        }
    }

    private boolean isProtectedArea(Player player, Block block) {
        // 1. Vérifier les mondes interdits
        String worldName = block.getWorld().getName();
        if (worldName.equalsIgnoreCase("event") || worldName.equalsIgnoreCase("Boatrace")) {
            return true;
        }

        // 2. Vérifier GriefPrevention
        if (isInGriefPreventionClaim(player, block)) {
            return true;
        }

        // 3. Vérifier WorldGuard
        if (isInWorldGuardProtectedRegion(player, block)) {
            return true;
        }

        return false;
    }

    private boolean isInGriefPreventionClaim(Player player, Block block) {
        try {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), true, null);

            if (claim != null) {
                // Vérifier si le joueur a la permission de casser dans ce claim
                String allowBuild = claim.allowBuild(player, block.getType());
                return allowBuild != null; // Si allowBuild retourne un message, c'est qu'il n'a pas la permission
            }
        } catch (Exception e) {
            // GriefPrevention n'est pas disponible ou erreur
        }
        return false;
    }

    private boolean isInWorldGuardProtectedRegion(Player player, Block block) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();

            com.sk89q.worldedit.util.Location location = BukkitAdapter.adapt(block.getLocation());
            ApplicableRegionSet set = query.getApplicableRegions(location);

            for (ProtectedRegion region : set) {
                String regionId = region.getId().toLowerCase();

                // Vérifier uniquement les noms de régions warzone et spawn
                if (regionId.contains("warzone") || regionId.contains("spawn")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // WorldGuard n'est pas disponible ou erreur
        }
        return false;
    }
}