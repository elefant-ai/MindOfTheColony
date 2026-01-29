package com.goodbird.mindofthecolony.client;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for citizen background data received from the server.
 */
public class ClientBackgroundCache {

    private static final Map<Integer, String> cache = new ConcurrentHashMap<>();

    public static void put(int citizenId, String backgroundInfo) {
        cache.put(citizenId, backgroundInfo);
    }

    @Nullable
    public static String get(int citizenId) {
        return cache.get(citizenId);
    }

    public static void clear() {
        cache.clear();
    }
}
