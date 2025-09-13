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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * Génère et sauvegarde un résumé Discord amélioré avec plus de détails
     */
    private void saveDiscordSummary(ContributionManager manager) {
        StringBuilder msg = new StringBuilder();

        // En-tête avec date
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

        msg.append("# 🏆 **NOUVELLE SEMAINE DE JEUX DE CLAN** 🏆\n");
        msg.append("*Mise à jour du ").append(now.format(dateFormatter)).append("*\n\n");
        msg.append("───────────────────────────────────────\n\n");

        // Résultats de la semaine précédente
        msg.append("## 📊 **RÉSULTATS DE LA SEMAINE PRÉCÉDENTE**\n\n");

        Map<UUID, Integer> totalClanGains = new HashMap<>(); // Pour tracker les gains totaux
        Map<UUID, Map<String, Integer>> clanDetailedGains = new HashMap<>(); // Gains détaillés par clan

        // Statistiques générales de la semaine
        int totalPointsDistributed = 0;
        int totalPlayersActive = 0;
        int totalClansActive = 0;

        for (Map.Entry<ChallengeCategory, ChallengeEntry> entry : previous.getAllChallenges().entrySet()) {
            ChallengeCategory category = entry.getKey();
            ChallengeEntry challenge = entry.getValue();
            int unitValue = challenge.getValue();

            String emoji = getCategoryEmoji(category);
            String categoryName = getCategoryDisplayName(category);

            msg.append("### ").append(emoji).append(" **").append(categoryName.toUpperCase()).append("**\n");
            msg.append("*Objectif: ").append(challenge.getTarget()).append(" (Valeur: ").append(unitValue).append(" PE)*\n\n");

            // TOP 10 CLANS pour cette catégorie
            List<Map.Entry<UUID, Integer>> topClans = manager.getTopClansByCategory(category);
            if (topClans == null || topClans.isEmpty()) {
                msg.append("❌ Aucun clan n'a participé à cette catégorie.\n\n");
            } else {
                msg.append("**🏅 CLASSEMENT CLANS:**\n");

                for (int i = 0; i < Math.min(10, topClans.size()); i++) {
                    UUID ownerId = topClans.get(i).getKey();
                    Clan clan = ClassyClan.getAPI().getClanOf(ownerId);
                    String clanName = (clan != null) ? clan.getRawName() : "Clan Inconnu";
                    int score = topClans.get(i).getValue();

                    String medal = getMedal(i + 1);
                    String rank = String.format("%2d", i + 1);

                    msg.append(medal).append(" **").append(rank).append(".** `").append(clanName)
                            .append("` → **").append(score).append("** ").append(challenge.getTarget().toLowerCase())
                            .append(" (").append(score * unitValue).append(" PE)\n");

                    // Calcul des récompenses pour ce clan
                    int classementBonus = ClassyClanChallenges.getInstance().getRewardManager()
                            .getClanRankingBonus(i + 1);
                    int productionReward = score * unitValue;

                    // Ajouter aux gains totaux
                    totalClanGains.put(ownerId, totalClanGains.getOrDefault(ownerId, 0) + classementBonus + productionReward);

                    // Détailler les gains
                    clanDetailedGains.computeIfAbsent(ownerId, k -> new HashMap<>())
                            .put(category.name() + "_classement", classementBonus);
                    clanDetailedGains.computeIfAbsent(ownerId, k -> new HashMap<>())
                            .put(category.name() + "_production", productionReward);
                }

                totalClansActive = Math.max(totalClansActive, topClans.size());
                msg.append("\n");
            }

            // TOP 5 JOUEURS pour cette catégorie
            List<Map.Entry<UUID, Integer>> topPlayers = manager.getTopPlayersByCategory(category);
            if (!topPlayers.isEmpty()) {
                msg.append("**👑 TOP JOUEURS:**\n");

                for (int i = 0; i < Math.min(5, topPlayers.size()); i++) {
                    UUID playerId = topPlayers.get(i).getKey();
                    OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
                    String playerName = player.getName() != null ? player.getName() : "Joueur Inconnu";
                    int score = topPlayers.get(i).getValue();

                    String medal = getMedal(i + 1);

                    // Trouver le clan du joueur
                    Clan playerClan = ClassyClan.getAPI().getClanOf(playerId);
                    String clanTag = playerClan != null ? " `[" + playerClan.getRawName() + "]`" : "";

                    msg.append(medal).append(" **").append(playerName).append("**").append(clanTag)
                            .append(" → **").append(score).append("** points\n");
                }

                totalPlayersActive = Math.max(totalPlayersActive, topPlayers.size());
                msg.append("\n");
            }

            // Bonus de participation pour les clans non-classés
            for (UUID clanId : manager.getAllClanContributions().keySet()) {
                boolean isInTop = topClans.stream().anyMatch(e -> e.getKey().equals(clanId));
                int amount = manager.getClanContribution(clanId).get(category);

                if (amount > 0 && !isInTop) {
                    int participationBonus = ClassyClanChallenges.getInstance().getRewardManager()
                            .getClanRankingBonus(0); // bonus par défaut
                    totalClanGains.put(clanId, totalClanGains.getOrDefault(clanId, 0) + participationBonus);

                    clanDetailedGains.computeIfAbsent(clanId, k -> new HashMap<>())
                            .put(category.name() + "_participation", participationBonus);
                }
            }

            // Calculer le total des points pour cette catégorie
            int categoryTotal = topClans.stream().mapToInt(Map.Entry::getValue).sum();
            totalPointsDistributed += categoryTotal;
        }

        // Résumé de la semaine
        msg.append("## 📈 **RÉSUMÉ DE LA SEMAINE ÉCOULÉE**\n\n");
        msg.append("🎯 **Points totaux générés:** ").append(String.format("%,d", totalPointsDistributed)).append("\n");
        msg.append("👥 **Joueurs actifs:** ").append(totalPlayersActive).append("\n");
        msg.append("🏰 **Clans participants:** ").append(totalClansActive).append("\n\n");

        // Gains détaillés par clan
        if (!totalClanGains.isEmpty()) {
            msg.append("## 💰 **RÉCOMPENSES DISTRIBUÉES**\n\n");

            List<Map.Entry<UUID, Integer>> sortedClans = totalClanGains.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .collect(Collectors.toList());

            for (int i = 0; i < sortedClans.size(); i++) {
                Map.Entry<UUID, Integer> entry = sortedClans.get(i);
                Clan clan = ClassyClan.getAPI().getClanOf(entry.getKey());
                String name = (clan != null) ? clan.getRawName() : "Clan Inconnu";
                int totalReward = entry.getValue();

                String rankEmoji = i < 3 ? getMedal(i + 1) : "🏰";

                msg.append(rankEmoji).append(" **").append(name).append("** → `")
                        .append(String.format("%,d", totalReward)).append(" PE`\n");

                // Détail des gains si disponible
                Map<String, Integer> details = clanDetailedGains.get(entry.getKey());
                if (details != null && details.size() > 1) {
                    StringBuilder detailStr = new StringBuilder("   *Détail: ");
                    details.entrySet().stream()
                            .filter(d -> d.getValue() > 0)
                            .forEach(d -> detailStr.append(d.getKey().replace("_", " ")).append(": ")
                                    .append(d.getValue()).append(" PE, "));

                    if (detailStr.length() > 12) {
                        detailStr.setLength(detailStr.length() - 2); // Enlever la dernière virgule
                        detailStr.append("*\n");
                        msg.append(detailStr);
                    }
                }
            }

            int totalDistributed = totalClanGains.values().stream().mapToInt(Integer::intValue).sum();
            msg.append("\n💎 **Total distribué:** `").append(String.format("%,d", totalDistributed)).append(" PE`\n\n");
        }

        msg.append("───────────────────────────────────────\n\n");

        // Défis de la nouvelle semaine
        msg.append("## 🎮 **DÉFIS DE CETTE SEMAINE**\n\n");
        msg.append("*Nouveaux objectifs disponibles dès maintenant !*\n\n");

        for (Map.Entry<ChallengeCategory, ChallengeEntry> entry : active.getAllChallenges().entrySet()) {
            String emoji = getCategoryEmoji(entry.getKey());
            String categoryName = getCategoryDisplayName(entry.getKey());
            ChallengeEntry challenge = entry.getValue();

            msg.append("### ").append(emoji).append(" **").append(categoryName).append("**\n");
            msg.append("🎯 Objectif: `").append(challenge.getTarget()).append("`\n");
            msg.append("💰 Valeur: ").append(challenge.getValue()).append(" PE par unité\n\n");
        }

        // Guide rapide
        msg.append("## ℹ️ **COMMENT PARTICIPER**\n\n");
        msg.append("1. **Rejoignez un clan** avec `/clan join <nom>`\n");
        msg.append("2. **Consultez les défis** avec `/jdc` en jeu\n");
        msg.append("3. **Réalisez les objectifs** listés ci-dessus\n");
        msg.append("4. **Gagnez des récompenses** individuelles et collectives !\n\n");

        // Pied de page
        msg.append("───────────────────────────────────────\n");
        msg.append("*Bonne chance à tous pour cette nouvelle semaine !* 🍀\n\n");
        msg.append("||@everyone||");

        // Sauvegarde du message
        File file = new File(ClassyClanChallenges.getInstance().getDataFolder(), "bloc-note.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("weekly-message", msg.toString());

        // Ajouter des statistiques additionnelles pour les admins
        yaml.set("statistics.total_points_distributed", totalPointsDistributed);
        yaml.set("statistics.active_players", totalPlayersActive);
        yaml.set("statistics.participating_clans", totalClansActive);
        yaml.set("statistics.reset_date", now.toString());

        // Sauvegarder le top 3 de chaque catégorie pour référence
        for (ChallengeCategory category : previous.getAllChallenges().keySet()) {
            List<Map.Entry<UUID, Integer>> topClans = manager.getTopClansByCategory(category);
            List<Map.Entry<UUID, Integer>> topPlayers = manager.getTopPlayersByCategory(category);

            for (int i = 0; i < Math.min(3, topClans.size()); i++) {
                UUID clanId = topClans.get(i).getKey();
                Clan clan = ClassyClan.getAPI().getClanOf(clanId);
                String clanName = clan != null ? clan.getRawName() : "Inconnu";
                int score = topClans.get(i).getValue();

                yaml.set("archives.clans." + category.name().toLowerCase() + "." + (i + 1) + ".name", clanName);
                yaml.set("archives.clans." + category.name().toLowerCase() + "." + (i + 1) + ".score", score);
                yaml.set("archives.clans." + category.name().toLowerCase() + "." + (i + 1) + ".uuid", clanId.toString());
            }

            for (int i = 0; i < Math.min(3, topPlayers.size()); i++) {
                UUID playerId = topPlayers.get(i).getKey();
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
                String playerName = player.getName() != null ? player.getName() : "Inconnu";
                int score = topPlayers.get(i).getValue();

                yaml.set("archives.players." + category.name().toLowerCase() + "." + (i + 1) + ".name", playerName);
                yaml.set("archives.players." + category.name().toLowerCase() + "." + (i + 1) + ".score", score);
                yaml.set("archives.players." + category.name().toLowerCase() + "." + (i + 1) + ".uuid", playerId.toString());
            }
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[ClassyClanChallenges] Impossible de sauvegarder bloc-note.yml: " + e.getMessage());
        }
    }

    // Méthodes utilitaires pour le formatage Discord
    private String getCategoryEmoji(ChallengeCategory category) {
        return switch (category) {
            case KILL -> "⚔️";
            case MINE -> "⛏️";
            case CRAFT -> "🔨";
            case ACTION -> "🏃";
            default -> "📋";
        };
    }

    private String getCategoryDisplayName(ChallengeCategory category) {
        return switch (category) {
            case KILL -> "Combat";
            case MINE -> "Minage";
            case CRAFT -> "Artisanat";
            case ACTION -> "Actions";
            default -> category.name();
        };
    }

    private String getMedal(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            case 4 -> "4️⃣";
            case 5 -> "5️⃣";
            default -> String.valueOf(rank) + ".";
        };
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
