package org.classyClanChallenges.challenges;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.contribution.ContributionManager;
import org.classyClanChallenges.reward.RewardManager;
import org.classyclan.ClassyClan;
import org.classyclan.clan.Clan;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ChallengeManager {

    private final Map<ChallengeCategory, List<ChallengeEntry>> pool = new EnumMap<>(ChallengeCategory.class);
    private final WeeklyChallenge active = new WeeklyChallenge();
    private final WeeklyChallenge previous = new WeeklyChallenge();

    public ChallengeManager() {
        loadChallenges();
    }

    // ===================== CHARGEMENT DES DÉFIS =====================

    public void loadChallenges() {
        pool.clear();
        File file = new File(ClassyClanChallenges.getInstance().getDataFolder(), "challenges.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection categories = config.getConfigurationSection("categories");
        if (categories == null) return;

        for (String catName : categories.getKeys(false)) {
            ChallengeCategory category = ChallengeCategory.fromString(catName);
            if (category == null) continue;

            ConfigurationSection entries = categories.getConfigurationSection(catName);
            if (entries == null) continue;

            List<ChallengeEntry> list = new ArrayList<>();
            for (String key : entries.getKeys(false)) {
                String valueString = entries.getString(key);
                try {
                    int value = Integer.parseInt(valueString);
                    list.add(new ChallengeEntry(category, key.toUpperCase(), value));
                } catch (NumberFormatException e) {
                    list.add(new ChallengeEntry(category, valueString.toUpperCase(), 1));
                }
            }
            pool.put(category, list);
        }
    }

    public void loadActiveChallengesFromFile() {
        File file = new File(ClassyClanChallenges.getInstance().getDataFolder(), "data/weekly.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        active.clear();

        for (String key : config.getKeys(false)) {
            ChallengeCategory category = ChallengeCategory.fromString(key);
            if (category == null) continue;

            String target = config.getString(key + ".target");
            int value = config.getInt(key + ".value", 1);
            ChallengeEntry entry = new ChallengeEntry(category, target.toUpperCase(), value);
            active.setChallenge(entry);
        }
    }

    public void saveActiveChallenges() {
        File file = new File(ClassyClanChallenges.getInstance().getDataFolder(), "data/weekly.yml");
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<ChallengeCategory, ChallengeEntry> entry : active.getAllChallenges().entrySet()) {
            String path = entry.getKey().name().toLowerCase();
            ChallengeEntry challenge = entry.getValue();
            config.set(path + ".target", challenge.getTarget());
            config.set(path + ".value", challenge.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateWeeklyChallenges() {
        WeeklyChallengeGenerator generator = new WeeklyChallengeGenerator(pool);
        WeeklyChallenge generated = generator.generate();

        active.clear();
        for (Map.Entry<ChallengeCategory, ChallengeEntry> entry : generated.getAllChallenges().entrySet()) {
            active.setChallenge(entry.getValue());
        }

        saveActiveChallenges();
    }

    // ===================== RESET HEBDOMADAIRE =====================

    public void resetWeeklyChallenges() {
        ClassyClanChallenges plugin = ClassyClanChallenges.getInstance();
        ContributionManager contributionManager = plugin.getContributionManager();
        RewardManager rewardManager = new RewardManager();

        // Copier les défis actifs dans "previous"
        previous.clear();
        for (Map.Entry<ChallengeCategory, ChallengeEntry> entry : active.getAllChallenges().entrySet()) {
            previous.setChallenge(entry.getValue());
        }

        // 1. Donner les récompenses
        rewardManager.distributeRewards(previous, contributionManager);

        // 2. Afficher dans logs les stats (avec les vraies récompenses données juste avant)
        displayStats(contributionManager);

        // 3. Générer et sauvegarder le résumé Discord
        saveDiscordSummary(contributionManager);

        // 4. Générer les nouveaux défis
        generateWeeklyChallenges();

        // 5. Réinitialiser les contributions
        contributionManager.resetAll();
        contributionManager.updateWeeklyChallenge(getActiveChallenges());

        // 6. Message global en jeu
        broadcastResetMessage();
    }


    // ===================== OUTILS =====================

    private void displayStats(ContributionManager manager) {
        Bukkit.getLogger().info("=== Statistiques de la semaine ===");

        for (ChallengeCategory category : previous.getAllChallenges().keySet()) {
            Bukkit.getLogger().info("Catégorie : " + category.name());

            List<Map.Entry<UUID, Integer>> topClans = manager.getTopClansByCategory(category);
            Bukkit.getLogger().info("  ➤ Top Clans :");
            for (int i = 0; i < Math.min(3, topClans.size()); i++) {
                UUID clanOwner = topClans.get(i).getKey();
                Clan clan = ClassyClan.getAPI().getClanOf(clanOwner);
                String name = (clan != null) ? clan.getRawName() : "Inconnu";
                int points = topClans.get(i).getValue();
                Bukkit.getLogger().info("    " + (i + 1) + ". " + name + " : " + points + " pts");
            }

            List<Map.Entry<UUID, Integer>> topPlayers = manager.getTopPlayersByCategory(category);
            Bukkit.getLogger().info("  ➤ Top Joueurs :");
            for (int i = 0; i < Math.min(3, topPlayers.size()); i++) {
                UUID playerId = topPlayers.get(i).getKey();
                String name = Bukkit.getOfflinePlayer(playerId).getName();
                int points = topPlayers.get(i).getValue();
                Bukkit.getLogger().info("    " + (i + 1) + ". " + name + " : " + points + " pts");
            }

            Bukkit.getLogger().info("");
        }
    }

    private void broadcastResetMessage() {
        Bukkit.broadcastMessage("§6§lNouvelle semaine de défis !");
        Bukkit.broadcastMessage("§7Les contributions ont été réinitialisées !");
        Bukkit.broadcastMessage("§eFaites §a/jdc §epour découvrir les nouveaux objectifs !");
    }

    private void saveDiscordSummary(ContributionManager manager) {
        StringBuilder msg = new StringBuilder("# __Nouvelle semaine de jeux de clan__\n\n");
        msg.append("## Résultats de la semaine précédente :\n\n");

        Map<UUID, Integer> totalClanGains = new HashMap<>();

        for (Map.Entry<ChallengeCategory, ChallengeEntry> entry : previous.getAllChallenges().entrySet()) {
            ChallengeCategory category = entry.getKey();
            ChallengeEntry challenge = entry.getValue();
            int unitValue = challenge.getValue();

            String emoji = switch (category) {
                case KILL -> ":sword:";
                case MINE -> ":pickaxe:";
                case CRAFT -> ":crafting_table:";
                case ACTION -> ":person_running:";
                default -> "•";
            };

            msg.append(" ").append(emoji).append(" **").append(category.name()).append("**\n");

            List<Map.Entry<UUID, Integer>> topClans = manager.getTopClansByCategory(category);
            if (topClans == null || topClans.isEmpty()) {
                msg.append("> Aucun clan n’a participé.\n");
            } else {
                for (int i = 0; i < Math.min(3, topClans.size()); i++) {
                    UUID ownerId = topClans.get(i).getKey();
                    Clan clan = ClassyClan.getAPI().getClanOf(ownerId);
                    String clanName = (clan != null) ? clan.getRawName() : "Inconnu";
                    int score = topClans.get(i).getValue();
                    msg.append("> ").append(i + 1).append(" : ").append(clanName)
                            .append(" -> ").append(score).append(" ").append(challenge.getTarget())
                            .append(" ").append(category.name().toLowerCase()).append("s\n");

                    int classementBonus = ClassyClanChallenges.getInstance().getRewardManager()
                            .getClanRankingBonus(i + 1);
                    totalClanGains.put(ownerId, totalClanGains.getOrDefault(ownerId, 0) + classementBonus);
                }
            }

            // Participations & valeurs
            for (UUID clanId : manager.getAllClanContributions().keySet()) {
                int amount = manager.getClanContribution(clanId).get(category);
                if (amount <= 0) continue;

                int value = amount * unitValue;
                totalClanGains.put(clanId, totalClanGains.getOrDefault(clanId, 0) + value);
            }

            // Bonus participation pour les non-tops
            for (UUID clanId : manager.getAllClanContributions().keySet()) {
                boolean isTop = topClans.stream().anyMatch(e -> e.getKey().equals(clanId));
                int amount = manager.getClanContribution(clanId).get(category);
                if (amount > 0 && !isTop) {
                    int participationBonus = ClassyClanChallenges.getInstance().getRewardManager()
                            .getClanRankingBonus(0); // default
                    totalClanGains.put(clanId, totalClanGains.getOrDefault(clanId, 0) + participationBonus);
                }
            }

            msg.append("\n");
        }

        // Section défis de la semaine
        msg.append("## Défis de cette semaine :\n\n");
        for (Map.Entry<ChallengeCategory, ChallengeEntry> entry : active.getAllChallenges().entrySet()) {
            String emoji = switch (entry.getKey()) {
                case KILL -> ":sword:";
                case MINE -> ":pickaxe:";
                case CRAFT -> ":crafting_table:";
                case ACTION -> ":person_running:";
                default -> "•";
            };

            msg.append("• ").append(emoji).append(" **").append(entry.getKey().name())
                    .append("** : (`").append(entry.getValue().getTarget()).append("`)\n");
        }

        // Section gains par clan
        msg.append("\n## Récompenses par clan :\n\n");
        totalClanGains.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(entry -> {
                    Clan clan = ClassyClan.getAPI().getClanOf(entry.getKey());
                    String name = (clan != null) ? clan.getRawName() : "Inconnu";
                    msg.append("🏅 ").append(name).append(" : ").append(entry.getValue()).append(" PE\n");
                });

        msg.append("\nBonne chance à tous ! ||@everyone||");

        File file = new File(ClassyClanChallenges.getInstance().getDataFolder(), "bloc-note.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("weekly-message", msg.toString());

        try {
            yaml.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[ClassyClanChallenges] Impossible de sauvegarder bloc-note.yml");
        }
    }


    // ===================== GETTERS =====================

    public WeeklyChallenge getActiveChallenges() {
        return active;
    }

    public WeeklyChallenge getPreviousChallenges() {
        return previous;
    }

    public Map<ChallengeCategory, List<ChallengeEntry>> getPool() {
        return pool;
    }
}
