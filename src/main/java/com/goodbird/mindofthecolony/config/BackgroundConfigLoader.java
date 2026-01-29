package com.goodbird.mindofthecolony.config;

import com.goodbird.mindofthecolony.background.*;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackgroundConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundConfigLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String CONFIG_DIR_NAME = "mindofthecolony";
    private static final String TRAITS_FILE = "traits.json";

    /**
     * Main entry point. Call once during server startup.
     * Creates config directory and files if missing, loads data into TraitRegistry.
     */
    public static void loadOrCreate() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR_NAME);

        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory: {}", configDir, e);
            return;
        }

        List<TraitDefinition> traits = loadTraits(configDir);

        TraitRegistry.load(traits);
        LOGGER.info("Loaded {} traits from config", traits.size());
    }

    // --- Traits ---

    private static List<TraitDefinition> loadTraits(Path configDir) {
        Path file = configDir.resolve(TRAITS_FILE);
        Type listType = new TypeToken<List<BackgroundConfigData.TraitData>>() {}.getType();

        if (!Files.exists(file)) {
            LOGGER.info("Traits config not found, generating defaults at: {}", file);
            writeDefaults(file, buildDefaultTraitData());
        }

        List<BackgroundConfigData.TraitData> raw = readJsonList(file, listType);
        if (raw == null || raw.isEmpty()) {
            LOGGER.warn("Traits config was empty or corrupt. Regenerating defaults.");
            raw = buildDefaultTraitData();
            writeDefaults(file, raw);
        }

        List<TraitDefinition> result = new ArrayList<>();
        for (BackgroundConfigData.TraitData t : raw) {
            if (t.id != null && t.displayText != null) {
                Map<String, Double> mods = t.modifiers != null ? t.modifiers : new HashMap<>();
                result.add(new TraitDefinition(t.id, t.displayText, t.weight, mods));
            } else {
                LOGGER.warn("Skipping trait entry with null id or displayText");
            }
        }

        if (result.isEmpty()) {
            LOGGER.error("No valid traits found after loading. Using defaults.");
            return getDefaultTraits();
        }
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

    private static List<BackgroundConfigData.TraitData> buildDefaultTraitData() {
        List<BackgroundConfigData.TraitData> list = new ArrayList<>();
        for (TraitDefinition trait : getDefaultTraits()) {
            BackgroundConfigData.TraitData t = new BackgroundConfigData.TraitData();
            t.id = trait.id();
            t.displayText = trait.displayText();
            t.weight = trait.weight();
            t.modifiers = new HashMap<>(trait.modifiers());
            list.add(t);
        }
        return list;
    }

    // --- Default definitions ---

    public static List<TraitDefinition> getDefaultTraits() {
        return List.of(
            new TraitDefinition("cheerful",
                "You tend to see the bright side of things and laugh easily.",
                1.0, Map.of("happinessBase", 0.5, "happinessDecayRate", 0.8)),
            new TraitDefinition("grumpy",
                "You are perpetually irritable and quick to complain.",
                1.0, Map.of("happinessBase", -0.3)),
            new TraitDefinition("cautious",
                "You are careful and suspicious, always expecting the worst.",
                1.0, Map.of("diseaseRate", 0.9, "contactDiseaseRate", 0.8)),
            new TraitDefinition("boastful",
                "You love to talk about your achievements, real or imagined.",
                1.0, Map.of("happinessBase", 0.2)),
            new TraitDefinition("quiet",
                "You are a person of few words, preferring to listen and observe.",
                1.0, Map.of()),
            new TraitDefinition("superstitious",
                "You believe in omens, curses, and old folk remedies.",
                1.0, Map.of("happinessBase", -0.1)),
            new TraitDefinition("kind_hearted",
                "You genuinely care about others and go out of your way to help.",
                1.0, Map.of("happinessBase", 0.3)),
            new TraitDefinition("sarcastic",
                "You have a sharp tongue and a dry wit that not everyone appreciates.",
                1.0, Map.of()),
            new TraitDefinition("ambitious",
                "You always want more -- more responsibility, more recognition, more success.",
                1.0, Map.of("happinessBase", 0.2, "workSpeed", 1.05)),
            new TraitDefinition("nostalgic",
                "You frequently reminisce about the past and the life you left behind.",
                1.0, Map.of("happinessBase", -0.2)),
            new TraitDefinition("robust",
                "You have an unusually strong constitution and rarely fall ill.",
                0.5, Map.of("diseaseRate", 0.5, "happinessBase", 0.2)),
            new TraitDefinition("sickly",
                "You have a weak constitution and fall ill more easily than others.",
                1.0, Map.of("diseaseRate", 1.5, "happinessBase", -0.3)),
            new TraitDefinition("clumsy",
                "You are remarkably clumsy and prone to accidents.",
                1.0, Map.of("workSpeed", 0.9)),
            new TraitDefinition("hardy",
                "You can endure harsh conditions that would break others.",
                0.5, Map.of("diseaseRate", 0.8, "foodConsumption", 0.9)),
            new TraitDefinition("hardworking",
                "You take pride in your work and always give your best effort.",
                0.7, Map.of("workSpeed", 1.1, "foodConsumption", 1.15, "happinessBase", 0.2)),
            new TraitDefinition("lazy",
                "You have a tendency toward laziness and must push yourself to work hard.",
                1.0, Map.of("workSpeed", 0.85, "foodConsumption", 0.9)),
            new TraitDefinition("perfectionist",
                "You obsess over details and won't rest until everything is just right.",
                0.8, Map.of("workSpeed", 0.95, "happinessBase", -0.1)),
            new TraitDefinition("outcast",
                "People from your previous home shunned you. You struggle to trust others.",
                1.0, Map.of("happinessBase", -0.4)),
            new TraitDefinition("charismatic",
                "People are naturally drawn to you and trust you easily.",
                0.5, Map.of("happinessBase", 0.3)),
            new TraitDefinition("short_temper",
                "You have a volatile temper that gets you into trouble.",
                1.0, Map.of("happinessDecayRate", 1.3)),
            new TraitDefinition("criminal_past",
                "You have a criminal history that you try to keep hidden.",
                1.0, Map.of("happinessBase", -0.2)),
            new TraitDefinition("haunted",
                "You sometimes hear voices of people from your past who are no longer alive.",
                1.0, Map.of("happinessBase", -0.3)),
            new TraitDefinition("cowardly",
                "You are easily frightened and tend to flee from danger.",
                1.0, Map.of()),
            new TraitDefinition("glutton",
                "You have an insatiable appetite and are always hungry.",
                0.8, Map.of("foodConsumption", 1.3, "diseaseRate", 0.9)),
            new TraitDefinition("ascetic",
                "You are accustomed to getting by on very little food.",
                0.5, Map.of("foodConsumption", 0.75, "diseaseRate", 1.1))
        );
    }
}
