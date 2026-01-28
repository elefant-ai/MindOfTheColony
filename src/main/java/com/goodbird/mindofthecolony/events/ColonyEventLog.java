package com.goodbird.mindofthecolony.events;

import com.goodbird.mindofthecolony.config.ModSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Per-colony log of active and historical events.
 */
public class ColonyEventLog {

    private final int colonyId;
    private final List<ColonyEvent> activeEvents = new ArrayList<>();
    private final List<ColonyEvent> eventHistory = new ArrayList<>();
    private int lastFlavorGenerationDay = -1;
    private boolean dirty = false;

    public ColonyEventLog(int colonyId) {
        this.colonyId = colonyId;
    }

    /**
     * Add a new event to the active list.
     */
    public void addEvent(ColonyEvent event) {
        activeEvents.add(event);
        dirty = true;
    }

    /**
     * Called periodically to expire old events and move them to history.
     */
    public void tick(int currentDay) {
        List<ColonyEvent> expired = new ArrayList<>();
        for (ColonyEvent event : activeEvents) {
            if (event.isExpired(currentDay)) {
                expired.add(event);
            }
        }

        if (!expired.isEmpty()) {
            activeEvents.removeAll(expired);
            eventHistory.addAll(expired);

            int maxHistory = ModSettings.EVENT_HISTORY_SIZE.get();
            while (eventHistory.size() > maxHistory) {
                eventHistory.remove(0);
            }
            dirty = true;
        }
    }

    /**
     * Check if there have been any real (non-flavor) detected events recently.
     */
    public boolean hasRecentRealEvents(int currentDay, int windowDays) {
        for (ColonyEvent event : activeEvents) {
            if (!isFlavorEvent(event.getEventTypeId()) &&
                currentDay - event.getCreatedAtDay() <= windowDays) {
                return true;
            }
        }
        return false;
    }

    private boolean isFlavorEvent(String eventTypeId) {
        for (EventDefinitions.EventTypeEntry entry : EventDefinitions.getFlavorEvents()) {
            if (entry.id().equals(eventTypeId)) {
                return true;
            }
        }
        return false;
    }

    // --- AI Context Generation ---

    /**
     * Generate JSON context string for AgentStatus (max 5 most recent events).
     */
    public String toContextString() {
        if (activeEvents.isEmpty()) {
            return "[]";
        }

        int limit = Math.min(activeEvents.size(), 5);
        List<ColonyEvent> recent = activeEvents.subList(activeEvents.size() - limit, activeEvents.size());

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < recent.size(); i++) {
            ColonyEvent e = recent.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format(
                "{\"type\":\"%s\",\"category\":\"%s\",\"description\":\"%s\",\"days_ago\":%d,\"mood\":\"%s\"}",
                e.getEventTypeId(),
                e.getCategory().name(),
                e.getDescription().replace("\"", "\\\""),
                0, // days_ago will be computed by caller if needed
                e.getHappinessModifier() > 0 ? "positive" : e.getHappinessModifier() < 0 ? "negative" : "neutral"
            ));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Generate plain text for the system prompt.
     */
    public String toSystemPromptSection() {
        if (activeEvents.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("CURRENT EVENTS IN THE COLONY:\n");
        for (ColonyEvent e : activeEvents) {
            sb.append(String.format("- [%s] %s\n", e.getCategory().name(), e.getDescription()));
        }
        sb.append("React to these events naturally based on your personality.");
        return sb.toString();
    }

    // --- Getters ---

    public int getColonyId() { return colonyId; }
    public List<ColonyEvent> getActiveEvents() { return Collections.unmodifiableList(activeEvents); }
    public List<ColonyEvent> getEventHistory() { return Collections.unmodifiableList(eventHistory); }
    public int getLastFlavorGenerationDay() { return lastFlavorGenerationDay; }
    public void setLastFlavorGenerationDay(int day) { this.lastFlavorGenerationDay = day; dirty = true; }
    public boolean isDirty() { return dirty; }
    public void clearDirty() { dirty = false; }

    // --- NBT Persistence ---

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("colonyId", colonyId);
        tag.putInt("lastFlavorDay", lastFlavorGenerationDay);

        ListTag activeTag = new ListTag();
        for (ColonyEvent event : activeEvents) {
            activeTag.add(event.toNBT());
        }
        tag.put("activeEvents", activeTag);

        ListTag historyTag = new ListTag();
        for (ColonyEvent event : eventHistory) {
            historyTag.add(event.toNBT());
        }
        tag.put("eventHistory", historyTag);

        return tag;
    }

    public static ColonyEventLog fromNBT(CompoundTag tag) {
        int colonyId = tag.getInt("colonyId");
        ColonyEventLog log = new ColonyEventLog(colonyId);
        log.lastFlavorGenerationDay = tag.getInt("lastFlavorDay");

        ListTag activeTag = tag.getList("activeEvents", Tag.TAG_COMPOUND);
        for (int i = 0; i < activeTag.size(); i++) {
            log.activeEvents.add(ColonyEvent.fromNBT(activeTag.getCompound(i)));
        }

        ListTag historyTag = tag.getList("eventHistory", Tag.TAG_COMPOUND);
        for (int i = 0; i < historyTag.size(); i++) {
            log.eventHistory.add(ColonyEvent.fromNBT(historyTag.getCompound(i)));
        }

        return log;
    }
}
