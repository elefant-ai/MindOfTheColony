package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.client.ClientBackgroundCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> Client: Background data for a citizen.
 */
public record BackgroundResponseMessage(
    int citizenId,
    String backgroundInfo
) implements CustomPacketPayload {

    public static final Type<BackgroundResponseMessage> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mindofthecolony", "background_response")
    );

    public static final StreamCodec<FriendlyByteBuf, BackgroundResponseMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, BackgroundResponseMessage::citizenId,
        ByteBufCodecs.STRING_UTF8, BackgroundResponseMessage::backgroundInfo,
        BackgroundResponseMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BackgroundResponseMessage msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientBackgroundCache.put(msg.citizenId(), msg.backgroundInfo());
        });
    }
}
