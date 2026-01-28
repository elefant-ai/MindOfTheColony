package com.goodbird.mindofthecolony.events;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * A single event instance that occurred in a colony.
 * Immutable after creation; serializable to/from NBT.
 */
public class ColonyEvent {

    private final String eventTypeId;
    private final EventCategory category;
    private final String description;
    private final long createdAtTick;
    private final int createdAtDay;
    private final int durationDays;
    private final double happinessModifier;
    private final UUID uuid;

    public ColonyEvent(String eventTypeId, EventCategory category, String description,
                       long createdAtTick, int createdAtDay, int durationDays,
                       double happinessModifier) {
        this.eventTypeId = eventTypeId;
        this.category = category;
        this.description = description;
        this.createdAtTick = createdAtTick;
        this.createdAtDay = createdAtDay;
        this.durationDays = durationDays;
        this.happinessModifier = happinessModifier;
        this.uuid = UUID.randomUUID();
    }

    private ColonyEvent(String eventTypeId, EventCategory category, String description,
                        long createdAtTick, int createdAtDay, int durationDays,
                        double happinessModifier, UUID uuid) {
        this.eventTypeId = eventTypeId;
        this.category = category;
        this.description = description;
        this.createdAtTick = createdAtTick;
        this.createdAtDay = createdAtDay;
        this.durationDays = durationDays;
        this.happinessModifier = happinessModifier;
        this.uuid = uuid;
    }

    public boolean isExpired(int currentDay) {
        return currentDay > createdAtDay + durationDays;
    }

    // --- Getters ---

    public String getEventTypeId() { return eventTypeId; }
    public EventCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public long getCreatedAtTick() { return createdAtTick; }
    public int getCreatedAtDay() { return createdAtDay; }
    public int getDurationDays() { return durationDays; }
    public double getHappinessModifier() { return happinessModifier; }
    public UUID getUuid() { return uuid; }

    // --- NBT serialization ---

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("eventTypeId", eventTypeId);
        tag.putString("category", category.name());
        tag.putString("description", description);
        tag.putLong("createdAtTick", createdAtTick);
        tag.putInt("createdAtDay", createdAtDay);
        tag.putInt("durationDays", durationDays);
        tag.putDouble("happinessModifier", happinessModifier);
        tag.putUUID("uuid", uuid);
        return tag;
    }

    public static ColonyEvent fromNBT(CompoundTag tag) {
        EventCategory cat;
        try {
            cat = EventCategory.valueOf(tag.getString("category"));
        } catch (IllegalArgumentException e) {
            cat = EventCategory.RUMOR;
        }

        return new ColonyEvent(
            tag.getString("eventTypeId"),
            cat,
            tag.getString("description"),
            tag.getLong("createdAtTick"),
            tag.getInt("createdAtDay"),
            tag.getInt("durationDays"),
            tag.getDouble("happinessModifier"),
            tag.hasUUID("uuid") ? tag.getUUID("uuid") : UUID.randomUUID()
        );
    }
}
