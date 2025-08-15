package org.classyClanChallenges.challenges;

public enum ChallengeCategory {
    CRAFT,
    MINE,
    KILL,
    ACTION;

    public static ChallengeCategory fromString(String input) {
        for (ChallengeCategory cat : values()) {
            if (cat.name().equalsIgnoreCase(input)) return cat;
        }
        return null;
    }
}
