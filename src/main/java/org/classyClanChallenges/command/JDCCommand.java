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
            // ===== COMMANDES POUR TOUS LES JOUEURS =====
            new MainCommand(),           // Menu principal (/jdc)
            new StatsCommand(),          // Voir ses stats (/jdc stats)
            new ClassementCommand(),     // Voir les classements (/jdc classement)
            new HelpCommand(),          // Aide générale (/jdc help)

            // ===== COMMANDES ADMINISTRATEUR =====

            // --- Gestion des données ---
            new InfoCommand(),          // Informations système
            new DebugCommand(),         // Outils de debug avancés
            new TopCommand(),           // Tops détaillés
            new AdvancedStatsCommand(), // Statistiques avancées (astats)

            // --- Gestion des logs et traçabilité ---
            new LogsCommand(),          // Consulter les logs d'activité

            // --- Manipulation des points ---
            new AddPointsCommand(),     // Ajouter des points
            new SetPointsCommand(),     // Définir des points exacts

            // --- Gestion des défis ---
            new SetChallengeCommand(),  // Forcer un défi spécifique
            new RotateCommand(),        // Changer de semaine manuellement

            // --- Gestion des fichiers ---
            new ReloadCommand(),        // Recharger les configurations (amélioré)
            new ConfigCommand(),        // Gestion avancée des fichiers de config
            new BlocksCommand(),        // Gérer les blocs tracés

            // --- Reset et nettoyage ---
            new ResetPlayerCommand(),   // Reset joueur spécifique
            new ResetClanCommand(),     // Reset clan spécifique
            new ResetAllCommand()       // Reset complet
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

                // Vérification des permissions
                if (sub.isAdminOnly() && !sender.hasPermission("jdc.admin")) {
                    sender.sendMessage("§c✗ Vous n'avez pas la permission d'utiliser cette commande.");
                    sender.sendMessage("§7Permission requise: §ejdc.admin");
                    return true;
                }

                // Exécution de la commande
                try {
                    sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
                } catch (Exception e) {
                    sender.sendMessage("§c✗ Erreur lors de l'exécution de la commande:");
                    sender.sendMessage("§c" + e.getMessage());

                    // Log de l'erreur si possible
                    try {
                        if (org.classyClanChallenges.ClassyClanChallenges.getInstance().getLogManager() != null) {
                            org.classyClanChallenges.ClassyClanChallenges.getInstance().getLogManager()
                                    .logSystemEvent("Erreur de commande",
                                            "Commande: " + subLabel + " | Erreur: " + e.getMessage(),
                                            sender.getName());
                        }
                    } catch (Exception ignored) {
                        // Ignore si le logging échoue aussi
                    }

                    org.classyClanChallenges.ClassyClanChallenges.getInstance().getLogger()
                            .severe("Erreur dans la commande " + subLabel + " par " + sender.getName() + ": " + e.getMessage());
                }
                return true;
            }
        }

        // Si aucune commande ne correspond
        sender.sendMessage("§c✗ Commande inconnue: §e" + subLabel);
        sender.sendMessage("§7Tapez §e/jdc help §7pour voir toutes les commandes disponibles.");

        // Suggestion de commandes similaires
        suggestSimilarCommands(sender, subLabel);

        return true;
    }

    /**
     * Suggère des commandes similaires en cas de typo
     */
    private void suggestSimilarCommands(CommandSender sender, String input) {
        List<String> suggestions = subCommands.stream()
                .map(SubCommand::getName)
                .filter(name -> !name.isEmpty())
                .filter(name -> {
                    // Vérifier les permissions avant de suggérer
                    if (sender.hasPermission("jdc.admin")) {
                        return true;
                    }
                    // Trouver la commande correspondante et vérifier si elle est admin-only
                    return subCommands.stream()
                            .filter(cmd -> cmd.getName().equals(name))
                            .noneMatch(SubCommand::isAdminOnly);
                })
                .filter(name -> calculateSimilarity(input.toLowerCase(), name.toLowerCase()) > 0.4)
                .sorted((a, b) -> Double.compare(
                        calculateSimilarity(input.toLowerCase(), b.toLowerCase()),
                        calculateSimilarity(input.toLowerCase(), a.toLowerCase())
                ))
                .limit(3)
                .toList();

        if (!suggestions.isEmpty()) {
            sender.sendMessage("§7Peut-être vouliez-vous dire:");
            for (String suggestion : suggestions) {
                sender.sendMessage("§7• §e/jdc " + suggestion);
            }
        }
    }

    /**
     * Calcule la similarité entre deux chaînes (algorithme simple)
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;

        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;

        return (maxLen - levenshteinDistance(s1, s2)) / (double) maxLen;
    }

    /**
     * Calcule la distance de Levenshtein entre deux chaînes
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            Math.min(
                                    dp[i - 1][j] + 1,
                                    dp[i][j - 1] + 1
                            ),
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Retourne la liste de toutes les sous-commandes
     */
    public static List<SubCommand> getSubCommands() {
        return subCommands;
    }

    /**
     * Retourne la liste des sous-commandes accessibles à un sender donné
     */
    public static List<SubCommand> getAccessibleSubCommands(CommandSender sender) {
        return subCommands.stream()
                .filter(cmd -> !cmd.isAdminOnly() || sender.hasPermission("jdc.admin"))
                .toList();
    }

    /**
     * Retourne une sous-commande par son nom
     */
    public static SubCommand getSubCommand(String name) {
        return subCommands.stream()
                .filter(cmd -> cmd.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}