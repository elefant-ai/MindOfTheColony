package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.client.ClientAudioPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network message sent from server to client containing TTS audio data
 * to be played positionally at a citizen entity's location.
 */
public record TtsAudioMessage(
    int entityId,
    byte[] audioData
) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger(TtsAudioMessage.class);

    public static final Type<TtsAudioMessage> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mindofthecolony", "tts_audio")
    );

    public static final StreamCodec<FriendlyByteBuf, TtsAudioMessage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TtsAudioMessage decode(FriendlyByteBuf buf) {
            int entityId = buf.readVarInt();
            byte[] audioData = buf.readByteArray();
            return new TtsAudioMessage(entityId, audioData);
        }

        @Override
        public void encode(FriendlyByteBuf buf, TtsAudioMessage msg) {
            buf.writeVarInt(msg.entityId());
            buf.writeByteArray(msg.audioData());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TtsAudioMessage msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientAudioPlayer.play(msg.entityId(), msg.audioData());
        });
    }
}
