package org.classyClanChallenges.command.sub;

import org.bukkit.command.CommandSender;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.command.JDCCommand;

public class HelpCommand extends SubCommand {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Affiche l’aide du plugin.";
    }

    @Override
    public String getUsage() {
        return "/jdc help";
    }

    @Override
    public boolean isAdminOnly() {
        return false;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6§l❖ Jeux de clan - Aide");

        for (SubCommand sub : JDCCommand.getSubCommands()) {
            if (!sub.isAdminOnly() || sender.isOp() || sender.hasPermission("jdc.admin")) {
                String usage = sub.getUsage().isEmpty() ? "/jdc " + sub.getName() : sub.getUsage();
                sender.sendMessage("§e" + usage + "§7 - " + sub.getDescription());
            }
        }
    }
}
