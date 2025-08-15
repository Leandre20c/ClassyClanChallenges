package org.classyClanChallenges.command.sub;

import org.bukkit.command.CommandSender;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.command.SubCommand;
import org.classyclan.ClassyClan;
import org.classyclan.clan.Clan;

import java.util.UUID;

public class ResetClanCommand extends SubCommand {

    @Override
    public String getName() {
        return "resetclan";
    }

    @Override
    public String getDescription() {
        return "Réinitialise les contributions d’un clan.";
    }

    @Override
    public String getUsage() {
        return "/jdc resetclan <clan_name>";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage : /jdc resetclan <nom>");
            return;
        }

        Clan clan = ClassyClan.getAPI().getClanByName(args[0]);
        if (clan == null) {
            sender.sendMessage("§cClan introuvable.");
            return;
        }

        UUID ownerId = clan.getOwner();
        ClassyClanChallenges.getInstance().getContributionManager().resetClan(ownerId);
        sender.sendMessage("§aContributions du clan §e" + clan.getRawName() + " §aréinitialisées.");
    }
}
