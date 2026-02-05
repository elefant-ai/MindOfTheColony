package com.goodbird.mindofthecolony.client;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for colony event data received from the server.
 */
public class ClientEventCache {

    private static final Map<Integer, ColonyEventData> cache = new ConcurrentHashMap<>();

    /**
     * Structured data for colony events.
     */
    public record ColonyEventData(
        String weather,
        List<String> recentEvents
    ) {}

    /**
     * Store colony event data.
     */
    public static void put(int colonyId, ColonyEventData data) {
        cache.put(colonyId, data);
    }

    /**
     * Store colony event data from individual components.
     */
    public static void put(int colonyId, String weather, List<String> recentEvents) {
        cache.put(colonyId, new ColonyEventData(weather, recentEvents));
    }

    /**
     * Get colony event data.
     */
    @Nullable
    public static ColonyEventData get(int colonyId) {
        return cache.get(colonyId);
    }

    public static void clear() {
        cache.clear();
    }
}
