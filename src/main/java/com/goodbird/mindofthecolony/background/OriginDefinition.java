package com.goodbird.mindofthecolony.background;

import java.util.Map;

/**
 * Represents an origin (backstory) definition loaded from config.
 *
 * @param id          Unique identifier for the origin
 * @param displayText Description shown in AI prompts and UI
 * @param modifiers   Optional map of modifier name to value
 */
public record OriginDefinition(
    String id,
    String displayText,
    Map<String, Double> modifiers
) {}
