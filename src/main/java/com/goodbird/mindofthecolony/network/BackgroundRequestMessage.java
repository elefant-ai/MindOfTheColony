package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client -> Server: Request background data for a citizen.
 */
public record BackgroundRequestMessage(
    int colonyId,
    int citizenId
) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundRequestMessage.class);

    public static final Type<BackgroundRequestMessage> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath("mindofthecolony", "background_request")
    );

    public static final StreamCodec<FriendlyByteBuf, BackgroundRequestMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, BackgroundRequestMessage::colonyId,
        ByteBufCodecs.VAR_INT, BackgroundRequestMessage::citizenId,
        BackgroundRequestMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BackgroundRequestMessage msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                IColony colony = IColonyManager.getInstance().getColonyByDimension(msg.colonyId(), player.level().dimension());
                if (colony == null) return;

                ICitizenData citizenData = colony.getCitizenManager().getCivilian(msg.citizenId());
                if (citizenData == null) return;

                StringBuilder info = new StringBuilder();

                // Add disease modifier info
                double diseaseModifier = citizenData.getDiseaseModifier();
                String jobName = citizenData.getJob() != null
                    ? citizenData.getJob().getJobRegistryEntry().getTranslationKey()
                    : "none";
                if (jobName.contains(".")) {
                    jobName = jobName.substring(jobName.lastIndexOf(".") + 1);
                }
                info.append(String.format("Disease Modifier: %.2f (job: %s)\n", diseaseModifier, jobName));

                // Add background info
                if (citizenData instanceof IExtendedCitizenData extData) {
                    CitizenBackground bg = extData.getCitizenBackground();
                    if (bg != null && bg.isInitialized()) {
                        info.append(bg.toSystemPromptSection());
                    } else {
                        info.append("No background data available.");
                    }
                } else {
                    info.append("No background data available.");
                }

                PacketDistributor.sendToPlayer(player,
                    new BackgroundResponseMessage(msg.citizenId(), info.toString()));
            }
        });
    }
}
