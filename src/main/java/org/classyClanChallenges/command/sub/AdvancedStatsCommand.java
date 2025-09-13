package org.classyClanChallenges.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.stats.StatsManager;
import org.classyclan.ClassyClan;
import org.classyclan.clan.Clan;

import java.util.Map;
import java.util.UUID;

public class AdvancedStatsCommand extends SubCommand {

    @Override
    public String getName() {
        return "astats";
    }

    @Override
    public String getDescription() {
        return "Affiche des statistiques avancées détaillées.";
    }

    @Override
    public String getUsage() {
        return "/jdc astats <player|clan|global|activity> [nom] [paramètres]";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        StatsManager statsManager = ClassyClanChallenges.getInstance().getStatsManager();

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "player" -> handlePlayerStats(sender, args, statsManager);
            case "clan" -> handleClanStats(sender, args, statsManager);
            case "global" -> handleGlobalStats(sender, statsManager);
            case "activity" -> handleActivityStats(sender, args, statsManager);
            case "system" -> handleSystemStats(sender, statsManager);
            default -> sendUsage(sender);
        }
    }

    private void handlePlayerStats(CommandSender sender, String[] args, StatsManager statsManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jdc astats player <nom>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            sender.sendMessage("§cJoueur inconnu: " + args[1]);
            return;
        }

        String stats = statsManager.generatePlayerStats(target.getUniqueId());

        // Envoyer les stats ligne par ligne
        String[] lines = stats.split("\n");
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    private void handleClanStats(CommandSender sender, String[] args, StatsManager statsManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jdc astats clan <nom>");
            return;
        }

        Clan clan = ClassyClan.getAPI().getClanByName(args[1]);
        if (clan == null) {
            sender.sendMessage("§cClan introuvable: " + args[1]);
            return;
        }

        String stats = statsManager.generateClanStats(clan.getOwner());

        // Envoyer les stats ligne par ligne
        String[] lines = stats.split("\n");
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    private void handleGlobalStats(CommandSender sender, StatsManager statsManager) {
        sender.sendMessage("§6§l═══════════════════════════════");
        sender.sendMessage("§6§l    STATISTIQUES GLOBALES");
        sender.sendMessage("§6§l═══════════════════════════════");

        Map<String, Object> globalStats = statsManager.generateGlobalStats();

        // Statistiques générales
        sender.sendMessage("§e§lRésumé général:");
        sender.sendMessage("§7• Joueurs actifs: §a" + globalStats.get("total_players"));
        sender.sendMessage("§7• Clans actifs: §a" + globalStats.get("total_clans"));

        // Points par catégorie
        sender.sendMessage("\n§e§lTotal des points par catégorie:");
        @SuppressWarnings("unchecked")
        Map<ChallengeCategory, Integer> totalByCategory = (Map<ChallengeCategory, Integer>) globalStats.get("total_by_category");

        int grandTotal = 0;
        for (Map.Entry<ChallengeCategory, Integer> entry : totalByCategory.entrySet()) {
            sender.sendMessage("§7• " + entry.getKey().name() + ": §a" + entry.getValue() + " §7points");
            grandTotal += entry.getValue();
        }
        sender.sendMessage("§7• §lTOTAL GÉNÉRAL: §a" + grandTotal + " §7points");

        // Top joueurs aujourd'hui
        @SuppressWarnings("unchecked")
        Map<UUID, Integer> activeToday = (Map<UUID, Integer>) globalStats.get("active_players_today");
        if (!activeToday.isEmpty()) {
            sender.sendMessage("\n§e§lTop 5 joueurs les plus actifs aujourd'hui:");
            activeToday.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                        String name = player.getName() != null ? player.getName() : "Inconnu";
                        sender.sendMessage("§7• " + name + ": §a+" + entry.getValue() + " §7points");
                    });
        }

        // Statistiques du serveur
        sender.sendMessage("\n§e§lStatistiques du serveur:");
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        sender.sendMessage("§7• Mémoire utilisée: §f" + usedMemory + "MB§7/§f" + maxMemory + "MB");
        sender.sendMessage("§7• Joueurs en ligne: §a" + Bukkit.getOnlinePlayers().size() + "§7/§f" + Bukkit.getMaxPlayers());

        sender.sendMessage("§6§l═══════════════════════════════");
    }

    private void handleActivityStats(CommandSender sender, String[] args, StatsManager statsManager) {
        int hours = 24;
        if (args.length > 1) {
            try {
                hours = Integer.parseInt(args[1]);
                hours = Math.min(hours, 168); // Max 7 jours
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNombre d'heures invalide: " + args[1]);
                return;
            }
        }

        String report = statsManager.generateActivityReport(hours);

        String[] lines = report.split("\n");
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    private void handleSystemStats(CommandSender sender, StatsManager statsManager) {
        String report = statsManager.generateSystemReport(20);

        String[] lines = report.split("\n");
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§cUsage: /jdc astats <sous-commande> [paramètres]");
        sender.sendMessage("§7Sous-commandes disponibles:");
        sender.sendMessage("§7• §eplayer <nom> §7- Stats détaillées d'un joueur");
        sender.sendMessage("§7• §eclan <nom> §7- Stats détaillées d'un clan");
        sender.sendMessage("§7• §eglobal §7- Statistiques globales du serveur");
        sender.sendMessage("§7• §eactivity [heures] §7- Rapport d'activité");
        sender.sendMessage("§7• §esystem §7- Événements système récents");
    }
}