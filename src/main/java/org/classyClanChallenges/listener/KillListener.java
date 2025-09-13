package org.classyClanChallenges.listener;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.contribution.ContributionManager;

public class KillListener implements Listener {

    private final ContributionManager contributionManager = ClassyClanChallenges.getInstance().getContributionManager();

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player killer)) return;

        EntityType type = event.getEntityType();
        ChallengeEntry challenge = ClassyClanChallenges.getInstance().getChallengeManager().getActiveChallenges().getChallenge(ChallengeCategory.KILL);
        if (challenge != null && challenge.getTarget().equalsIgnoreCase(type.name())) {
            contributionManager.addContribution(killer.getUniqueId(), ChallengeCategory.KILL, 1);
        }
    }
}
