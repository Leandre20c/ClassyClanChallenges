package org.classyClanChallenges.command.sub;

import org.bukkit.command.CommandSender;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.WeeklyChallenge;
import org.classyClanChallenges.command.SubCommand;

import java.util.List;
import java.util.Map;

public class SetChallengeCommand extends SubCommand {

    @Override
    public String getName() {
        return "setchallenge";
    }

    @Override
    public String getDescription() {
        return "Force un défi spécifique pour une catégorie.";
    }

    @Override
    public String getUsage() {
        return "/jdc setchallenge <catégorie> <cible> [valeur]";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage : /jdc setchallenge <catégorie> <cible> [valeur]");
            sender.sendMessage("§7Exemple : /jdc setchallenge MINE DIAMOND_ORE 50");
            sender.sendMessage("§7Catégories disponibles : CRAFT, MINE, KILL, ACTION");
            return;
        }

        // Vérifier la catégorie
        ChallengeCategory category;
        try {
            category = ChallengeCategory.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cCatégorie invalide : " + args[0]);
            sender.sendMessage("§7Catégories disponibles : CRAFT, MINE, KILL, ACTION");
            return;
        }

        String target = args[1].toUpperCase();
        int value = 1; // Valeur par défaut

        // Vérifier la valeur si fournie
        if (args.length >= 3) {
            try {
                value = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cValeur invalide : " + args[2]);
                return;
            }

            if (value <= 0) {
                sender.sendMessage("§cLa valeur doit être positive.");
                return;
            }
        }

        // Vérifier si la cible existe dans le pool de défis
        Map<ChallengeCategory, List<ChallengeEntry>> pool = ClassyClanChallenges.getInstance()
                .getChallengeManager().getPool();

        List<ChallengeEntry> categoryEntries = pool.get(category);
        if (categoryEntries == null || categoryEntries.isEmpty()) {
            sender.sendMessage("§cAucun défi disponible pour la catégorie " + category.name());
            return;
        }

        boolean targetExists = categoryEntries.stream()
                .anyMatch(entry -> entry.getTarget().equalsIgnoreCase(target));

        if (!targetExists) {
            sender.sendMessage("§cCible '§e" + target + "§c' non trouvée dans la catégorie " + category.name());
            sender.sendMessage("§7Cibles disponibles :");
            categoryEntries.forEach(entry ->
                    sender.sendMessage("  §7- §f" + entry.getTarget() + " §7(valeur: §e" + entry.getValue() + "§7)")
            );
            return;
        }

        // Créer et définir le nouveau défi
        ChallengeEntry newChallenge = new ChallengeEntry(category, target, value);
        WeeklyChallenge weekly = ClassyClanChallenges.getInstance().getChallengeManager().getActiveChallenges();
        weekly.setChallenge(newChallenge);

        // Sauvegarder
        ClassyClanChallenges.getInstance().getChallengeManager().saveActiveChallenges();

        // Mettre à jour le ContributionManager
        ClassyClanChallenges.getInstance().getContributionManager().updateWeeklyChallenge(weekly);

        sender.sendMessage("§a✓ Défi forcé pour §6" + category.name() + " §7: §e" + target + " §7(valeur: §a" + value + "§7)");

        // Log pour traçabilité
        ClassyClanChallenges.getInstance().getLogger().info(
                sender.getName() + " a forcé le défi " + category.name() +
                        " vers " + target + " (valeur: " + value + ")"
        );
    }
}