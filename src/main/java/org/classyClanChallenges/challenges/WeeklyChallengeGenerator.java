package org.classyClanChallenges.challenges;

import org.classyClanChallenges.ClassyClanChallenges;

import java.util.*;

public class WeeklyChallengeGenerator {

    private final Map<ChallengeCategory, List<ChallengeEntry>> pool;

    public WeeklyChallengeGenerator(Map<ChallengeCategory, List<ChallengeEntry>> pool) {
        this.pool = pool;
    }

    public WeeklyChallenge generate() {
        WeeklyChallenge result = new WeeklyChallenge();
        List<ChallengeCategory> availableCategories = new ArrayList<>(pool.keySet());
        Collections.shuffle(availableCategories);

        int count = 0;
        for (ChallengeCategory category : availableCategories) {
            List<ChallengeEntry> entries = pool.get(category);
            if (entries == null || entries.isEmpty()) continue;

            ChallengeEntry random = entries.get(new Random().nextInt(entries.size()));
            result.setChallenge(random);
            count++;

            if (count >= 3) break;
        }

        if (count < 3) {
            ClassyClanChallenges.getInstance().getLogger().warning("[Challenges] Moins de 3 catégories disponibles !");
        }

        return result;
    }
}
