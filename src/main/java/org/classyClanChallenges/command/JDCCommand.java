package org.classyClanChallenges.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.classyClanChallenges.command.sub.*;

import java.util.Arrays;
import java.util.List;

public class JDCCommand implements CommandExecutor {

    private static final List<SubCommand> subCommands = List.of(
            // Commandes pour tous
            new MainCommand(),           // Commande par défaut (menu principal)
            new StatsCommand(),          // Voir ses stats
            new ClassementCommand(),     // Voir les classements
            new HelpCommand(),          // Aide

            // Commandes admin uniquement
            new InfoCommand(),          // Informations système
            new TopCommand(),           // Tops détaillés
            new AddPointsCommand(),     // Ajouter des points
            new SetPointsCommand(),     // Définir des points (NOUVEAU)
            new SetChallengeCommand(),  // Forcer un défi
            new BlocksCommand(),        // Gérer les blocs tracés
            new DebugCommand(),         // Outils de debug
            new ReloadCommand(),        // Recharger configs
            new RotateCommand(),        // Changer de semaine
            new ResetPlayerCommand(),   // Reset joueur
            new ResetClanCommand(),     // Reset clan
            new ResetAllCommand()       // Reset tout
    );

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Si aucune sous-commande (juste /jdc)
        if (args.length == 0) {
            for (SubCommand sub : subCommands) {
                if (sub.getName().isEmpty()) {
                    if (sender instanceof Player player) {
                        sub.execute(player, args);
                    } else {
                        sender.sendMessage("§cCette commande est réservée aux joueurs.");
                    }
                    return true;
                }
            }
            sender.sendMessage("§cAucune commande par défaut définie.");
            return true;
        }

        // Recherche de la sous-commande
        String subLabel = args[0].toLowerCase();
        for (SubCommand sub : subCommands) {
            if (sub.getName().equalsIgnoreCase(subLabel)) {
                if (sub.isAdminOnly() && !sender.hasPermission("jdc.admin")) {
                    sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
                    return true;
                }
                sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
        }

        // Si aucune commande ne correspond
        sender.sendMessage("§cCommande inconnue. Tapez §e/jdc help §cpour la liste.");
        return true;
    }

    public static List<SubCommand> getSubCommands() {
        return subCommands;
    }
}