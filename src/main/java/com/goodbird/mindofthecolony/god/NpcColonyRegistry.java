package com.goodbird.mindofthecolony.god;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persisted registry of colony IDs that are NPC-managed.
 * ColonyGod and event hooks only activate for colonies in this set.
 */
public class NpcColonyRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(NpcColonyRegistry.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_PATH = Paths.get("config", "mindofthecolony", "npc_colonies.json");

    private static final Set<Integer> npcColonyIds = ConcurrentHashMap.newKeySet();

    public static boolean isNpcColony(int colonyId) {
        return npcColonyIds.contains(colonyId);
    }

    public static void register(int colonyId) {
        if (npcColonyIds.add(colonyId)) {
            LOGGER.info("Registered colony {} as NPC-managed", colonyId);
            save();
        }
    }

    public static void unregister(int colonyId) {
        if (npcColonyIds.remove(colonyId)) {
            LOGGER.info("Unregistered colony {} from NPC management", colonyId);
            save();
        }
    }

    public static Set<Integer> getAll() {
        return Collections.unmodifiableSet(npcColonyIds);
    }

    public static void load() {
        try {
            if (!Files.exists(SAVE_PATH)) {
                LOGGER.debug("No NPC colony registry file found, starting empty");
                return;
            }

            String json = Files.readString(SAVE_PATH);
            Type type = new TypeToken<RegistryData>() {}.getType();
            RegistryData data = GSON.fromJson(json, type);

            npcColonyIds.clear();
            if (data != null && data.npcColonyIds != null) {
                npcColonyIds.addAll(data.npcColonyIds);
            }

            LOGGER.info("Loaded NPC colony registry: {} colonies", npcColonyIds.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load NPC colony registry", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(SAVE_PATH.getParent());

            RegistryData data = new RegistryData();
            data.npcColonyIds = new HashSet<>(npcColonyIds);

            Files.writeString(SAVE_PATH, GSON.toJson(data));
            LOGGER.debug("Saved NPC colony registry: {} colonies", npcColonyIds.size());
        } catch (IOException e) {
            LOGGER.error("Failed to save NPC colony registry", e);
        }
    }

    private static class RegistryData {
        Set<Integer> npcColonyIds;
    }
}
