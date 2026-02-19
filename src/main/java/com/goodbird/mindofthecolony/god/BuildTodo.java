package com.goodbird.mindofthecolony.god;

/**
 * An entry in the Colony God's build queue.
 * Represents a building type that the god has decided should be constructed.
 */
public class BuildTodo {

    private final String buildingType;
    private final int priority;
    private final String reason;
    private final long timestamp;

    public BuildTodo(String buildingType, int priority, String reason) {
        this.buildingType = buildingType;
        this.priority = priority;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }

    public BuildTodo(String buildingType, int priority, String reason, long timestamp) {
        this.buildingType = buildingType;
        this.priority = priority;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public String getBuildingType() {
        return buildingType;
    }

    public int getPriority() {
        return priority;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return buildingType + " (priority " + priority + "): " + reason;
    }
}
