package org.classyClanChallenges.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.WeeklyChallenge;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.contribution.ContributionManager;
import org.classyClanChallenges.util.GUIBuilder;
import org.classyclan.ClassyClan;
import org.classyclan.clan.Clan;

import java.util.*;

public class ClassementCommand extends SubCommand implements Listener {

    private static final String GUI_TITLE = "§cᴊᴇᴜх ᴅᴇ ᴄʟᴀɴ - ᴄʟᴀѕѕᴇᴍᴇɴᴛѕ";

    public ClassementCommand() {
        // Enregistrer les événements dans le constructeur ou lors de l'initialisation du plugin
        Bukkit.getPluginManager().registerEvents(this, ClassyClanChallenges.getInstance());
    }

    @Override
    public String getName() {
        return "classement";
    }

    @Override
    public String getDescription() {
        return "Ouvre un menu affichant les classements des clans/joueurs";
    }

    @Override
    public String getUsage() {
        return "/jdc classement";
    }

    @Override
    public boolean isAdminOnly() {
        return false;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSeuls les joueurs peuvent utiliser cette commande.");
            return;
        }

        ClassyClanChallenges plugin = ClassyClanChallenges.getInstance();
        ContributionManager manager = plugin.getContributionManager();
        WeeklyChallenge weekly = plugin.getChallengeManager().getActiveChallenges();

        GUIBuilder gui = new GUIBuilder(GUI_TITLE, 5);

        int[] clanSlots = {11, 13, 15};
        int[] playerSlots = {29, 31, 33};

        int i = 0;
        for (Map.Entry<ChallengeCategory, ChallengeEntry> entry : weekly.getAllChallenges().entrySet()) {
            ChallengeCategory category = entry.getKey();
            ChallengeEntry ce = entry.getValue();

            // === CLANS ===
            ItemStack clanItem = new ItemStack(Material.PAPER);
            ItemMeta clanMeta = clanItem.getItemMeta();
            List<String> clanLore = new ArrayList<>();
            clanMeta.setDisplayName("§eTop Clans - §6" + category.name() + " : " + ce.getTarget());

            List<Map.Entry<UUID, Integer>> topClans = manager.getTopClansByCategory(category);
            for (int j = 0; j < Math.min(topClans.size(), 3); j++) {
                UUID uuid = topClans.get(j).getKey();
                Clan clan = ClassyClan.getAPI().getClanOf(uuid);
                String name = (clan != null) ? clan.getRawName() : "Inconnu";
                clanLore.add("§6#" + (j + 1) + " §f" + name + " §7- §a" + topClans.get(j).getValue());
            }

            Clan playerClan = ClassyClan.getAPI().getClanOf(player.getUniqueId());
            if (playerClan != null) {
                int rank = topClans.stream().map(Map.Entry::getKey).toList().indexOf(playerClan.getOwner()) + 1;
                int points = manager.getClanContribution(playerClan.getOwner()).get(category);
                clanLore.add("");
                clanLore.add("§eTon clan - §7#" + (rank <= 0 ? "?" : rank) + " : §a" + points + " points");
            }

            clanMeta.setLore(clanLore);
            clanItem.setItemMeta(clanMeta);
            gui.setItem(clanSlots[i], clanItem);

            // === JOUEURS ===
            ItemStack playerItem = new ItemStack(Material.WRITTEN_BOOK);
            ItemMeta playerMeta = playerItem.getItemMeta();
            List<String> playerLore = new ArrayList<>();
            playerMeta.setDisplayName("§dTop Joueurs - §5" + category.name() + " : " + ce.getTarget());

            List<Map.Entry<UUID, Integer>> topPlayers = manager.getTopPlayersByCategory(category);
            for (int j = 0; j < Math.min(topPlayers.size(), 3); j++) {
                UUID uuid = topPlayers.get(j).getKey();
                OfflinePlayer top = Bukkit.getOfflinePlayer(uuid);
                playerLore.add("§d#" + (j + 1) + " §f" + top.getName() + " §7- §a" + topPlayers.get(j).getValue());
            }

            int playerRank = topPlayers.stream().map(Map.Entry::getKey).toList().indexOf(player.getUniqueId()) + 1;
            int points = manager.getPlayerContribution(player.getUniqueId()).get(category);
            playerLore.add("");
            playerLore.add("§dToi - §7#" + (playerRank <= 0 ? "?" : playerRank) + " : §a" + points + " points");

            playerMeta.setLore(playerLore);
            playerItem.setItemMeta(playerMeta);
            gui.setItem(playerSlots[i], playerItem);

            i++;
        }

        player.openInventory(gui.build());
    }

    // Événement pour bloquer les clics dans l'inventaire
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Vérifier si c'est notre GUI
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }

        // Annuler tous les clics pour empêcher la duplication/modification
        event.setCancelled(true);

        // Optionnel : Ajouter des actions spécifiques selon le slot cliqué
        // int slot = event.getSlot();
        // Player player = (Player) event.getWhoClicked();
        //
        // switch (slot) {
        //     case 11, 13, 15 -> {
        //         // Actions pour les slots des clans
        //         player.sendMessage("§eVous avez cliqué sur un classement de clan !");
        //     }
        //     case 29, 31, 33 -> {
        //         // Actions pour les slots des joueurs
        //         player.sendMessage("§dVous avez cliqué sur un classement de joueur !");
        //     }
        // }
    }

    // Événement pour bloquer le drag d'items
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // Vérifier si c'est notre GUI
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }

        // Annuler le drag pour empêcher la duplication
        event.setCancelled(true);
    }
}