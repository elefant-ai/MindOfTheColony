package com.goodbird.mindofthecolony.config;

import com.goodbird.mindofthecolony.background.BackgroundDefinitions;
import com.goodbird.mindofthecolony.background.BackgroundDefinitions.BackgroundEntry;
import com.goodbird.mindofthecolony.background.BackgroundDefinitions.PenaltyCategory;
import com.goodbird.mindofthecolony.background.BackgroundDefinitions.PenaltyEntry;
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

public class BackgroundConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundConfigLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String CONFIG_DIR_NAME = "mindofthecolony";
    private static final String ORIGINS_FILE = "origins.json";
    private static final String PERSONALITIES_FILE = "personalities.json";
    private static final String PENALTIES_FILE = "penalties.json";

    /**
     * Main entry point. Call once during server startup.
     * Creates config directory and files if missing, loads data into BackgroundDefinitions.
     */
    public static void loadOrCreate() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR_NAME);

        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory: {}", configDir, e);
            return;
        }

        List<BackgroundEntry> origins = loadOrigins(configDir);
        List<BackgroundEntry> traits = loadPersonalities(configDir);
        List<PenaltyEntry> penalties = loadPenalties(configDir);

        BackgroundDefinitions.load(origins, traits, penalties);
        LOGGER.info("Loaded {} origins, {} personality traits, {} penalties from config",
            origins.size(), traits.size(), penalties.size());
    }

    // --- Origins ---

    private static List<BackgroundEntry> loadOrigins(Path configDir) {
        Path file = configDir.resolve(ORIGINS_FILE);
        Type listType = new TypeToken<List<BackgroundConfigData.OriginData>>() {}.getType();

        if (!Files.exists(file)) {
            LOGGER.info("Origins config not found, generating defaults at: {}", file);
            writeDefaults(file, buildDefaultOriginData());
        }

        List<BackgroundConfigData.OriginData> raw = readJsonList(file, listType);
        if (raw == null || raw.isEmpty()) {
            LOGGER.warn("Origins config was empty or corrupt. Regenerating defaults.");
            raw = buildDefaultOriginData();
            writeDefaults(file, raw);
        }

        List<BackgroundEntry> result = new ArrayList<>();
        for (BackgroundConfigData.OriginData o : raw) {
            if (o.id != null && o.displayText != null) {
                result.add(new BackgroundEntry(o.id, o.displayText));
            } else {
                LOGGER.warn("Skipping origin entry with null id or displayText");
            }
        }

        if (result.isEmpty()) {
            LOGGER.error("No valid origins found after loading. Using defaults.");
            return BackgroundDefinitions.getDefaultOrigins();
        }
        return result;
    }

    // --- Personalities ---

    private static List<BackgroundEntry> loadPersonalities(Path configDir) {
        Path file = configDir.resolve(PERSONALITIES_FILE);
        Type listType = new TypeToken<List<BackgroundConfigData.TraitData>>() {}.getType();

        if (!Files.exists(file)) {
            LOGGER.info("Personalities config not found, generating defaults at: {}", file);
            writeDefaults(file, buildDefaultTraitData());
        }

        List<BackgroundConfigData.TraitData> raw = readJsonList(file, listType);
        if (raw == null || raw.isEmpty()) {
            LOGGER.warn("Personalities config was empty or corrupt. Regenerating defaults.");
            raw = buildDefaultTraitData();
            writeDefaults(file, raw);
        }

        List<BackgroundEntry> result = new ArrayList<>();
        for (BackgroundConfigData.TraitData t : raw) {
            if (t.id != null && t.displayText != null) {
                result.add(new BackgroundEntry(t.id, t.displayText));
            } else {
                LOGGER.warn("Skipping personality trait entry with null id or displayText");
            }
        }

        if (result.isEmpty()) {
            LOGGER.error("No valid personality traits found after loading. Using defaults.");
            return BackgroundDefinitions.getDefaultPersonalityTraits();
        }
        return result;
    }

    // --- Penalties ---

    private static List<PenaltyEntry> loadPenalties(Path configDir) {
        Path file = configDir.resolve(PENALTIES_FILE);
        Type listType = new TypeToken<List<BackgroundConfigData.PenaltyData>>() {}.getType();

        if (!Files.exists(file)) {
            LOGGER.info("Penalties config not found, generating defaults at: {}", file);
            writeDefaults(file, buildDefaultPenaltyData());
        }

        List<BackgroundConfigData.PenaltyData> raw = readJsonList(file, listType);
        if (raw == null) {
            LOGGER.warn("Penalties config was corrupt. Regenerating defaults.");
            raw = buildDefaultPenaltyData();
            writeDefaults(file, raw);
        }

        List<PenaltyEntry> result = new ArrayList<>();
        for (BackgroundConfigData.PenaltyData p : raw) {
            if (p.id != null && p.displayText != null && p.category != null) {
                try {
                    PenaltyCategory cat = PenaltyCategory.valueOf(p.category.toUpperCase());
                    result.add(new PenaltyEntry(p.id, cat, p.displayText));
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Skipping penalty '{}': unknown category '{}'. Valid: CRIMINAL, CONVERSATION, SOCIAL, FLAW",
                        p.id, p.category);
                }
            } else {
                LOGGER.warn("Skipping penalty entry with null id, category, or displayText");
            }
        }

        // Penalties CAN be empty -- user may intentionally want no penalties
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

    private static List<BackgroundConfigData.OriginData> buildDefaultOriginData() {
        List<BackgroundConfigData.OriginData> list = new ArrayList<>();
        for (BackgroundEntry entry : BackgroundDefinitions.getDefaultOrigins()) {
            BackgroundConfigData.OriginData o = new BackgroundConfigData.OriginData();
            o.id = entry.id();
            o.displayText = entry.displayText();
            list.add(o);
        }
        return list;
    }

    private static List<BackgroundConfigData.TraitData> buildDefaultTraitData() {
        List<BackgroundConfigData.TraitData> list = new ArrayList<>();
        for (BackgroundEntry entry : BackgroundDefinitions.getDefaultPersonalityTraits()) {
            BackgroundConfigData.TraitData t = new BackgroundConfigData.TraitData();
            t.id = entry.id();
            t.displayText = entry.displayText();
            list.add(t);
        }
        return list;
    }

    private static List<BackgroundConfigData.PenaltyData> buildDefaultPenaltyData() {
        List<BackgroundConfigData.PenaltyData> list = new ArrayList<>();
        for (PenaltyEntry entry : BackgroundDefinitions.getDefaultPenalties()) {
            BackgroundConfigData.PenaltyData p = new BackgroundConfigData.PenaltyData();
            p.id = entry.id();
            p.category = entry.category().name();
            p.displayText = entry.displayText();
            list.add(p);
        }
        return list;
    }
}
