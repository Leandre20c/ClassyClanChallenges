package org.classyClanChallenges.command.sub;

import org.bukkit.command.CommandSender;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.WeeklyChallenge;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.contribution.ContributionManager;

import java.util.Map;

public class InfoCommand extends SubCommand {

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Affiche les informations détaillées du système.";
    }

    @Override
    public String getUsage() {
        return "/jdc info";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ClassyClanChallenges plugin = ClassyClanChallenges.getInstance();
        WeeklyChallenge weekly = plugin.getChallengeManager().getActiveChallenges();
        ContributionManager contributionManager = plugin.getContributionManager();

        sender.sendMessage("§6§l═══════════════════════════════");
        sender.sendMessage("§6§l       INFORMATIONS SYSTÈME");
        sender.sendMessage("§6§l═══════════════════════════════");

        // Défis actifs
        sender.sendMessage("§e§l» Défis actifs cette semaine :");
        Map<ChallengeCategory, ChallengeEntry> challenges = weekly.getAllChallenges();
        if (challenges.isEmpty()) {
            sender.sendMessage("  §cAucun défi actif.");
        } else {
            for (Map.Entry<ChallengeCategory, ChallengeEntry> entry : challenges.entrySet()) {
                ChallengeEntry challenge = entry.getValue();
                sender.sendMessage("  §7- §f" + challenge.getCategory().name() + " §7: §a" + challenge.getTarget() +
                        " §7(valeur: §e" + challenge.getValue() + "§7)");
            }
        }

        sender.sendMessage("");

        // Statistiques générales
        sender.sendMessage("§e§l» Statistiques :");
        int totalPlayers = contributionManager.getAllPlayerContributions().size();
        int totalClans = contributionManager.getAllClanContributions().size();
        int totalBlocks = plugin.getBlockDataManager().getStoredBlockCount();

        sender.sendMessage("  §7- Joueurs actifs : §a" + totalPlayers);
        sender.sendMessage("  §7- Clans actifs : §a" + totalClans);
        sender.sendMessage("  §7- Blocs tracés : §a" + totalBlocks);

        sender.sendMessage("");

        // Contributions totales par catégorie
        sender.sendMessage("§e§l» Contributions totales par catégorie :");
        for (ChallengeCategory category : ChallengeCategory.values()) {
            int totalContrib = contributionManager.getAllPlayerContributions().values().stream()
                    .mapToInt(pc -> pc.get(category))
                    .sum();
            sender.sendMessage("  §7- " + category.name() + " : §a" + totalContrib + " §7points");
        }

        sender.sendMessage("§6§l═══════════════════════════════");
    }
}