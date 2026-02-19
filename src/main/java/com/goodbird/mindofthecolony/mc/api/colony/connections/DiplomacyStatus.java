package com.goodbird.mindofthecolony.mc.api.colony.connections;

/**
 * Diplomacy Status between two colonies.
 */
public enum DiplomacyStatus
{
    ALLIES,
    NEUTRAL,
    HOSTILE;

    /**
     * Get translation key for the diplomacy status.
     * @return the string key.
     */
    public String translationKey()
    {
        return "com.goodbird.mindofthecolony.mc.core.colony.diplomacy.status." + name().toLowerCase();
    }
}
