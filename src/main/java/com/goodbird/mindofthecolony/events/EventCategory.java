package com.goodbird.mindofthecolony.events;

/**
 * Categories for colony events, each associated with a chat message priority color.
 */
public enum EventCategory {
    COLONY,       // Colony-wide infrastructure/management (gray)
    SOCIAL,       // Interpersonal events (gray)
    CRISIS,       // Negative events like raids, deaths (red)
    CELEBRATION,  // Positive events like festivals, births (gold)
    RUMOR         // Atmospheric flavor events (gray)
}
