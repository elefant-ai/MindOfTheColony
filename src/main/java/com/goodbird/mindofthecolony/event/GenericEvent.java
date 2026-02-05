package com.goodbird.mindofthecolony.event;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * A generic colony event for simple occurrences that don't need special handling.
 */
public class GenericEvent implements ColonyEvent {
    private final String id;
    private final String type;
    private final long timestamp;
    private final int colonyId;
    private final String description;

    public GenericEvent(String type, long timestamp, int colonyId, String description) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.timestamp = timestamp;
        this.colonyId = colonyId;
        this.description = description;
    }

    private GenericEvent(String id, String type, long timestamp, int colonyId, String description) {
        this.id = id;
        this.type = type;
        this.timestamp = timestamp;
        this.colonyId = colonyId;
        this.description = description;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int getColonyId() {
        return colonyId;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("type", type);
        tag.putLong("timestamp", timestamp);
        tag.putInt("colonyId", colonyId);
        tag.putString("description", description);
        return tag;
    }

    public static GenericEvent fromNBT(CompoundTag tag) {
        return new GenericEvent(
            tag.getString("id"),
            tag.getString("type"),
            tag.getLong("timestamp"),
            tag.getInt("colonyId"),
            tag.getString("description")
        );
    }

    @Override
    public String toString() {
        return "GenericEvent{" +
            "type='" + type + '\'' +
            ", description='" + description + '\'' +
            '}';
    }
}
