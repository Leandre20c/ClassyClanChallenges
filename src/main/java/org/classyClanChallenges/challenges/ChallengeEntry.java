package org.classyClanChallenges.challenges;

public class ChallengeEntry {

    private final ChallengeCategory category;
    private final String target;
    private final int value;
    private final boolean isFormula;

    public ChallengeEntry(ChallengeCategory category, String target, int value) {
        this.category = category;
        this.target = target;
        this.value = value;
        this.isFormula = false;
    }

    public ChallengeEntry(ChallengeCategory category, String target, boolean isFormula) {
        this.category = category;
        this.target = target;
        this.value = 0;
        this.isFormula = isFormula;
    }

    public ChallengeCategory getCategory() {
        return category;
    }

    public String getTarget() {
        return target;
    }

    public int getValue() {
        return value;
    }

    public boolean isFormula() {
        return isFormula;
    }
}
