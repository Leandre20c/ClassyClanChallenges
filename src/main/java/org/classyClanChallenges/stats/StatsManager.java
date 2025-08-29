package org.classyClanChallenges.stats;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.contribution.ContributionManager;
import org.classyClanChallenges.logging.LogEntry;
import org.classyClanChallenges.logging.LogManager;
import org.classyClanChallenges.logging.LogType;
import org.classyclan.ClassyClan;
import org.classyclan.clan.Clan;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestionnaire des statistiques avancées
 */
public class StatsManager {

    private final LogManager logManager;
    private final ContributionManager contributionManager;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public StatsManager(LogManager logManager, ContributionManager contributionManager) {
        this.logManager = logManager;
        this.contributionManager = contributionManager;
    }

    /**
     * Génère un rapport complet d'activité
     */
    public String generateActivityReport(int hours) {
        StringBuilder report = new StringBuilder();
        Map<String, Object> summary = logManager.getActivitySummary(hours);

        report.append("§6§l═══════════════════════════════\n");
        report.append("§6§l    RAPPORT D'ACTIVITÉ (").append(hours).append("h)\n");
        report.append("§6§l═══════════════════════════════\n\n");

        // Résumé général
        report.append("§e§lRésumé général :\n");
        report.append("§7• Total d'événements : §f").append(summary.get("total_events")).append("\n");
        report.append("§7• Points distribués : §a+").append(summary.get("points_distributed")).append("\n\n");

        // Activité par catégorie
        @SuppressWarnings("unchecked")
        Map<ChallengeCategory, Integer> byCategory = (Map<ChallengeCategory, Integer>) summary.get("by_category");
        if (!byCategory.isEmpty()) {
            report.append("§e§lActivité par catégorie :\n");
            byCategory.entrySet().stream()
                    .sorted(Map.Entry.<ChallengeCategory, Integer>comparingByValue().reversed())
                    .forEach(entry ->
                            report.append("§7• ").append(entry.getKey().name()).append(" : §a+").append(entry.getValue()).append("\n"));
            report.append("\n");
        }

        // Top joueurs actifs
        @SuppressWarnings("unchecked")
        Map<UUID, Integer> topPlayers = (Map<UUID, Integer>) summary.get("top_players");
        if (!topPlayers.isEmpty()) {
            report.append("§e§lTop 10 joueurs les plus actifs :\n");
            List<Map.Entry<UUID, Integer>> sortedPlayers = topPlayers.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .limit(10)
                    .collect(Collectors.toList());

            for (int i = 0; i < sortedPlayers.size(); i++) {
                Map.Entry<UUID, Integer> entry = sortedPlayers.get(i);
                OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                String name = player.getName() != null ? player.getName() : "Inconnu";
                report.append("§7").append(i + 1).append(". §f").append(name).append(" §7- §a+").append(entry.getValue()).append("\n");
            }
        }

        report.append("§6§l═══════════════════════════════");
        return report.toString();
    }

    /**
     * Génère des statistiques détaillées pour un joueur
     */
    public String generatePlayerStats(UUID playerId) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        String playerName = player.getName() != null ? player.getName() : "Inconnu";

        StringBuilder stats = new StringBuilder();
        stats.append("§6§l═══ STATS DÉTAILLÉES: ").append(playerName).append(" ═══\n\n");

        // Informations de base
        stats.append("§e§lInformations générales :\n");
        stats.append("§7• UUID : §f").append(playerId.toString().substring(0, 8)).append("...\n");
        stats.append("§7• Statut : ").append(player.isOnline() ? "§aEn ligne" : "§cHors ligne").append("\n");

        Clan clan = ClassyClan.getAPI().getClanOf(playerId);
        if (clan != null) {
            stats.append("§7• Clan : §f").append(clan.getRawName()).append(" §7(").append(clan.getMembers().size()).append(" membres)\n");
        } else {
            stats.append("§7• Clan : §cAucun\n");
        }
        stats.append("\n");

        // Contributions actuelles
        var playerContrib = contributionManager.getPlayerContribution(playerId);
        stats.append("§e§lContributions cette semaine :\n");
        for (ChallengeCategory category : ChallengeCategory.values()) {
            int points = playerContrib.get(category);
            if (points > 0) {
                stats.append("§7• ").append(category.name()).append(" : §a").append(points).append(" points\n");
            }
        }
        stats.append("§7• §lTotal : §a").append(playerContrib.getTotal()).append(" points\n\n");

