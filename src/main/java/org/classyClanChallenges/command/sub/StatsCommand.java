package org.classyClanChallenges.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.WeeklyChallenge;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.contribution.ClanContribution;
import org.classyClanChallenges.contribution.ContributionManager;
import org.classyClanChallenges.contribution.PlayerContribution;
import org.classyclan.ClassyClan;
import org.classyclan.api.ClassyClanAPI;
import org.classyclan.clan.Clan;

import java.util.Map;
import java.util.UUID;

public class StatsCommand extends SubCommand {

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "Affiche vos contributions personnelles et celles de votre clan.";
    }

    @Override
    public String getUsage() {
        return "/jdc stats";
    }

    @Override
    public boolean isAdminOnly() {
        return false;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande réservée aux joueurs.");
            return;
        }

        UUID uuid = player.getUniqueId();
        ContributionManager manager = ClassyClanChallenges.getInstance().getContributionManager();
        WeeklyChallenge active = ClassyClanChallenges.getInstance().getChallengeManager().getActiveChallenges();

        PlayerContribution pc = manager.getPlayerContribution(uuid);

        player.sendMessage("§e§l» Tes contributions personnelles :");
        int idx = 0;
        for (Map.Entry<ChallengeCategory, Integer> entry : pc.getAll().entrySet()) {
            player.sendMessage("§7- §f" + entry.getKey().name() + " : §a" + entry.getValue());
            idx += 1;
        }
        if (idx == 0) {
            player.sendMessage("§c» Tu n'as pas rempli de défis cette semaine.");
        }

        ClassyClanAPI api = ClassyClan.getAPI();
        if (api == null) {
            player.sendMessage("§cErreur : L'API ClassyClan est indisponible.");
        } else {
            Clan clan = api.getClanOf(((Player) sender).getUniqueId());
            if (clan != null) {
                ClanContribution cc = manager.getClanContribution(clan.getOwner());

                player.sendMessage("§e§l» Contributions de ton clan §6" + clan.getRawName() + "§e :");
                idx = 0;
                for (Map.Entry<ChallengeCategory, Integer> entry : cc.getAll().entrySet()) {
                    player.sendMessage("§7- §f" + entry.getKey().name() + " : §a" + entry.getValue());
                    idx += 1;
                }
                if (idx == 0) {
                    player.sendMessage("§c» Ton clan n'a pas rempli de défis cette semaine.");
                }
            } else {
                player.sendMessage("§cVous n'êtes dans aucun clan.");
            }
        }
    }
}
