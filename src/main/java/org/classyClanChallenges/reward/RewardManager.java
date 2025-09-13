package org.classyClanChallenges.reward;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.WeeklyChallenge;
import org.classyClanChallenges.contribution.ContributionManager;
import org.classyclan.ClassyClan;
import org.classyclan.clan.Clan;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RewardManager {

    private YamlConfiguration config;
    private File configFile;
    private Map<Integer, Integer> clanRankingRewards = new HashMap<>();
    private Map<Integer, List<String>> playerRankingRewards = new HashMap<>();

    public RewardManager() {
        loadRewards();
    }

    /**
     * Charge ou recharge les récompenses depuis le fichier
     */
    public void loadRewards() {
        configFile = new File(ClassyClanChallenges.getInstance().getDataFolder(), "rewards.yml");

        // Créer le fichier par défaut s'il n'existe pas
        if (!configFile.exists()) {
            createDefaultRewardsFile();
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Charger les récompenses de classement des clans
        clanRankingRewards.clear();
        if (config.contains("clan-ranking-rewards")) {
            for (String key : config.getConfigurationSection("clan-ranking-rewards").getKeys(false)) {
                try {
                    if (key.equals("default")) {
                        clanRankingRewards.put(0, config.getInt("clan-ranking-rewards.default"));
                    } else {
                        int rank = Integer.parseInt(key);
                        clanRankingRewards.put(rank, config.getInt("clan-ranking-rewards." + key));
                    }
                } catch (NumberFormatException e) {
                    ClassyClanChallenges.getInstance().getLogger().warning(
                            "Rang de clan invalide dans rewards.yml: " + key);
                }
            }
        }

        // Charger les récompenses de classement des joueurs
        playerRankingRewards.clear();
        if (config.contains("player-ranking-rewards")) {
            for (String key : config.getConfigurationSection("player-ranking-rewards").getKeys(false)) {
                try {
                    int rank = Integer.parseInt(key);
                    List<String> commands = config.getStringList("player-ranking-rewards." + key);
                    playerRankingRewards.put(rank, commands);
                } catch (NumberFormatException e) {
                    ClassyClanChallenges.getInstance().getLogger().warning(
                            "Rang de joueur invalide dans rewards.yml: " + key);
                }
            }
        }

        ClassyClanChallenges.getInstance().getLogger().info(
                "Récompenses chargées: " + clanRankingRewards.size() + " rangs de clans, " +
                        playerRankingRewards.size() + " rangs de joueurs");
    }

    /**
     * Crée le fichier rewards.yml par défaut
     */
    private void createDefaultRewardsFile() {
        try {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();

            YamlConfiguration defaultConfig = new YamlConfiguration();

            // Récompenses par défaut pour les clans
            defaultConfig.set("clan-ranking-rewards.1", 7500);
            defaultConfig.set("clan-ranking-rewards.2", 4000);
            defaultConfig.set("clan-ranking-rewards.3", 2000);
            defaultConfig.set("clan-ranking-rewards.default", 500);

            // Récompenses par défaut pour les joueurs
            defaultConfig.set("player-ranking-rewards.1", Arrays.asList("crates give %player_name% leg 2"));
            defaultConfig.set("player-ranking-rewards.2", Arrays.asList("crates give %player_name% leg 1"));
            defaultConfig.set("player-ranking-rewards.3", Arrays.asList("crates give %player_name% epic 1"));

            // Récompenses bonus (nouvelles)
            defaultConfig.set("clan-bonus-rewards.participation.enabled", true);
            defaultConfig.set("clan-bonus-rewards.participation.min-points", 100);
            defaultConfig.set("clan-bonus-rewards.participation.reward", 250);

            defaultConfig.set("player-bonus-rewards.active-participation.enabled", true);
            defaultConfig.set("player-bonus-rewards.active-participation.min-points", 50);
            defaultConfig.set("player-bonus-rewards.active-participation.commands",
                    Arrays.asList("crates give %player_name% rare 1"));

            // Commentaires
            defaultConfig.setComments("clan-ranking-rewards", Arrays.asList(
                    "Récompenses pour les clans selon leur classement",
                    "1, 2, 3 = positions dans le top",
                    "default = bonus de participation pour tous les autres clans"
            ));

            defaultConfig.setComments("player-ranking-rewards", Arrays.asList(
                    "Récompenses pour les joueurs selon leur classement",
                    "Utilisez %player_name% pour le nom du joueur"
            ));

            defaultConfig.save(configFile);

        } catch (IOException e) {
            ClassyClanChallenges.getInstance().getLogger().severe(
                    "Impossible de créer rewards.yml par défaut: " + e.getMessage());
        }
    }

    /**
     * Distribue toutes les récompenses de fin de semaine
     */
    public void distributeRewards(WeeklyChallenge challenge, ContributionManager manager) {
        Map<UUID, Integer> totalClanGains = new HashMap<>();

        for (ChallengeCategory category : ChallengeCategory.values()) {
            ChallengeEntry entry = challenge.getChallenge(category);
            if (entry == null) continue;

            String target = entry.getTarget().toUpperCase();
            int unitValue = entry.getValue();

            // 🧮 Récompenses de production (base)
            distributeProductionRewards(category, manager, unitValue, totalClanGains);

            // 🏆 Récompenses de classement (clans)
            distributeClanRankingRewards(category, manager, totalClanGains);

            // 🎁 Bonus de participation (clans)
            distributeClanParticipationBonus(category, manager, totalClanGains);

            // 🎖 Récompenses de classement (joueurs)
            distributePlayerRankingRewards(category, manager);

            // 🌟 Bonus de participation (joueurs)
            distributePlayerParticipationBonus(category, manager);
        }

        // Log des gains totaux
        logTotalGains(totalClanGains);
    }

    private void distributeProductionRewards(ChallengeCategory category, ContributionManager manager,
                                             int unitValue, Map<UUID, Integer> totalClanGains) {
        for (Map.Entry<UUID, Clan> clanEntry : getAllClansWithContributions(manager, category).entrySet()) {
            UUID clanId = clanEntry.getKey();
            Clan clan = clanEntry.getValue();
            int contribution = manager.getClanContribution(clanId).get(category);

            if (contribution > 0) {
                double reward = (double) (contribution * unitValue) / 100; // Divisé par 100 comme avant
                clan.deposit(reward);

                totalClanGains.put(clanId, totalClanGains.getOrDefault(clanId, 0) + (int) reward);
            }
        }
    }

    private void distributeClanRankingRewards(ChallengeCategory category, ContributionManager manager,
                                              Map<UUID, Integer> totalClanGains) {
        List<Map.Entry<UUID, Integer>> topClans = manager.getTopClansByCategory(category);

        for (int i = 0; i < topClans.size(); i++) {
            UUID clanId = topClans.get(i).getKey();
            int rank = i + 1;
            int reward = getClanRankingBonus(rank);

            if (reward > 0) {
                Clan clan = ClassyClan.getAPI().getClanOf(clanId);
                if (clan != null) {
                    clan.deposit(reward);
                    totalClanGains.put(clanId, totalClanGains.getOrDefault(clanId, 0) + reward);

                    // Log spécifique pour les récompenses de classement
                    if (ClassyClanChallenges.getInstance().getLogManager() != null) {
                        ClassyClanChallenges.getInstance().getLogManager().logSystemEvent(
                                "Récompense classement clan",
                                String.format("Clan %s - Rang %d en %s: %d PE",
                                        clan.getRawName(), rank, category.name(), reward),
                                null
                        );
                    }
                }
            }
        }
    }

    private void distributeClanParticipationBonus(ChallengeCategory category, ContributionManager manager,
                                                  Map<UUID, Integer> totalClanGains) {
        int participationBonus = getClanRankingBonus(0); // Bonus par défaut
        if (participationBonus <= 0) return;

        List<Map.Entry<UUID, Integer>> topClans = manager.getTopClansByCategory(category);
        Set<UUID> topClanIds = new HashSet<>();
        topClans.forEach(entry -> topClanIds.add(entry.getKey()));

        // Donner le bonus aux clans non-classés mais participants
        for (UUID clanId : manager.getAllClanContributions().keySet()) {
            if (!topClanIds.contains(clanId)) {
                int score = manager.getClanContribution(clanId).get(category);
                if (score > 0) {
                    Clan clan = ClassyClan.getAPI().getClanOf(clanId);
                    if (clan != null) {
                        clan.deposit(participationBonus);
                        totalClanGains.put(clanId, totalClanGains.getOrDefault(clanId, 0) + participationBonus);
                    }
                }
            }
        }
    }

    private void distributePlayerRankingRewards(ChallengeCategory category, ContributionManager manager) {
        List<Map.Entry<UUID, Integer>> topPlayers = manager.getTopPlayersByCategory(category);

        for (int i = 0; i < topPlayers.size(); i++) {
            UUID playerId = topPlayers.get(i).getKey();
            int rank = i + 1;

            List<String> commands = playerRankingRewards.get(rank);
            if (commands != null && !commands.isEmpty()) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
                String playerName = player.getName();

                if (playerName != null) {
                    for (String cmd : commands) {
                        String finalCommand = cmd.replace("%player_name%", playerName);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                    }

                    // Log des récompenses joueurs
                    if (ClassyClanChallenges.getInstance().getLogManager() != null) {
                        ClassyClanChallenges.getInstance().getLogManager().logSystemEvent(
                                "Récompense classement joueur",
                                String.format("Joueur %s - Rang %d en %s: %d commandes",
                                        playerName, rank, category.name(), commands.size()),
                                null
                        );
                    }
                }
            }
        }
    }

    private void distributePlayerParticipationBonus(ChallengeCategory category, ContributionManager manager) {
        if (!config.getBoolean("player-bonus-rewards.active-participation.enabled", false)) {
            return;
        }

        int minPoints = config.getInt("player-bonus-rewards.active-participation.min-points", 50);
        List<String> commands = config.getStringList("player-bonus-rewards.active-participation.commands");

        if (commands.isEmpty()) return;

        List<Map.Entry<UUID, Integer>> topPlayers = manager.getTopPlayersByCategory(category);
        Set<UUID> topPlayerIds = new HashSet<>();
        topPlayers.stream().limit(3).forEach(entry -> topPlayerIds.add(entry.getKey())); // Top 3 exclus

        // Donner le bonus aux joueurs actifs mais non-classés top 3
        for (UUID playerId : manager.getAllPlayerContributions().keySet()) {
            if (!topPlayerIds.contains(playerId)) {
                int points = manager.getPlayerContribution(playerId).get(category);
                if (points >= minPoints) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
                    String playerName = player.getName();

                    if (playerName != null) {
                        for (String cmd : commands) {
                            String finalCommand = cmd.replace("%player_name%", playerName);
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                        }
                    }
                }
            }
        }
    }

    private void logTotalGains(Map<UUID, Integer> totalClanGains) {
        if (ClassyClanChallenges.getInstance().getLogManager() != null) {
            int totalDistributed = totalClanGains.values().stream().mapToInt(Integer::intValue).sum();

            ClassyClanChallenges.getInstance().getLogManager().logSystemEvent(
                    "Distribution récompenses hebdomadaires",
                    String.format("Total distribué: %d PE à %d clans", totalDistributed, totalClanGains.size()),
                    null
            );
        }
    }

    // ===================== MÉTHODES UTILITAIRES =====================

    private Map<UUID, Clan> getAllClansWithContributions(ContributionManager manager, ChallengeCategory category) {
        Map<UUID, Clan> result = new HashMap<>();
        for (UUID clanId : manager.getAllClanContributions().keySet()) {
            int amount = manager.getClanContribution(clanId).get(category);
            if (amount > 0) {
                Clan clan = ClassyClan.getAPI().getClanOf(clanId);
                if (clan != null) {
                    result.put(clanId, clan);
                }
            }
        }
        return result;
    }

    public int getClanRankingBonus(int rank) {
        if (rank == 0) return clanRankingRewards.getOrDefault(0, 0);
        return clanRankingRewards.getOrDefault(rank, 0);
    }

    /**
     * Retourne les informations de configuration actuelles
     */
    public Map<String, Object> getConfigInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("clan_rewards", clanRankingRewards);
        info.put("player_rewards", playerRankingRewards.keySet());
        info.put("config_file_exists", configFile.exists());
        info.put("config_file_size", configFile.length());
        info.put("last_modified", new Date(configFile.lastModified()));
        return info;
    }

    /**
     * Sauvegarde la configuration actuelle
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            ClassyClanChallenges.getInstance().getLogger().severe(
                    "Erreur lors de la sauvegarde de rewards.yml: " + e.getMessage());
        }
    }

    /**
     * Ajoute une nouvelle récompense de clan
     */
    public void addClanReward(int rank, int amount) {
        clanRankingRewards.put(rank, amount);
        config.set("clan-ranking-rewards." + (rank == 0 ? "default" : rank), amount);
        saveConfig();
    }

    /**
     * Ajoute une nouvelle récompense de joueur
     */
    public void addPlayerReward(int rank, List<String> commands) {
        playerRankingRewards.put(rank, commands);
        config.set("player-ranking-rewards." + rank, commands);
        saveConfig();
    }
}