        // Activité récente
        List<LogEntry> recentLogs = logManager.getPlayerLogs(playerId, 10);
        if (!recentLogs.isEmpty()) {
            stats.append("§e§lActivité récente (10 derniers événements) :\n");
            for (LogEntry log : recentLogs) {
                String timeStr = log.getTimestamp().format(formatter);
                String pointsStr = log.getPoints() > 0 ? "§a+" + log.getPoints() : log.getPoints() < 0 ? "§c" + log.getPoints() : "§70";
                stats.append("§7[").append(timeStr).append("] ").append(pointsStr).append(" ")
                        .append(log.getCategory() != null ? log.getCategory().name() : "SYSTÈME")
                        .append(" §7- ").append(log.getReason()).append("\n");
            }
        } else {
            stats.append("§e§lActivité récente :\n§cAucune activité récente trouvée.\n");
        }

        // Classements
        stats.append("\n§e§lPosition dans les classements :\n");
        for (ChallengeCategory category : ChallengeCategory.values()) {
            var topPlayers = contributionManager.getTopPlayersByCategory(category);
            int rank = -1;
            for (int i = 0; i < topPlayers.size(); i++) {
                if (topPlayers.get(i).getKey().equals(playerId)) {
                    rank = i + 1;
                    break;
                }
            }

            if (rank > 0) {
                stats.append("§7• ").append(category.name()).append(" : §6#").append(rank)
                        .append(" §7(").append(playerContrib.get(category)).append(" points)\n");
            } else {
                stats.append("§7• ").append(category.name()).append(" : §8Non classé\n");
            }
        }

