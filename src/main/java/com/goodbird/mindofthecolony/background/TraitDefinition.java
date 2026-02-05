package com.goodbird.mindofthecolony.background;

import java.util.Map;

/**
 * Represents a single trait definition loaded from config.
 *
 * @param id            Unique identifier for the trait
 * @param displayText   Description shown in AI prompts and UI
 * @param weight        Selection weight (higher = more likely to be chosen)
 * @param modifiers     Map of modifier name to value (e.g., "diseaseRate" -> 1.5)
 * @param temporaryOnly If true, this trait can only be applied by events (not assigned at birth)
 */
public record TraitDefinition(
    String id,
    String displayText,
    double weight,
    Map<String, Double> modifiers,
    boolean temporaryOnly
) {
    /**
     * Constructor for backwards compatibility - defaults temporaryOnly to false.
     */
    public TraitDefinition(String id, String displayText, double weight, Map<String, Double> modifiers) {
        this(id, displayText, weight, modifiers, false);
    }
}
