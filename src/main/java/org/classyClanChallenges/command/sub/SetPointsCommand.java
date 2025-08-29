package org.classyClanChallenges.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.contribution.ContributionManager;

import java.util.UUID;

public class SetPointsCommand extends SubCommand {

    @Override
    public String getName() {
        return "setpoints";
    }

    @Override
    public String getDescription() {
        return "Définit les points exacts d'un joueur dans une catégorie.";
    }

    @Override
    public String getUsage() {
        return "/jdc setpoints <joueur> <catégorie> <montant>";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage("§cUsage : /jdc setpoints <joueur> <catégorie> <montant>");
            sender.sendMessage("§7Catégories disponibles : CRAFT, MINE, KILL, ACTION");
            sender.sendMessage("§7Exemple : /jdc setpoints PlayerName MINE 100");
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
        int newAmount;
        try {
            newAmount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cMontant invalide : " + args[2]);
            return;
        }

        if (newAmount < 0) {
            sender.sendMessage("§cLe montant ne peut pas être négatif.");
            return;
        }

        // Récupérer le manager et les contributions actuelles
        ContributionManager manager = ClassyClanChallenges.getInstance().getContributionManager();
        UUID playerId = target.getUniqueId();

        // Sauvegarder l'ancienne valeur pour les logs
        int oldAmount = manager.getPlayerContribution(playerId).get(category);

        // Utiliser la nouvelle méthode pour définir les points
        manager.setPlayerPoints(playerId, category, newAmount);

        // Messages de confirmation
        String playerName = target.getName() != null ? target.getName() : args[0];
        int difference = newAmount - oldAmount;
        sender.sendMessage("§a✓ Points de §e" + playerName + " §adéfinis à §6" + newAmount +
                " §aen §6" + category.name());
        sender.sendMessage("§7Ancienne valeur : §f" + oldAmount + " §7→ Nouvelle valeur : §f" + newAmount +
                " §7(différence : " + (difference >= 0 ? "§a+" : "§c") + difference + "§7)");

        // Log pour traçabilité
        ClassyClanChallenges.getInstance().getLogger().info(
                sender.getName() + " a défini les points " + category.name() + " de " + playerName +
                        " à " + newAmount + " (était " + oldAmount + ", différence: " + difference + ")"
        );
    }
}