        stats.append("\n§6§l═══════════════════════════════");
        return stats.toString();
    }

    /**
     * Génère des statistiques pour un clan
     */
    public String generateClanStats(UUID clanOwnerId) {
        Clan clan = ClassyClan.getAPI().getClanOf(clanOwnerId);
        if (clan == null) return "§cClan introuvable.";

        StringBuilder stats = new StringBuilder();
        stats.append("§6§l═══ STATS CLAN: ").append(clan.getRawName()).append(" ═══\n\n");

        // Informations générales du clan
        stats.append("§e§lInformations générales :\n");
        stats.append("§7• Nom : §f").append(clan.getRawName()).append("\n");
        stats.append("§7• Tag : §f").append(clan.getRawPrefix()).append("\n");
        stats.append("§7• Propriétaire : §f").append(Bukkit.getOfflinePlayer(clan.getOwner()).getName()).append("\n");
        stats.append("§7• Membres : §f").append(clan.getMembers().size()).append("\n\n");

        // Contributions totales du clan
        var clanContrib = contributionManager.getClanContribution(clanOwnerId);
        stats.append("§e§lContributions du clan cette semaine :\n");
        for (ChallengeCategory category : ChallengeCategory.values()) {
            int points = clanContrib.get(category);
            if (points > 0) {
                stats.append("§7• ").append(category.name()).append(" : §a").append(points).append(" points\n");

                // Position dans le classement pour cette catégorie
                var topClans = contributionManager.getTopClansByCategory(category);
                int rank = -1;
                for (int i = 0; i < topClans.size(); i++) {
                    if (topClans.get(i).getKey().equals(clanOwnerId)) {
                        rank = i + 1;
                        break;
                    }
                }
                stats.append("  §8└ Position: ").append(rank > 0 ? "§6#" + rank : "§8Non classé").append("\n");
            }
        }
        stats.append("§7• §lTotal : §a").append(clanContrib.getTotal()).append(" points\n\n");

        // Membres les plus actifs
        stats.append("§e§lMembres les plus actifs cette semaine :\n");
        Map<UUID, Integer> memberContribs = new HashMap<>();
        for (UUID memberId : clan.getMembers()) {
            int total = contributionManager.getPlayerContribution(memberId).getTotal();
            if (total > 0) {
                memberContribs.put(memberId, total);
            }
        }

        List<Map.Entry<UUID, Integer>> sortedMembers = memberContribs.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        if (sortedMembers.isEmpty()) {
            stats.append("§cAucun membre actif cette semaine.\n");
        } else {
            for (int i = 0; i < sortedMembers.size(); i++) {
                Map.Entry<UUID, Integer> entry = sortedMembers.get(i);
                OfflinePlayer member = Bukkit.getOfflinePlayer(entry.getKey());
                String name = member.getName() != null ? member.getName() : "Inconnu";
                stats.append("§7").append(i + 1).append(". §f").append(name).append(" §7- §a").append(entry.getValue()).append(" points\n");
            }
        }

        // Activité récente du clan
        List<LogEntry> clanLogs = logManager.getClanLogs(clanOwnerId, 10);
        if (!clanLogs.isEmpty()) {
            stats.append("\n§e§lActivité récente du clan :\n");
            for (LogEntry log : clanLogs) {
                OfflinePlayer member = Bukkit.getOfflinePlayer(log.getPlayerId());
                String memberName = member.getName() != null ? member.getName() : "Inconnu";
                String timeStr = log.getTimestamp().format(formatter);
                stats.append("§7[").append(timeStr).append("] §f").append(memberName)
                        .append(" §a+").append(log.getPoints()).append(" ")
                        .append(log.getCategory() != null ? log.getCategory().name() : "").append("\n");
            }
        }

        stats.append("\n§6§l═══════════════════════════════");
        return stats.toString();
    }

    /**
     * Génère un rapport des événements système récents
     */
    public String generateSystemReport(int limit) {
        StringBuilder report = new StringBuilder();
        report.append("§6§l═══════════════════════════════\n");
        report.append("§6§l    ÉVÉNEMENTS SYSTÈME\n");
        report.append("§6§l═══════════════════════════════\n\n");

        List<LogEntry> systemLogs = logManager.getLogsByType(LogType.SYSTEM_EVENT, limit);
        List<LogEntry> adminLogs = logManager.getLogsByType(LogType.ADMIN_ACTION, limit);
        List<LogEntry> modificationLogs = logManager.getLogsByType(LogType.POINT_MODIFICATION, limit);

        // Événements système
        if (!systemLogs.isEmpty()) {
            report.append("§e§lÉvénements système récents :\n");
            for (LogEntry log : systemLogs) {
                String timeStr = log.getTimestamp().format(formatter);
                report.append("§7[").append(timeStr).append("] §f").append(log.getReason()).append("\n");
                if (log.getDetails() != null && !log.getDetails().isEmpty()) {
                    report.append("  §8└ ").append(log.getDetails()).append("\n");
                }
            }
            report.append("\n");
        }

        // Actions admin
        if (!adminLogs.isEmpty()) {
            report.append("§e§lActions administrateur :\n");
            for (LogEntry log : adminLogs) {
                String timeStr = log.getTimestamp().format(formatter);
                report.append("§7[").append(timeStr).append("] §f").append(log.getReason()).append("\n");
                if (log.getDetails() != null && !log.getDetails().isEmpty()) {
                    report.append("  §8└ ").append(log.getDetails()).append("\n");
                }
            }
            report.append("\n");
        }

        // Modifications de points
        if (!modificationLogs.isEmpty()) {
            report.append("§e§lModifications de points :\n");
            for (LogEntry log : modificationLogs) {
                OfflinePlayer player = log.getPlayerId() != null ? Bukkit.getOfflinePlayer(log.getPlayerId()) : null;
                String playerName = player != null && player.getName() != null ? player.getName() : "Inconnu";
                String timeStr = log.getTimestamp().format(formatter);
                report.append("§7[").append(timeStr).append("] §f").append(playerName).append(" §7- ").append(log.getReason()).append("\n");
                report.append("  §8└ ").append(log.getDetails()).append("\n");
            }
        }

        if (systemLogs.isEmpty() && adminLogs.isEmpty() && modificationLogs.isEmpty()) {
            report.append("§cAucun événement système récent trouvé.\n");
        }

        report.append("§6§l═══════════════════════════════");
        return report.toString();
    }

    /**
     * Génère des statistiques globales du serveur
     */
    public Map<String, Object> generateGlobalStats() {
        Map<String, Object> stats = new HashMap<>();

        // Statistiques générales
        stats.put("total_players", contributionManager.getAllPlayerContributions().size());
        stats.put("total_clans", contributionManager.getAllClanContributions().size());
        stats.put("active_players_today", logManager.getActivitySummary(24).get("top_players"));

        // Total de points par catégorie
        Map<ChallengeCategory, Integer> totalByCategory = new HashMap<>();
        for (ChallengeCategory category : ChallengeCategory.values()) {
            int total = contributionManager.getAllPlayerContributions().values().stream()
                    .mapToInt(pc -> pc.get(category))
                    .sum();
            totalByCategory.put(category, total);
        }
        stats.put("total_by_category", totalByCategory);

        return stats;
    }
}