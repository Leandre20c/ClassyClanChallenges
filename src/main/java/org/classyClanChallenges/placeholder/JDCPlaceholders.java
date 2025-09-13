package org.classyClanChallenges.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.WeeklyChallenge;
import org.classyClanChallenges.contribution.ContributionManager;
import org.classyclan.ClassyClan;
import org.classyclan.clan.Clan;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JDCPlaceholders extends PlaceholderExpansion {

    private final ClassyClanChallenges plugin;


    public JDCPlaceholders(ClassyClanChallenges plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "jdc";
    }

    @Override
    public String getAuthor() {
        return "TonNom";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        WeeklyChallenge weekly = plugin.getChallengeManager().getActiveChallenges();
        ContributionManager contributionManager = plugin.getContributionManager();

        // %jdc_defi_1%
        if (identifier.equalsIgnoreCase("defi_1")) {
            return getDefiNameByIndex(0, weekly);
        }
        if (identifier.equalsIgnoreCase("defi_2")) {
            return getDefiNameByIndex(1, weekly);
        }
        if (identifier.equalsIgnoreCase("defi_3")) {
            return getDefiNameByIndex(2, weekly);
        }

        // %jdc_defi_1_clan_top_1%
        if (identifier.matches("defi_\\d+_clan_top_\\d+")) {
            int defiIndex = Integer.parseInt(identifier.split("_")[1]) - 1;
            int rank = Integer.parseInt(identifier.split("_")[4]);
            return getClanTop(weekly, contributionManager, defiIndex, rank);
        }

        // %jdc_defi_1_player_top_1%
        if (identifier.matches("defi_\\d+_player_top_\\d+")) {
            int defiIndex = Integer.parseInt(identifier.split("_")[1]) - 1;
            int rank = Integer.parseInt(identifier.split("_")[4]);
            return getPlayerTop(weekly, contributionManager, defiIndex, rank);
        }

        // %jdc_top_global_player_1%
        if (identifier.matches("top_global_player_\\d+")) {
            int rank = Integer.parseInt(identifier.split("_")[3]);
            return getTopGlobalPlayer(rank, contributionManager);
        }

// %jdc_top_global_clan_1%
        if (identifier.matches("top_global_clan_\\d+")) {
            int rank = Integer.parseInt(identifier.split("_")[3]);
            return getTopGlobalClan(rank, contributionManager);
        }

// %jdc_player_points%
        if (identifier.equalsIgnoreCase("player_points")) {
            return String.valueOf(contributionManager.getPlayerContribution(player.getUniqueId()).getTotal());
        }

// %jdc_player_points_<categorie>%
        if (identifier.startsWith("player_points_")) {
            String rawCat = identifier.replace("player_points_", "").toUpperCase();
            try {
                ChallengeCategory category = ChallengeCategory.valueOf(rawCat);
                return String.valueOf(contributionManager.getPlayerContribution(player.getUniqueId()).getValue(category));
            } catch (IllegalArgumentException e) {
                return "N/A";
            }
        }

        return null;

    }

    private String getDefiNameByIndex(int index, WeeklyChallenge weekly) {
        List<Map.Entry<ChallengeCategory, ChallengeEntry>> list = weekly.getAllChallenges().entrySet().stream().toList();
        if (index >= list.size()) return "N/A";
        ChallengeEntry entry = list.get(index).getValue();
        return entry.getCategory().name() + " : " + entry.getTarget();
    }

    private String getClanTop(WeeklyChallenge weekly, ContributionManager manager, int index, int rank) {
        List<Map.Entry<ChallengeCategory, ChallengeEntry>> list = weekly.getAllChallenges().entrySet().stream().toList();
        if (index >= list.size()) return "N/A";

        ChallengeCategory category = list.get(index).getKey();
        List<Map.Entry<UUID, Integer>> top = manager.getTopClansByCategory(category);
        if (rank <= 0 || rank > top.size()) return "N/A";

        UUID ownerId = top.get(rank - 1).getKey();
        Clan clan = ClassyClan.getAPI().getClanOf(ownerId);
        String name = (clan != null) ? clan.getRawName() : "Inconnu";
        int points = top.get(rank - 1).getValue();
        return name + " : " + points;
    }

    private String getPlayerTop(WeeklyChallenge weekly, ContributionManager manager, int index, int rank) {
        List<Map.Entry<ChallengeCategory, ChallengeEntry>> list = weekly.getAllChallenges().entrySet().stream().toList();
        if (index >= list.size()) return "N/A";

        ChallengeCategory category = list.get(index).getKey();
        List<Map.Entry<UUID, Integer>> top = manager.getTopPlayersByCategory(category);
        if (rank <= 0 || rank > top.size()) return "N/A";

        UUID playerId = top.get(rank - 1).getKey();
        String name = plugin.getServer().getOfflinePlayer(playerId).getName();
        int points = top.get(rank - 1).getValue();
        return name + " : " + points;
    }


    private String getTopGlobalPlayer(int rank, ContributionManager manager) {
        List<Map.Entry<UUID, Integer>> top = manager.getTopPlayersGlobal();
        if (rank <= 0 || rank > top.size()) return "N/A";

        UUID uuid = top.get(rank - 1).getKey();
        int points = top.get(rank - 1).getValue();
        String name = plugin.getServer().getOfflinePlayer(uuid).getName();
        return (name != null ? name : "???") + " : " + points;
    }

    private String getTopGlobalClan(int rank, ContributionManager manager) {
        List<Map.Entry<UUID, Integer>> top = manager.getTopClansGlobal();
        if (rank <= 0 || rank > top.size()) return "N/A";

        UUID ownerId = top.get(rank - 1).getKey();
        Clan clan = ClassyClan.getAPI().getClanOf(ownerId);
        String name = (clan != null) ? clan.getRawName() : "Inconnu";
        int points = top.get(rank - 1).getValue();
        return name + " : " + points;
    }


}
