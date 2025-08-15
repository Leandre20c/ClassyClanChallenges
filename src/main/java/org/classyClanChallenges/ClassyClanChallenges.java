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
import org.classyClanChallenges.placeholder.JDCPlaceholders;
import org.classyClanChallenges.reward.RewardManager;
import org.classyClanChallenges.scheduler.WeeklyResetTask;
import org.classyclan.ClassyClan;

public final class ClassyClanChallenges extends JavaPlugin {

    private static ClassyClanChallenges instance;

    private ChallengeManager challengeManager;
    private ContributionManager contributionManager;
    private RewardManager rewardManager;

    public static ClassyClanChallenges getInstance() {
        return instance;
    }

    public ChallengeManager getChallengeManager() {
        return challengeManager;
    }

    public ContributionManager getContributionManager() {
        return contributionManager;
    }

    public RewardManager getRewardManager() {return rewardManager;}

    @Override
    public void onEnable() {
        if (ClassyClan.getAPI() == null) {
            getLogger().severe("ClassyClan API is not available! Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new JDCPlaceholders(this).register();
        }


        instance = this;

        // Init
        saveDefaultConfig();
        challengeManager = new ChallengeManager();
        WeeklyChallenge weeklyChallenge = challengeManager.getActiveChallenges();
        contributionManager = new ContributionManager(weeklyChallenge, ClassyClan.getInstance().getClanManager());
        this.rewardManager = new RewardManager();

        Bukkit.getPluginManager().registerEvents(new ClassementCommand(), this);


        // Génération des défis hebdomadaires
        getChallengeManager().loadActiveChallengesFromFile();
        getContributionManager().loadAll();

        // Listeners avec paramètres correctement passés
        Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(weeklyChallenge), this);
        Bukkit.getPluginManager().registerEvents(new MiningListener(challengeManager, contributionManager), this);
        Bukkit.getPluginManager().registerEvents(new CraftingListener(challengeManager, contributionManager), this);
        Bukkit.getPluginManager().registerEvents(new KillListener(), this);
        Bukkit.getPluginManager().registerEvents(new ActionListener(), this);
        Bukkit.getPluginManager().registerEvents(new PistonListener(), this);
        Bukkit.getPluginManager().registerEvents(
                new EnchantListener(getChallengeManager().getActiveChallenges(), getContributionManager()), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        new WeeklyResetTask(this).runTaskTimerAsynchronously(this, 0L, 20L * 60);

        getCommand("jdc").setExecutor(new JDCCommand());

        getLogger().info("ClassyClanChallenges chargé avec succès !");
    }

    @Override
    public void onDisable() {
        challengeManager.saveActiveChallenges();
        contributionManager.saveAll();
        getLogger().info("ClassyClanChallenges désactivé.");
    }
}
