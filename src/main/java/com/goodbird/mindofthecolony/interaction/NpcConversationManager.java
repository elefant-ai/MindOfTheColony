package com.goodbird.mindofthecolony.interaction;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.goodbird.mindofthecolony.bridge.CitizenNpcBridge;
import com.goodbird.mindofthecolony.config.NpcInteractionConfig;
import com.goodbird.mindofthecolony.event.ColonyEventManager;
import com.goodbird.mindofthecolony.network.NpcChatBroadcastMessage;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.jobs.IJob;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Manages NPC-to-NPC conversations for a colony.
 * Singleton per colony, similar to ColonyEventManager.
 */
public class NpcConversationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NpcConversationManager.class);

    // Manager instances per colony
    private static final Map<Integer, NpcConversationManager> managers = new ConcurrentHashMap<>();

    private final int colonyId;
    private final List<NpcConversation> activeConversations = new ArrayList<>();
    private final Map<Integer, Long> citizenCooldowns = new HashMap<>();  // citizenId -> nextAvailableTick
    private final Map<Long, CitizenRelationship> relationships = new HashMap<>();  // key -> relationship
    private final Random random = new Random();

    // NPC -> Player greeting cooldowns
    // Key: (citizenId << 32) | (playerUUID.hashCode() & 0xFFFFFFFFL)
    private final Map<Long, Long> playerGreetingCooldowns = new HashMap<>();
    // Global cooldown per player (any NPC greeting them)
    private final Map<UUID, Long> playerGlobalGreetingCooldowns = new HashMap<>();

    private NpcConversationManager(int colonyId) {
        this.colonyId = colonyId;
    }

    /**
     * Get or create manager for a colony.
     */
    public static NpcConversationManager getInstance(int colonyId) {
        return managers.computeIfAbsent(colonyId, NpcConversationManager::new);
    }

    /**
     * Get manager if it exists, null otherwise.
     */
    @Nullable
    public static NpcConversationManager getIfExists(int colonyId) {
        return managers.get(colonyId);
    }

    /**
     * Remove manager for a colony.
     */
    public static void removeInstance(int colonyId) {
        managers.remove(colonyId);
    }

    /**
     * Clear all managers (on server shutdown).
     */
    public static void clearAll() {
        managers.clear();
    }

    /**
     * Check if a citizen can start a new conversation.
     */
    public boolean canStartConversation(int citizenId, long currentTick) {
        if (!NpcInteractionConfig.isEnabled()) {
            return false;
        }

        // Check if in cooldown
        Long cooldownUntil = citizenCooldowns.get(citizenId);
        if (cooldownUntil != null && currentTick < cooldownUntil) {
            return false;
        }

        // Check if already in conversation
        if (isInConversation(citizenId)) {
            return false;
        }

        // Check if player is chatting with this citizen
        if (CitizenNpcManager.getInstance().getChattingPlayer(citizenId) != null) {
            return false;
        }

        // Check max active conversations
        var config = NpcInteractionConfig.getPerformanceConfig();
        if (activeConversations.size() >= config.maxActiveConversationsPerColony) {
            return false;
        }

        return true;
    }

    /**
     * Check if a citizen is in an active conversation.
     */
    public boolean isInConversation(int citizenId) {
        return activeConversations.stream()
            .anyMatch(conv -> conv.involvesCitizen(citizenId) && !conv.isFinished());
    }

    /**
     * Get the conversation a citizen is in, if any.
     */
    @Nullable
    public NpcConversation getConversation(int citizenId) {
        return activeConversations.stream()
            .filter(conv -> conv.involvesCitizen(citizenId) && !conv.isFinished())
            .findFirst()
            .orElse(null);
    }

    /**
     * Start a new conversation between two citizens.
     */
    public NpcConversation startConversation(int initiatorId, int responderId, long currentTick) {
        NpcConversation conversation = new NpcConversation(initiatorId, responderId, colonyId, currentTick);
        activeConversations.add(conversation);

        LOGGER.info("Started NPC conversation: {} <-> {} in colony {}",
            initiatorId, responderId, colonyId);

        return conversation;
    }

    /**
     * End a conversation and update relationships.
     */
    public void endConversation(NpcConversation conversation, boolean completedSuccessfully) {
        if (conversation.isFinished()) {
            return;
        }

        if (completedSuccessfully) {
            conversation.complete();
        } else {
            conversation.abort("ended");
        }

        // Update relationship
        CitizenRelationship relationship = getOrCreateRelationship(
            conversation.getInitiatorId(), conversation.getResponderId());
        relationship.recordConversation(conversation.getStartTick(), completedSuccessfully);

        // Apply cooldowns
        applyCooldown(conversation.getInitiatorId());
        applyCooldown(conversation.getResponderId());

        LOGGER.info("Ended NPC conversation: {} <-> {} (success: {})",
            conversation.getInitiatorId(), conversation.getResponderId(), completedSuccessfully);
    }

    /**
     * Apply cooldown to a citizen after conversation.
     */
    private void applyCooldown(int citizenId) {
        var config = NpcInteractionConfig.getConversationConfig();
        int cooldownTicks = config.minCooldownTicks +
            random.nextInt(config.maxCooldownTicks - config.minCooldownTicks + 1);
        long currentTick = getCurrentTick();
        citizenCooldowns.put(citizenId, currentTick + cooldownTicks);
    }

    private long getCurrentTick() {
        // Get from colony world if available
        IColony colony = getColony();
        if (colony != null && colony.getWorld() != null) {
            return colony.getWorld().getGameTime();
        }
        return 0;
    }

    @Nullable
    private IColony getColony() {
        ServerLevel level = CitizenNpcManager.getInstance().getCurrentLevel();
        if (level == null) return null;
        return IColonyManager.getInstance().getColonyByWorld(colonyId, level);
    }

    /**
     * Get or create a relationship between two citizens.
     */
    public CitizenRelationship getOrCreateRelationship(int citizenA, int citizenB) {
        long key = CitizenRelationship.makeKey(citizenA, citizenB);
        return relationships.computeIfAbsent(key, k -> new CitizenRelationship(citizenA, citizenB));
    }

    /**
     * Get relationship if it exists.
     */
    @Nullable
    public CitizenRelationship getRelationship(int citizenA, int citizenB) {
        long key = CitizenRelationship.makeKey(citizenA, citizenB);
        return relationships.get(key);
    }

    // --- NPC -> Player Greeting Methods ---

    /**
     * Check if an NPC can greet a specific player (not on cooldown).
     */
    public boolean canGreetPlayer(int citizenId, UUID playerUuid, long currentTick) {
        var greetingConfig = NpcInteractionConfig.getPlayerGreetingConfig();
        if (!greetingConfig.enabled) {
            return false;
        }

        // Check if citizen is busy (in conversation or chatting)
        if (isInConversation(citizenId)) {
            return false;
        }
        if (CitizenNpcManager.getInstance().getChattingPlayer(citizenId) != null) {
            return false;
        }

        // Check global cooldown for this player (any NPC)
        Long globalCooldown = playerGlobalGreetingCooldowns.get(playerUuid);
        if (globalCooldown != null && currentTick < globalCooldown) {
            return false;
        }

        // Check specific citizen -> player cooldown
        long key = makePlayerGreetingKey(citizenId, playerUuid);
        Long specificCooldown = playerGreetingCooldowns.get(key);
        if (specificCooldown != null && currentTick < specificCooldown) {
            return false;
        }

        return true;
    }

    /**
     * Record that an NPC greeted a player, setting cooldowns.
     */
    public void recordPlayerGreeting(int citizenId, UUID playerUuid, long currentTick) {
        var greetingConfig = NpcInteractionConfig.getPlayerGreetingConfig();

        // Set specific citizen -> player cooldown
        long key = makePlayerGreetingKey(citizenId, playerUuid);
        playerGreetingCooldowns.put(key, currentTick + greetingConfig.cooldownTicks);

        // Set global cooldown for this player
        playerGlobalGreetingCooldowns.put(playerUuid, currentTick + greetingConfig.globalCooldownTicks);

        LOGGER.debug("Recorded player greeting: citizen {} -> player {}, next specific={}, next global={}",
            citizenId, playerUuid, currentTick + greetingConfig.cooldownTicks,
            currentTick + greetingConfig.globalCooldownTicks);
    }

    private long makePlayerGreetingKey(int citizenId, UUID playerUuid) {
        return ((long) citizenId << 32) | (playerUuid.hashCode() & 0xFFFFFFFFL);
    }

    /**
     * Called each tick to process conversations.
     */
    public void onTick(IColony colony, long currentTick) {
        if (!NpcInteractionConfig.isEnabled()) {
            return;
        }

        // Process active conversations
        Iterator<NpcConversation> iterator = activeConversations.iterator();
        while (iterator.hasNext()) {
            NpcConversation conversation = iterator.next();

            // Remove finished conversations
            if (conversation.isFinished()) {
                iterator.remove();
                continue;
            }

            // Check for timeout
            if (conversation.hasTimedOut(currentTick)) {
                LOGGER.info("Conversation timed out: {}", conversation);
                endConversation(conversation, false);
                iterator.remove();
                continue;
            }

            // Check if citizens are still nearby
            if (!areCitizensNearby(conversation, colony, currentTick)) {
                LOGGER.info("Citizens moved apart: {}", conversation);
                endConversation(conversation, false);
                iterator.remove();
                continue;
            }

            // Check if player interrupted
            if (wasInterruptedByPlayer(conversation)) {
                LOGGER.info("Conversation interrupted by player: {}", conversation);
                endConversation(conversation, false);
                iterator.remove();
                continue;
            }

            // Process turns
            if (conversation.isReadyForNextTurn(currentTick)) {
                processTurn(conversation, colony, currentTick);
            }
        }
    }

    /**
     * Check if the two citizens in a conversation are still nearby.
     */
    private boolean areCitizensNearby(NpcConversation conversation, IColony colony, long currentTick) {
        // Grace period: Skip distance check for first 30 seconds (600 ticks) to allow conversation to complete
        // This prevents conversations ending due to minor position changes during the chat
        long ticksSinceStart = currentTick - conversation.getStartTick();
        if (ticksSinceStart < 600) {
            return true;
        }

        ICitizenData citizen1 = colony.getCitizenManager().getCivilian(conversation.getInitiatorId());
        ICitizenData citizen2 = colony.getCitizenManager().getCivilian(conversation.getResponderId());

        if (citizen1 == null || citizen2 == null) {
            LOGGER.debug("Citizen data not found: citizen1={}, citizen2={}", citizen1 != null, citizen2 != null);
            return false;
        }

        var entity1 = citizen1.getEntity();
        var entity2 = citizen2.getEntity();

        if (entity1.isEmpty() || entity2.isEmpty()) {
            LOGGER.debug("Entity not present: entity1={}, entity2={}",
                entity1.isPresent(), entity2.isPresent());
            return false;
        }

        double radius = NpcInteractionConfig.getProximityConfig().interactionRadius * 1.5;
        double distSq = entity1.get().distanceToSqr(entity2.get());
        boolean nearby = distSq <= radius * radius;

        if (!nearby) {
            LOGGER.debug("Citizens too far apart: distance={}, max radius={}",
                Math.sqrt(distSq), radius);
        }

        return nearby;
    }

    /**
     * Check if a player has started chatting with either citizen.
     */
    private boolean wasInterruptedByPlayer(NpcConversation conversation) {
        return CitizenNpcManager.getInstance().getChattingPlayer(conversation.getInitiatorId()) != null ||
               CitizenNpcManager.getInstance().getChattingPlayer(conversation.getResponderId()) != null;
    }

    /**
     * Process a conversation turn.
     */
    private void processTurn(NpcConversation conversation, IColony colony, long currentTick) {
        int speakerId = conversation.getCurrentSpeakerId();
        int listenerId = conversation.getListenerId();

        LOGGER.info("Processing turn {} for conversation {}: speaker={}, listener={}",
            conversation.getCurrentTurn(), conversation, speakerId, listenerId);

        CitizenNpcBridge speakerBridge = CitizenNpcManager.getInstance().getBridge(speakerId);
        CitizenNpcBridge listenerBridge = CitizenNpcManager.getInstance().getBridge(listenerId);

        if (speakerBridge == null || !speakerBridge.isReady()) {
            LOGGER.warn("Speaker bridge not ready (bridge={}, ready={}), aborting conversation",
                speakerBridge != null, speakerBridge != null && speakerBridge.isReady());
            endConversation(conversation, false);
            return;
        }

        ICitizenData speakerData = speakerBridge.getCitizenData();
        ICitizenData listenerData = listenerBridge != null ? listenerBridge.getCitizenData() : null;

        if (listenerData == null) {
            LOGGER.warn("Listener data not available, aborting conversation");
            endConversation(conversation, false);
            return;
        }

        // Generate prompt based on conversation state
        String prompt = generatePrompt(conversation, speakerData, listenerData, colony, currentTick);

        // Mark waiting for response
        conversation.markWaitingForResponse(currentTick);

        // Send to NPC
        speakerBridge.sendNpcToNpcMessage(listenerData.getName(), prompt, colonyId);

        LOGGER.info("Sent turn {} to speaker {}: {}",
            conversation.getCurrentTurn(), speakerData.getName(), prompt);
    }

    /**
     * Generate prompt for the current turn.
     */
    private String generatePrompt(NpcConversation conversation, ICitizenData speaker,
                                  ICitizenData listener, IColony colony, long currentTick) {
        String listenerName = listener.getName();
        String listenerJob = getJobName(listener);

        // Get relationship info
        CitizenRelationship rel = getRelationship(speaker.getId(), listener.getId());
        String relLevel = rel != null ? rel.getRelationshipLevel() : "stranger";
        int convCount = rel != null ? rel.getConversationCount() : 0;

        // Get recent events for context
        ColonyEventManager eventManager = ColonyEventManager.getIfExists(colonyId);
        String recentEvents = "";
        if (eventManager != null) {
            recentEvents = eventManager.getEventsAsContext(3, currentTick);
        }

        // Get weather
        String weather = "clear";
        if (colony.getWorld() != null) {
            if (colony.getWorld().isThundering()) {
                weather = "thunderstorm";
            } else if (colony.getWorld().isRaining()) {
                weather = "raining";
            }
        }

        StringBuilder prompt = new StringBuilder();

        switch (conversation.getState()) {
            case STARTING -> {
                prompt.append("[You notice ").append(listenerName)
                    .append(", a ").append(listenerJob).append(", nearby. ");

                if (convCount > 0) {
                    prompt.append("You've spoken ").append(convCount)
                        .append(" time").append(convCount > 1 ? "s" : "")
                        .append(" before and consider them a ").append(relLevel).append(". ");
                } else {
                    prompt.append("You haven't really talked to them before. ");
                }

                prompt.append("Start a brief, natural conversation. Keep it to 1-2 sentences.]");
            }
            case ACTIVE -> {
                String lastMsg = conversation.getLastMessage();
                prompt.append("[").append(listenerName).append(" said: \"")
                    .append(lastMsg != null ? lastMsg : "...").append("\"\n");
                prompt.append("Respond naturally to continue the conversation. ");
                prompt.append("Keep it brief (1-2 sentences). ");

                if (!recentEvents.isEmpty()) {
                    prompt.append("Recent events you might mention: ").append(recentEvents).append(" ");
                }
                prompt.append("Weather is ").append(weather).append(".]");
            }
            case ENDING -> {
                String lastMsg = conversation.getLastMessage();
                prompt.append("[").append(listenerName).append(" said: \"")
                    .append(lastMsg != null ? lastMsg : "...").append("\"\n");
                prompt.append("It's time to wrap up this conversation. ");
                prompt.append("Say a brief goodbye or indicate you need to get back to work.]");
            }
            default -> prompt.append("[Continue the conversation naturally.]");
        }

        return prompt.toString();
    }

    private String getJobName(ICitizenData citizen) {
        IJob<?> job = citizen.getJob();
        return job != null ? job.getJobRegistryEntry().getKey().getPath() : "colonist";
    }

    /**
     * Handle response from an NPC that's in a conversation.
     */
    public void handleResponse(NpcConversation conversation, int speakerId, String message) {
        LOGGER.info("Received NPC-NPC response from citizen {}: {}", speakerId, message);

        if (conversation.isFinished()) {
            LOGGER.warn("Conversation already finished, ignoring response");
            return;
        }

        if (conversation.getCurrentSpeakerId() != speakerId) {
            LOGGER.warn("Received response from wrong speaker: expected {}, got {}",
                conversation.getCurrentSpeakerId(), speakerId);
            return;
        }

        long currentTick = getCurrentTick();

        // Capture listener ID BEFORE handleResponse switches the speaker
        int listenerId = conversation.getListenerId();

        // Handle the response (this switches currentSpeakerId)
        conversation.handleResponse(message, currentTick);

        // Get speaker info for broadcast
        CitizenNpcBridge speakerBridge = CitizenNpcManager.getInstance().getBridge(speakerId);
        CitizenNpcBridge listenerBridge = CitizenNpcManager.getInstance().getBridge(listenerId);

        String speakerName = speakerBridge != null ? speakerBridge.getCitizenData().getName() : "Unknown";
        String listenerName = listenerBridge != null ? listenerBridge.getCitizenData().getName() : "Unknown";

        // Broadcast to nearby players
        broadcastToNearbyPlayers(conversation, speakerId, speakerName,
            listenerId, listenerName, message,
            conversation.getCurrentTurn() == 1,
            conversation.shouldEnd());

        // Check if conversation should end
        if (conversation.shouldEnd()) {
            endConversation(conversation, true);
        }
    }

    /**
     * Broadcast NPC-NPC chat to nearby players.
     */
    private void broadcastToNearbyPlayers(NpcConversation conversation, int speakerId, String speakerName,
                                          int listenerId, String listenerName, String message,
                                          boolean isStart, boolean isEnd) {
        IColony colony = getColony();
        if (colony == null || colony.getWorld() == null) {
            return;
        }

        // Get speaker position
        ICitizenData speakerData = colony.getCitizenManager().getCivilian(speakerId);
        if (speakerData == null || speakerData.getEntity().isEmpty()) {
            return;
        }

        BlockPos speakerPos = speakerData.getEntity().get().blockPosition();
        double radiusSq = Math.pow(NpcInteractionConfig.getPlayerVisibilityConfig().overhearRadius, 2);

        // Find and notify nearby players
        ServerLevel level = (ServerLevel) colony.getWorld();
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(speakerPos) <= radiusSq) {
                PacketDistributor.sendToPlayer(player, new NpcChatBroadcastMessage(
                    colonyId,
                    speakerId,
                    speakerName,
                    listenerId,
                    listenerName,
                    message,
                    isStart,
                    isEnd
                ));
            }
        }
    }

    // --- NBT Persistence ---

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("colonyId", colonyId);

        // Save cooldowns
        CompoundTag cooldownsTag = new CompoundTag();
        citizenCooldowns.forEach((id, tick) -> cooldownsTag.putLong(String.valueOf(id), tick));
        tag.put("cooldowns", cooldownsTag);

        // Save relationships
        ListTag relList = new ListTag();
        for (CitizenRelationship rel : relationships.values()) {
            relList.add(rel.toNBT());
        }
        tag.put("relationships", relList);

        return tag;
    }

    public void loadFromNBT(CompoundTag tag) {
        // Load cooldowns
        citizenCooldowns.clear();
        if (tag.contains("cooldowns", Tag.TAG_COMPOUND)) {
            CompoundTag cooldownsTag = tag.getCompound("cooldowns");
            for (String key : cooldownsTag.getAllKeys()) {
                try {
                    int citizenId = Integer.parseInt(key);
                    long tick = cooldownsTag.getLong(key);
                    citizenCooldowns.put(citizenId, tick);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Load relationships
        relationships.clear();
        if (tag.contains("relationships", Tag.TAG_LIST)) {
            ListTag relList = tag.getList("relationships", Tag.TAG_COMPOUND);
            for (int i = 0; i < relList.size(); i++) {
                try {
                    CitizenRelationship rel = CitizenRelationship.fromNBT(relList.getCompound(i));
                    relationships.put(rel.getKey(), rel);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load relationship from NBT: {}", e.getMessage());
                }
            }
        }

        LOGGER.debug("Loaded {} relationships for colony {}", relationships.size(), colonyId);
    }

    public static NpcConversationManager fromNBT(CompoundTag tag) {
        int colonyId = tag.getInt("colonyId");
        NpcConversationManager manager = getInstance(colonyId);
        manager.loadFromNBT(tag);
        return manager;
    }
}
