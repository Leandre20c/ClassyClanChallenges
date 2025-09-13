package org.classyClanChallenges.logging;

import org.classyClanChallenges.challenges.ChallengeCategory;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Représente une entrée de log pour traçabilité
 */
public class LogEntry {

    private final UUID playerId;
    private final ChallengeCategory category;
    private final LogType type;
    private final int points;
    private final String reason;
    private final String details;
    private final LocalDateTime timestamp;

    public LogEntry(UUID playerId, ChallengeCategory category, LogType type, int points, String reason, String details, LocalDateTime timestamp) {
        this.playerId = playerId;
        this.category = category;
        this.type = type;
        this.points = points;
        this.reason = reason;
        this.details = details;
        this.timestamp = timestamp;
    }

    // Getters
    public UUID getPlayerId() { return playerId; }
    public ChallengeCategory getCategory() { return category; }
    public LogType getType() { return type; }
    public int getPoints() { return points; }
    public String getReason() { return reason; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] %s: %+d points (%s) - %s",
                timestamp.toString(), type, points, reason, details);
    }
}