package org.classyClanChallenges.contribution;

import java.util.EnumMap;
import java.util.Map;
import org.classyClanChallenges.challenges.ChallengeCategory;

public class ClanContribution {

    private final Map<ChallengeCategory, Integer> progress = new EnumMap<>(ChallengeCategory.class);

    public void add(ChallengeCategory category, int amount) {
        progress.put(category, progress.getOrDefault(category, 0) + amount);
    }

    public int get(ChallengeCategory category) {
        return progress.getOrDefault(category, 0);
    }

    public int getTotal() {
        return progress.values().stream().mapToInt(Integer::intValue).sum();
    }


    public Map<ChallengeCategory, Integer> getAll() {
        return progress;
    }

    public void reset(ChallengeCategory category) {
        progress.remove(category);
    }

    public void resetAll() {
        progress.clear();
    }
}

