package org.classyClanChallenges.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.command.SubCommand;
import org.classyClanChallenges.util.BlockDataManager;

public class BlocksCommand extends SubCommand {

    @Override
    public String getName() {
        return "blocks";
    }

    @Override
    public String getDescription() {
        return "Gère les blocs tracés par le système.";
    }

    @Override
    public String getUsage() {
        return "/jdc blocks <count|cleanup|clear|check>";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage : /jdc blocks <count|cleanup|clear|check>");
            sender.sendMessage("§7- §ecount §7: Affiche le nombre de blocs tracés");
            sender.sendMessage("§7- §ecleanup §7: Nettoie les blocs des mondes inexistants");
            sender.sendMessage("§7- §eclear §7: Vide complètement la liste");
            sender.sendMessage("§7- §echeck §7: Vérifie le bloc que vous regardez");
            return;
        }

        BlockDataManager blockManager = ClassyClanChallenges.getInstance().getBlockDataManager();
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "count" -> {
                int count = blockManager.getStoredBlockCount();
                sender.sendMessage("§a✓ Nombre de blocs tracés : §e" + count);
            }

            case "cleanup" -> {
                sender.sendMessage("§eNettoyage des blocs invalides...");
                blockManager.cleanupInvalidWorlds();
                sender.sendMessage("§a✓ Nettoyage terminé.");
            }

            case "clear" -> {
                sender.sendMessage("§c⚠ Êtes-vous sûr de vouloir vider la liste ?");
                sender.sendMessage("§cTapez §e/jdc blocks confirmclear §cpour confirmer.");
            }

            case "confirmclear" -> {
                blockManager.clearAllBlocks();
                sender.sendMessage("§a✓ Liste des blocs vidée.");
                ClassyClanChallenges.getInstance().getLogger().warning(
                        sender.getName() + " a vidé la liste des blocs tracés."
                );
            }

            case "check" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cSeuls les joueurs peuvent utiliser cette sous-commande.");
                    return;
                }

                var targetBlock = player.getTargetBlockExact(5);
                if (targetBlock == null) {
                    sender.sendMessage("§cAucun bloc trouvé dans votre visée.");
                    return;
                }

                boolean isPlayerPlaced = blockManager.isPlayerPlaced(targetBlock.getLocation());
                sender.sendMessage("§eBLoc : §f" + targetBlock.getType().name());
                sender.sendMessage("§ePosition : §f" + targetBlock.getX() + ", " + targetBlock.getY() + ", " + targetBlock.getZ());
                sender.sendMessage("§ePlacé par un joueur : " + (isPlayerPlaced ? "§aOui" : "§cNon"));
            }

            default -> {
                sender.sendMessage("§cSous-commande inconnue : " + subCommand);
                sender.sendMessage("§7Disponibles : count, cleanup, clear, check");
            }
        }
    }
}