package com.goodbird.mindofthecolony.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Client-side handler for AI chat responses.
 * Manages the conversation history and notifies listeners (GUI) of new messages.
 */
@OnlyIn(Dist.CLIENT)
public class ClientChatHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientChatHandler.class);

    // Current citizen being chatted with
    private static int currentCitizenId = -1;

    // Conversation history for current citizen
    private static final List<ChatEntry> conversationHistory = new CopyOnWriteArrayList<>();

    // Listeners that want to be notified of new messages (e.g., the GUI)
    private static final List<Consumer<ChatEntry>> messageListeners = new CopyOnWriteArrayList<>();

    /**
     * Represents a single chat entry (either player message or NPC response).
     */
    public record ChatEntry(String senderName, String message, boolean isPlayer) {}

    /**
     * Called when the player opens the interaction GUI for a citizen.
     */
    public static void startConversation(int citizenId) {
        if (currentCitizenId != citizenId) {
            // Starting conversation with new citizen, clear history
            currentCitizenId = citizenId;
            conversationHistory.clear();
            LOGGER.debug("Started conversation with citizen {}", citizenId);
        }
    }

    /**
     * Called when the player closes the interaction GUI.
     */
    public static void endConversation() {
        currentCitizenId = -1;
        conversationHistory.clear();
        messageListeners.clear();
        LOGGER.debug("Ended conversation");
    }

    /**
     * Add a player message to the conversation history.
     */
    public static void addPlayerMessage(String playerName, String message) {
        ChatEntry entry = new ChatEntry(playerName, message, true);
        conversationHistory.add(entry);
        notifyListeners(entry);
    }

    /**
     * Called when server sends an NPC response.
     */
    public static void handleResponse(int citizenId, String citizenName, String message) {
        if (currentCitizenId == citizenId || currentCitizenId == -1) {
            ChatEntry entry = new ChatEntry(citizenName, message, false);
            conversationHistory.add(entry);
            notifyListeners(entry);
            LOGGER.debug("Received response from {}: {}", citizenName, message);
        } else {
            LOGGER.debug("Ignoring response for citizen {} (current: {})", citizenId, currentCitizenId);
        }
    }

    /**
     * Get the current conversation history.
     */
    public static List<ChatEntry> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }

    /**
     * Register a listener to be notified of new messages.
     */
    public static void addMessageListener(Consumer<ChatEntry> listener) {
        messageListeners.add(listener);
    }

    /**
     * Remove a message listener.
     */
    public static void removeMessageListener(Consumer<ChatEntry> listener) {
        messageListeners.remove(listener);
    }

    private static void notifyListeners(ChatEntry entry) {
        LOGGER.debug("notifyListeners called, listener count: {}", messageListeners.size());
        for (Consumer<ChatEntry> listener : messageListeners) {
            try {
                listener.accept(entry);
            } catch (Exception e) {
                LOGGER.error("Error notifying message listener", e);
            }
        }
    }

    /**
     * Get the current citizen ID being chatted with.
     */
    public static int getCurrentCitizenId() {
        return currentCitizenId;
    }
}
