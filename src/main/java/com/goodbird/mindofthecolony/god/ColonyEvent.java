package com.goodbird.mindofthecolony.god;

/**
 * Represents a colony event that the Colony God should react to.
 */
public class ColonyEvent {

    public enum Type {
        CITIZEN_JOINED,
        CITIZEN_DIED
    }

    private final Type type;
    private final String citizenName;
    private final int citizenId;
    private final String jobName;
    private final long timestamp;

    public ColonyEvent(Type type, String citizenName, int citizenId, String jobName) {
        this.type = type;
        this.citizenName = citizenName;
        this.citizenId = citizenId;
        this.jobName = jobName;
        this.timestamp = System.currentTimeMillis();
    }

    public Type getType() {
        return type;
    }

    public String getCitizenName() {
        return citizenName;
    }

    public int getCitizenId() {
        return citizenId;
    }

    public String getJobName() {
        return jobName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return switch (type) {
            case CITIZEN_JOINED -> citizenName + " (ID " + citizenId + ") has joined the colony" +
                    (jobName.isEmpty() ? " (unemployed)" : " as a " + jobName);
            case CITIZEN_DIED -> citizenName + " (ID " + citizenId + ") has died" +
                    (jobName.isEmpty() ? "" : " (was working as " + jobName + ")");
        };
    }
}
