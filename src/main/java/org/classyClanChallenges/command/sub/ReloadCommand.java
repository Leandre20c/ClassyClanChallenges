package org.classyClanChallenges.command.sub;

import org.bukkit.command.CommandSender;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.challenges.ChallengeManager;
import org.classyClanChallenges.reward.RewardManager;

public class ReloadCommand extends SubCommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Recharge les fichiers de configuration.";
    }

    @Override
    public String getUsage() {
        return "/jdc reload";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ClassyClanChallenges plugin = ClassyClanChallenges.getInstance();

        // Reload config.yml
        plugin.reloadConfig();

        // Reload challenges.yml
        ChallengeManager challengeManager = plugin.getChallengeManager();
        challengeManager.loadChallenges();
        challengeManager.loadActiveChallengesFromFile();

        // Reload rewards
        RewardManager rm = plugin.getRewardManager();


        sender.sendMessage("§aFichiers rechargés : §fconfig.yml, challenges.yml, rewards.yml, data/weekly.yml");
    }
}
