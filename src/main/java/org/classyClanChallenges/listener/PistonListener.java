package org.classyClanChallenges.listener;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.classyClanChallenges.ClassyClanChallenges;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PistonListener implements Listener {

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        List<Block> blocks = event.getBlocks();
        Map<Block, Boolean> playerPlacedBlocks = new HashMap<>();

        // Sauvegarder les métadonnées des blocs qui vont être déplacés
        for (Block block : blocks) {
            if (block.hasMetadata("placed_by_player")) {
                playerPlacedBlocks.put(block, true);
            }
        }

        // Appliquer les métadonnées aux nouvelles positions après le déplacement
        // On utilise un scheduler pour s'assurer que le déplacement est terminé
        ClassyClanChallenges.getInstance().getServer().getScheduler().runTaskLater(
                ClassyClanChallenges.getInstance(), () -> {
                    for (int i = 0; i < blocks.size(); i++) {
                        Block originalBlock = blocks.get(i);
                        if (playerPlacedBlocks.containsKey(originalBlock)) {
                            // Calculer la nouvelle position du bloc
                            Block newBlock = originalBlock.getRelative(event.getDirection());
                            newBlock.setMetadata("placed_by_player",
                                    new FixedMetadataValue(ClassyClanChallenges.getInstance(), true));
                        }
                    }
                }, 1L);
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        List<Block> blocks = event.getBlocks();
        Map<Block, Boolean> playerPlacedBlocks = new HashMap<>();

        // Sauvegarder les métadonnées des blocs qui vont être déplacés
        for (Block block : blocks) {
            if (block.hasMetadata("placed_by_player")) {
                playerPlacedBlocks.put(block, true);
            }
        }

        // Appliquer les métadonnées aux nouvelles positions après le déplacement
        ClassyClanChallenges.getInstance().getServer().getScheduler().runTaskLater(
                ClassyClanChallenges.getInstance(), () -> {
                    for (int i = 0; i < blocks.size(); i++) {
                        Block originalBlock = blocks.get(i);
                        if (playerPlacedBlocks.containsKey(originalBlock)) {
                            // Pour la rétraction, les blocs se déplacent vers le piston
                            Block newBlock = originalBlock.getRelative(event.getDirection());
                            newBlock.setMetadata("placed_by_player",
                                    new FixedMetadataValue(ClassyClanChallenges.getInstance(), true));
                        }
                    }
                }, 1L);
    }
}