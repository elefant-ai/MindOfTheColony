package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.config.NpcInteractionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network message sent from server to client when NPCs talk to each other.
 * Nearby players receive this to "overhear" the conversation.
 */
public record NpcChatBroadcastMessage(
    int colonyId,
    int speakerId,
    String speakerName,
    int listenerId,
    String listenerName,
    String message,
    boolean isConversationStart,
    boolean isConversationEnd
) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger(NpcChatBroadcastMessage.class);

    public static final Type<NpcChatBroadcastMessage> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mindofthecolony", "npc_chat_broadcast")
    );

    public static final StreamCodec<FriendlyByteBuf, NpcChatBroadcastMessage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public NpcChatBroadcastMessage decode(FriendlyByteBuf buf) {
            return new NpcChatBroadcastMessage(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readBoolean()
            );
        }

        @Override
        public void encode(FriendlyByteBuf buf, NpcChatBroadcastMessage msg) {
            buf.writeVarInt(msg.colonyId());
            buf.writeVarInt(msg.speakerId());
            buf.writeUtf(msg.speakerName());
            buf.writeVarInt(msg.listenerId());
            buf.writeUtf(msg.listenerName());
            buf.writeUtf(msg.message());
            buf.writeBoolean(msg.isConversationStart());
            buf.writeBoolean(msg.isConversationEnd());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Color palette for NPC names (avoiding dark colors for readability)
    private static final ChatFormatting[] NAME_COLORS = {
        ChatFormatting.AQUA,
        ChatFormatting.GREEN,
        ChatFormatting.LIGHT_PURPLE,
        ChatFormatting.YELLOW,
        ChatFormatting.GOLD,
        ChatFormatting.BLUE,
        ChatFormatting.RED
    };

    /**
     * Get a consistent color for a citizen based on their ID.
     * Uses the conversation pair to ensure both get different colors.
     */
    private static ChatFormatting getColorForCitizen(int citizenId, int otherId) {
        // Use citizen ID to pick a base color
        int colorIndex = Math.abs(citizenId) % NAME_COLORS.length;
        ChatFormatting color = NAME_COLORS[colorIndex];

        // If other citizen would get the same color, shift this one
        int otherColorIndex = Math.abs(otherId) % NAME_COLORS.length;
        if (colorIndex == otherColorIndex) {
            colorIndex = (colorIndex + 1) % NAME_COLORS.length;
            color = NAME_COLORS[colorIndex];
        }

        return color;
    }

    /**
     * Handle the message on the client side.
     */
    public static void handle(NpcChatBroadcastMessage msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Get consistent colors for this conversation pair
            ChatFormatting speakerColor = getColorForCitizen(msg.speakerId(), msg.listenerId());
            ChatFormatting listenerColor = getColorForCitizen(msg.listenerId(), msg.speakerId());

            // Build the chat message with colored names
            // Format: [SpeakerName] (to ListenerName): message
            Component chatComponent = Component.literal("[")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(msg.speakerName()).withStyle(speakerColor))
                .append(Component.literal("] (to ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(msg.listenerName()).withStyle(listenerColor))
                .append(Component.literal("): ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(msg.message()).withStyle(ChatFormatting.WHITE));

            // Display in Minecraft chat
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(chatComponent, false);
            }

            LOGGER.debug("NPC chat broadcast: {} -> {}: {}",
                msg.speakerName(), msg.listenerName(), msg.message());
        });
    }
}
