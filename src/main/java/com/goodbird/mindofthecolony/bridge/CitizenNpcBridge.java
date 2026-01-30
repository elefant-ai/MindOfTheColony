package com.goodbird.mindofthecolony.bridge;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.config.ModSettings;
import com.goodbird.mindofthecolony.effect.TemporaryTrait;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.goodbird.mindofthecolony.status.AgentStatus;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.jobs.IJob;
import game.player2.npc.Player2NpcLib;
import game.player2.npc.api.NpcHandle;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
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

    private NpcHandle npcHandle;
    private CompletableFuture<UUID> pendingSpawn;
    private boolean ready = false;

    // Heartbeat tracking
    private static long lastHeartbeatTime = System.nanoTime();
    private static final long HEARTBEAT_INTERVAL_NS = 60_000_000_000L; // 60 seconds

    public CitizenNpcBridge(ICitizenData citizenData, String gameId) {
        this.citizenData = citizenData;
        this.gameId = gameId;
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

        pendingSpawn = Player2NpcLib.builder(shortName)
            .name(citizenData.getName())
            .description(description)
            .systemPrompt(systemPrompt)
            .keepGameState(false) // Fresh conversation each session
            .spawn(gameId)
            .thenApply(handle -> {
                this.npcHandle = handle;
                this.ready = true;
                LOGGER.info("NPC handle ready for citizen: {}", citizenData.getName());
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
     * Sends a player message to the NPC.
     */
    public void sendPlayerMessage(String playerName, String message) {
        if (!ready || npcHandle == null) {
            LOGGER.warn("NPC not ready for citizen: {}", citizenData.getName());
            return;
        }

        String context = getGameStateContext();
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
