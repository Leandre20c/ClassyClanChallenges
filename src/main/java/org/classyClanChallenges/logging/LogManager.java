package org.classyClanChallenges.logging;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyclan.ClassyClan;
import org.classyclan.clan.Clan;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestionnaire des logs détaillés pour traçabilité complète
 */
public class LogManager {

    private final List<LogEntry> recentLogs = new ArrayList<>();
    private final Map<UUID, List<LogEntry>> playerLogs = new HashMap<>();
    private final Map<UUID, List<LogEntry>> clanLogs = new HashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int MAX_RECENT_LOGS = 1000;
    private static final int MAX_PLAYER_LOGS = 100;

    /**
     * Enregistre un événement de gain de points
     */
    public void logPointGain(UUID playerId, ChallengeCategory category, int points, String reason, String details) {
        LogEntry entry = new LogEntry(
                playerId,
                category,
                LogType.POINT_GAIN,
                points,
                reason,
                details,
                LocalDateTime.now()
        );

        addLog(entry);

        // Log console pour les admins
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        String playerName = player.getName() != null ? player.getName() : "Inconnu";
        Clan clan = ClassyClan.getAPI().getClanOf(playerId);
        String clanName = clan != null ? clan.getRawName() : "Aucun";

        ClassyClanChallenges.getInstance().getLogger().info(
                String.format("[POINTS] %s (%s) +%d %s | %s | Clan: %s",
                        playerName, playerId.toString().substring(0, 8),
                        points, category.name(), reason, clanName)
        );
    }

    /**
     * Enregistre une modification de points (set/reset)
     */
    public void logPointModification(UUID playerId, ChallengeCategory category, int oldValue, int newValue, String reason, String admin) {
        LogEntry entry = new LogEntry(
                playerId,
                category,
                LogType.POINT_MODIFICATION,
                newValue - oldValue,
                reason + " (Admin: " + admin + ")",
                String.format("Ancien: %d → Nouveau: %d", oldValue, newValue),
                LocalDateTime.now()
        );

        addLog(entry);

        // Log console pour audit
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        String playerName = player.getName() != null ? player.getName() : "Inconnu";

        ClassyClanChallenges.getInstance().getLogger().warning(
                String.format("[MODIFICATION] Admin %s a modifié les points de %s: %d → %d (%s, %s)",
                        admin, playerName, oldValue, newValue, category.name(), reason)
        );
    }

    /**
     * Enregistre un événement système (reset, rotation, etc.)
     */
    public void logSystemEvent(String eventType, String details, String admin) {
        LogEntry entry = new LogEntry(
                null,
                null,
                LogType.SYSTEM_EVENT,
                0,
                eventType,
                details + (admin != null ? " (Admin: " + admin + ")" : ""),
                LocalDateTime.now()
        );

        addLog(entry);

        ClassyClanChallenges.getInstance().getLogger().info(
                String.format("[SYSTÈME] %s | %s", eventType, details)
        );
    }

    private void addLog(LogEntry entry) {
        // Ajouter aux logs récents
        recentLogs.add(0, entry);
        if (recentLogs.size() > MAX_RECENT_LOGS) {
            recentLogs.remove(recentLogs.size() - 1);
        }

        // Ajouter aux logs du joueur
        if (entry.getPlayerId() != null) {
            List<LogEntry> playerLogList = playerLogs.computeIfAbsent(entry.getPlayerId(), k -> new ArrayList<>());
            playerLogList.add(0, entry);
            if (playerLogList.size() > MAX_PLAYER_LOGS) {
                playerLogList.remove(playerLogList.size() - 1);
            }

            // Ajouter aux logs du clan si applicable
            Clan clan = ClassyClan.getAPI().getClanOf(entry.getPlayerId());
            if (clan != null) {
                List<LogEntry> clanLogList = clanLogs.computeIfAbsent(clan.getOwner(), k -> new ArrayList<>());
                clanLogList.add(0, entry);
                if (clanLogList.size() > MAX_PLAYER_LOGS * 5) { // Plus de logs pour les clans
                    clanLogList.remove(clanLogList.size() - 1);
                }
            }
        }

        // Sauvegarder périodiquement
        if (recentLogs.size() % 50 == 0) {
            saveLogsAsync();
        }
    }

