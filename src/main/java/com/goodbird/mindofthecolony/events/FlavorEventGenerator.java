package com.goodbird.mindofthecolony.events;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;

import java.util.List;
import java.util.Random;

/**
 * Generates random flavor events using weighted random selection.
 */
public final class FlavorEventGenerator {

    private static final Random RANDOM = new Random();

    private FlavorEventGenerator() {}

    /**
     * Generate a random flavor event for the given colony.
     *
     * @param colony     the colony to generate the event for.
     * @param currentDay the current colony day.
     * @return a new ColonyEvent, or null if no flavor events are configured.
     */
    public static ColonyEvent generate(IColony colony, int currentDay) {
        List<EventDefinitions.EventTypeEntry> candidates = EventDefinitions.getFlavorEvents();
        if (candidates.isEmpty()) return null;

        EventDefinitions.EventTypeEntry selected = weightedSelect(candidates);
        if (selected == null) return null;

        String description = resolveTemplate(selected.descriptionTemplate(), colony);

        return new ColonyEvent(
            selected.id(),
            selected.category(),
            description,
            System.currentTimeMillis(),
            currentDay,
            selected.durationDays(),
            selected.happinessModifier()
        );
    }

    /**
     * Select a random entry based on probability weights.
     */
    private static EventDefinitions.EventTypeEntry weightedSelect(List<EventDefinitions.EventTypeEntry> entries) {
        double totalWeight = 0;
        for (EventDefinitions.EventTypeEntry entry : entries) {
            totalWeight += entry.weight();
        }

        if (totalWeight <= 0) return null;

        double roll = RANDOM.nextDouble() * totalWeight;
        double cumulative = 0;
        for (EventDefinitions.EventTypeEntry entry : entries) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry;
            }
        }

        // Fallback (shouldn't happen)
        return entries.get(entries.size() - 1);
    }

    /**
     * Resolve template variables in the event description.
     */
    private static String resolveTemplate(String template, IColony colony) {
        String result = template;
        result = result.replace("{{colonyName}}", colony.getName());

        if (result.contains("{{randomCitizenName}}")) {
            String citizenName = getRandomCitizenName(colony);
            result = result.replace("{{randomCitizenName}}", citizenName);
        }

        return result;
    }

    /**
     * Get a random citizen name from the colony, or a fallback.
     */
    private static String getRandomCitizenName(IColony colony) {
        List<ICitizenData> citizens = colony.getCitizenManager().getCitizens()
            .stream().toList();

        if (citizens.isEmpty()) {
            return "a colonist";
        }

        return citizens.get(RANDOM.nextInt(citizens.size())).getName();
    }
}
