package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.client.ClientBackgroundCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client: Background data for a citizen.
 */
public record BackgroundResponseMessage(
    int citizenId,
    String backstory,
    List<String> permanentTraits,
    List<String> temporaryTraits,
    List<String> permanentEffects,
    List<String> temporaryEffects
) implements CustomPacketPayload {

    public static final Type<BackgroundResponseMessage> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mindofthecolony", "background_response")
    );

    public static final StreamCodec<FriendlyByteBuf, BackgroundResponseMessage> STREAM_CODEC = StreamCodec.of(
        BackgroundResponseMessage::encode,
        BackgroundResponseMessage::decode
    );

    private static void encode(FriendlyByteBuf buf, BackgroundResponseMessage msg) {
        buf.writeVarInt(msg.citizenId);
        buf.writeUtf(msg.backstory);
        writeStringList(buf, msg.permanentTraits);
        writeStringList(buf, msg.temporaryTraits);
        writeStringList(buf, msg.permanentEffects);
        writeStringList(buf, msg.temporaryEffects);
    }

    private static BackgroundResponseMessage decode(FriendlyByteBuf buf) {
        int citizenId = buf.readVarInt();
        String backstory = buf.readUtf();
        List<String> permanentTraits = readStringList(buf);
        List<String> temporaryTraits = readStringList(buf);
        List<String> permanentEffects = readStringList(buf);
        List<String> temporaryEffects = readStringList(buf);
        return new BackgroundResponseMessage(citizenId, backstory, permanentTraits, temporaryTraits, permanentEffects, temporaryEffects);
    }

    private static void writeStringList(FriendlyByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) {
            buf.writeUtf(s);
        }
    }

    private static List<String> readStringList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf());
        }
        return list;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BackgroundResponseMessage msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientBackgroundCache.putData(msg.citizenId(), msg.backstory(),
                msg.permanentTraits(), msg.temporaryTraits(), msg.permanentEffects(), msg.temporaryEffects());
        });
    }
}
