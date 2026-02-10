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

    /**
     * Handle the message on the client side.
     */
    public static void handle(NpcChatBroadcastMessage msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Format the message for chat display
            String format = NpcInteractionConfig.getPlayerVisibilityConfig().chatFormat;
            String formattedMessage = format
                .replace("%speaker%", msg.speakerName())
                .replace("%listener%", msg.listenerName())
                .replace("%message%", msg.message());

            // Create chat component with styling
            Component chatComponent = Component.literal(formattedMessage)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);

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
