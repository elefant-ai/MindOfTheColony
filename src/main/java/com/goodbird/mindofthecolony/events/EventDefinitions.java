package com.goodbird.mindofthecolony.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry of event type definitions.
 * Initialized with defaults, then overwritten by config on server start.
 */
public final class EventDefinitions {

    private EventDefinitions() {}

    public record EventTypeEntry(
        String id,
        EventCategory category,
        String descriptionTemplate,
        int durationDays,
        double happinessModifier,
        double weight
    ) {}

    // Mutable runtime data, initialized with defaults
    private static List<EventTypeEntry> detectedEventTemplates = getDefaultDetectedTemplates();
    private static List<EventTypeEntry> flavorEvents = getDefaultFlavorEvents();

    // --- Public read-only accessors ---

    public static List<EventTypeEntry> getDetectedEventTemplates() {
        return Collections.unmodifiableList(detectedEventTemplates);
    }

    public static List<EventTypeEntry> getFlavorEvents() {
        return Collections.unmodifiableList(flavorEvents);
    }

    /**
     * Lookup a detected event template by ID.
     */
    public static EventTypeEntry getDetectedTemplate(String id) {
        for (EventTypeEntry entry : detectedEventTemplates) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Called by EventConfigLoader to replace the lists with config-loaded data.
     */
    public static void load(List<EventTypeEntry> newDetected, List<EventTypeEntry> newFlavor) {
        detectedEventTemplates = new ArrayList<>(newDetected);
        flavorEvents = new ArrayList<>(newFlavor);
    }

    // --- Default detected event templates ---

    public static List<EventTypeEntry> getDefaultDetectedTemplates() {
        return List.of(
            new EventTypeEntry("citizen_died", EventCategory.CRISIS,
                "{{citizenName}} has perished. The colony mourns their loss.",
                5, -1.5, 1.0),
            new EventTypeEntry("citizen_born", EventCategory.SOCIAL,
                "A child named {{citizenName}} has been born into the colony!",
                3, 1.0, 1.0),
            new EventTypeEntry("citizen_hired", EventCategory.SOCIAL,
                "A new worker named {{citizenName}} has joined the colony.",
                2, 0.5, 1.0),
            new EventTypeEntry("building_built", EventCategory.COLONY,
                "Construction of the {{buildingName}} has been completed!",
                2, 1.0, 1.0),
            new EventTypeEntry("building_upgraded", EventCategory.COLONY,
                "The {{buildingName}} has been upgraded to level {{level}}!",
                2, 0.5, 1.0),
            new EventTypeEntry("raid_started", EventCategory.CRISIS,
                "The colony is under attack! Raiders have been spotted!",
                1, -2.0, 1.0),
            new EventTypeEntry("raid_ended", EventCategory.CELEBRATION,
                "The raiders have been repelled! The colony is safe once more.",
                2, 1.5, 1.0),
            new EventTypeEntry("citizen_sick", EventCategory.CRISIS,
                "{{citizenName}} has fallen ill.",
                2, -0.5, 1.0),
            new EventTypeEntry("citizen_job_changed", EventCategory.COLONY,
                "{{citizenName}} has been assigned as a {{jobName}}.",
                1, 0.0, 1.0)
        );
    }

    // --- Default flavor events ---

    public static List<EventTypeEntry> getDefaultFlavorEvents() {
        return List.of(
            new EventTypeEntry("traveling_merchant", EventCategory.RUMOR,
                "Word has spread that a traveling merchant was spotted near the colony borders.",
                3, 0.5, 10.0),
            new EventTypeEntry("beautiful_sunset", EventCategory.CELEBRATION,
                "The colonists gathered to watch a breathtaking sunset over the colony.",
                1, 0.5, 8.0),
            new EventTypeEntry("strange_noises", EventCategory.RUMOR,
                "Strange noises were heard from the mines during the night. Some citizens are uneasy.",
                2, -0.5, 6.0),
            new EventTypeEntry("bumper_harvest", EventCategory.CELEBRATION,
                "The farms have produced an exceptional harvest this season!",
                3, 1.0, 7.0),
            new EventTypeEntry("old_ruins_found", EventCategory.RUMOR,
                "A citizen claims to have found ancient ruins while exploring near the colony.",
                4, 0.0, 4.0),
            new EventTypeEntry("colony_festival", EventCategory.CELEBRATION,
                "The colonists are celebrating an impromptu festival in the town square!",
                2, 1.5, 5.0),
            new EventTypeEntry("supply_shortage_rumor", EventCategory.RUMOR,
                "Rumors are circulating that food supplies might be running low soon.",
                3, -0.5, 6.0),
            new EventTypeEntry("wild_animals", EventCategory.RUMOR,
                "Wild animals have been spotted unusually close to the colony walls.",
                2, -0.3, 7.0),
            new EventTypeEntry("friendly_visitor", EventCategory.SOCIAL,
                "A friendly traveler passed through and shared stories of distant lands.",
                2, 0.5, 8.0),
            new EventTypeEntry("mysterious_omen", EventCategory.RUMOR,
                "An elder citizen claims to have seen an omen in the stars last night.",
                3, 0.0, 3.0),
            new EventTypeEntry("construction_milestone", EventCategory.CELEBRATION,
                "The colony has reached a new milestone in development. Citizens are proud of their home.",
                2, 1.0, 4.0),
            new EventTypeEntry("dispute_between_workers", EventCategory.SOCIAL,
                "Two workers had a heated argument over tool usage. Tensions are slightly raised.",
                2, -0.3, 6.0)
        );
    }
}
