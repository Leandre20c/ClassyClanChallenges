package org.classyClanChallenges.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.logging.LogEntry;
import org.classyClanChallenges.logging.LogManager;
import org.classyClanChallenges.logging.LogType;
import org.classyclan.ClassyClan;
import org.classyclan.clan.Clan;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class LogsCommand extends SubCommand {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @Override
    public String getName() {
        return "logs";
    }

    @Override
    public String getDescription() {
        return "Consulte les logs détaillés d'activité.";
    }

    @Override
    public String getUsage() {
        return "/jdc logs <recent|player|clan|category|type|activity> [paramètres]";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        LogManager logManager = ClassyClanChallenges.getInstance().getLogManager();

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "recent" -> handleRecentLogs(sender, args, logManager);
            case "player" -> handlePlayerLogs(sender, args, logManager);
            case "clan" -> handleClanLogs(sender, args, logManager);
            case "category" -> handleCategoryLogs(sender, args, logManager);
            case "type" -> handleTypeLogs(sender, args, logManager);
            case "activity" -> handleActivityReport(sender, args, logManager);
            default -> sendUsage(sender);
        }
    }

    private void handleRecentLogs(CommandSender sender, String[] args, LogManager logManager) {
        int limit = 20;
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                limit = Math.min(limit, 100); // Maximum 100
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNombre invalide: " + args[1]);
                return;
            }
        }

        List<LogEntry> logs = logManager.getRecentLogs(limit);
        if (logs.isEmpty()) {
            sender.sendMessage("§cAucun log récent trouvé.");
            return;
        }

        sender.sendMessage("§6§l═══ LOGS RÉCENTS (" + logs.size() + ") ═══");
        for (LogEntry log : logs) {
            displayLogEntry(sender, log);
        }
        sender.sendMessage("§6§l═══════════════════════════════");
    }

    private void handlePlayerLogs(CommandSender sender, String[] args, LogManager logManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jdc logs player <nom> [limite]");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            sender.sendMessage("§cJoueur inconnu: " + args[1]);
            return;
        }

        int limit = 20;
        if (args.length > 2) {
            try {
                limit = Integer.parseInt(args[2]);
                limit = Math.min(limit, 50);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNombre invalide: " + args[2]);
                return;
            }
        }

        List<LogEntry> logs = logManager.getPlayerLogs(target.getUniqueId(), limit);
        if (logs.isEmpty()) {
            sender.sendMessage("§cAucun log trouvé pour " + (target.getName() != null ? target.getName() : args[1]));
            return;
        }

        String playerName = target.getName() != null ? target.getName() : args[1];
        sender.sendMessage("§6§l═══ LOGS DE " + playerName.toUpperCase() + " (" + logs.size() + ") ═══");
        for (LogEntry log : logs) {
            displayLogEntry(sender, log);
        }
        sender.sendMessage("§6§l═══════════════════════════════");
    }

    private void handleClanLogs(CommandSender sender, String[] args, LogManager logManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jdc logs clan <nom> [limite]");
            return;
        }

        Clan clan = ClassyClan.getAPI().getClanByName(args[1]);
        if (clan == null) {
            sender.sendMessage("§cClan introuvable: " + args[1]);
            return;
        }

        int limit = 30;
        if (args.length > 2) {
            try {
                limit = Integer.parseInt(args[2]);
                limit = Math.min(limit, 100);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNombre invalide: " + args[2]);
                return;
            }
        }

        List<LogEntry> logs = logManager.getClanLogs(clan.getOwner(), limit);
        if (logs.isEmpty()) {
            sender.sendMessage("§cAucun log trouvé pour le clan " + clan.getRawName());
            return;
        }

        sender.sendMessage("§6§l═══ LOGS DU CLAN " + clan.getRawName().toUpperCase() + " (" + logs.size() + ") ═══");
        for (LogEntry log : logs) {
            displayLogEntry(sender, log);
        }
        sender.sendMessage("§6§l═══════════════════════════════");
    }

    private void handleCategoryLogs(CommandSender sender, String[] args, LogManager logManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jdc logs category <catégorie> [limite]");
            sender.sendMessage("§7Catégories: CRAFT, MINE, KILL, ACTION");
            return;
        }

        ChallengeCategory category;
        try {
            category = ChallengeCategory.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cCatégorie invalide: " + args[1]);
            sender.sendMessage("§7Catégories: CRAFT, MINE, KILL, ACTION");
            return;
        }

        int limit = 30;
        if (args.length > 2) {
            try {
                limit = Integer.parseInt(args[2]);
                limit = Math.min(limit, 100);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNombre invalide: " + args[2]);
                return;
            }
        }

        List<LogEntry> logs = logManager.getLogsByCategory(category, limit);
        if (logs.isEmpty()) {
            sender.sendMessage("§cAucun log trouvé pour la catégorie " + category.name());
            return;
        }

        sender.sendMessage("§6§l═══ LOGS " + category.name() + " (" + logs.size() + ") ═══");
        for (LogEntry log : logs) {
            displayLogEntry(sender, log);
        }
        sender.sendMessage("§6§l═══════════════════════════════");
    }

    private void handleTypeLogs(CommandSender sender, String[] args, LogManager logManager) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jdc logs type <type> [limite]");
            sender.sendMessage("§7Types: POINT_GAIN, POINT_MODIFICATION, SYSTEM_EVENT, ADMIN_ACTION");
            return;
        }

        LogType type;
        try {
            type = LogType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cType invalide: " + args[1]);
            sender.sendMessage("§7Types: POINT_GAIN, POINT_MODIFICATION, SYSTEM_EVENT, ADMIN_ACTION");
            return;
        }

        int limit = 30;
        if (args.length > 2) {
            try {
                limit = Integer.parseInt(args[2]);
                limit = Math.min(limit, 100);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNombre invalide: " + args[2]);
                return;
            }
        }

        List<LogEntry> logs = logManager.getLogsByType(type, limit);
        if (logs.isEmpty()) {
            sender.sendMessage("§cAucun log trouvé pour le type " + type.name());
            return;
        }

        sender.sendMessage("§6§l═══ LOGS " + type.getDisplayName().toUpperCase() + " (" + logs.size() + ") ═══");
        for (LogEntry log : logs) {
            displayLogEntry(sender, log);
        }
        sender.sendMessage("§6§l═══════════════════════════════");
    }

    private void handleActivityReport(CommandSender sender, String[] args, LogManager logManager) {
        int hours = 24; // Dernières 24h par défaut
        if (args.length > 1) {
            try {
                hours = Integer.parseInt(args[1]);
                hours = Math.min(hours, 168); // Maximum 7 jours
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNombre d'heures invalide: " + args[1]);
                return;
            }
        }

        String report = ClassyClanChallenges.getInstance().getStatsManager().generateActivityReport(hours);

        // Envoyer le rapport ligne par ligne pour éviter les problèmes de longueur
        String[] lines = report.split("\n");
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }

    private void displayLogEntry(CommandSender sender, LogEntry log) {
        StringBuilder line = new StringBuilder();

        // Timestamp
        line.append("§7[").append(log.getTimestamp().format(formatter)).append("] ");

        // Type et catégorie
        if (log.getCategory() != null) {
            line.append("§e").append(log.getCategory().name()).append(" ");
        }

        // Points (si applicable)
        if (log.getPoints() != 0) {
            if (log.getPoints() > 0) {
                line.append("§a+").append(log.getPoints());
            } else {
                line.append("§c").append(log.getPoints());
            }
            line.append(" ");
        }

        // Joueur (si applicable)
        if (log.getPlayerId() != null) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(log.getPlayerId());
            String name = player.getName() != null ? player.getName() : "Inconnu";
            line.append("§f").append(name).append(" ");
        }

        // Raison
        line.append("§7- ").append(log.getReason());

        sender.sendMessage(line.toString());

        // Détails sur une ligne séparée si présents
        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
            sender.sendMessage("  §8└ " + log.getDetails());
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§cUsage: /jdc logs <sous-commande> [paramètres]");
        sender.sendMessage("§7Sous-commandes disponibles:");
        sender.sendMessage("§7• §erecent [limite] §7- Logs récents");
        sender.sendMessage("§7• §eplayer <nom> [limite] §7- Logs d'un joueur");
        sender.sendMessage("§7• §eclan <nom> [limite] §7- Logs d'un clan");
        sender.sendMessage("§7• §ecategory <catégorie> [limite] §7- Logs d'une catégorie");
        sender.sendMessage("§7• §etype <type> [limite] §7- Logs d'un type d'événement");
        sender.sendMessage("§7• §eactivity [heures] §7- Rapport d'activité");
    }
}