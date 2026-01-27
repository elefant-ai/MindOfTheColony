package com.goodbird.mindofthecolony.event;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.goodbird.mindofthecolony.bridge.CitizenNpcBridge;
import com.goodbird.mindofthecolony.network.AIChatResponseMessage;
import game.player2.npc.event.NpcCommandEvent;
import game.player2.npc.event.NpcConnectionEvent;
import game.player2.npc.event.NpcErrorEvent;
import game.player2.npc.event.NpcMessageEvent;
import game.player2.npc.event.Player2EventListener;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            case "minecraft_command" -> {
                LOGGER.debug("Citizen tried to execute command (disabled)");
            }
        }

        return false;
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
