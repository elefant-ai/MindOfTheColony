package com.goodbird.mindofthecolony.client;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for citizen background data received from the server.
 */
public class ClientBackgroundCache {

    private static final Map<Integer, ClientBackgroundData> cache = new ConcurrentHashMap<>();

    /**
     * Store structured background data.
     */
    public static void putData(int citizenId, ClientBackgroundData data) {
        cache.put(citizenId, data);
    }

    /**
     * Store structured background data from individual components.
     */
    public static void putData(int citizenId, String backstory, List<String> permanentTraits,
                               List<String> temporaryTraits, List<String> activeModifiers) {
        cache.put(citizenId, new ClientBackgroundData(backstory, permanentTraits, temporaryTraits, activeModifiers));
    }

    /**
     * Get structured background data.
     */
    @Nullable
    public static ClientBackgroundData getData(int citizenId) {
        return cache.get(citizenId);
    }

    public static void clear() {
        cache.clear();
    }
}
