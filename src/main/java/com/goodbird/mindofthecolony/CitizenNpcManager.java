package com.goodbird.mindofthecolony;

import com.goodbird.mindofthecolony.bridge.CitizenNpcBridge;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.ICitizenData;
import game.player2.npc.Player2NpcLib;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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

    // Game session ID - unique per server instance
    private String gameId;

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

        // Get any saved conversation history NBT
        CompoundTag historyNBT = null;
        if (citizenData instanceof IExtendedCitizenData extData) {
            historyNBT = extData.getLoadedConversationHistoryNBT();
        }

        // Create bridge and spawn NPC
        CitizenNpcBridge bridge = new CitizenNpcBridge(citizenData, gameId);
        bridges.put(citizenData.getId(), bridge);

        // Spawn the NPC asynchronously
        bridge.spawn().thenAccept(npcId -> {
            if (npcId != null) {
                npcToCitizen.put(npcId, citizenData.getId());
                LOGGER.info("NPC spawned for citizen: {} (npcId: {})", citizenData.getName(), npcId);
            }
        }).exceptionally(ex -> {
            LOGGER.error("Failed to spawn NPC for citizen: {}", citizenData.getName(), ex);
            return null;
        });

        LOGGER.info("AI Bridge created for citizen: {}", citizenData.getName());
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
     * Broadcasts a player message to all nearby citizens.
     */
    public void broadcastPlayerMessage(Player player, String message) {
        bridges.values().forEach(bridge -> {
            if (bridge.isReady() && bridge.isEntityNear(player, PLAYER_INPUT_CHAT_RADIUS)) {
                bridge.sendPlayerMessage(player.getName().getString(), message);
            }
        });
    }

    /**
     * Broadcasts a citizen's speech to nearby players and other citizens.
     */
    public void broadcastCitizenMessage(CitizenNpcBridge sender, String message) {
        ICitizenData citizenData = sender.getCitizenData();
        if (citizenData.getEntity().isEmpty()) return;

        var senderEntity = citizenData.getEntity().get();

        // Send to nearby players
        senderEntity.level().getEntitiesOfClass(ServerPlayer.class,
            senderEntity.getBoundingBox().inflate(LISTEN_CHAT_RADIUS)
        ).forEach(player -> {
            player.sendSystemMessage(Component.literal("<" + citizenData.getName() + "> " + message));
        });

        // Notify other nearby citizens
        bridges.values().forEach(bridge -> {
            if (bridge != sender && bridge.isReady()) {
                var otherEntity = bridge.getCitizenData().getEntity();
                if (otherEntity.isPresent() &&
                    otherEntity.get().distanceToSqr(senderEntity) < LISTEN_CHAT_RADIUS * LISTEN_CHAT_RADIUS) {
                    bridge.addHeardMessage(citizenData.getName(), message);
                }
            }
        });
    }

    /**
     * Called every server tick to update bridges.
     */
    public void onServerTick() {
        bridges.values().forEach(CitizenNpcBridge::onTick);
    }

    /**
     * Clears all AI bridges and shuts down connections.
     */
    public void clearAllAIs() {
        bridges.forEach((id, bridge) -> bridge.shutdown());
        bridges.clear();
        npcToCitizen.clear();

        // Shutdown the java-npc library
        Player2NpcLib.shutdown();

        LOGGER.info("All AI bridges cleared");
    }

    public String getGameId() {
        return gameId;
    }
}