    /**
     * Récupère les logs récents
     */
    public List<LogEntry> getRecentLogs(int limit) {
        return recentLogs.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Récupère les logs d'un joueur
     */
    public List<LogEntry> getPlayerLogs(UUID playerId, int limit) {
        List<LogEntry> logs = playerLogs.getOrDefault(playerId, new ArrayList<>());
        return logs.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Récupère les logs d'un clan
     */
    public List<LogEntry> getClanLogs(UUID clanOwnerId, int limit) {
        List<LogEntry> logs = clanLogs.getOrDefault(clanOwnerId, new ArrayList<>());
        return logs.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Récupère les logs par catégorie
     */
    public List<LogEntry> getLogsByCategory(ChallengeCategory category, int limit) {
        return recentLogs.stream()
                .filter(log -> log.getCategory() == category)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les logs par type d'événement
     */
    public List<LogEntry> getLogsByType(LogType type, int limit) {
        return recentLogs.stream()
                .filter(log -> log.getType() == type)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Génère un résumé des activités récentes
     */
    public Map<String, Object> getActivitySummary(int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        List<LogEntry> recentActivity = recentLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(cutoff))
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("total_events", recentActivity.size());
        summary.put("points_distributed", recentActivity.stream()
                .filter(log -> log.getType() == LogType.POINT_GAIN)
                .mapToInt(LogEntry::getPoints)
                .sum());

        // Activité par catégorie
        Map<ChallengeCategory, Integer> byCategory = recentActivity.stream()
                .filter(log -> log.getCategory() != null)
                .collect(Collectors.groupingBy(
                        LogEntry::getCategory,
                        Collectors.summingInt(LogEntry::getPoints)
                ));
        summary.put("by_category", byCategory);

        // Joueurs les plus actifs
        Map<UUID, Integer> playerActivity = recentActivity.stream()
                .filter(log -> log.getPlayerId() != null && log.getType() == LogType.POINT_GAIN)
                .collect(Collectors.groupingBy(
                        LogEntry::getPlayerId,
                        Collectors.summingInt(LogEntry::getPoints)
                ));
        summary.put("top_players", playerActivity);

        return summary;
    }

    /**
     * Sauvegarde les logs de manière asynchrone
     */
    private void saveLogsAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(ClassyClanChallenges.getInstance(), this::saveLogs);
    }

    /**
     * Sauvegarde les logs dans un fichier
     */
    public void saveLogs() {
        File file = new File(ClassyClanChallenges.getInstance().getDataFolder(), "data/activity-logs.yml");
        YamlConfiguration config = new YamlConfiguration();

        // Sauvegarder seulement les 500 logs les plus récents
        List<LogEntry> logsToSave = recentLogs.stream().limit(500).collect(Collectors.toList());

        for (int i = 0; i < logsToSave.size(); i++) {
            LogEntry log = logsToSave.get(i);
            String path = "logs." + i;

            if (log.getPlayerId() != null) {
                config.set(path + ".player_id", log.getPlayerId().toString());
            }
            if (log.getCategory() != null) {
                config.set(path + ".category", log.getCategory().name());
            }
            config.set(path + ".type", log.getType().name());
            config.set(path + ".points", log.getPoints());
            config.set(path + ".reason", log.getReason());
            config.set(path + ".details", log.getDetails());
            config.set(path + ".timestamp", log.getTimestamp().format(formatter));
        }

        config.set("metadata.last_save", LocalDateTime.now().format(formatter));
        config.set("metadata.total_logs", logsToSave.size());

        try {
            config.save(file);
        } catch (IOException e) {
            ClassyClanChallenges.getInstance().getLogger().severe("Erreur lors de la sauvegarde des logs: " + e.getMessage());
        }
    }

    /**
     * Charge les logs depuis le fichier
     */
    public void loadLogs() {
        File file = new File(ClassyClanChallenges.getInstance().getDataFolder(), "data/activity-logs.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection logsSection = config.getConfigurationSection("logs");
        if (logsSection == null) return;

        recentLogs.clear();

        for (String key : logsSection.getKeys(false)) {
            try {
                ConfigurationSection logSection = logsSection.getConfigurationSection(key);
                if (logSection == null) continue;

                UUID playerId = null;
                String playerIdStr = logSection.getString("player_id");
                if (playerIdStr != null) {
                    playerId = UUID.fromString(playerIdStr);
                }

                ChallengeCategory category = null;
                String categoryStr = logSection.getString("category");
                if (categoryStr != null) {
                    category = ChallengeCategory.valueOf(categoryStr);
                }

                LogType type = LogType.valueOf(logSection.getString("type"));
                int points = logSection.getInt("points");
                String reason = logSection.getString("reason", "");
                String details = logSection.getString("details", "");
                LocalDateTime timestamp = LocalDateTime.parse(logSection.getString("timestamp"), formatter);

                LogEntry entry = new LogEntry(playerId, category, type, points, reason, details, timestamp);
                recentLogs.add(entry);

                // Réorganiser dans les maps appropriées
                if (playerId != null) {
                    playerLogs.computeIfAbsent(playerId, k -> new ArrayList<>()).add(entry);

                    Clan clan = ClassyClan.getAPI().getClanOf(playerId);
                    if (clan != null) {
                        clanLogs.computeIfAbsent(clan.getOwner(), k -> new ArrayList<>()).add(entry);
                    }
                }

            } catch (Exception e) {
                ClassyClanChallenges.getInstance().getLogger().warning("Erreur lors du chargement du log " + key + ": " + e.getMessage());
            }
        }

        ClassyClanChallenges.getInstance().getLogger().info("Chargé " + recentLogs.size() + " entrées de logs.");
    }
}