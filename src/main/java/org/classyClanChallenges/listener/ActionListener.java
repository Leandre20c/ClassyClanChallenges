package org.classyClanChallenges.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.util.Vector;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.contribution.ContributionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionListener implements Listener {

    private final ContributionManager contributionManager = ClassyClanChallenges.getInstance().getContributionManager();
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    @EventHandler
    public void onJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Vector from = event.getFrom().toVector();
        Vector to = event.getTo().toVector();

        if (to.getY() > from.getY() && player.getVelocity().getY() > 0.1 && player.isOnGround()) {
            ChallengeEntry challenge = getChallenge("JUMP");
            if (challenge != null) {
                contributionManager.addContribution(player.getUniqueId(), ChallengeCategory.ACTION, 1);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        double distance = event.getFrom().distance(event.getTo());
        if (distance <= 0) return;

        ChallengeEntry challenge = getActiveChallenge();
        if (challenge == null) return;

        String target = challenge.getTarget();
        if (target.equalsIgnoreCase("WALK") && !player.isSprinting() && !player.isGliding()) {
            contributionManager.addContribution(player.getUniqueId(), ChallengeCategory.ACTION, 1);
        }

        else if (target.equalsIgnoreCase("RUN")
                && player.isSprinting()
                && !player.isGliding()) {
            contributionManager.addContribution(player.getUniqueId(), ChallengeCategory.ACTION, 1);
        }

        else if (target.equalsIgnoreCase("SWIM") && player.isSwimming()) {
            contributionManager.addContribution(player.getUniqueId(), ChallengeCategory.ACTION, 1);
        }

        else if (target.equalsIgnoreCase("SNEAK") && player.isSneaking() && player.isOnGround() && !player.isGliding()) {
            contributionManager.addContribution(player.getUniqueId(), ChallengeCategory.ACTION, 1);
        }
    }

    @EventHandler
    public void onElytra(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        ChallengeEntry challenge = getChallenge("ELYTRA_FLY");
        if (challenge != null &&
                player.getInventory().getChestplate() != null &&
                player.getInventory().getChestplate().getType() == Material.ELYTRA) {
            contributionManager.addContribution(player.getUniqueId(), ChallengeCategory.ACTION, 1);
        }
    }

    @EventHandler
    public void onTrade(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory() instanceof MerchantInventory)) return;
        if (event.getSlot() != 2) return;

        ItemStack traded = event.getCurrentItem();
        if (traded == null || traded.getType().isAir()) return;

        int amount = traded.getAmount(); // ✅ nombre réel obtenu

        ChallengeEntry challenge = getChallenge("TRADE");
        if (challenge != null) {
            contributionManager.addContribution(player.getUniqueId(), ChallengeCategory.ACTION, amount);
        }
    }


    @EventHandler
    public void onRide(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getVehicle() == null) return;

        UUID uuid = player.getUniqueId();
        Location current = player.getLocation();
        Location last = lastLocations.get(uuid);

        // Vérifie s'il a changé de bloc (donc il a bougé)
        if (last != null && last.getBlockX() == current.getBlockX()
                && last.getBlockY() == current.getBlockY()
                && last.getBlockZ() == current.getBlockZ()) {
            return; // il est sur place
        }

        lastLocations.put(uuid, current); // met à jour

        // Défi actif
        ChallengeEntry challenge = getActiveChallenge();
        if (challenge == null) return;

        String target = challenge.getTarget();
        if ((target.equalsIgnoreCase("RIDE_HORSE") && player.getVehicle() instanceof AbstractHorse) ||
                (target.equalsIgnoreCase("RIDE_BOAT") && player.getVehicle() instanceof Boat)) {
            contributionManager.addContribution(uuid, ChallengeCategory.ACTION, 1);
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ChallengeEntry challenge = getChallenge("SHOOT_BOW");
        if (challenge != null) {
            contributionManager.addContribution(player.getUniqueId(), ChallengeCategory.ACTION, 1);
        }
    }

    private ChallengeEntry getChallenge(String targetName) {
        ChallengeEntry challenge = getActiveChallenge();
        return (challenge != null && challenge.getTarget().equalsIgnoreCase(targetName)) ? challenge : null;
    }

    private ChallengeEntry getActiveChallenge() {
        return ClassyClanChallenges.getInstance()
                .getChallengeManager()
                .getActiveChallenges()
                .getChallenge(ChallengeCategory.ACTION);
    }
}
