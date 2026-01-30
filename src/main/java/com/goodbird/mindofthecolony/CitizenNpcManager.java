package com.goodbird.mindofthecolony;

import com.goodbird.mindofthecolony.background.BackgroundGenerationService;
import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.bridge.CitizenNpcBridge;
import com.goodbird.mindofthecolony.event.ColonyEventManager;
import com.goodbird.mindofthecolony.event.evaluator.DiseaseOutbreakEvaluator;
import com.goodbird.mindofthecolony.event.evaluator.WeatherEventEvaluator;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import game.player2.npc.Player2NpcLib;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages AI NPC instances for all loaded citizens.
 * Maps citizen IDs to NpcHandle instances from the java-npc library.
 */
public class CitizenNpcManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CitizenNpcManager.class);

    public static final int PLAYER_INPUT_CHAT_RADIUS = 16;
    public static final int LISTEN_CHAT_RADIUS = 16;

    private static final CitizenNpcManager INSTANCE = new CitizenNpcManager();

    // Maps citizenId -> CitizenNpcBridge
    private final Map<Integer, CitizenNpcBridge> bridges = new ConcurrentHashMap<>();

    // Maps npcId (UUID) -> citizenId for reverse lookup
    private final Map<UUID, Integer> npcToCitizen = new ConcurrentHashMap<>();

    // Maps citizenId -> player currently chatting with that citizen
    private final Map<Integer, ServerPlayer> activeConversations = new ConcurrentHashMap<>();

    // Game session ID - unique per server instance
    private String gameId;

    // Reference to the current server level for event ticking
    private ServerLevel currentLevel;

    private CitizenNpcManager() {
    }

    public static CitizenNpcManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the manager with a game session ID.
     * Should be called when the server starts.
     */
    public void initialize() {
        // Generate a unique game ID for this session
        this.gameId = "minecolonies_" + System.currentTimeMillis();
        LOGGER.info("CitizenNpcManager initialized with gameId: {}", gameId);
    }

    /**
     * Called when a citizen is loaded into the world.
     */
    public void onCitizenLoad(ICitizenData citizenData) {
        if (citizenData == null || bridges.containsKey(citizenData.getId())) {
            return;
        }

        if (gameId == null) {
            initialize();
        }

        // Check if citizen needs a background generated
        boolean needsBackgroundGeneration = false;
        if (citizenData instanceof IExtendedCitizenData extData) {
            CitizenBackground bg = extData.getCitizenBackground();
            needsBackgroundGeneration = (bg == null || !bg.isInitialized());
        }

        // Create bridge now (will spawn NPC after background is ready)
        CitizenNpcBridge bridge = new CitizenNpcBridge(citizenData, gameId);
        bridges.put(citizenData.getId(), bridge);

        if (needsBackgroundGeneration) {
            // Generate background using AI, then spawn NPC
            LOGGER.info("Requesting AI background generation for citizen: {}", citizenData.getName());

            BackgroundGenerationService.getInstance().generateBackground(citizenData, gameId)
                .thenAccept(background -> {
                    if (citizenData instanceof IExtendedCitizenData extData) {
                        extData.setCitizenBackground(background);
                        LOGGER.info("AI generated background for citizen {}: backstory='{}...', traits={}",
                            citizenData.getName(),
                            background.getBackstory() != null
                                ? background.getBackstory().substring(0, Math.min(50, background.getBackstory().length()))
                                : "none",
                            background.getTraits());
                    }

                    // Now spawn the NPC with the generated background
                    spawnNpcForCitizen(bridge, citizenData);
                })
                .exceptionally(ex -> {
                    LOGGER.error("Failed to generate background for citizen: {}", citizenData.getName(), ex);
                    // Spawn anyway with whatever background exists
                    spawnNpcForCitizen(bridge, citizenData);
                    return null;
                });
        } else {
            // Background already exists, spawn NPC immediately
            spawnNpcForCitizen(bridge, citizenData);
        }

        LOGGER.info("AI Bridge created for citizen: {}", citizenData.getName());
    }

    /**
     * Spawns the NPC for a citizen after their background is ready.
     */
    private void spawnNpcForCitizen(CitizenNpcBridge bridge, ICitizenData citizenData) {
        bridge.spawn().thenAccept(npcId -> {
            if (npcId != null) {
                npcToCitizen.put(npcId, citizenData.getId());
                LOGGER.info("NPC spawned for citizen: {} (npcId: {})", citizenData.getName(), npcId);
            }
        }).exceptionally(ex -> {
            LOGGER.error("Failed to spawn NPC for citizen: {}", citizenData.getName(), ex);
            return null;
        });
    }

    /**
     * Called when a citizen is unloaded from the world.
     */
    public void onCitizenUnload(int citizenId) {
        CitizenNpcBridge bridge = bridges.remove(citizenId);
        if (bridge != null) {
            UUID npcId = bridge.getNpcId();
            if (npcId != null) {
                npcToCitizen.remove(npcId);
            }
            bridge.shutdown();
            LOGGER.info("AI Bridge removed for citizen ID: {}", citizenId);
        }
    }

    /**
     * Gets the bridge for a specific citizen.
     */
    @Nullable
    public CitizenNpcBridge getBridge(int citizenId) {
        return bridges.get(citizenId);
    }

    /**
     * Gets the bridge for a specific NPC ID.
     */
    @Nullable
    public CitizenNpcBridge getBridgeByNpcId(UUID npcId) {
        Integer citizenId = npcToCitizen.get(npcId);
        return citizenId != null ? bridges.get(citizenId) : null;
    }

    /**
     * Starts a conversation between a player and a citizen.
     * Only one player can chat with a citizen at a time.
     */
    public void startConversation(int citizenId, ServerPlayer player) {
        ServerPlayer existing = activeConversations.get(citizenId);
        if (existing != null && existing != player) {
            LOGGER.debug("Player {} taking over conversation from {} with citizen {}",
                player.getName().getString(), existing.getName().getString(), citizenId);
        }
        activeConversations.put(citizenId, player);
        LOGGER.debug("Started conversation: {} <-> citizen {}",
            player.getName().getString(), citizenId);
    }

    /**
     * Ends a conversation with a citizen.
     */
    public void endConversation(int citizenId) {
        ServerPlayer removed = activeConversations.remove(citizenId);
        if (removed != null) {
            LOGGER.debug("Ended conversation: {} <-> citizen {}",
                removed.getName().getString(), citizenId);
        }
    }

    /**
     * Gets the player currently chatting with a citizen.
     * @return The player, or null if no one is chatting with this citizen.
     */
    @Nullable
    public ServerPlayer getChattingPlayer(int citizenId) {
        return activeConversations.get(citizenId);
    }

    /**
     * Gets the citizen ID for a given NPC UUID.
     */
    @Nullable
    public Integer getCitizenIdByNpcId(UUID npcId) {
        return npcToCitizen.get(npcId);
    }

    /**
     * Called every server tick to update bridges and event managers.
     */
    public void onServerTick() {
        bridges.values().forEach(CitizenNpcBridge::onTick);

        // Tick event managers for all colonies
        if (currentLevel != null) {
            long currentTick = currentLevel.getGameTime();
            for (IColony colony : IColonyManager.getInstance().getColonies(currentLevel)) {
                ColonyEventManager eventManager = ColonyEventManager.getInstance(colony.getID());
                eventManager.onTick(colony, currentLevel, currentTick);
            }
        }
    }

    /**
     * Sets the current server level reference for event ticking.
     */
    public void setCurrentLevel(ServerLevel level) {
        this.currentLevel = level;
    }

    /**
     * Initializes event managers for all colonies with evaluators.
     */
    public void initializeEventManagers(ServerLevel level) {
        this.currentLevel = level;

        for (IColony colony : IColonyManager.getInstance().getColonies(level)) {
            ColonyEventManager eventManager = ColonyEventManager.getInstance(colony.getID());

            // Register evaluators
            eventManager.registerEvaluator(new DiseaseOutbreakEvaluator());
            eventManager.registerEvaluator(new WeatherEventEvaluator());

            LOGGER.info("Initialized event manager for colony {} with evaluators", colony.getID());
        }
    }

    /**
     * Clears all AI bridges and shuts down connections.
     */
    public void clearAllAIs() {
        bridges.forEach((id, bridge) -> bridge.shutdown());
        bridges.clear();
        npcToCitizen.clear();

        // Shutdown background generation service
        BackgroundGenerationService.getInstance().shutdown();

        // Shutdown the java-npc library
        Player2NpcLib.shutdown();

        LOGGER.info("All AI bridges cleared");
    }

    /**
     * Respawns all NPCs with fresh system prompts.
     * Used when settings like language change at runtime.
     */
    public void respawnAllNpcs() {
        LOGGER.info("Respawning all NPCs with updated settings...");
        npcToCitizen.clear();

        bridges.forEach((citizenId, bridge) -> {
            bridge.respawn().thenAccept(npcId -> {
                if (npcId != null) {
                    npcToCitizen.put(npcId, citizenId);
                    LOGGER.debug("NPC respawned for citizen ID: {} (npcId: {})", citizenId, npcId);
                }
            }).exceptionally(ex -> {
                LOGGER.error("Failed to respawn NPC for citizen ID: {}", citizenId, ex);
                return null;
            });
        });

        LOGGER.info("Initiated respawn for {} NPCs", bridges.size());
    }

    public String getGameId() {
        return gameId;
    }

    /**
     * Scans all colonies and citizens to find any without backgrounds.
     * Generates AI backgrounds for any that are missing.
     * Should be called on server start after colonies are loaded.
     */
    public void checkAndGenerateMissingBackgrounds(ServerLevel level) {
        if (gameId == null) {
            initialize();
        }

        LOGGER.info("Checking for citizens with missing backgrounds...");

        int missingCount = 0;
        int totalCount = 0;

        for (IColony colony : IColonyManager.getInstance().getColonies(level)) {
            // Check recruited citizens
            for (ICitizenData citizenData : colony.getCitizenManager().getCitizens()) {
                totalCount++;

                if (citizenData instanceof IExtendedCitizenData extData) {
                    CitizenBackground bg = extData.getCitizenBackground();
                    if (bg == null || !bg.isInitialized()) {
                        missingCount++;
                        LOGGER.info("Citizen {} (ID: {}) missing background, generating...",
                            citizenData.getName(), citizenData.getId());

                        // Generate background asynchronously
                        generateBackgroundForCitizen(citizenData);
                    }
                }
            }

            // Check visitors (non-recruited citizens in tavern)
            for (ICivilianData civilianData : colony.getVisitorManager().getCivilianDataMap().values()) {
                if (civilianData instanceof ICitizenData citizenData) {
                    totalCount++;

                    if (citizenData instanceof IExtendedCitizenData extData) {
                        CitizenBackground bg = extData.getCitizenBackground();
                        if (bg == null || !bg.isInitialized()) {
                            missingCount++;
                            LOGGER.info("Visitor {} (ID: {}) missing background, generating...",
                                citizenData.getName(), citizenData.getId());

                            // Generate background asynchronously
                            generateBackgroundForCitizen(citizenData);
                        }
                    }
                }
            }
        }

        if (missingCount > 0) {
            LOGGER.info("Found {} citizens without backgrounds out of {} total. Generation started.",
                missingCount, totalCount);
        } else {
            LOGGER.info("All {} citizens have backgrounds.", totalCount);
        }
    }

    /**
     * Generates a background for a single citizen (without creating a bridge).
     * Used for citizens that already exist but are missing backgrounds.
     */
    private void generateBackgroundForCitizen(ICitizenData citizenData) {
        BackgroundGenerationService.getInstance().generateBackground(citizenData, gameId)
            .thenAccept(background -> {
                if (citizenData instanceof IExtendedCitizenData extData) {
                    extData.setCitizenBackground(background);
                    LOGGER.info("Generated missing background for citizen {}: backstory='{}...', traits={}",
                        citizenData.getName(),
                        background.getBackstory() != null
                            ? background.getBackstory().substring(0, Math.min(50, background.getBackstory().length()))
                            : "none",
                        background.getTraits());

                    // If there's an existing bridge, respawn the NPC with new background
                    CitizenNpcBridge bridge = bridges.get(citizenData.getId());
                    if (bridge != null) {
                        bridge.respawn();
                    }
                }
            })
            .exceptionally(ex -> {
                LOGGER.error("Failed to generate missing background for citizen {}: {}",
                    citizenData.getName(), ex.getMessage());
                return null;
            });
    }
}
