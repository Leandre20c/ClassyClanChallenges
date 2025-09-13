package org.classyClanChallenges.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.contribution.ContributionManager;
import org.classyclan.ClassyClan;
import org.classyclan.clan.Clan;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TopCommand extends SubCommand {

    @Override
    public String getName() {
        return "top";
    }

    @Override
    public String getDescription() {
        return "Affiche les classements détaillés.";
    }

    @Override
    public String getUsage() {
        return "/jdc top [global|catégorie] [players|clans]";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ContributionManager manager = ClassyClanChallenges.getInstance().getContributionManager();

        // Commande sans arguments = top global
        if (args.length == 0) {
            showGlobalTop(sender, manager);
            return;
        }

        String category = args[0].toLowerCase();
        String type = args.length > 1 ? args[1].toLowerCase() : "both";

        if (category.equals("global")) {
            showGlobalTop(sender, manager);
        } else {
            // Vérifier si c'est une catégorie valide
            ChallengeCategory challengeCategory;
            try {
                challengeCategory = ChallengeCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cCatégorie invalide : " + category);
                sender.sendMessage("§7Disponibles : global, craft, mine, kill, action");
                return;
            }

            showCategoryTop(sender, manager, challengeCategory, type);
        }
    }

    private void showGlobalTop(CommandSender sender, ContributionManager manager) {
        sender.sendMessage("§6§l═══════════════════════════════");
        sender.sendMessage("§6§l       TOP GLOBAL");
        sender.sendMessage("§6§l═══════════════════════════════");

        // Top joueurs global
        sender.sendMessage("§e§l» Top 10 Joueurs (toutes catégories) :");
        List<Map.Entry<UUID, Integer>> topPlayers = manager.getTopPlayersGlobal();
        for (int i = 0; i < Math.min(10, topPlayers.size()); i++) {
            UUID playerId = topPlayers.get(i).getKey();
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            String name = player.getName() != null ? player.getName() : "Inconnu";
            int points = topPlayers.get(i).getValue();
            sender.sendMessage("  §7" + (i + 1) + ". §f" + name + " §7- §a" + points + " §7points");
        }

        sender.sendMessage("");

        // Top clans global
        sender.sendMessage("§e§l» Top 10 Clans (toutes catégories) :");
        List<Map.Entry<UUID, Integer>> topClans = manager.getTopClansGlobal();
        for (int i = 0; i < Math.min(10, topClans.size()); i++) {
            UUID clanId = topClans.get(i).getKey();
            Clan clan = ClassyClan.getAPI().getClanOf(clanId);
            String name = clan != null ? clan.getRawName() : "Inconnu";
            int points = topClans.get(i).getValue();
            sender.sendMessage("  §7" + (i + 1) + ". §f" + name + " §7- §a" + points + " §7points");
        }

        sender.sendMessage("§6§l═══════════════════════════════");
    }

    private void showCategoryTop(CommandSender sender, ContributionManager manager, ChallengeCategory category, String type) {
        sender.sendMessage("§6§l═══════════════════════════════");
        sender.sendMessage("§6§l       TOP " + category.name());
        sender.sendMessage("§6§l═══════════════════════════════");

        if (type.equals("players") || type.equals("both")) {
            sender.sendMessage("§e§l» Top 10 Joueurs - " + category.name() + " :");
            List<Map.Entry<UUID, Integer>> topPlayers = manager.getTopPlayersByCategory(category);
            for (int i = 0; i < Math.min(10, topPlayers.size()); i++) {
                UUID playerId = topPlayers.get(i).getKey();
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
                String name = player.getName() != null ? player.getName() : "Inconnu";
                int points = topPlayers.get(i).getValue();
                sender.sendMessage("  §7" + (i + 1) + ". §f" + name + " §7- §a" + points + " §7points");
            }

            if (type.equals("both")) {
                sender.sendMessage("");
            }
        }

        if (type.equals("clans") || type.equals("both")) {
            sender.sendMessage("§e§l» Top 10 Clans - " + category.name() + " :");
            List<Map.Entry<UUID, Integer>> topClans = manager.getTopClansByCategory(category);
            for (int i = 0; i < Math.min(10, topClans.size()); i++) {
                UUID clanId = topClans.get(i).getKey();
                Clan clan = ClassyClan.getAPI().getClanOf(clanId);
                String name = clan != null ? clan.getRawName() : "Inconnu";
                int points = topClans.get(i).getValue();
                sender.sendMessage("  §7" + (i + 1) + ". §f" + name + " §7- §a" + points + " §7points");
            }
        }

        sender.sendMessage("§6§l═══════════════════════════════");
    }
}