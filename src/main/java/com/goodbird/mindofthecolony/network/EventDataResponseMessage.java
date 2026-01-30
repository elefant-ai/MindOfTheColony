package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.client.ClientEventCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server -> Client: Event data for a colony.
 */
public record EventDataResponseMessage(
    int colonyId,
    String weatherStatus,
    List<String> recentEvents
) implements CustomPacketPayload {

    public static final Type<EventDataResponseMessage> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mindofthecolony", "event_data_response")
    );

    public static final StreamCodec<FriendlyByteBuf, EventDataResponseMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, EventDataResponseMessage::colonyId,
        ByteBufCodecs.STRING_UTF8, EventDataResponseMessage::weatherStatus,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), EventDataResponseMessage::recentEvents,
        EventDataResponseMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EventDataResponseMessage msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientEventCache.put(msg.colonyId(), msg.weatherStatus(), msg.recentEvents());
        });
    }
}
