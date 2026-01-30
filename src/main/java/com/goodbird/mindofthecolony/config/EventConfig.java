package com.goodbird.mindofthecolony.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the colony event system.
 * Loads from config/mindofthecolony/event_config.json
 */
public class EventConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR_NAME = "mindofthecolony";
    private static final String CONFIG_FILE = "event_config.json";

    private static EventConfigData config = null;

    /**
     * Load or create the event config.
     */
    public static void loadOrCreate() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR_NAME);
        Path configFile = configDir.resolve(CONFIG_FILE);

        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory: {}", configDir, e);
            config = getDefaultConfig();
            return;
        }

        if (!Files.exists(configFile)) {
            LOGGER.info("Event config not found, generating defaults at: {}", configFile);
            config = getDefaultConfig();
            saveConfig(configFile);
            return;
        }

        try {
            String json = Files.readString(configFile);
            config = GSON.fromJson(json, EventConfigData.class);
            if (config == null) {
                LOGGER.warn("Event config was null, using defaults");
                config = getDefaultConfig();
            }
            LOGGER.info("Loaded event config from {}", configFile);
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to load event config: {}", e.getMessage());
            config = getDefaultConfig();
        }
    }

    private static void saveConfig(Path path) {
        try {
            String json = GSON.toJson(config);
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to save event config: {}", e.getMessage());
        }
    }

    private static EventConfigData getDefaultConfig() {
        EventConfigData data = new EventConfigData();

        // General settings
        data.checkIntervalMinTicks = 6000;    // ~5 minutes
        data.checkIntervalMaxTicks = 24000;   // ~20 minutes
        data.maxEventHistory = 50;

        // Disease outbreak settings
        data.diseaseOutbreak = new DiseaseOutbreakConfig();
        data.diseaseOutbreak.enabled = true;
        data.diseaseOutbreak.baseChance = 0.05;
        data.diseaseOutbreak.rainMultiplier = 1.5;
        data.diseaseOutbreak.lowFoodMultiplier = 2.0;
        data.diseaseOutbreak.lowHappinessMultiplier = 1.3;
        data.diseaseOutbreak.lowFoodThreshold = 5.0;
        data.diseaseOutbreak.lowHappinessThreshold = 4.0;

        // Thunderstorm settings
        data.thunderstorm = new ThunderstormConfig();
        data.thunderstorm.enabled = true;
        data.thunderstorm.happinessPenalty = -1.0;
        data.thunderstorm.affectedTraits = List.of("fearful", "superstitious", "nervous", "cowardly");
        data.thunderstorm.temporaryTrait = "frightened";
        data.thunderstorm.traitDurationTicks = 24000;

        // Prolonged rain settings
        data.prolongedRain = new ProlongedRainConfig();
        data.prolongedRain.enabled = true;
        data.prolongedRain.rainTicksThreshold = 12000;
        data.prolongedRain.temporaryTrait = "damp";
        data.prolongedRain.traitDurationTicks = 6000;
        data.prolongedRain.diseaseRateMultiplier = 1.5;

        return data;
    }

    // --- Getters ---

    public static int getCheckIntervalMinTicks() {
        return config != null ? config.checkIntervalMinTicks : 6000;
    }

    public static int getCheckIntervalMaxTicks() {
        return config != null ? config.checkIntervalMaxTicks : 24000;
    }

    public static int getMaxEventHistory() {
        return config != null ? config.maxEventHistory : 50;
    }

    public static DiseaseOutbreakConfig getDiseaseOutbreakConfig() {
        return config != null && config.diseaseOutbreak != null
            ? config.diseaseOutbreak
            : getDefaultConfig().diseaseOutbreak;
    }

    public static ThunderstormConfig getThunderstormConfig() {
        return config != null && config.thunderstorm != null
            ? config.thunderstorm
            : getDefaultConfig().thunderstorm;
    }

    public static ProlongedRainConfig getProlongedRainConfig() {
        return config != null && config.prolongedRain != null
            ? config.prolongedRain
            : getDefaultConfig().prolongedRain;
    }

    // --- Config Data Classes ---

    public static class EventConfigData {
        public int checkIntervalMinTicks;
        public int checkIntervalMaxTicks;
        public int maxEventHistory;
        public DiseaseOutbreakConfig diseaseOutbreak;
        public ThunderstormConfig thunderstorm;
        public ProlongedRainConfig prolongedRain;
    }

    public static class DiseaseOutbreakConfig {
        public boolean enabled;
        public double baseChance;
        public double rainMultiplier;
        public double lowFoodMultiplier;
        public double lowHappinessMultiplier;
        public double lowFoodThreshold;
        public double lowHappinessThreshold;
    }

    public static class ThunderstormConfig {
        public boolean enabled;
        public double happinessPenalty;
        public List<String> affectedTraits;
        public String temporaryTrait;
        public int traitDurationTicks;
    }

    public static class ProlongedRainConfig {
        public boolean enabled;
        public int rainTicksThreshold;
        public String temporaryTrait;
        public int traitDurationTicks;
        public double diseaseRateMultiplier;
    }
}
