package com.goodbird.mindofthecolony.bridge;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.config.ModSettings;
import com.goodbird.mindofthecolony.effect.TemporaryTrait;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.goodbird.mindofthecolony.status.AgentStatus;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.colony.permissions.IPermissions;
import com.minecolonies.api.colony.permissions.Rank;
import game.player2.npc.Player2NpcLib;
import net.minecraft.server.level.ServerPlayer;
import game.player2.npc.api.NpcHandle;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Bridge between a MineColonies citizen and an AI NPC from the java-npc library.
 * Each citizen gets one bridge that manages their NpcHandle lifecycle.
 */
public class CitizenNpcBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(CitizenNpcBridge.class);

    private final ICitizenData citizenData;
    private final String gameId;
    private final String shortName;
    @Nullable
    private final UUID existingNpcId;

    private NpcHandle npcHandle;
    private CompletableFuture<UUID> pendingSpawn;
    private boolean ready = false;

    // Track how many times this citizen has chatted with each player
    private final Map<UUID, Integer> playerChatCounts = new HashMap<>();

    // Heartbeat tracking
    private static long lastHeartbeatTime = System.nanoTime();
    private static final long HEARTBEAT_INTERVAL_NS = 60_000_000_000L; // 60 seconds

    public CitizenNpcBridge(ICitizenData citizenData, String gameId, @Nullable UUID existingNpcId) {
        this.citizenData = citizenData;
        this.gameId = gameId;
        this.existingNpcId = existingNpcId;
        // Create a unique short name for the NPC
        this.shortName = "citizen_" + citizenData.getColony().getID() + "_" + citizenData.getId();
    }

    /**
     * Spawns the NPC asynchronously.
     * @return CompletableFuture with the NPC's UUID
     */
    public CompletableFuture<UUID> spawn() {
        String systemPrompt = generateSystemPrompt();
        String description = generateCharacterDescription();

        var builder = Player2NpcLib.builder(shortName)
            .name(citizenData.getName())
            .description(description)
            .systemPrompt(systemPrompt);

        // If we have an existing NPC ID, resume from it to restore memories
        if (existingNpcId != null) {
            builder.resumeFrom(existingNpcId);
            LOGGER.info("Resuming NPC from existing ID {} for citizen: {}", existingNpcId, citizenData.getName());
        }

        pendingSpawn = builder.spawn(gameId)
            .thenApply(handle -> {
                this.npcHandle = handle;
                this.ready = true;
                LOGGER.info("NPC handle ready for citizen: {} (npcId: {})", citizenData.getName(), handle.getId());
                return handle.getId();
            });

        return pendingSpawn;
    }

    /**
     * Generates the system prompt that defines the citizen's personality.
     */
    private String generateSystemPrompt() {
        IJob<?> job = citizenData.getJob();
        String jobName = (job != null) ? job.getJobRegistryEntry().getKey().getPath() : "unemployed citizen";
        String gender = citizenData.isFemale() ? "Female" : "Male";
        String age = citizenData.isChild() ? "Child" : "Adult";
        double happiness = citizenData.getCitizenHappinessHandler().getHappiness(citizenData.getColony(), citizenData);

        String backgroundSection = "";
        if (citizenData instanceof IExtendedCitizenData extData) {
            CitizenBackground bg = extData.getCitizenBackground();
            if (bg != null && bg.isInitialized()) {
                // Include temporary traits in the system prompt
                List<TemporaryTrait> tempTraits = extData.getTemporaryTraits();
                long currentTick = citizenData.getColony().getWorld() != null
                    ? citizenData.getColony().getWorld().getGameTime() : 0;
                backgroundSection = bg.toSystemPromptSection(tempTraits, currentTick);
            }
        }

        return """
            You are %s, a %s living in the colony of %s in the world of Minecraft.

            PERSONALITY:
            - Gender: %s
            - Age: %s
            - Current mood: %s
            - Happiness level: %.1f/10

            %s

            GUIDELINES:
            - Speak naturally in first person as this character
            - Reference your job, colony life, and current situation when relevant
            - React to your happiness and mood appropriately
            - Your background and any dark history should subtly influence your speech and attitudes
            - Keep responses conversational and concise (under 200 characters)
            - You can express opinions about colony management and other citizens
            - If the player asks for another colonist by name, politely redirect them
            - When your work order shows Level 0 -> 1, that means you are building something new (not upgrading). Say "I'm building a new X" rather than "I'm upgrading X".
            - Stay in character at all times

            CONTEXT FORMAT:
            Messages may include JSON context about your current status. Use this to inform your responses.
            """.formatted(
                citizenData.getName(),
                jobName,
                citizenData.getColony().getName(),
                gender,
                age,
                getMoodDescription(happiness),
                happiness,
                backgroundSection
            );
    }

    /**
     * Generates the character description for the NPC.
     */
    private String generateCharacterDescription() {
        IJob<?> job = citizenData.getJob();
        String jobName = (job != null) ? job.getJobRegistryEntry().getKey().getPath() : "unemployed";

        String traitDesc = "";
        if (citizenData instanceof IExtendedCitizenData extData) {
            CitizenBackground bg = extData.getCitizenBackground();
            if (bg != null && !bg.getTraits().isEmpty()) {
                String firstTrait = bg.getTraits().get(0).replace("_", " ");
                traitDesc = " Known for being " + firstTrait + ".";
            }
        }

        return String.format("A %s %s named %s from the colony of %s. Currently feeling %s.%s",
            citizenData.isFemale() ? "female" : "male",
            jobName,
            citizenData.getName(),
            citizenData.getColony().getName(),
            getMoodDescription(citizenData.getCitizenHappinessHandler().getHappiness(citizenData.getColony(), citizenData)),
            traitDesc
        );
    }

    private String getMoodDescription(double happiness) {
        if (happiness > 8) return "very happy";
        if (happiness > 6) return "content";
        if (happiness > 4) return "a bit down";
        return "unhappy";
    }

    /**
     * Gets the current game state context as a string.
     */
    public String getGameStateContext() {
        String language = ModSettings.NPC_LANGUAGE.get();
        String languageInstruction = "Respond in " + language + " only. ";
        return languageInstruction + AgentStatus.fromCitizenAndColony(citizenData).toString();
    }

    /**
     * Gets the number of times this citizen has chatted with a player.
     */
    public int getPlayerChatCount(UUID playerUuid) {
        return playerChatCounts.getOrDefault(playerUuid, 0);
    }

    /**
     * Increments the chat count for a player.
     */
    public void incrementPlayerChatCount(UUID playerUuid) {
        playerChatCounts.merge(playerUuid, 1, Integer::sum);
    }

    /**
     * Gets rich context about a player for NPC awareness.
     * Includes colony relationship, what they're holding, armor, time of day, and chat history.
     */
    public String getPlayerContext(ServerPlayer player) {
        StringBuilder context = new StringBuilder();
        IColony colony = citizenData.getColony();
        IPermissions perms = colony.getPermissions();

        // Colony relationship
        Rank rank = perms.getRank(player);
        String relationship = getRelationshipString(rank, perms, player);
        context.append("[").append(player.getName().getString())
               .append(" is ").append(relationship).append("]\n");

        // What player is holding
        net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            String itemName = mainHand.getHoverName().getString();
            context.append("[Player is holding: ").append(itemName).append("]\n");
        }

        // Sneaking
        if (player.isShiftKeyDown()) {
            context.append("[Player is sneaking]\n");
        }

        // Armor level
        int armor = player.getArmorValue();
        if (armor >= 15) {
            context.append("[Player is heavily armored]\n");
        } else if (armor >= 8) {
            context.append("[Player is wearing some armor]\n");
        } else if (armor > 0) {
            context.append("[Player is lightly armored]\n");
        }

        // Time of day
        long dayTime = player.level().getDayTime() % 24000;
        String timeOfDay = getTimeOfDayString(dayTime);
        context.append("[Time: ").append(timeOfDay).append("]\n");

        // Chat history with this player
        int chatCount = getPlayerChatCount(player.getUUID());
        if (chatCount == 0) {
            context.append("[You have never spoken to this player before]\n");
        } else if (chatCount == 1) {
            context.append("[You have spoken to this player once before]\n");
        } else {
            context.append("[You have spoken to this player ")
                   .append(chatCount).append(" times before]\n");
        }

        return context.toString().trim();
    }

    private String getRelationshipString(Rank rank, IPermissions perms, ServerPlayer player) {
        if (rank.getId() == IPermissions.OWNER_RANK_ID) {
            return "the owner of this colony";
        } else if (rank.getId() == IPermissions.OFFICER_RANK_ID) {
            return "an officer of this colony";
        } else if (rank.getId() == IPermissions.FRIEND_RANK_ID) {
            return "a friend of this colony";
        } else if (rank.getId() == IPermissions.HOSTILE_RANK_ID) {
            return "hostile to this colony";
        } else if (perms.isColonyMember(player)) {
            return "a member of this colony";
        } else {
            return "an outsider (not part of this colony)";
        }
    }

    private String getTimeOfDayString(long dayTime) {
        if (dayTime < 6000) return "night";
        if (dayTime < 12000) return "morning";
        if (dayTime < 18000) return "afternoon";
        return "evening";
    }

    /**
     * Sends a player message to the NPC.
     */
    public void sendPlayerMessage(String playerName, String message) {
        sendPlayerMessage(playerName, message, null);
    }

    /**
     * Sends a player message to the NPC with additional player context.
     */
    public void sendPlayerMessage(String playerName, String message, @Nullable String playerContext) {
        if (!ready || npcHandle == null) {
            LOGGER.warn("NPC not ready for citizen: {}", citizenData.getName());
            return;
        }

        String context = getGameStateContext();
        if (playerContext != null) {
            context += "\n" + playerContext;
        }
        npcHandle.chat(playerName, message, context);
    }

    /**
     * Sends an interaction message (e.g., player approached).
     */
    public void sendInteractionMessage(String playerName, String message) {
        if (!ready || npcHandle == null) {
            return;
        }

        String context = getGameStateContext();
        npcHandle.chat(playerName, message, context);
    }

    /**
     * Sends a message to another NPC for NPC-to-NPC conversations.
     * The colonyId is used for response routing.
     */
    public void sendNpcToNpcMessage(String listenerName, String prompt, int colonyId) {
        if (!ready || npcHandle == null) {
            return;
        }

        String context = getGameStateContext() +
            "\n[You are having a conversation with " + listenerName + ", another colonist.]";
        npcHandle.chat(listenerName, prompt, context);
    }

    /**
     * Adds a message heard from another citizen to context.
     * This notifies the NPC about nearby conversations.
     */
    public void addHeardMessage(String speakerName, String message) {
        if (!ready || npcHandle == null) {
            return;
        }

        // Send as contextual message
        String context = getGameStateContext();
        npcHandle.chat(speakerName, "[You overhear " + speakerName + " saying: " + message + "]", context);
    }

    /**
     * Called every tick to update the bridge.
     */
    public void onTick() {
        // Periodic health check
        long now = System.nanoTime();
        if (now - lastHeartbeatTime > HEARTBEAT_INTERVAL_NS) {
            Player2NpcLib.checkHealth();
            lastHeartbeatTime = now;
        }
    }

    /**
     * Checks if the citizen entity is near a player.
     */
    public boolean isEntityNear(Player player, double radius) {
        return citizenData.getEntity().isPresent() &&
            citizenData.getEntity().get().distanceToSqr(player) < radius * radius;
    }

    /**
     * Returns true if the NPC is ready to receive messages.
     */
    public boolean isReady() {
        return ready && npcHandle != null && npcHandle.isAlive();
    }

    /**
     * Gets the NPC's UUID.
     */
    @Nullable
    public UUID getNpcId() {
        return npcHandle != null ? npcHandle.getId() : null;
    }

    /**
     * Gets the citizen data.
     */
    public ICitizenData getCitizenData() {
        return citizenData;
    }

    /**
     * Shuts down the bridge and kills the NPC.
     */
    public void shutdown() {
        ready = false;
        if (npcHandle != null && npcHandle.isAlive()) {
            npcHandle.kill();
        }
        npcHandle = null;
    }

    /**
     * Respawns the NPC with a fresh system prompt.
     * Used when settings like language change at runtime.
     */
    public CompletableFuture<UUID> respawn() {
        shutdown();
        return spawn();
    }
}
