package org.classyClanChallenges.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.contribution.ContributionManager;

public class AddPointsCommand extends SubCommand {

    @Override
    public String getName() {
        return "addpoints";
    }

    @Override
    public String getDescription() {
        return "Ajoute des points à un joueur dans une catégorie.";
    }

    @Override
    public String getUsage() {
        return "/jdc addpoints <joueur> <catégorie> <montant>";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage("§cUsage : /jdc addpoints <joueur> <catégorie> <montant>");
            sender.sendMessage("§7Catégories disponibles : CRAFT, MINE, KILL, ACTION");
            return;
        }

        // Vérifier le joueur
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            sender.sendMessage("§cJoueur inconnu : " + args[0]);
            return;
        }

        // Vérifier la catégorie
        ChallengeCategory category;
        try {
            category = ChallengeCategory.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cCatégorie invalide : " + args[1]);
            sender.sendMessage("§7Catégories disponibles : CRAFT, MINE, KILL, ACTION");
            return;
        }

        // Vérifier le montant
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cMontant invalide : " + args[2]);
            return;
        }

        if (amount <= 0) {
            sender.sendMessage("§cLe montant doit être positif.");
            return;
        }

        // Ajouter les points
        ContributionManager manager = ClassyClanChallenges.getInstance().getContributionManager();
        manager.addContribution(target.getUniqueId(), category, amount);

        sender.sendMessage("§a✓ Ajouté §e" + amount + " §apoints en §6" + category.name() +
                " §aà §e" + (target.getName() != null ? target.getName() : args[0]));

        // Log pour traçabilité
        ClassyClanChallenges.getInstance().getLogger().info(
                sender.getName() + " a ajouté " + amount + " points " + category.name() +
                        " à " + (target.getName() != null ? target.getName() : args[0])
        );
    }
}