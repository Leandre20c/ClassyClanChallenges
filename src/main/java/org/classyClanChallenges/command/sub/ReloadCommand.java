package org.classyClanChallenges.command.sub;

import org.bukkit.command.CommandSender;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeManager;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.contribution.ContributionManager;
import org.classyClanChallenges.logging.LogManager;
import org.classyClanChallenges.reward.RewardManager;
import org.classyClanChallenges.util.BlockDataManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReloadCommand extends SubCommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Recharge les fichiers de configuration du plugin.";
    }

    @Override
    public String getUsage() {
        return "/jdc reload [fichier] ou /jdc reload all";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ClassyClanChallenges plugin = ClassyClanChallenges.getInstance();

        // Si aucun argument ou "all", recharger tout
        if (args.length == 0 || args[0].equalsIgnoreCase("all")) {
            reloadAll(sender, plugin);
            return;
        }

        // Sinon, recharger le fichier spécifique
        String fileToReload = args[0].toLowerCase();

        switch (fileToReload) {
            case "config" -> reloadConfig(sender, plugin);
            case "challenges" -> reloadChallenges(sender, plugin);
            case "rewards" -> reloadRewards(sender, plugin);
            case "weekly" -> reloadWeekly(sender, plugin);
            case "contributions" -> reloadContributions(sender, plugin);
            case "logs" -> reloadLogs(sender, plugin);
            case "blocks" -> reloadBlocks(sender, plugin);
            case "help" -> showHelp(sender);
            default -> {
                sender.sendMessage("§cFichier inconnu: " + fileToReload);
                sender.sendMessage("§7Tapez §e/jdc reload help §7pour voir les options disponibles.");
            }
        }
    }

    private void reloadAll(CommandSender sender, ClassyClanChallenges plugin) {
        sender.sendMessage("§e§l=== RECHARGEMENT COMPLET ===");

        List<String> reloadedFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        // 1. Config principal
        try {
            plugin.reloadConfig();
            reloadedFiles.add("config.yml");
            sender.sendMessage("§a✓ config.yml rechargé");
        } catch (Exception e) {
            failedFiles.add("config.yml: " + e.getMessage());
            sender.sendMessage("§c✗ Erreur lors du rechargement de config.yml");
        }

        // 2. Challenges
        try {
            ChallengeManager challengeManager = plugin.getChallengeManager();
            challengeManager.loadChallenges();
            reloadedFiles.add("challenges.yml");
            sender.sendMessage("§a✓ challenges.yml rechargé");
        } catch (Exception e) {
            failedFiles.add("challenges.yml: " + e.getMessage());
            sender.sendMessage("§c✗ Erreur lors du rechargement de challenges.yml");
        }

        // 3. Rewards
        try {
            RewardManager rewardManager = new RewardManager();
            plugin.setRewardManager(rewardManager); // Nécessite d'ajouter un setter
            reloadedFiles.add("rewards.yml");
            sender.sendMessage("§a✓ rewards.yml rechargé");
        } catch (Exception e) {
            failedFiles.add("rewards.yml: " + e.getMessage());
            sender.sendMessage("§c✗ Erreur lors du rechargement de rewards.yml");
        }

        // 4. Weekly challenges
        try {
            plugin.getChallengeManager().loadActiveChallengesFromFile();
            reloadedFiles.add("data/weekly.yml");
            sender.sendMessage("§a✓ data/weekly.yml rechargé");
        } catch (Exception e) {
            failedFiles.add("data/weekly.yml: " + e.getMessage());
            sender.sendMessage("§c✗ Erreur lors du rechargement de data/weekly.yml");
        }

        // 5. Contributions
        try {
            plugin.getContributionManager().loadAll();
            reloadedFiles.add("data/contributions.yml");
            sender.sendMessage("§a✓ data/contributions.yml rechargé");
        } catch (Exception e) {
            failedFiles.add("data/contributions.yml: " + e.getMessage());
            sender.sendMessage("§c✗ Erreur lors du rechargement de data/contributions.yml");
        }

        // 6. Logs
        try {
            if (plugin.getLogManager() != null) {
                plugin.getLogManager().loadLogs();
                reloadedFiles.add("data/activity-logs.yml");
                sender.sendMessage("§a✓ data/activity-logs.yml rechargé");
            } else {
                sender.sendMessage("§e⚠ LogManager non initialisé, logs ignorés");
            }
        } catch (Exception e) {
            failedFiles.add("data/activity-logs.yml: " + e.getMessage());
            sender.sendMessage("§c✗ Erreur lors du rechargement de data/activity-logs.yml");
        }

        // 7. Block data
        try {
            plugin.getBlockDataManager().cleanupInvalidWorlds();
            reloadedFiles.add("data/player-blocks.yml");
            sender.sendMessage("§a✓ data/player-blocks.yml vérifié");
        } catch (Exception e) {
            failedFiles.add("data/player-blocks.yml: " + e.getMessage());
            sender.sendMessage("§c✗ Erreur lors de la vérification de data/player-blocks.yml");
        }

        // Résumé
        sender.sendMessage("§e§l=== RÉSUMÉ ===");
        sender.sendMessage("§a✓ Fichiers rechargés avec succès: §f" + reloadedFiles.size());

        if (!failedFiles.isEmpty()) {
            sender.sendMessage("§c✗ Erreurs rencontrées: §f" + failedFiles.size());
            sender.sendMessage("§7Tapez §e/jdc reload help §7pour plus de détails sur chaque fichier.");
        }

        sender.sendMessage("§e§l========================");

        // Log de l'action
        if (plugin.getLogManager() != null) {
            plugin.getLogManager().logSystemEvent("Rechargement complet",
                    "Rechargés: " + String.join(", ", reloadedFiles) +
                            (failedFiles.isEmpty() ? "" : " | Erreurs: " + failedFiles.size()),
                    sender.getName());
        }
    }

    private void reloadConfig(CommandSender sender, ClassyClanChallenges plugin) {
        try {
            plugin.reloadConfig();
            sender.sendMessage("§a✓ config.yml rechargé avec succès.");

            if (plugin.getLogManager() != null) {
                plugin.getLogManager().logSystemEvent("Rechargement config",
                        "config.yml rechargé", sender.getName());
            }
        } catch (Exception e) {
            sender.sendMessage("§c✗ Erreur lors du rechargement de config.yml: " + e.getMessage());
        }
    }

    private void reloadChallenges(CommandSender sender, ClassyClanChallenges plugin) {
        try {
            ChallengeManager challengeManager = plugin.getChallengeManager();
            int oldPoolSize = challengeManager.getPool().values().stream()
                    .mapToInt(List::size).sum();

            challengeManager.loadChallenges();

            int newPoolSize = challengeManager.getPool().values().stream()
                    .mapToInt(List::size).sum();

            sender.sendMessage("§a✓ challenges.yml rechargé avec succès.");
            sender.sendMessage("§7Pool de défis: " + oldPoolSize + " → " + newPoolSize + " défis");

            if (plugin.getLogManager() != null) {
                plugin.getLogManager().logSystemEvent("Rechargement challenges",
                        "challenges.yml rechargé. Défis: " + oldPoolSize + " → " + newPoolSize,
                        sender.getName());
            }
        } catch (Exception e) {
            sender.sendMessage("§c✗ Erreur lors du rechargement de challenges.yml: " + e.getMessage());
        }
    }

    private void reloadRewards(CommandSender sender, ClassyClanChallenges plugin) {
        try {
            RewardManager newRewardManager = new RewardManager();
            plugin.setRewardManager(newRewardManager);

            sender.sendMessage("§a✓ rewards.yml rechargé avec succès.");
            sender.sendMessage("§7Nouvelles récompenses chargées depuis le fichier.");

            if (plugin.getLogManager() != null) {
                plugin.getLogManager().logSystemEvent("Rechargement rewards",
                        "rewards.yml rechargé", sender.getName());
            }
        } catch (Exception e) {
            sender.sendMessage("§c✗ Erreur lors du rechargement de rewards.yml: " + e.getMessage());
        }
    }

    private void reloadWeekly(CommandSender sender, ClassyClanChallenges plugin) {
        try {
            plugin.getChallengeManager().loadActiveChallengesFromFile();

            var challenges = plugin.getChallengeManager().getActiveChallenges().getAllChallenges();
            sender.sendMessage("§a✓ data/weekly.yml rechargé avec succès.");
            sender.sendMessage("§7Défis actifs: §f" + challenges.size());

            for (var entry : challenges.entrySet()) {
                sender.sendMessage("§7• " + entry.getKey().name() + ": §f" + entry.getValue().getTarget());
            }

            if (plugin.getLogManager() != null) {
                plugin.getLogManager().logSystemEvent("Rechargement weekly",
                        "data/weekly.yml rechargé. Défis: " + challenges.size(),
                        sender.getName());
            }
        } catch (Exception e) {
            sender.sendMessage("§c✗ Erreur lors du rechargement de data/weekly.yml: " + e.getMessage());
        }
    }

    private void reloadContributions(CommandSender sender, ClassyClanChallenges plugin) {
        try {
            ContributionManager manager = plugin.getContributionManager();
            int oldPlayers = manager.getAllPlayerContributions().size();
            int oldClans = manager.getAllClanContributions().size();

            manager.loadAll();

            int newPlayers = manager.getAllPlayerContributions().size();
            int newClans = manager.getAllClanContributions().size();

            sender.sendMessage("§a✓ data/contributions.yml rechargé avec succès.");
            sender.sendMessage("§7Joueurs: " + oldPlayers + " → " + newPlayers);
            sender.sendMessage("§7Clans: " + oldClans + " → " + newClans);

            if (plugin.getLogManager() != null) {
                plugin.getLogManager().logSystemEvent("Rechargement contributions",
                        "data/contributions.yml rechargé. Joueurs: " + newPlayers + ", Clans: " + newClans,
                        sender.getName());
            }
        } catch (Exception e) {
            sender.sendMessage("§c✗ Erreur lors du rechargement de data/contributions.yml: " + e.getMessage());
        }
    }

    private void reloadLogs(CommandSender sender, ClassyClanChallenges plugin) {
        try {
            if (plugin.getLogManager() != null) {
                plugin.getLogManager().loadLogs();
                sender.sendMessage("§a✓ data/activity-logs.yml rechargé avec succès.");
                sender.sendMessage("§7Logs chargés et réorganisés.");
            } else {
                sender.sendMessage("§e⚠ LogManager non initialisé.");
            }
        } catch (Exception e) {
            sender.sendMessage("§c✗ Erreur lors du rechargement de data/activity-logs.yml: " + e.getMessage());
        }
    }

    private void reloadBlocks(CommandSender sender, ClassyClanChallenges plugin) {
        try {
            BlockDataManager blockManager = plugin.getBlockDataManager();
            int beforeCount = blockManager.getStoredBlockCount();

            blockManager.cleanupInvalidWorlds();

            int afterCount = blockManager.getStoredBlockCount();
            sender.sendMessage("§a✓ data/player-blocks.yml vérifié avec succès.");
            sender.sendMessage("§7Blocs tracés: " + beforeCount + " → " + afterCount);

            if (beforeCount > afterCount) {
                sender.sendMessage("§e⚠ " + (beforeCount - afterCount) + " blocs invalides supprimés.");
            }

            if (plugin.getLogManager() != null) {
                plugin.getLogManager().logSystemEvent("Vérification blocks",
                        "data/player-blocks.yml vérifié. Blocs: " + beforeCount + " → " + afterCount,
                        sender.getName());
            }
        } catch (Exception e) {
            sender.sendMessage("§c✗ Erreur lors de la vérification de data/player-blocks.yml: " + e.getMessage());
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== AIDE RECHARGEMENT ===");
        sender.sendMessage("§e/jdc reload all §7- Recharge tous les fichiers");
        sender.sendMessage("§e/jdc reload config §7- config.yml (configuration générale)");
        sender.sendMessage("§e/jdc reload challenges §7- challenges.yml (pool de défis)");
        sender.sendMessage("§e/jdc reload rewards §7- rewards.yml (récompenses)");
        sender.sendMessage("§e/jdc reload weekly §7- data/weekly.yml (défis actifs)");
        sender.sendMessage("§e/jdc reload contributions §7- data/contributions.yml (points joueurs/clans)");
        sender.sendMessage("§e/jdc reload logs §7- data/activity-logs.yml (logs d'activité)");
        sender.sendMessage("§e/jdc reload blocks §7- data/player-blocks.yml (blocs tracés)");
        sender.sendMessage("§6§l=========================");

        // Afficher l'état des fichiers
        ClassyClanChallenges plugin = ClassyClanChallenges.getInstance();
        sender.sendMessage("§7État actuel des fichiers:");

        checkFileExists(sender, "config.yml");
        checkFileExists(sender, "challenges.yml");
        checkFileExists(sender, "rewards.yml");
        checkFileExists(sender, "data/weekly.yml");
        checkFileExists(sender, "data/contributions.yml");
        checkFileExists(sender, "data/activity-logs.yml");
        checkFileExists(sender, "data/player-blocks.yml");
        checkFileExists(sender, "bloc-note.yml");
    }

    private void checkFileExists(CommandSender sender, String fileName) {
        File file = new File(ClassyClanChallenges.getInstance().getDataFolder(), fileName);
        if (file.exists()) {
            sender.sendMessage("§a✓ " + fileName + " §7(taille: " + file.length() + " bytes)");
        } else {
            sender.sendMessage("§c✗ " + fileName + " §7(introuvable)");
        }
    }
}