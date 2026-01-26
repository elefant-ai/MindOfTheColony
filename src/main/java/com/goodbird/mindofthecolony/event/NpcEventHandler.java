package com.goodbird.mindofthecolony.event;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.goodbird.mindofthecolony.bridge.CitizenNpcBridge;
import com.goodbird.mindofthecolony.network.AIChatResponseMessage;
import game.player2.npc.event.NpcCommandEvent;
import game.player2.npc.event.NpcConnectionEvent;
import game.player2.npc.event.NpcErrorEvent;
import game.player2.npc.event.NpcMessageEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles events from the java-npc library.
 * Routes NPC responses back to the appropriate citizens and players.
 */
@EventBusSubscriber(modid = "mindofthecolony")
public class NpcEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NpcEventHandler.class);

    /**
     * Handles text messages from NPCs.
     * Sends the response to the player currently chatting with the citizen.
     */
    @SubscribeEvent
    public static void onNpcMessage(NpcMessageEvent event) {
        CitizenNpcBridge bridge = CitizenNpcManager.getInstance().getBridgeByNpcId(event.getNpcId());
        if (bridge == null) {
            LOGGER.debug("Received message for unknown NPC: {}", event.getNpcId());
            return;
        }

        String message = event.getMessage();
        if (message == null || message.isEmpty()) {
            return;
        }

        int citizenId = bridge.getCitizenData().getId();
        String citizenName = bridge.getCitizenData().getName();

        // Find the player chatting with this citizen
        ServerPlayer chattingPlayer = CitizenNpcManager.getInstance().getChattingPlayer(citizenId);
        if (chattingPlayer != null) {
            // Send targeted response to the player's GUI
            PacketDistributor.sendToPlayer(chattingPlayer, new AIChatResponseMessage(
                citizenId,
                citizenName,
                message
            ));
            LOGGER.debug("Sent response from {} to player {}: {}",
                citizenName, chattingPlayer.getName().getString(), message);
        } else {
            LOGGER.debug("No player chatting with citizen {} - message ignored: {}",
                citizenName, message);
        }
    }

    /**
     * Handles command invocations from NPCs.
     * Currently not used but could handle emotes, gestures, etc.
     */
    @SubscribeEvent
    public static void onNpcCommand(NpcCommandEvent event) {
        CitizenNpcBridge bridge = CitizenNpcManager.getInstance().getBridgeByNpcId(event.getNpcId());
        if (bridge == null) {
            return;
        }

        String commandName = event.getCommandName();
        LOGGER.debug("Citizen {} invoked command: {}", bridge.getCitizenData().getName(), commandName);

        // Handle specific commands if needed
        // For example: emotes, gestures, looking at things, etc.
        switch (commandName) {
            case "emote" -> {
                // Could trigger citizen animations
                String emote = event.getStringArgument("emote");
                LOGGER.debug("Citizen {} emotes: {}", bridge.getCitizenData().getName(), emote);
            }
            case "minecraft_command" -> {
                // Currently not enabling command execution for citizens
                LOGGER.debug("Citizen tried to execute command (disabled)");
            }
        }
    }

    /**
     * Handles connection status changes.
     */
    @SubscribeEvent
    public static void onNpcConnection(NpcConnectionEvent event) {
        switch (event.getStatus()) {
            case CONNECTED -> LOGGER.info("Connected to Player2 API for game: {}", event.getGameId());
            case DISCONNECTED -> LOGGER.warn("Disconnected from Player2 API: {}", event.getMessage());
            case RECONNECTING -> LOGGER.info("Reconnecting to Player2 API...");
            case RECONNECT_FAILED -> LOGGER.error("Failed to reconnect to Player2 API after max attempts");
        }
    }

    /**
     * Handles errors from the NPC system.
     */
    @SubscribeEvent
    public static void onNpcError(NpcErrorEvent event) {
        LOGGER.error("NPC error [{}]: {}", event.getType(), event.getMessage());

        if (event.getCause() != null) {
            LOGGER.error("Caused by:", event.getCause());
        }

        // Handle specific error types
        switch (event.getType()) {
            case AUTH_ERROR -> LOGGER.error("Authentication failed - check your Player2 API key");
            case INSUFFICIENT_CREDITS -> LOGGER.warn("Insufficient credits/joules for NPC operation");
            case NPC_NOT_FOUND -> {
                // NPC might have been cleaned up, remove from registry
                if (event.getNpcId() != null) {
                    CitizenNpcBridge bridge = CitizenNpcManager.getInstance().getBridgeByNpcId(event.getNpcId());
                    if (bridge != null) {
                        LOGGER.warn("NPC not found for citizen: {}, cleaning up", bridge.getCitizenData().getName());
                    }
                }
            }
            default -> {
                // Log other errors for debugging
            }
        }
    }
}
