package com.goodbird.mindofthecolony.client;

import com.goodbird.mindofthecolony.network.AIChatMessage;
import com.ldtteam.blockui.controls.Button;
import com.ldtteam.blockui.controls.ButtonHandler;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.controls.TextField;
import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.blockui.views.ScrollingGroup;
import com.minecolonies.api.colony.ICitizenDataView;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Chat window for conversing with a citizen via AI.
 */
@OnlyIn(Dist.CLIENT)
public class ChatWindowCitizen extends BOWindow implements ButtonHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatWindowCitizen.class);

    private static final String CHAT_INPUT_ID = "chatInput";
    private static final String CHAT_SEND_ID = "sendButton";
    private static final String CHAT_HISTORY_ID = "chatHistory";
    private static final String CLOSE_BUTTON_ID = "closeButton";
    private static final String TITLE_ID = "title";

    private final ICitizenDataView citizen;
    // Constructor accepts Object to bridge fork types (compile-time) and MC types (runtime)
    private final Consumer<ClientChatHandler.ChatEntry> messageListener = this::onMessageReceived;
    private Text chatHistoryText;
    private ScrollingGroup chatScroll;
    private int lastHistorySize = 0;

    public ChatWindowCitizen(Object citizenObj) {
        super(ResourceLocation.fromNamespaceAndPath("mindofthecolony", "gui/chatwindow.xml"));
        this.citizen = (ICitizenDataView) citizenObj;
    }

    @Override
    public void onOpened() {
        super.onOpened();

        // Set title - use first name only to fit
        Text title = findPaneOfTypeByID(TITLE_ID, Text.class);
        if (title != null) {
            String name = citizen.getName();
            // Use first name only if name is too long
            if (name.length() > 15) {
                String[] parts = name.split(" ");
                name = parts[0];
            }
            title.setText(Component.literal("Chat: " + name));
        }

        // Start conversation tracking
        ClientChatHandler.startConversation(citizen.getId());

        // Register for message updates
        ClientChatHandler.addMessageListener(messageListener);

        // Set up the chat history text and scroll container
        chatHistoryText = findPaneOfTypeByID(CHAT_HISTORY_ID, Text.class);
        chatScroll = findPaneOfTypeByID("chatScroll", ScrollingGroup.class);
        LOGGER.debug("Chat window opened, chatHistoryText={}", chatHistoryText);
        updateChatHistory();
    }

    @Override
    public void onClosed() {
        super.onClosed();
        ClientChatHandler.removeMessageListener(messageListener);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        // Check if history changed and needs refresh
        int currentSize = ClientChatHandler.getConversationHistory().size();
        if (currentSize != lastHistorySize) {
            updateChatHistory();
        }
    }

    @Override
    public boolean onKeyTyped(final char ch, final int key) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
            sendMessage();
            return true;
        }
        return super.onKeyTyped(ch, key);
    }

    @Override
    public void onButtonClicked(@NotNull Button button) {
        switch (button.getID()) {
            case CHAT_SEND_ID -> sendMessage();
            case CLOSE_BUTTON_ID -> this.close();
        }
    }

    private void sendMessage() {
        TextField chatInput = findPaneOfTypeByID(CHAT_INPUT_ID, TextField.class);
        if (chatInput == null) return;

        String message = chatInput.getText();
        if (message == null || message.trim().isEmpty()) return;

        // Add to local history
        String playerName = Minecraft.getInstance().player != null
            ? Minecraft.getInstance().player.getName().getString()
            : "Player";
        ClientChatHandler.addPlayerMessage(playerName, message);

        // Send to server
        PacketDistributor.sendToServer(new AIChatMessage(
            citizen.getColonyId(),
            citizen.getId(),
            message
        ));

        // Clear input
        chatInput.setText("");

        // Update display
        updateChatHistory();
    }

    private void onMessageReceived(ClientChatHandler.ChatEntry entry) {
        LOGGER.debug("onMessageReceived: {}", entry.message());
        updateChatHistory();
    }

    /**
     * Updates the chat history display with all messages in a single text element.
     */
    private void updateChatHistory() {
        if (chatHistoryText == null) return;

        List<ClientChatHandler.ChatEntry> history = ClientChatHandler.getConversationHistory().stream()
            .filter(e -> e.message() != null && !e.message().trim().isEmpty())
            .toList();

        lastHistorySize = ClientChatHandler.getConversationHistory().size();

        StringBuilder sb = new StringBuilder();
        for (ClientChatHandler.ChatEntry entry : history) {
            String prefix = entry.isPlayer() ? "You" : entry.senderName();
            if (sb.length() > 0) sb.append("\n\n");  // Double newline between messages
            sb.append(prefix).append(": ").append(entry.message());
        }

        String text = sb.toString();
        chatHistoryText.setText(Component.literal(text));

        // Estimate actual text height since getRenderedTextHeight() is lazy (computed on draw)
        float textScale = 0.8f;
        int textWidth = chatHistoryText.getWidth();
        int scaledWidth = (int) (textWidth / textScale);
        var font = Minecraft.getInstance().font;
        var lines = font.split(Component.literal(text), scaledWidth);
        int estimatedHeight = (int) ((lines.size() * font.lineHeight + 8) * textScale);
        chatHistoryText.setSize(textWidth, Math.max(12, estimatedHeight));

        // Recompute scroll content height after resize, then scroll to bottom
        if (chatScroll != null) {
            chatScroll.getContainer().computeContentHeight();
            chatScroll.setScrollY(chatScroll.getContentHeight());
        }
    }
}
