package org.classyClanChallenges;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.classyClanChallenges.challenges.ChallengeManager;
import org.classyClanChallenges.challenges.WeeklyChallenge;
import org.classyClanChallenges.command.JDCCommand;
import org.classyClanChallenges.command.sub.ClassementCommand;
import org.classyClanChallenges.contribution.ContributionManager;
import org.classyClanChallenges.listener.*;
import org.classyClanChallenges.logging.LogManager;
import org.classyClanChallenges.placeholder.JDCPlaceholders;
import org.classyClanChallenges.reward.RewardManager;
import org.classyClanChallenges.scheduler.WeeklyResetTask;
import org.classyClanChallenges.stats.StatsManager;
import org.classyClanChallenges.util.BlockDataManager;
import org.classyclan.ClassyClan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClassyClanChallenges extends JavaPlugin {

    private static ClassyClanChallenges instance;

    private ChallengeManager challengeManager;
    private ContributionManager contributionManager;
    private RewardManager rewardManager;
    private BlockDataManager blockDataManager;
    private LogManager logManager;
    private StatsManager statsManager;

    public static ClassyClanChallenges getInstance() {
        return instance;
    }

    // ===================== GETTERS =====================

    public ChallengeManager getChallengeManager() {
        return challengeManager;
    }

    public ContributionManager getContributionManager() {
        return contributionManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public BlockDataManager getBlockDataManager() {
        return blockDataManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    // ===================== SETTERS POUR RELOAD =====================

    public void setRewardManager(RewardManager rewardManager) {
        this.rewardManager = rewardManager;
        getLogger().info("RewardManager rechargé avec succès.");
    }

    public void setChallengeManager(ChallengeManager challengeManager) {
        this.challengeManager = challengeManager;

        // Mettre à jour le ContributionManager avec les nouveaux défis
        if (this.contributionManager != null) {
            this.contributionManager.updateWeeklyChallenge(challengeManager.getActiveChallenges());
        }

        getLogger().info("ChallengeManager rechargé avec succès.");
    }

    @Override
    public void onEnable() {
        if (ClassyClan.getAPI() == null) {
            getLogger().severe("ClassyClan API is not available! Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        instance = this;

        // Créer les dossiers nécessaires
        createDirectories();

        // Initialisation des composants principaux
        initializeComponents();

        // PlaceholderAPI support
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new JDCPlaceholders(this).register();
            getLogger().info("PlaceholderAPI hook activé.");
        }

        // Enregistrement des événements
        registerEvents();

        // Chargement des données
        loadData();

        // Démarrage du scheduler de reset hebdomadaire
        new WeeklyResetTask(this).runTaskTimerAsynchronously(this, 0L, 20L * 60);

        // Enregistrement des commandes
        getCommand("jdc").setExecutor(new JDCCommand());

        // Nettoyage initial des blocs invalides
        Bukkit.getScheduler().runTaskLater(this, () -> {
            blockDataManager.cleanupInvalidWorlds();
        }, 100L);

        // Log de démarrage avec statistiques
        logStartupInfo();

        getLogger().info("ClassyClanChallenges activé avec succès !");
    }

    @Override
    public void onDisable() {
        // Sauvegarde de toutes les données
        saveAllData();

        // Log de fermeture
        if (logManager != null) {
            logManager.logSystemEvent("Arrêt du plugin",
                    "ClassyClanChallenges désactivé. Données sauvegardées.", null);
            logManager.saveLogs();
        }

        getLogger().info("ClassyClanChallenges désactivé proprement.");
    }

    // ===================== MÉTHODES PRIVÉES D'INITIALISATION =====================

    private void createDirectories() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        java.io.File dataDir = new java.io.File(getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    private void initializeComponents() {
        saveDefaultConfig();

        // Initialisation dans l'ordre correct
        challengeManager = new ChallengeManager();
        WeeklyChallenge weeklyChallenge = challengeManager.getActiveChallenges();

        contributionManager = new ContributionManager(weeklyChallenge, ClassyClan.getInstance().getClanManager());
        rewardManager = new RewardManager();
        blockDataManager = new BlockDataManager();

        // Initialisation du système de logs
        logManager = new LogManager();

        // Connecter le LogManager au ContributionManager
        contributionManager.setLogManager(logManager);

        // Initialisation du gestionnaire de statistiques
        statsManager = new StatsManager(logManager, contributionManager);

        getLogger().info("Tous les gestionnaires initialisés avec succès.");
    }

    private void registerEvents() {
        WeeklyChallenge weeklyChallenge = challengeManager.getActiveChallenges();

        // Enregistrement des listeners
        Bukkit.getPluginManager().registerEvents(new ClassementCommand(), this);
        Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(weeklyChallenge), this);
        Bukkit.getPluginManager().registerEvents(new MiningListener(challengeManager, contributionManager), this);
        Bukkit.getPluginManager().registerEvents(new CraftingListener(challengeManager, contributionManager), this);
        Bukkit.getPluginManager().registerEvents(new KillListener(), this);
        Bukkit.getPluginManager().registerEvents(new ActionListener(), this);
        Bukkit.getPluginManager().registerEvents(new PistonListener(), this);
        Bukkit.getPluginManager().registerEvents(
                new EnchantListener(challengeManager.getActiveChallenges(), contributionManager), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        getLogger().info("Tous les listeners enregistrés.");
    }

    private void loadData() {
        // Chargement des défis hebdomadaires
        challengeManager.loadActiveChallengesFromFile();

        // Chargement des contributions
        contributionManager.loadAll();

        // Chargement des logs
        logManager.loadLogs();

        getLogger().info("Toutes les données chargées.");
    }

    private void saveAllData() {
        try {
            challengeManager.saveActiveChallenges();
            contributionManager.saveAll();

            if (logManager != null) {
                logManager.saveLogs();
            }

            getLogger().info("Toutes les données sauvegardées.");
        } catch (Exception e) {
            getLogger().severe("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    private void logStartupInfo() {
        if (logManager != null) {
            StringBuilder startupInfo = new StringBuilder();
            startupInfo.append("Plugin démarré. ");
            startupInfo.append("Blocs tracés: ").append(blockDataManager.getStoredBlockCount()).append(", ");
            startupInfo.append("Joueurs en cache: ").append(contributionManager.getAllPlayerContributions().size()).append(", ");
            startupInfo.append("Clans en cache: ").append(contributionManager.getAllClanContributions().size());

            logManager.logSystemEvent("Démarrage du plugin", startupInfo.toString(), null);
        }

        getLogger().info("=== ÉTAT AU DÉMARRAGE ===");
        getLogger().info("Blocs tracés: " + blockDataManager.getStoredBlockCount());
        getLogger().info("Joueurs actifs: " + contributionManager.getAllPlayerContributions().size());
        getLogger().info("Clans actifs: " + contributionManager.getAllClanContributions().size());
        getLogger().info("Défis actifs: " + challengeManager.getActiveChallenges().getAllChallenges().size());
        getLogger().info("========================");
    }

    // ===================== MÉTHODES UTILITAIRES =====================

    /**
     * Recharge complètement tous les gestionnaires (pour debug)
     */
    public void reloadAll() {
        try {
            reloadConfig();
            challengeManager.loadChallenges();
            challengeManager.loadActiveChallengesFromFile();
            contributionManager.loadAll();
            rewardManager.loadRewards();
            logManager.loadLogs();
            blockDataManager.cleanupInvalidWorlds();

            getLogger().info("Rechargement complet effectué avec succès.");

            if (logManager != null) {
                logManager.logSystemEvent("Rechargement complet",
                        "Tous les gestionnaires rechargés", "SYSTÈME");
            }
        } catch (Exception e) {
            getLogger().severe("Erreur lors du rechargement complet: " + e.getMessage());
        }
    }

    /**
     * Retourne des informations de debug sur l'état du plugin
     */
    public Map<String, Object> getPluginStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("plugin_version", getDescription().getVersion());
        status.put("bukkit_version", Bukkit.getVersion());
        status.put("classyclan_api", ClassyClan.getAPI() != null);
        status.put("players_online", Bukkit.getOnlinePlayers().size());
        status.put("max_players", Bukkit.getMaxPlayers());

        // État des gestionnaires
        status.put("challenge_manager", challengeManager != null);
        status.put("contribution_manager", contributionManager != null);
        status.put("reward_manager", rewardManager != null);
        status.put("log_manager", logManager != null);
        status.put("stats_manager", statsManager != null);
        status.put("block_manager", blockDataManager != null);

        // Statistiques
        if (contributionManager != null) {
            status.put("active_players", contributionManager.getAllPlayerContributions().size());
            status.put("active_clans", contributionManager.getAllClanContributions().size());
        }

        if (blockDataManager != null) {
            status.put("tracked_blocks", blockDataManager.getStoredBlockCount());
        }

        if (challengeManager != null) {
            status.put("active_challenges", challengeManager.getActiveChallenges().getAllChallenges().size());
            status.put("total_challenges_pool", challengeManager.getPool().values().stream()
                    .mapToInt(List::size).sum());
        }

        // Mémoire
        Runtime runtime = Runtime.getRuntime();
        status.put("memory_used_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        status.put("memory_max_mb", runtime.maxMemory() / 1024 / 1024);

        return status;
    }
}