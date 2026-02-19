package com.goodbird.mindofthecolony.event;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.goodbird.mindofthecolony.bridge.CitizenNpcBridge;
import com.goodbird.mindofthecolony.config.ModSettings;
import com.goodbird.mindofthecolony.interaction.NpcConversation;
import com.goodbird.mindofthecolony.interaction.NpcConversationManager;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.goodbird.mindofthecolony.network.AIChatResponseMessage;
import com.goodbird.mindofthecolony.network.TtsAudioMessage;
import com.goodbird.mindofthecolony.preference.WorkPreferences;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.CitizenData;
import game.player2.npc.Player2NpcLib;
import game.player2.npc.dto.TtsSpeakRequest;
import game.player2.npc.event.NpcCommandEvent;
import game.player2.npc.event.NpcConnectionEvent;
import game.player2.npc.event.NpcErrorEvent;
import game.player2.npc.event.NpcMessageEvent;
import game.player2.npc.event.Player2EventListener;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.List;

/**
 * Handles events from the java-npc library.
 * Routes NPC responses back to the appropriate citizens and players.
 */
public class NpcEventHandler implements Player2EventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NpcEventHandler.class);

    @Override
    public boolean onMessageEvent(NpcMessageEvent event) {
        CitizenNpcBridge bridge = CitizenNpcManager.getInstance().getBridgeByNpcId(event.getNpcId());
        if (bridge == null) {
            LOGGER.debug("Received message for unknown NPC: {}", event.getNpcId());
            return false;
        }

        String message = event.getMessage();
        if (message == null || message.isEmpty()) {
            return false;
        }

        int citizenId = bridge.getCitizenData().getId();
        int colonyId = bridge.getCitizenData().getColony().getID();
        String citizenName = bridge.getCitizenData().getName();

        // Check if this citizen is in an NPC-NPC conversation
        NpcConversationManager convManager = NpcConversationManager.getIfExists(colonyId);
        if (convManager != null) {
            NpcConversation conversation = convManager.getConversation(citizenId);
            LOGGER.info("Checking NPC-NPC routing for {}: convManager exists, conversation={}",
                citizenName, conversation);
            if (conversation != null && !conversation.isFinished()) {
                // Route to NPC-NPC conversation manager
                LOGGER.info("Routing NPC-NPC response from {} to conversation manager", citizenName);
                convManager.handleResponse(conversation, citizenId, message);
                return false;
            }
        }

        // Find the player chatting with this citizen
        ServerPlayer chattingPlayer = CitizenNpcManager.getInstance().getChattingPlayer(citizenId);
        if (chattingPlayer != null) {
            // Send targeted response to the player's GUI
            PacketDistributor.sendToPlayer(chattingPlayer, new AIChatResponseMessage(
                citizenId,
                citizenName,
                message
            ));

            // Speak the response aloud via TTS if enabled
            if (ModSettings.TTS_ENABLED.get()) {
                double speed = ModSettings.TTS_SPEED.get();
                String voiceId = null;
                if (bridge.getCitizenData() instanceof IExtendedCitizenData extData) {
                    voiceId = extData.getVoiceId();
                }

                // Get the citizen's entity ID for positional audio
                int entityId = bridge.getCitizenData().getEntity()
                    .map(e -> e.getId())
                    .orElse(-1);

                // Use citizen's gender as a hard constraint on the TTS API
                String voiceGender = bridge.getCitizenData().isFemale() ? "female" : "male";

                // Request audio data (play_in_app=false) for 3D positional playback
                TtsSpeakRequest ttsRequest = new TtsSpeakRequest(
                    message, false, speed,
                    voiceId != null ? List.of(voiceId) : null,
                    voiceGender, null, "wav", null
                );

                Player2NpcLib.ttsSpeak(ttsRequest)
                    .thenAccept(response -> {
                        String data = response.getData();
                        if (data != null && !data.isEmpty()) {
                            byte[] audioBytes = Base64.getDecoder().decode(data);
                            PacketDistributor.sendToPlayer(chattingPlayer,
                                new TtsAudioMessage(entityId, audioBytes));
                        }
                    })
                    .exceptionally(ex -> {
                        LOGGER.warn("TTS failed for {}: {}", citizenName, ex.getMessage());
                        return null;
                    });
            }

            LOGGER.debug("Sent response from {} to player {}: {}",
                citizenName, chattingPlayer.getName().getString(), message);
        } else {
            LOGGER.debug("No player chatting with citizen {} - message ignored: {}",
                citizenName, message);
        }

        return false;
    }

    @Override
    public boolean onCommandEvent(NpcCommandEvent event) {
        CitizenNpcBridge bridge = CitizenNpcManager.getInstance().getBridgeByNpcId(event.getNpcId());
        if (bridge == null) {
            return false;
        }

        String commandName = event.getCommandName();
        LOGGER.debug("Citizen {} invoked command: {}", bridge.getCitizenData().getName(), commandName);

        switch (commandName) {
            case "emote" -> {
                String emote = event.getStringArgument("emote");
                LOGGER.debug("Citizen {} emotes: {}", bridge.getCitizenData().getName(), emote);
            }
            case "set_work_preference" -> {
                handleSetWorkPreference(bridge.getCitizenData(), event);
            }
            case "set_building_preference" -> {
                handleSetBuildingPreference(bridge.getCitizenData(), event);
            }
            case "minecraft_command" -> {
                LOGGER.debug("Citizen tried to execute command (disabled)");
            }
        }

        return false;
    }

    /**
     * Handle set_work_preference function call from NPC.
     */
    private void handleSetWorkPreference(ICitizenData citizen, NpcCommandEvent event) {
        if (!(citizen instanceof IExtendedCitizenData extData)) {
            return;
        }

        String category = event.getStringArgument("category");
        String action = event.getStringArgument("action");
        int preference = event.getIntArgument("preference", 0);

        WorkPreferences prefs = extData.getWorkPreferences();
        if (action != null && !action.isEmpty()) {
            // Set preference for specific category+action combination
            prefs.setCategoryPreference(category, preference);
            prefs.setActionPreference(action, preference);
            LOGGER.info("Citizen {} set preference for {}:{} to {}",
                citizen.getName(), category, action, preference);
        } else {
            prefs.setCategoryPreference(category, preference);
            LOGGER.info("Citizen {} set preference for {} to {}",
                citizen.getName(), category, preference);
        }

        // Mark dirty to persist
        if (citizen instanceof CitizenData citizenData) {
            citizenData.markDirty(0);
        }
    }

    /**
     * Handle set_building_preference function call from NPC.
     */
    private void handleSetBuildingPreference(ICitizenData citizen, NpcCommandEvent event) {
        if (!(citizen instanceof IExtendedCitizenData extData)) {
            return;
        }

        String buildingType = event.getStringArgument("building_type");
        int preference = event.getIntArgument("preference", 0);

        WorkPreferences prefs = extData.getWorkPreferences();
        prefs.setBuildingTypePreference(buildingType, preference);

        LOGGER.info("Citizen {} set building preference for {} to {}",
            citizen.getName(), buildingType, preference);

        // Mark dirty to persist
        if (citizen instanceof CitizenData citizenData) {
            citizenData.markDirty(0);
        }
    }

    @Override
    public void onConnectionEvent(NpcConnectionEvent event) {
        switch (event.getStatus()) {
            case CONNECTED -> LOGGER.info("Connected to Player2 API for game: {}", event.getGameId());
            case DISCONNECTED -> LOGGER.warn("Disconnected from Player2 API: {}", event.getMessage());
            case RECONNECTING -> LOGGER.info("Reconnecting to Player2 API...");
            case RECONNECT_FAILED -> LOGGER.error("Failed to reconnect to Player2 API after max attempts");
        }
    }

    @Override
    public void onErrorEvent(NpcErrorEvent event) {
        LOGGER.error("NPC error [{}]: {}", event.getType(), event.getMessage());

        if (event.getCause() != null) {
            LOGGER.error("Caused by:", event.getCause());
        }

        switch (event.getType()) {
            case AUTH_ERROR -> LOGGER.error("Authentication failed - check your Player2 API key");
            case INSUFFICIENT_CREDITS -> LOGGER.warn("Insufficient credits/joules for NPC operation");
            case NPC_NOT_FOUND -> {
                if (event.getNpcId() != null) {
                    CitizenNpcBridge bridge = CitizenNpcManager.getInstance().getBridgeByNpcId(event.getNpcId());
                    if (bridge != null) {
                        LOGGER.warn("NPC not found for citizen: {}, cleaning up", bridge.getCitizenData().getName());
                    }
                }
            }
            default -> {}
        }
    }
}
