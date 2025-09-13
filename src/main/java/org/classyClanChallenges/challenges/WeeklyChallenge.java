package org.classyClanChallenges.challenges;

import java.util.HashMap;
import java.util.Map;

public class WeeklyChallenge {

    private final Map<ChallengeCategory, ChallengeEntry> activeChallenges = new HashMap<>();

    public void setChallenge(ChallengeEntry entry) {
        activeChallenges.put(entry.getCategory(), entry);
    }

    public ChallengeEntry getChallenge(ChallengeCategory category) {
        return activeChallenges.get(category);
    }

    public Map<ChallengeCategory, ChallengeEntry> getAllChallenges() {
        return activeChallenges;
    }

    public void clear() {
        activeChallenges.clear();
    }
}
