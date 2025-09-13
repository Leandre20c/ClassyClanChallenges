package org.classyClanChallenges.contribution;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.WeeklyChallenge;
import org.classyClanChallenges.logging.LogManager;
import org.classyClanChallenges.util.HotbarPointBuffer;
import org.classyclan.clan.Clan;
import org.classyclan.clan.ClanManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ContributionManager {

    private final Map<UUID, PlayerContribution> playerMap = new HashMap<>();
    private final Map<UUID, ClanContribution> clanMap = new HashMap<>();
    private final ClanManager clanManager;
    private WeeklyChallenge weeklyChallenge;

    private List<Map.Entry<UUID, Integer>> topPlayersGlobal = new ArrayList<>();
    private final Map<ChallengeCategory, List<Map.Entry<UUID, Integer>>> topPlayersByCategory = new HashMap<>();
    private final Map<ChallengeCategory, List<Map.Entry<UUID, Integer>>> topClansByCategory = new HashMap<>();

    // Référence au LogManager pour traçabilité
    private LogManager logManager;

    public ContributionManager(WeeklyChallenge activeChallenges, ClanManager clanManager) {
        this.weeklyChallenge = activeChallenges;
        this.clanManager = clanManager;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    // ===================== AJOUTS AVEC LOGS =====================

    public void addContribution(UUID playerId, ChallengeCategory category, int amount) {
        addContribution(playerId, category, amount, "Action en jeu", "Événement automatique");
    }

    public void addContribution(UUID playerId, ChallengeCategory category, int amount, String reason, String details) {
        if (amount <= 0) return;

        playerMap.computeIfAbsent(playerId, id -> new PlayerContribution()).add(category, amount);

        Clan clan = getClanOf(playerId);
        if (clan != null) {
            UUID clanId = clan.getOwner();
            clanMap.computeIfAbsent(clanId, id -> new ClanContribution()).add(category, amount);
        }

        HotbarPointBuffer.addPoints(playerId, category, amount);

        // LOG DÉTAILLÉ
        if (logManager != null) {
            String enrichedDetails = details;
            if (clan != null) {
                enrichedDetails += " | Clan: " + clan.getRawName();
            }
            logManager.logPointGain(playerId, category, amount, reason, enrichedDetails);
        }

        recalculateTops();
        saveAll();
    }

    /**
     * Définit les points exacts d'un joueur dans une catégorie avec logs admin
     */
    public void setPlayerPoints(UUID playerId, ChallengeCategory category, int newAmount, String adminName) {
        if (newAmount < 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être négatif");
        }

        PlayerContribution playerContrib = getPlayerContribution(playerId);
        int oldPlayerAmount = playerContrib.get(category);
        int playerDifference = newAmount - oldPlayerAmount;

        // LOG AVANT MODIFICATION
        if (logManager != null) {
            logManager.logPointModification(playerId, category, oldPlayerAmount, newAmount,
                    "Définition manuelle des points", adminName);
        }

        // Définir la nouvelle valeur pour le joueur
        playerContrib.set(category, newAmount);

        // Mettre à jour le clan si applicable
        Clan clan = getClanOf(playerId);
        if (clan != null) {
            UUID clanId = clan.getOwner();
            ClanContribution clanContrib = getClanContribution(clanId);
            int oldClanAmount = clanContrib.get(category);
            int newClanAmount = Math.max(0, oldClanAmount + playerDifference);
            clanContrib.set(category, newClanAmount);
        }

        recalculateTops();
        saveAll();
    }

    // Version de compatibilité sans admin name (pour l'ancien code)
    public void setPlayerPoints(UUID playerId, ChallengeCategory category, int newAmount) {
        setPlayerPoints(playerId, category, newAmount, "Système");
    }

    public void addGenericContribution(UUID playerId, ChallengeCategory category, String target, int count) {
        addGenericContribution(playerId, category, target, count, "Action spécifique", target);
    }

    public void addGenericContribution(UUID playerId, ChallengeCategory category, String target, int count, String reason, String details) {
        ChallengeEntry challenge = weeklyChallenge.getChallenge(category);
        if (challenge != null && challenge.getTarget().equalsIgnoreCase(target)) {
            addContribution(playerId, category, count, reason, details);
        }
    }

    // ===================== ACCÈS (IDENTIQUE) =====================

    public PlayerContribution getPlayerContribution(UUID playerId) {
        return playerMap.computeIfAbsent(playerId, id -> new PlayerContribution());
    }

    public ClanContribution getClanContribution(UUID clanOwnerId) {
        return clanMap.computeIfAbsent(clanOwnerId, id -> new ClanContribution());
    }

    private Clan getClanOf(UUID playerId) {
        Clan clan = clanManager.getClanByPlayer(playerId);
        if (clan == null) {
            for (Clan c : clanManager.getAllClans()) {
                if (c.getMembers().contains(playerId)) {
                    clanManager.setPlayerClan(playerId, c);
                    return c;
                }
            }
        }
        return clan;
    }

    public Map<UUID, PlayerContribution> getAllPlayerContributions() {
        return Collections.unmodifiableMap(playerMap);
    }

    public Map<UUID, ClanContribution> getAllClanContributions() {
        return Collections.unmodifiableMap(clanMap);
    }

    public void updateWeeklyChallenge(WeeklyChallenge newChallenge) {
        this.weeklyChallenge = newChallenge;
    }

    // ===================== RESET AVEC LOGS =====================

    public void resetPlayer(UUID playerId, String adminName) {
        if (logManager != null) {
            logManager.logSystemEvent("Reset joueur",
                    "Joueur: " + playerId + " réinitialisé", adminName);
        }
        playerMap.remove(playerId);
        saveAll();
    }

    public void resetPlayer(UUID playerId) {
        resetPlayer(playerId, "Système");
    }

    public void resetClan(UUID clanOwnerId, String adminName) {
        if (logManager != null) {
            logManager.logSystemEvent("Reset clan",
                    "Clan: " + clanOwnerId + " réinitialisé", adminName);
        }
        clanMap.remove(clanOwnerId);
        saveAll();
    }

    public void resetClan(UUID clanOwnerId) {
        resetClan(clanOwnerId, "Système");
    }

    public void resetAll(String adminName) {
        if (logManager != null) {
            logManager.logSystemEvent("Reset complet",
                    "Toutes les contributions réinitialisées. " +
                            "Joueurs: " + playerMap.size() + ", Clans: " + clanMap.size(), adminName);
        }
        playerMap.clear();
        clanMap.clear();
        saveAll();
    }

    public void resetAll() {
        resetAll("Système");
    }

    // ===================== TOPS (IDENTIQUE) =====================

    public void recalculateTops() {
        // Global player
        topPlayersGlobal = playerMap.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().getTotal()))
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .toList();

        // Per category
        for (ChallengeCategory category : ChallengeCategory.values()) {
            List<Map.Entry<UUID, Integer>> topPlayers = playerMap.entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), entry.getValue().get(category)))
                    .filter(entry -> entry.getValue() > 0)
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .toList();
            topPlayersByCategory.put(category, topPlayers);

            List<Map.Entry<UUID, Integer>> topClans = clanMap.entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), entry.getValue().get(category)))
                    .filter(entry -> entry.getValue() > 0)
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .toList();
            topClansByCategory.put(category, topClans);
        }
    }

    public List<Map.Entry<UUID, Integer>> getTopPlayersGlobal() {
        return topPlayersGlobal;
    }

    public List<Map.Entry<UUID, Integer>> getTopPlayersByCategory(ChallengeCategory category) {
        return topPlayersByCategory.getOrDefault(category, List.of());
    }

    public List<Map.Entry<UUID, Integer>> getTopClansByCategory(ChallengeCategory category) {
        return topClansByCategory.getOrDefault(category, List.of());
    }

    public List<Map.Entry<UUID, Integer>> getTopClansGlobal() {
        Map<UUID, Integer> totals = new HashMap<>();

        for (Map.Entry<UUID, ClanContribution> entry : clanMap.entrySet()) {
            totals.put(entry.getKey(), entry.getValue().getTotal());
        }

        return totals.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .toList();
    }

    // ===================== SAUVEGARDE / CHARGEMENT (IDENTIQUE) =====================

    public void saveAll() {
        File file = new File(ClassyClanChallenges.getInstance().getDataFolder(), "data/contributions.yml");
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, ClanContribution> clanEntry : clanMap.entrySet()) {
            UUID clanId = clanEntry.getKey();
            for (Map.Entry<ChallengeCategory, Integer> entry : clanEntry.getValue().getAll().entrySet()) {
                config.set("clans." + clanId + "." + entry.getKey().name(), entry.getValue());
            }
        }

        for (Map.Entry<UUID, PlayerContribution> playerEntry : playerMap.entrySet()) {
            UUID playerId = playerEntry.getKey();
            for (Map.Entry<ChallengeCategory, Integer> entry : playerEntry.getValue().getAll().entrySet()) {
                config.set("players." + playerId + "." + entry.getKey().name(), entry.getValue());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadAll() {
        File file = new File(ClassyClanChallenges.getInstance().getDataFolder(), "data/contributions.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection clans = config.getConfigurationSection("clans");
        if (clans != null) {
            for (String clanIdStr : clans.getKeys(false)) {
                UUID clanId = UUID.fromString(clanIdStr);
                ConfigurationSection clanSection = clans.getConfigurationSection(clanIdStr);
                if (clanSection == null) continue;

                for (String cat : clanSection.getKeys(false)) {
                    ChallengeCategory category = ChallengeCategory.valueOf(cat);
                    int value = clanSection.getInt(cat);
                    getClanContribution(clanId).add(category, value);
                }
            }
        }

        ConfigurationSection players = config.getConfigurationSection("players");
        if (players != null) {
            for (String playerIdStr : players.getKeys(false)) {
                UUID playerId = UUID.fromString(playerIdStr);
                ConfigurationSection playerSection = players.getConfigurationSection(playerIdStr);
                if (playerSection == null) continue;

                for (String cat : playerSection.getKeys(false)) {
                    ChallengeCategory category = ChallengeCategory.valueOf(cat);
                    int value = playerSection.getInt(cat);
                    getPlayerContribution(playerId).add(category, value);
                }
            }
        }

        recalculateTops();

        // LOG DU CHARGEMENT
        if (logManager != null) {
            logManager.logSystemEvent("Chargement des données",
                    "Chargé " + playerMap.size() + " joueurs et " + clanMap.size() + " clans", null);
        }
    }
}