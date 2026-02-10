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

/**
 * Configuration for NPC-to-NPC interaction system.
 * Loads from config/mindofthecolony/npc_interaction_config.json
 */
public class NpcInteractionConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(NpcInteractionConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR_NAME = "mindofthecolony";
    private static final String CONFIG_FILE = "npc_interaction_config.json";

    private static NpcInteractionConfigData config = null;

    /**
     * Load or create the NPC interaction config.
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
            LOGGER.info("NPC interaction config not found, generating defaults at: {}", configFile);
            config = getDefaultConfig();
            saveConfig(configFile);
            return;
        }

        try {
            String json = Files.readString(configFile);
            config = GSON.fromJson(json, NpcInteractionConfigData.class);
            if (config == null) {
                LOGGER.warn("NPC interaction config was null, using defaults");
                config = getDefaultConfig();
            }
            LOGGER.info("Loaded NPC interaction config from {}", configFile);
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to load NPC interaction config: {}", e.getMessage());
            config = getDefaultConfig();
        }
    }

    private static void saveConfig(Path path) {
        try {
            String json = GSON.toJson(config);
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to save NPC interaction config: {}", e.getMessage());
        }
    }

    private static NpcInteractionConfigData getDefaultConfig() {
        NpcInteractionConfigData data = new NpcInteractionConfigData();

        // General settings
        data.enabled = true;

        // Proximity detection
        data.proximity = new ProximityConfig();
        data.proximity.interactionRadius = 5.0;
        data.proximity.checkIntervalTicks = 100;  // ~5 seconds

        // Conversation settings
        data.conversation = new ConversationConfig();
        data.conversation.startChance = 0.1;  // 10% chance per check
        data.conversation.minCooldownTicks = 12000;  // ~10 minutes
        data.conversation.maxCooldownTicks = 24000;  // ~20 minutes
        data.conversation.minTurns = 2;
        data.conversation.maxTurns = 5;
        data.conversation.turnDelayTicks = 60;  // ~3 seconds
        data.conversation.responseTimeoutTicks = 200;  // ~10 seconds

        // Player visibility
        data.playerVisibility = new PlayerVisibilityConfig();
        data.playerVisibility.overhearRadius = 16.0;
        data.playerVisibility.chatFormat = "[%speaker%] (to %listener%): %message%";

        // Performance
        data.performance = new PerformanceConfig();
        data.performance.maxActiveConversationsPerColony = 3;
        data.performance.maxCitizenPairsToCheck = 50;

        // Relationship settings
        data.relationship = new RelationshipConfig();
        data.relationship.affinityGainOnSuccess = 0.1f;
        data.relationship.affinityGainOnInterrupt = 0.02f;
        data.relationship.affinityLossOnArgument = 0.1f;
        data.relationship.friendlyThreshold = 0.5f;
        data.relationship.acquaintanceThreshold = 0.0f;

        return data;
    }

    // --- Getters ---

    public static boolean isEnabled() {
        return config != null && config.enabled;
    }

    public static ProximityConfig getProximityConfig() {
        return config != null && config.proximity != null
            ? config.proximity
            : getDefaultConfig().proximity;
    }

    public static ConversationConfig getConversationConfig() {
        return config != null && config.conversation != null
            ? config.conversation
            : getDefaultConfig().conversation;
    }

    public static PlayerVisibilityConfig getPlayerVisibilityConfig() {
        return config != null && config.playerVisibility != null
            ? config.playerVisibility
            : getDefaultConfig().playerVisibility;
    }

    public static PerformanceConfig getPerformanceConfig() {
        return config != null && config.performance != null
            ? config.performance
            : getDefaultConfig().performance;
    }

    public static RelationshipConfig getRelationshipConfig() {
        return config != null && config.relationship != null
            ? config.relationship
            : getDefaultConfig().relationship;
    }

    // --- Config Data Classes ---

    public static class NpcInteractionConfigData {
        public boolean enabled;
        public ProximityConfig proximity;
        public ConversationConfig conversation;
        public PlayerVisibilityConfig playerVisibility;
        public PerformanceConfig performance;
        public RelationshipConfig relationship;
    }

    public static class ProximityConfig {
        public double interactionRadius;
        public int checkIntervalTicks;
    }

    public static class ConversationConfig {
        public double startChance;
        public int minCooldownTicks;
        public int maxCooldownTicks;
        public int minTurns;
        public int maxTurns;
        public int turnDelayTicks;
        public int responseTimeoutTicks;
    }

    public static class PlayerVisibilityConfig {
        public double overhearRadius;
        public String chatFormat;
    }

    public static class PerformanceConfig {
        public int maxActiveConversationsPerColony;
        public int maxCitizenPairsToCheck;
    }

    public static class RelationshipConfig {
        public float affinityGainOnSuccess;
        public float affinityGainOnInterrupt;
        public float affinityLossOnArgument;
        public float friendlyThreshold;
        public float acquaintanceThreshold;
    }
}
