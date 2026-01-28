package com.goodbird.mindofthecolony.config;

import com.goodbird.mindofthecolony.events.EventCategory;
import com.goodbird.mindofthecolony.events.EventDefinitions;
import com.goodbird.mindofthecolony.events.EventDefinitions.EventTypeEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EventConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventConfigLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String CONFIG_DIR_NAME = "mindofthecolony";
    private static final String DETECTED_EVENTS_FILE = "detected_events.json";
    private static final String FLAVOR_EVENTS_FILE = "flavor_events.json";

    /**
     * Main entry point. Call once during server startup.
     * Creates config files if missing, loads data into EventDefinitions.
     */
    public static void loadOrCreate() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR_NAME);

        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory: {}", configDir, e);
            return;
        }

        List<EventTypeEntry> detected = loadDetectedEvents(configDir);
        List<EventTypeEntry> flavor = loadFlavorEvents(configDir);

        EventDefinitions.load(detected, flavor);
        LOGGER.info("Loaded {} detected event templates, {} flavor events from config",
            detected.size(), flavor.size());
    }

    // --- Detected Events ---

    private static List<EventTypeEntry> loadDetectedEvents(Path configDir) {
        Path file = configDir.resolve(DETECTED_EVENTS_FILE);
        Type listType = new TypeToken<List<EventConfigData.DetectedEventData>>() {}.getType();

        if (!Files.exists(file)) {
            LOGGER.info("Detected events config not found, generating defaults at: {}", file);
            writeDefaults(file, buildDefaultDetectedData());
        }

        List<EventConfigData.DetectedEventData> raw = readJsonList(file, listType);
        if (raw == null || raw.isEmpty()) {
            LOGGER.warn("Detected events config was empty or corrupt. Regenerating defaults.");
            raw = buildDefaultDetectedData();
            writeDefaults(file, raw);
        }

        List<EventTypeEntry> result = new ArrayList<>();
        for (EventConfigData.DetectedEventData d : raw) {
            if (d.id != null && d.category != null && d.descriptionTemplate != null) {
                try {
                    EventCategory cat = EventCategory.valueOf(d.category.toUpperCase());
                    result.add(new EventTypeEntry(d.id, cat, d.descriptionTemplate,
                        d.durationDays, d.happinessModifier, 1.0));
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Skipping detected event '{}': unknown category '{}'. Valid: COLONY, SOCIAL, CRISIS, CELEBRATION, RUMOR",
                        d.id, d.category);
                }
            } else {
                LOGGER.warn("Skipping detected event entry with null id, category, or descriptionTemplate");
            }
        }

        if (result.isEmpty()) {
            LOGGER.error("No valid detected events found after loading. Using defaults.");
            return EventDefinitions.getDefaultDetectedTemplates();
        }
        return result;
    }

    // --- Flavor Events ---

    private static List<EventTypeEntry> loadFlavorEvents(Path configDir) {
        Path file = configDir.resolve(FLAVOR_EVENTS_FILE);
        Type listType = new TypeToken<List<EventConfigData.FlavorEventData>>() {}.getType();

        if (!Files.exists(file)) {
            LOGGER.info("Flavor events config not found, generating defaults at: {}", file);
            writeDefaults(file, buildDefaultFlavorData());
        }

        List<EventConfigData.FlavorEventData> raw = readJsonList(file, listType);
        if (raw == null) {
            LOGGER.warn("Flavor events config was corrupt. Regenerating defaults.");
            raw = buildDefaultFlavorData();
            writeDefaults(file, raw);
        }

        List<EventTypeEntry> result = new ArrayList<>();
        for (EventConfigData.FlavorEventData f : raw) {
            if (f.id != null && f.category != null && f.descriptionTemplate != null) {
                try {
                    EventCategory cat = EventCategory.valueOf(f.category.toUpperCase());
                    double weight = f.weight > 0 ? f.weight : 1.0;
                    result.add(new EventTypeEntry(f.id, cat, f.descriptionTemplate,
                        f.durationDays, f.happinessModifier, weight));
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Skipping flavor event '{}': unknown category '{}'", f.id, f.category);
                }
            } else {
                LOGGER.warn("Skipping flavor event entry with null id, category, or descriptionTemplate");
            }
        }

        // Flavor events CAN be empty -- user may intentionally want none
        return result;
    }

    // --- JSON I/O ---

    private static <T> T readJsonList(Path path, Type type) {
        try {
            String json = Files.readString(path);
            return GSON.fromJson(json, type);
        } catch (IOException e) {
            LOGGER.error("Failed to read config file: {}", path, e);
            return null;
        } catch (JsonSyntaxException e) {
            LOGGER.error("Invalid JSON syntax in {}: {}", path, e.getMessage());
            return null;
        }
    }

    private static void writeDefaults(Path path, Object data) {
        try {
            String json = GSON.toJson(data);
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to write config file: {}", path, e);
        }
    }

    // --- Default data builders ---

    private static List<EventConfigData.DetectedEventData> buildDefaultDetectedData() {
        List<EventConfigData.DetectedEventData> list = new ArrayList<>();
        for (EventTypeEntry entry : EventDefinitions.getDefaultDetectedTemplates()) {
            EventConfigData.DetectedEventData d = new EventConfigData.DetectedEventData();
            d.id = entry.id();
            d.category = entry.category().name();
            d.descriptionTemplate = entry.descriptionTemplate();
            d.durationDays = entry.durationDays();
            d.happinessModifier = entry.happinessModifier();
            list.add(d);
        }
        return list;
    }

    private static List<EventConfigData.FlavorEventData> buildDefaultFlavorData() {
        List<EventConfigData.FlavorEventData> list = new ArrayList<>();
        for (EventTypeEntry entry : EventDefinitions.getDefaultFlavorEvents()) {
            EventConfigData.FlavorEventData f = new EventConfigData.FlavorEventData();
            f.id = entry.id();
            f.category = entry.category().name();
            f.descriptionTemplate = entry.descriptionTemplate();
            f.durationDays = entry.durationDays();
            f.happinessModifier = entry.happinessModifier();
            f.weight = entry.weight();
            list.add(f);
        }
        return list;
    }
}
