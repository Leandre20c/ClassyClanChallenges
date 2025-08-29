package org.classyClanChallenges.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.command.SubCommand;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigCommand extends SubCommand {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    @Override
    public String getName() {
        return "config";
    }

    @Override
    public String getDescription() {
        return "Gère les fichiers de configuration du plugin.";
    }

    @Override
    public String getUsage() {
        return "/jdc config <list|view|backup|restore|validate> [fichier] [options]";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showUsage(sender);
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "list" -> handleList(sender);
            case "view" -> handleView(sender, args);
            case "backup" -> handleBackup(sender, args);
            case "restore" -> handleRestore(sender, args);
            case "validate" -> handleValidate(sender, args);
            case "create" -> handleCreate(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "compare" -> handleCompare(sender, args);
            default -> showUsage(sender);
        }
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage("§6§l=== FICHIERS DE CONFIGURATION ===");

        Map<String, File> configFiles = getConfigFiles();

        for (Map.Entry<String, File> entry : configFiles.entrySet()) {
            String name = entry.getKey();
            File file = entry.getValue();

            if (file.exists()) {
                long sizeKB = file.length() / 1024;
                String lastModified = dateFormat.format(new Date(file.lastModified()));

                sender.sendMessage(String.format("§a✓ §f%s §7- %dKB - Modifié: %s",
                        name, sizeKB, lastModified));

                // Afficher quelques infos supplémentaires selon le type
                if (name.endsWith(".yml")) {
                    try {
                        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                        int keys = yaml.getKeys(true).size();
                        sender.sendMessage("  §8└ " + keys + " clés de configuration");
                    } catch (Exception e) {
                        sender.sendMessage("  §c└ Erreur de lecture YAML: " + e.getMessage());
                    }
                }
            } else {
                sender.sendMessage("§c✗ §f" + name + " §7- Fichier manquant");
            }
        }

        sender.sendMessage("§6§l==================================");
    }

    private void handleView(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jdc config view <fichier> [section]");
            sender.sendMessage("§7Fichiers disponibles: config, challenges, rewards, weekly, contributions");
            return;
        }

        String fileName = args[1].toLowerCase();
        File file = getFileByName(fileName);

        if (file == null || !file.exists()) {
            sender.sendMessage("§cFichier introuvable: " + fileName);
            return;
        }

        try {
            if (fileName.endsWith(".yml") || fileName.contains("yml")) {
                viewYamlFile(sender, file, args.length > 2 ? args[2] : null);
            } else {
                viewTextFile(sender, file);
            }
        } catch (Exception e) {
            sender.sendMessage("§cErreur lors de la lecture du fichier: " + e.getMessage());
        }
    }

    private void viewYamlFile(CommandSender sender, File file, String section) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        sender.sendMessage("§6§l=== CONTENU DE " + file.getName().toUpperCase() + " ===");

        Set<String> keys;
        if (section != null) {
            keys = yaml.getConfigurationSection(section) != null ?
                    yaml.getConfigurationSection(section).getKeys(true) :
                    new HashSet<>();
            sender.sendMessage("§eSection: §f" + section);
        } else {
            keys = yaml.getKeys(false);
        }

        if (keys.isEmpty()) {
            sender.sendMessage("§cAucune donnée trouvée" + (section != null ? " dans la section " + section : ""));
            return;
        }

        int displayCount = 0;
        for (String key : keys.stream().sorted().collect(Collectors.toList())) {
            if (displayCount >= 20) {
                sender.sendMessage("§7... et " + (keys.size() - 20) + " autres entrées");
                break;
            }

            String fullKey = section != null ? section + "." + key : key;
            Object value = yaml.get(fullKey);

            if (value instanceof List) {
                List<?> list = (List<?>) value;
                sender.sendMessage("§e" + key + "§7: §f[Liste de " + list.size() + " éléments]");
                if (list.size() <= 3) {
                    for (Object item : list) {
                        sender.sendMessage("  §7- §f" + item);
                    }
                }
            } else if (value instanceof Map) {
                sender.sendMessage("§e" + key + "§7: §f{Objet avec " + ((Map<?, ?>) value).size() + " clés}");
            } else {
                String valueStr = value != null ? value.toString() : "null";
                if (valueStr.length() > 50) {
                    valueStr = valueStr.substring(0, 47) + "...";
                }
                sender.sendMessage("§e" + key + "§7: §f" + valueStr);
            }

            displayCount++;
        }

        sender.sendMessage("§6§l" + "=".repeat(file.getName().length() + 15));
    }

    private void viewTextFile(CommandSender sender, File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());

        sender.sendMessage("§6§l=== CONTENU DE " + file.getName().toUpperCase() + " ===");

        int maxLines = 30;
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            String line = lines.get(i);
            sender.sendMessage("§7" + (i + 1) + ". §f" + line);
        }

        if (lines.size() > maxLines) {
            sender.sendMessage("§7... et " + (lines.size() - maxLines) + " autres lignes");
        }

        sender.sendMessage("§6§l" + "=".repeat(file.getName().length() + 15));
    }

    private void handleBackup(CommandSender sender, String[] args) {
        String fileName = args.length > 1 ? args[1] : "all";

        try {
            File backupDir = new File(ClassyClanChallenges.getInstance().getDataFolder(), "backups");
            backupDir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

            if (fileName.equals("all")) {
                backupAllFiles(sender, backupDir, timestamp);
            } else {
                backupSingleFile(sender, fileName, backupDir, timestamp);
            }

        } catch (Exception e) {
            sender.sendMessage("§cErreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    private void backupAllFiles(CommandSender sender, File backupDir, String timestamp) throws IOException {
        File timestampDir = new File(backupDir, "backup_" + timestamp);
        timestampDir.mkdirs();

        Map<String, File> configFiles = getConfigFiles();
        int backedUp = 0;

        for (Map.Entry<String, File> entry : configFiles.entrySet()) {
            File source = entry.getValue();
            if (source.exists()) {
                File dest = new File(timestampDir, source.getName());
                Files.copy(source.toPath(), dest.toPath());
                backedUp++;
            }
        }

        sender.sendMessage("§a✓ Sauvegarde complète créée: §f" + backedUp + " fichiers");
        sender.sendMessage("§7Dossier: §f" + timestampDir.getPath());

        // Log de la sauvegarde
        if (ClassyClanChallenges.getInstance().getLogManager() != null) {
            ClassyClanChallenges.getInstance().getLogManager().logSystemEvent(
                    "Sauvegarde de configuration",
                    "Sauvegarde complète: " + backedUp + " fichiers dans " + timestampDir.getName(),
                    sender.getName()
            );
        }
    }

    private void backupSingleFile(CommandSender sender, String fileName, File backupDir, String timestamp) throws IOException {
        File source = getFileByName(fileName);
        if (source == null || !source.exists()) {
            sender.sendMessage("§cFichier introuvable: " + fileName);
            return;
        }

        String backupName = source.getName().replace(".yml", "_" + timestamp + ".yml");
        File dest = new File(backupDir, backupName);

        Files.copy(source.toPath(), dest.toPath());

        sender.sendMessage("§a✓ Sauvegarde créée: §f" + dest.getName());
        sender.sendMessage("§7Fichier: §f" + dest.getPath());
    }

    private void handleValidate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /jdc config validate <fichier>");
            return;
        }

        String fileName = args[1].toLowerCase();
        File file = getFileByName(fileName);

        if (file == null || !file.exists()) {
            sender.sendMessage("§cFichier introuvable: " + fileName);
            return;
        }

        sender.sendMessage("§e⚠ Validation de " + file.getName() + "...");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            if (fileName.contains("yml")) {
                validateYamlFile(file, errors, warnings);
            }

            // Validation spécifique selon le type de fichier
            switch (fileName) {
                case "challenges" -> validateChallengesFile(file, errors, warnings);
                case "rewards" -> validateRewardsFile(file, errors, warnings);
                case "config" -> validateConfigFile(file, errors, warnings);
            }

        } catch (Exception e) {
            errors.add("Erreur générale: " + e.getMessage());
        }

        // Affichage des résultats
        if (errors.isEmpty() && warnings.isEmpty()) {
            sender.sendMessage("§a✓ Fichier valide, aucun problème détecté.");
        } else {
            if (!errors.isEmpty()) {
                sender.sendMessage("§c✗ Erreurs trouvées (" + errors.size() + "):");
                for (String error : errors) {
                    sender.sendMessage("  §c• " + error);
                }
            }

            if (!warnings.isEmpty()) {
                sender.sendMessage("§e⚠ Avertissements (" + warnings.size() + "):");
                for (String warning : warnings) {
                    sender.sendMessage("  §e• " + warning);
                }
            }
        }
    }

    private void validateYamlFile(File file, List<String> errors, List<String> warnings) {
        try {
            YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            errors.add("Syntaxe YAML invalide: " + e.getMessage());
        }
    }

    private void validateChallengesFile(File file, List<String> errors, List<String> warnings) {
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            if (!yaml.contains("categories")) {
                errors.add("Section 'categories' manquante");
                return;
            }

            for (String category : yaml.getConfigurationSection("categories").getKeys(false)) {
                if (!isValidCategory(category)) {
                    warnings.add("Catégorie inconnue: " + category);
                }

                var section = yaml.getConfigurationSection("categories." + category);
                if (section == null || section.getKeys(false).isEmpty()) {
                    warnings.add("Catégorie " + category + " est vide");
                }
            }

        } catch (Exception e) {
            errors.add("Erreur de validation challenges: " + e.getMessage());
        }
    }

    private void validateRewardsFile(File file, List<String> errors, List<String> warnings) {
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            // Vérifier les récompenses de clans
            if (yaml.contains("clan-ranking-rewards")) {
                for (String key : yaml.getConfigurationSection("clan-ranking-rewards").getKeys(false)) {
                    if (!key.equals("default")) {
                        try {
                            Integer.parseInt(key);
                        } catch (NumberFormatException e) {
                            errors.add("Rang de clan invalide: " + key);
                        }
                    }

                    if (yaml.getInt("clan-ranking-rewards." + key) < 0) {
                        warnings.add("Récompense négative pour le rang " + key);
                    }
                }
            } else {
                warnings.add("Section 'clan-ranking-rewards' manquante");
            }

        } catch (Exception e) {
            errors.add("Erreur de validation rewards: " + e.getMessage());
        }
    }

    private void validateConfigFile(File file, List<String> errors, List<String> warnings) {
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            // Vérifier les paramètres de reset hebdomadaire
            if (yaml.contains("weekly-reset")) {
                String day = yaml.getString("weekly-reset.day", "SUNDAY");
                try {
                    java.time.DayOfWeek.valueOf(day.toUpperCase());
                } catch (IllegalArgumentException e) {
                    errors.add("Jour invalide dans weekly-reset.day: " + day);
                }

                int hour = yaml.getInt("weekly-reset.hour", 18);
                if (hour < 0 || hour > 23) {
                    errors.add("Heure invalide dans weekly-reset.hour: " + hour);
                }
            }

        } catch (Exception e) {
            errors.add("Erreur de validation config: " + e.getMessage());
        }
    }

    private boolean isValidCategory(String category) {
        return Arrays.asList("CRAFT", "MINE", "KILL", "ACTION", "ENCHANT").contains(category.toUpperCase());
    }

    // ===================== MÉTHODES UTILITAIRES =====================

    private Map<String, File> getConfigFiles() {
        Map<String, File> files = new LinkedHashMap<>();
        File dataFolder = ClassyClanChallenges.getInstance().getDataFolder();

        files.put("config.yml", new File(dataFolder, "config.yml"));
        files.put("challenges.yml", new File(dataFolder, "challenges.yml"));
        files.put("rewards.yml", new File(dataFolder, "rewards.yml"));
        files.put("data/weekly.yml", new File(dataFolder, "data/weekly.yml"));
        files.put("data/contributions.yml", new File(dataFolder, "data/contributions.yml"));
        files.put("data/activity-logs.yml", new File(dataFolder, "data/activity-logs.yml"));
        files.put("data/player-blocks.yml", new File(dataFolder, "data/player-blocks.yml"));
        files.put("bloc-note.yml", new File(dataFolder, "bloc-note.yml"));

        return files;
    }

    private File getFileByName(String name) {
        Map<String, File> files = getConfigFiles();

        // Recherche exacte
        if (files.containsKey(name)) {
            return files.get(name);
        }

        // Recherche par nom court
        for (Map.Entry<String, File> entry : files.entrySet()) {
            String fileName = entry.getKey();
            if (fileName.contains(name) || fileName.replace(".yml", "").equals(name)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage("§cUsage: /jdc config <action> [paramètres]");
        sender.sendMessage("§7Actions disponibles:");
        sender.sendMessage("§7• §elist §7- Liste tous les fichiers de configuration");
        sender.sendMessage("§7• §eview <fichier> [section] §7- Affiche le contenu d'un fichier");
        sender.sendMessage("§7• §ebackup [fichier|all] §7- Crée une sauvegarde");
        sender.sendMessage("§7• §evalidate <fichier> §7- Vérifie la validité d'un fichier");
        sender.sendMessage("§7Fichiers: config, challenges, rewards, weekly, contributions, logs, blocks");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        // Fonctionnalité pour créer un nouveau fichier template
        sender.sendMessage("§e⚠ Fonctionnalité de création en cours de développement.");
    }

    private void handleEdit(CommandSender sender, String[] args) {
        // Fonctionnalité pour éditer rapidement des valeurs
        sender.sendMessage("§e⚠ Fonctionnalité d'édition en cours de développement.");
    }

    private void handleCompare(CommandSender sender, String[] args) {
        // Fonctionnalité pour comparer deux fichiers
        sender.sendMessage("§e⚠ Fonctionnalité de comparaison en cours de développement.");
    }

    private void handleRestore(CommandSender sender, String[] args) {
        sender.sendMessage("§e⚠ Fonctionnalité de restauration en cours de développement.");
        sender.sendMessage("§7Pour restaurer manuellement, copiez les fichiers depuis le dossier backups/");
    }
}