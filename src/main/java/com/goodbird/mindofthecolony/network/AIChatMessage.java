package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.goodbird.mindofthecolony.bridge.CitizenNpcBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network message sent from client to server when player sends a chat message
 * to a citizen through the interaction GUI.
 */
public record AIChatMessage(
    int colonyId,
    int citizenId,
    String message
) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIChatMessage.class);

    public static final Type<AIChatMessage> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mindofthecolony", "ai_chat")
    );

    public static final StreamCodec<FriendlyByteBuf, AIChatMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, AIChatMessage::colonyId,
        ByteBufCodecs.VAR_INT, AIChatMessage::citizenId,
        ByteBufCodecs.STRING_UTF8, AIChatMessage::message,
        AIChatMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handle the message on the server side.
     */
    public static void handle(AIChatMessage msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                CitizenNpcBridge bridge = CitizenNpcManager.getInstance().getBridge(msg.citizenId());
                if (bridge != null && bridge.isReady()) {
                    // Start/continue conversation with this player
                    CitizenNpcManager.getInstance().startConversation(msg.citizenId(), player);

                    // Increment chat count for this player
                    bridge.incrementPlayerChatCount(player.getUUID());

                    // Send the message to the NPC with player's colony relationship context
                    String playerName = player.getName().getString();
                    String playerContext = bridge.getPlayerContext(player);
                    bridge.sendPlayerMessage(playerName, msg.message(), playerContext);

                    LOGGER.debug("Player {} sent message to citizen {}: {}",
                        playerName, msg.citizenId(), msg.message());
                } else {
                    LOGGER.warn("Cannot send message - citizen {} not ready", msg.citizenId());
                }
            }
        });
    }
}
