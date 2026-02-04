package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Network message sent from client to server when player opens or closes
 * the chat menu for a citizen. Used to freeze/unfreeze citizen movement.
 */
public record ChatMenuStateMessage(
    int colonyId,
    int citizenId,
    boolean menuOpen
) implements CustomPacketPayload {

    public static final Type<ChatMenuStateMessage> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mindofthecolony", "chat_menu_state")
    );

    public static final StreamCodec<FriendlyByteBuf, ChatMenuStateMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ChatMenuStateMessage::colonyId,
        ByteBufCodecs.VAR_INT, ChatMenuStateMessage::citizenId,
        ByteBufCodecs.BOOL, ChatMenuStateMessage::menuOpen,
        ChatMenuStateMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handle the message on the server side.
     * Freezes or unfreezes the citizen based on menu state.
     */
    public static void handle(ChatMenuStateMessage msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                CitizenNpcManager manager = CitizenNpcManager.getInstance();

                if (msg.menuOpen()) {
                    manager.freezeCitizen(msg.citizenId(), player);
                } else {
                    manager.unfreezeCitizen(msg.citizenId());
                }
            }
        });
    }
}
