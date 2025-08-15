package org.classyClanChallenges.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.contribution.ContributionManager;

public class ResetAllCommand extends SubCommand {
    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Réinitialise toutes les contributions (admin seulement).";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ContributionManager manager = ClassyClanChallenges.getInstance().getContributionManager();
        manager.resetAll();
        sender.sendMessage("§aToutes les contributions ont été réinitialisées.");
    }
}
