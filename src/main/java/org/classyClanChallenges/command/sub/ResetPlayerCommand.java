package org.classyClanChallenges.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.contribution.ContributionManager;

import java.util.UUID;

public class ResetPlayerCommand extends SubCommand {

    @Override
    public String getName() {
        return "resetplayer";
    }

    @Override
    public String getDescription() {
        return "Réinitialise les contributions d’un joueur.";
    }

    @Override
    public String getUsage() {
        return "/jdc resetplayer <player_name>";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUtilisation : /jdc resetplayer <joueur>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            sender.sendMessage("§cJoueur inconnu ou jamais connecté : " + args[0]);
            return;
        }

        UUID uuid = target.getUniqueId();
        ContributionManager manager = ClassyClanChallenges.getInstance().getContributionManager();
        manager.resetPlayer(uuid);

        sender.sendMessage("§aContributions du joueur §e" + (target.getName() != null ? target.getName() : args[0]) + " §aréinitialisées.");
    }
}
