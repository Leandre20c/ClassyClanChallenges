package org.classyClanChallenges.reward;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.WeeklyChallenge;
import org.classyClanChallenges.contribution.ClanContribution;
import org.classyClanChallenges.contribution.ContributionManager;
import org.classyclan.ClassyClan;
import org.classyclan.clan.Clan;

import java.io.File;
import java.util.*;

public class RewardManager {

    private final YamlConfiguration config;

    public RewardManager() {
        File file = new File(ClassyClanChallenges.getInstance().getDataFolder(), "rewards.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void distributeRewards(WeeklyChallenge challenge, ContributionManager manager) {
        for (ChallengeCategory category : ChallengeCategory.values()) {
            ChallengeEntry entry = challenge.getChallenge(category);
            if (entry == null) continue;

            String target = entry.getTarget().toUpperCase();
            int unitValue = entry.getValue(); // valeur du défi

            // 🧮 Récompense de base = total du clan * valeur
            for (Map.Entry<UUID, Clan> clanEntry : getAllClansWithContributions(manager, category).entrySet()) {
                UUID clanId = clanEntry.getKey();
                Clan clan = clanEntry.getValue();
                int contribution = manager.getClanContribution(clanId).get(category);
                if (contribution > 0) {
                    double reward = (double) (contribution * unitValue) / 100;
                    clan.deposit(reward);
                }
            }

            // 🏆 Top 3 Clans
            List<Map.Entry<UUID, Integer>> topClans = manager.getTopClansByCategory(category);
            Set<UUID> rewardedClans = new HashSet<>();
            for (int i = 0; i < topClans.size(); i++) {
                UUID clanId = topClans.get(i).getKey();
                int amount = config.getInt("clan-ranking-rewards." + (i + 1), 0);
                Clan clan = ClassyClan.getAPI().getClanOf(clanId);
                if (clan != null && amount > 0) {
                    clan.deposit(amount);
                    rewardedClans.add(clanId);
                }
            }

            // 🎁 Récompense par participation
            int defaultReward = config.getInt("clan-ranking-rewards.default", 0);
            for (UUID clanId : manager.getAllClanContributions().keySet()) {
                if (rewardedClans.contains(clanId)) continue;
                int score = manager.getClanContribution(clanId).get(category);
                if (score > 0) {
                    Clan clan = ClassyClan.getAPI().getClanOf(clanId);
                    if (clan != null) {
                        clan.deposit(defaultReward);
                    }
                }
            }

            // 🎖 Récompenses joueurs top 3
            List<Map.Entry<UUID, Integer>> topPlayers = manager.getTopPlayersByCategory(category);
            for (int i = 0; i < topPlayers.size(); i++) {
                UUID uuid = topPlayers.get(i).getKey();
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                if (player.getName() == null) continue;

                int rank = i + 1;
                List<String> commands = config.getStringList("player-ranking-rewards." + rank);
                for (String cmd : commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player_name%", player.getName()));
                }
            }
        }
    }

    private Map<UUID, Clan> getAllClansWithContributions(ContributionManager manager, ChallengeCategory category) {
        Map<UUID, Clan> result = new HashMap<>();
        for (UUID clanId : manager.getAllClanContributions().keySet()) {
            int amount = manager.getClanContribution(clanId).get(category);
            if (amount > 0) {
                Clan clan = ClassyClan.getAPI().getClanOf(clanId);
                if (clan != null) {
                    result.put(clanId, clan);
                }
            }
        }
        return result;
    }

    public int getClanRankingBonus(int rank) {
        if (rank == 0) return config.getInt("clan-ranking-rewards.default", 0);
        return config.getInt("clan-ranking-rewards." + rank, 0);
    }



}
