package org.classyClanChallenges.command;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.WeeklyChallenge;
import org.classyClanChallenges.contribution.ClanContribution;
import org.classyClanChallenges.contribution.ContributionManager;
import org.classyClanChallenges.contribution.PlayerContribution;
import org.classyClanChallenges.util.GUIBuilder;
import org.classyclan.ClassyClan;
import org.classyclan.api.ClassyClanAPI;
import org.classyclan.clan.Clan;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainCommand extends SubCommand {

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Ouvre le menu principal - Défis, Statistiques, Récompenses, Classements";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean isAdminOnly() {
        return false;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommande réservée aux joueurs.");
            return;
        }

        ClassyClanAPI api = ClassyClan.getAPI();
        ContributionManager manager = ClassyClanChallenges.getInstance().getContributionManager();
        WeeklyChallenge challenges = ClassyClanChallenges.getInstance().getChallengeManager().getActiveChallenges();
        PlayerContribution pc = manager.getPlayerContribution(player.getUniqueId());

        // Vérification si le joueur est dans un clan
        boolean hasclan = api != null && api.isInClan(player.getUniqueId());
        Clan clan = null;
        ClanContribution cc = null;

        if (hasclan) {
            clan = api.getClanOf(player.getUniqueId());
            if (clan != null) {
                cc = manager.getClanContribution(clan.getOwner());
            }
        }

        GUIBuilder gui = new GUIBuilder("§cᴊᴇᴜх ᴅᴇ ᴄʟᴀɴ", 5);
        int[] slots = {10, 13, 16};
        int index = 0;

        for (Map.Entry<ChallengeCategory, ChallengeEntry> entry : challenges.getAllChallenges().entrySet()) {
            ChallengeCategory category = entry.getKey();
            ChallengeEntry ce = entry.getValue();

            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            String verb = switch (category.name()) {
                case "CRAFT" -> "fabriquer";
                case "MINE" -> "miner";
                case "ACTION" -> "faire l'action de";
                case "KILL" -> "tuer";
                default -> "accomplir";
            };

            meta.setDisplayName("§6" + category.name());

            List<String> lore = new ArrayList<>();
            lore.add("§7Objectif : §b" + ce.getTarget());
            lore.add("");
            lore.add("§7L'objectif est de " + verb + " le plus de §b" + ce.getTarget());
            lore.add("");
            lore.add("§eToi : §a" + pc.get(category));

            if (hasclan && cc != null) {
                lore.add("§eClan : §a" + cc.get(category));
            } else if (hasclan) {
                lore.add("§eClan : §cAucune contribution");
            } else {
                lore.add("§eClan : §7(Pas de clan)");
                lore.add("");
                lore.add("§7§oRejoins un clan pour participer");
                lore.add("§7§oaux défis collectifs !");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            if (index < slots.length) {
                gui.setItem(slots[index], item);
                index++;
            }
        }

        // Item de stats personnelles
        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta playerMeta = playerItem.getItemMeta();
        if (playerMeta != null) {
            playerMeta.setDisplayName("§e§l» Tes contributions personnelles :");

            List<String> lore = new ArrayList<>();
            Map<ChallengeCategory, Integer> contributions = pc.getAll();
            if (contributions.isEmpty()) {
                lore.add("§cAucune contribution cette semaine.");
            } else {
                for (Map.Entry<ChallengeCategory, Integer> entry : contributions.entrySet()) {
                    lore.add("§7- §f" + entry.getKey().name() + " : §a" + entry.getValue());
                }
            }

            if (!hasclan) {
                lore.add("");
                lore.add("§7§oTu participes en solo !");
                lore.add("§7§oRejoins un clan pour plus de récompenses.");
            }

            playerMeta.setLore(lore);
            playerItem.setItemMeta(playerMeta);
            gui.setItem(30, playerItem);
        }

        // Item de stats de clan ou invitation à rejoindre un clan
        ItemStack clanItem;
        ItemMeta clanMeta;

        if (hasclan && clan != null) {
            clanItem = clan.getBanner();
            clanMeta = clanItem.getItemMeta();
            if (clanMeta != null) {
                clanMeta.setDisplayName("§e§l» Les contributions de ton clan :");

                List<String> lore = new ArrayList<>();
                lore.add("Clan - " + clan.getColoredName() + " : ");
                lore.add("");
                if (cc != null && !cc.getAll().isEmpty()) {
                    for (Map.Entry<ChallengeCategory, Integer> entry : cc.getAll().entrySet()) {
                        lore.add("§7- §f" + entry.getKey().name() + " : §a" + entry.getValue());
                    }
                } else {
                    lore.add("§cAucune contribution cette semaine.");
                }

                clanMeta.setLore(lore);
            }
        } else {
            // Joueur sans clan - Item d'invitation
            clanItem = new ItemStack(Material.BARRIER);
            clanMeta = clanItem.getItemMeta();
            if (clanMeta != null) {
                clanMeta.setDisplayName("§c§l» Tu n'es pas dans un clan !");
                clanMeta.setLore(List.of(
                        "§7Rejoins un clan pour :",
                        "",
                        "§a• Participer aux défis collectifs",
                        "§a• Gagner des récompenses de clan",
                        "§a• Collaborer avec d'autres joueurs",
                        "§a• Accéder aux bonus de clan",
                        "",
                        "§bCommandes utiles :",
                        "§7/clan create <nom> <tag> - Créer un clan",
                        "§7/clan join <nom> - Rejoindre un clan",
                        "§7/clan list - Voir les clans disponibles"
                ));
            }
        }

        if (clanMeta != null) {
            clanItem.setItemMeta(clanMeta);
        }
        gui.setItem(32, clanItem);

        ItemStack classementItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta classementMeta = classementItem.getItemMeta();
        if (classementMeta != null) {
            classementMeta.setDisplayName("§e§l» Voir les classements");
            classementMeta.setLore(List.of(
                    "§7Consulte les meilleurs joueurs",
                    "§7et clans dans chaque catégorie !",
                    "",
                    "§aClique pour ouvrir le classement"
            ));
            // Ajoute un tag pour l'identifier facilement
            classementMeta.getPersistentDataContainer().set(
                    new NamespacedKey(ClassyClanChallenges.getInstance(), "jdc_classement"),
                    PersistentDataType.INTEGER,
                    1
            );
            classementItem.setItemMeta(classementMeta);
        }
        gui.setItem(28, classementItem);

        // Bouton récompenses
        ItemStack rewardItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta rewardMeta = rewardItem.getItemMeta();
        if (rewardMeta != null) {
            rewardMeta.setDisplayName("§e§l» Récompenses hebdomadaires");
            List<String> rewardLore = new ArrayList<>();
            rewardLore.add("§7À la fin de chaque semaine :");
            rewardLore.add("");
            rewardLore.add("§6Top 3 des clans par catégorie :");
            rewardLore.add("§71ᵉʳ §f: §e60 000 PE à la banque de clan");
            rewardLore.add("§72ᵉ §f: §e40 000 PE à la banque de clan");
            rewardLore.add("§73ᵉ §f: §e20 000 PE à la banque de clan");
            rewardLore.add("§7 3 000 PE pour le reste des participants");
            rewardLore.add("");
            rewardLore.add("§dTop 3 des joueurs par catégorie :");
            rewardLore.add("§71ᵉʳ §f: §62 clés légendaires");
            rewardLore.add("§72ᵉ §f: §61 clé légendaire");
            rewardLore.add("§73ᵉ §f: §65 clé épique");
            rewardLore.add("");

            if (hasclan) {
                rewardLore.add("§cAussi, chaque clan gagne un bonus en fonction");
                rewardLore.add("§cdu nombre de bloc/items farmés.");
            } else {
                rewardLore.add("§7§oRejoins un clan pour débloquer");
                rewardLore.add("§7§oles récompenses collectives !");
            }

            rewardMeta.setLore(rewardLore);
            rewardItem.setItemMeta(rewardMeta);
        }
        gui.setItem(34, rewardItem);

        gui.setCancelClicks(true);
        player.openInventory(gui.build());
    }

}