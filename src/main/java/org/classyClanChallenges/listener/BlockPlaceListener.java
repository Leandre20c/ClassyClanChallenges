package org.classyClanChallenges.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.WeeklyChallenge;

public class BlockPlaceListener implements Listener {

    private final WeeklyChallenge weeklyChallenge;

    public BlockPlaceListener(WeeklyChallenge weeklyChallenge) {
        this.weeklyChallenge = weeklyChallenge;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ChallengeEntry challenge = weeklyChallenge.getChallenge(ChallengeCategory.MINE);
        if (challenge == null) return;

        Material target = Material.matchMaterial(challenge.getTarget());
        if (target == null) return;

        if (event.getBlock().getType() == target) {
            // Utilise le nouveau gestionnaire au lieu des métadonnées
            ClassyClanChallenges.getInstance().getBlockDataManager().addPlayerBlock(event.getBlock().getLocation());
        }
    }
}