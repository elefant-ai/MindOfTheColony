package com.goodbird.mindofthecolony.event;

import net.minecraft.nbt.CompoundTag;

/**
 * Represents an event that occurred in a colony.
 * Events are recorded for LLM context and can affect citizens.
 */
public interface ColonyEvent {

    /**
     * Unique identifier for this event instance.
     */
    String getId();

    /**
     * Type of event (e.g., "disease_outbreak", "thunderstorm", "prolonged_rain").
     */
    String getType();

    /**
     * Game tick when this event occurred.
     */
    long getTimestamp();

    /**
     * Colony ID where this event occurred.
     */
    int getColonyId();

    /**
     * Human-readable description for LLM context.
     * Example: "A disease outbreak affected 3 citizens including Martha"
     */
    String getDescription();

    /**
     * Serialize to NBT for persistence.
     */
    CompoundTag toNBT();

    /**
     * Factory method to deserialize from NBT.
     */
    static ColonyEvent fromNBT(CompoundTag tag) {
        String type = tag.getString("type");
        return switch (type) {
            case "disease_outbreak" -> DiseaseOutbreakEvent.fromNBT(tag);
            case "weather" -> WeatherEvent.fromNBT(tag);
            default -> GenericEvent.fromNBT(tag);
        };
    }
}
