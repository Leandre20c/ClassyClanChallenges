package org.classyClanChallenges.command.sub;

import org.bukkit.command.CommandSender;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.command.SubCommand;

public class RotateCommand extends SubCommand {

    @Override
    public String getName() {
        return "rotate";
    }

    @Override
    public String getDescription() {
        return "Force le changement de semaine, génère de nouveaux défis, reset et récompenses.";
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
        ClassyClanChallenges.getInstance().getChallengeManager().resetWeeklyChallenges();
        sender.sendMessage("§aNouvelle semaine générée avec succès.");
    }
}
