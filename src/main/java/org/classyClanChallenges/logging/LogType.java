package org.classyClanChallenges.logging;

/**
 * Types d'événements loggés
 */
public enum LogType {
    POINT_GAIN("Gain de points"),
    POINT_MODIFICATION("Modification de points"),
    SYSTEM_EVENT("Événement système"),
    CHALLENGE_COMPLETION("Défi terminé"),
    WEEKLY_RESET("Reset hebdomadaire"),
    ADMIN_ACTION("Action administrateur");

    private final String displayName;

    LogType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}