package org.classyClanChallenges.contribution;

import org.classyClanChallenges.challenges.ChallengeCategory;

import java.util.EnumMap;
import java.util.Map;

public class PlayerContribution {

    private final Map<ChallengeCategory, Integer> categoryMap = new EnumMap<>(ChallengeCategory.class);

    public void add(ChallengeCategory category, int amount) {
        categoryMap.put(category, categoryMap.getOrDefault(category, 0) + amount);
    }

    /**
     * Définit une valeur exacte pour une catégorie (nouveau)
     */
    public void set(ChallengeCategory category, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("La valeur ne peut pas être négative");
        }
        categoryMap.put(category, amount);
    }

    /**
     * Remet à zéro une catégorie spécifique (nouveau)
     */
    public void reset(ChallengeCategory category) {
        categoryMap.remove(category);
    }

    /**
     * Remet à zéro toutes les catégories (nouveau)
     */
    public void resetAll() {
        categoryMap.clear();
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