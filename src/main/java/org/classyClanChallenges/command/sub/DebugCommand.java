package org.classyClanChallenges.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.contribution.ContributionManager;
import org.classyClanChallenges.contribution.PlayerContribution;
import org.classyclan.ClassyClan;
import org.classyclan.clan.Clan;

import java.util.Map;
import java.util.UUID;

public class DebugCommand extends SubCommand {

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public String getDescription() {
        return "Outils de debug et diagnostic.";
    }

    @Override
    public String getUsage() {
        return "/jdc debug <player|clan|system> [nom]";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage : /jdc debug <player|clan|system> [nom]");
            sender.sendMessage("§7- §eplayer <nom> §7: Debug d'un joueur spécifique");
            sender.sendMessage("§7- §eclan <nom> §7: Debug d'un clan spécifique");
            sender.sendMessage("§7- §esystem §7: Debug général du système");
            return;
        }

        String debugType = args[0].toLowerCase();

        switch (debugType) {
            case "player" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage : /jdc debug player <nom>");
                    return;
                }
                debugPlayer(sender, args[1]);
            }

            case "clan" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage : /jdc debug clan <nom>");
                    return;
                }
                debugClan(sender, args[1]);
            }

            case "system" -> debugSystem(sender);

            default -> {
                sender.sendMessage("§cType de debug invalide : " + debugType);
                sender.sendMessage("§7Types disponibles : player, clan, system");
            }
        }
    }

    private void debugPlayer(CommandSender sender, String playerName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            sender.sendMessage("§cJoueur inconnu : " + playerName);
            return;
        }

        UUID playerId = target.getUniqueId();
        ContributionManager manager = ClassyClanChallenges.getInstance().getContributionManager();
        PlayerContribution pc = manager.getPlayerContribution(playerId);

        sender.sendMessage("§6§l═══ DEBUG JOUEUR: " + (target.getName() != null ? target.getName() : playerName) + " ═══");
        sender.sendMessage("§eUUID: §f" + playerId);
        sender.sendMessage("§eEn ligne: " + (target.isOnline() ? "§aOui" : "§cNon"));

        // Clan info
        Clan clan = ClassyClan.getAPI().getClanOf(playerId);
        if (clan != null) {
            sender.sendMessage("§eClan: §f" + clan.getRawName() + " §7(Propriétaire: " + clan.getOwner() + ")");
        } else {
            sender.sendMessage("§eClan: §cAucun");
        }

        // Contributions
        sender.sendMessage("§eContributions personnelles:");
        Map<ChallengeCategory, Integer> contributions = pc.getAll();
        if (contributions.isEmpty()) {
            sender.sendMessage("  §cAucune contribution");
        } else {
            for (Map.Entry<ChallengeCategory, Integer> entry : contributions.entrySet()) {
                sender.sendMessage("  §7- " + entry.getKey().name() + ": §a" + entry.getValue());
            }
        }
        sender.sendMessage("§eTotal: §a" + pc.getTotal() + " §7points");

        sender.sendMessage("§6§l═══════════════════════════════");
    }

    private void debugClan(CommandSender sender, String clanName) {
        Clan clan = ClassyClan.getAPI().getClanByName(clanName);
        if (clan == null) {
            sender.sendMessage("§cClan introuvable : " + clanName);
            return;
        }

        ContributionManager manager = ClassyClanChallenges.getInstance().getContributionManager();

        sender.sendMessage("§6§l═══ DEBUG CLAN: " + clan.getRawName() + " ═══");
        sender.sendMessage("§eNom: §f" + clan.getRawName());
        sender.sendMessage("§eTag: §f" + clan.getRawPrefix());
        sender.sendMessage("§ePropriétaire: §f" + Bukkit.getOfflinePlayer(clan.getOwner()).getName());
        sender.sendMessage("§eMembres: §f" + clan.getMembers().size());

        // Contributions du clan
        var clanContrib = manager.getClanContribution(clan.getOwner());
        sender.sendMessage("§eContributions du clan:");
        if (clanContrib.getAll().isEmpty()) {
            sender.sendMessage("  §cAucune contribution");
        } else {
            for (Map.Entry<ChallengeCategory, Integer> entry : clanContrib.getAll().entrySet()) {
                sender.sendMessage("  §7- " + entry.getKey().name() + ": §a" + entry.getValue());
            }
        }
        sender.sendMessage("§eTotal: §a" + clanContrib.getTotal() + " §7points");

        // Membres actifs
        sender.sendMessage("§eMembres avec contributions:");
        int activeMembers = 0;
        for (UUID memberId : clan.getMembers()) {
            PlayerContribution pc = manager.getPlayerContribution(memberId);
            if (pc.getTotal() > 0) {
                activeMembers++;
                OfflinePlayer member = Bukkit.getOfflinePlayer(memberId);
                sender.sendMessage("  §7- " + (member.getName() != null ? member.getName() : "Inconnu") +
                        ": §a" + pc.getTotal() + " §7points");
            }
        }
        sender.sendMessage("§eMembres actifs: §a" + activeMembers + "§7/§f" + clan.getMembers().size());

        sender.sendMessage("§6§l═══════════════════════════════");
    }

    private void debugSystem(CommandSender sender) {
        ClassyClanChallenges plugin = ClassyClanChallenges.getInstance();
        ContributionManager manager = plugin.getContributionManager();

        sender.sendMessage("§6§l═══ DEBUG SYSTÈME ═══");

        // Version et état
        sender.sendMessage("§eVersion plugin: §f" + plugin.getDescription().getVersion());
        sender.sendMessage("§eAPI ClassyClan: " + (ClassyClan.getAPI() != null ? "§aDisponible" : "§cIndisponible"));

        // Défis actifs
        sender.sendMessage("§eDéfis actifs:");
        var challenges = plugin.getChallengeManager().getActiveChallenges().getAllChallenges();
        if (challenges.isEmpty()) {
            sender.sendMessage("  §cAucun défi actif");
        } else {
            for (Map.Entry<ChallengeCategory, ChallengeEntry> entry : challenges.entrySet()) {
                ChallengeEntry challenge = entry.getValue();
                sender.sendMessage("  §7- " + challenge.getCategory().name() + ": §f" + challenge.getTarget() +
                        " §7(valeur: §e" + challenge.getValue() + "§7)");
            }
        }

        // Statistiques
        sender.sendMessage("§eStatistiques:");
        sender.sendMessage("  §7- Joueurs dans cache: §a" + manager.getAllPlayerContributions().size());
        sender.sendMessage("  §7- Clans dans cache: §a" + manager.getAllClanContributions().size());
        sender.sendMessage("  §7- Blocs tracés: §a" + plugin.getBlockDataManager().getStoredBlockCount());

        // Pool de défis
        var pool = plugin.getChallengeManager().getPool();
        sender.sendMessage("§ePool de défis:");
        for (Map.Entry<ChallengeCategory, java.util.List<ChallengeEntry>> entry : pool.entrySet()) {
            sender.sendMessage("  §7- " + entry.getKey().name() + ": §a" + entry.getValue().size() + " §7défis");
        }

        // Mémoire
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        sender.sendMessage("§eMémoire utilisée: §f" + usedMemory + "MB§7/§f" + maxMemory + "MB");

        sender.sendMessage("§6§l═══════════════════════════════");
    }
}