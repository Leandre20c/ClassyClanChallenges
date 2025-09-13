package org.classyClanChallenges.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.classyClanChallenges.ClassyClanChallenges;

import java.util.ArrayList;
import java.util.List;

public class PistonListener implements Listener {

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        List<Block> blocks = event.getBlocks();
        List<Location> playerPlacedBlocks = new ArrayList<>();

        // Identifier les blocs placés par des joueurs qui vont être déplacés
        for (Block block : blocks) {
            if (ClassyClanChallenges.getInstance().getBlockDataManager().isPlayerPlaced(block.getLocation())) {
                playerPlacedBlocks.add(block.getLocation());
            }
        }

        // Appliquer les changements après le déplacement
        ClassyClanChallenges.getInstance().getServer().getScheduler().runTaskLater(
                ClassyClanChallenges.getInstance(), () -> {
                    for (int i = 0; i < blocks.size(); i++) {
                        Block originalBlock = blocks.get(i);
                        if (playerPlacedBlocks.contains(originalBlock.getLocation())) {
                            // Retirer l'ancienne position
                            ClassyClanChallenges.getInstance().getBlockDataManager().removePlayerBlock(originalBlock.getLocation());

                            // Ajouter la nouvelle position
                            Block newBlock = originalBlock.getRelative(event.getDirection());
                            ClassyClanChallenges.getInstance().getBlockDataManager().addPlayerBlock(newBlock.getLocation());
                        }
                    }
                }, 1L);
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        List<Block> blocks = event.getBlocks();
        List<Location> playerPlacedBlocks = new ArrayList<>();

        // Identifier les blocs placés par des joueurs qui vont être déplacés
        for (Block block : blocks) {
            if (ClassyClanChallenges.getInstance().getBlockDataManager().isPlayerPlaced(block.getLocation())) {
                playerPlacedBlocks.add(block.getLocation());
            }
        }

        // Appliquer les changements après le déplacement
        ClassyClanChallenges.getInstance().getServer().getScheduler().runTaskLater(
                ClassyClanChallenges.getInstance(), () -> {
                    for (int i = 0; i < blocks.size(); i++) {
                        Block originalBlock = blocks.get(i);
                        if (playerPlacedBlocks.contains(originalBlock.getLocation())) {
                            // Retirer l'ancienne position
                            ClassyClanChallenges.getInstance().getBlockDataManager().removePlayerBlock(originalBlock.getLocation());

                            // Ajouter la nouvelle position (pour la rétraction, les blocs se déplacent vers le piston)
                            Block newBlock = originalBlock.getRelative(event.getDirection());
                            ClassyClanChallenges.getInstance().getBlockDataManager().addPlayerBlock(newBlock.getLocation());
                        }
                    }
                }, 1L);
    }
}