package org.classyClanChallenges.contribution;

import org.classyClanChallenges.challenges.ChallengeCategory;

import java.util.EnumMap;
import java.util.Map;

public class PlayerContribution {

    private final Map<ChallengeCategory, Integer> categoryMap = new EnumMap<>(ChallengeCategory.class);

    public void add(ChallengeCategory category, int amount) {
        categoryMap.put(category, categoryMap.getOrDefault(category, 0) + amount);
    }

    public int get(ChallengeCategory category) {
        return categoryMap.getOrDefault(category, 0);
    }

    public Map<ChallengeCategory, Integer> getAll() {
        return categoryMap;
    }

    public int getTotal() {
        return categoryMap.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getValue(ChallengeCategory category) {
        return this.categoryMap.getOrDefault(category, 0);
    }

}
