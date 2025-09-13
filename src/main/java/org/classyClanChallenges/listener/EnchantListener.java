package org.classyClanChallenges.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.classyClanChallenges.ClassyClanChallenges;
import org.classyClanChallenges.challenges.ChallengeCategory;
import org.classyClanChallenges.challenges.ChallengeEntry;
import org.classyClanChallenges.challenges.WeeklyChallenge;
import org.classyClanChallenges.contribution.ContributionManager;

public class EnchantListener implements Listener {

    private final WeeklyChallenge weeklyChallenge;
    private final ContributionManager contributionManager;

    public EnchantListener(WeeklyChallenge weeklyChallenge, ContributionManager contributionManager) {
        this.weeklyChallenge = weeklyChallenge;
        this.contributionManager = contributionManager;
    }
}
