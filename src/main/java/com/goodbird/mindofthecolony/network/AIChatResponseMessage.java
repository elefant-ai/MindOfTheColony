package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.client.ClientChatHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network message sent from server to client when an NPC responds to a player.
 * The client will display this in the interaction GUI.
 */
public record AIChatResponseMessage(
    int citizenId,
    String citizenName,
    String message
) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIChatResponseMessage.class);

    public static final Type<AIChatResponseMessage> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mindofthecolony", "ai_chat_response")
    );

    public static final StreamCodec<FriendlyByteBuf, AIChatResponseMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, AIChatResponseMessage::citizenId,
        ByteBufCodecs.STRING_UTF8, AIChatResponseMessage::citizenName,
        ByteBufCodecs.STRING_UTF8, AIChatResponseMessage::message,
        AIChatResponseMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handle the message on the client side.
     */
    public static void handle(AIChatResponseMessage msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // Delegate to client-side handler to update the GUI
            ClientChatHandler.handleResponse(msg.citizenId(), msg.citizenName(), msg.message());
            LOGGER.debug("Received AI response for citizen {}: {}", msg.citizenId(), msg.message());
        });
    }
}
