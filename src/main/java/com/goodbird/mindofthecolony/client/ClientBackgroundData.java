package com.goodbird.mindofthecolony.client;

import java.util.List;

/**
 * Structured data for citizen background information cached on the client.
 */
public record ClientBackgroundData(
    String backstory,
    List<String> permanentTraits,
    List<String> temporaryTraits,
    List<String> activeModifiers
) {}
