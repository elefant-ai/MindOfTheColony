package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.background.TraitModifiers;
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

                // Check both citizen manager and visitor manager
                ICitizenData citizenData = colony.getCitizenManager().getCivilian(msg.citizenId());
                if (citizenData == null) {
                    // Try visitor manager (visitors have negative IDs)
                    citizenData = colony.getVisitorManager().getCivilian(msg.citizenId());
                }
                if (citizenData == null) return;

                StringBuilder info = new StringBuilder();

                // Add job info
                String jobName = citizenData.getJob() != null
                    ? citizenData.getJob().getJobRegistryEntry().getTranslationKey()
                    : "none";
                if (jobName.contains(".")) {
                    jobName = jobName.substring(jobName.lastIndexOf(".") + 1);
                }
                info.append(String.format("Job: %s\n", jobName));

                // Add disease modifier (combined job + trait)
                double diseaseModifier = citizenData.getDiseaseModifier();
                info.append(String.format("Disease Modifier (combined): %.2f\n", diseaseModifier));

                // Add background and trait modifiers
                if (citizenData instanceof IExtendedCitizenData extData) {
                    CitizenBackground bg = extData.getCitizenBackground();
                    if (bg != null && bg.isInitialized()) {
                        TraitModifiers mods = bg.getModifiers();
                        info.append("\nTRAIT MODIFIERS:\n");
                        info.append(String.format("- Disease Rate: %.2f\n", mods.diseaseRate()));
                        info.append(String.format("- Contact Disease: %.2f\n", mods.contactDiseaseRate()));
                        info.append(String.format("- Happiness Base: %+.2f\n", mods.happinessBase()));
                        info.append(String.format("- Happiness Decay: %.2f\n", mods.happinessDecayRate()));
                        info.append(String.format("- Work Speed: %.2f\n", mods.workSpeed()));
                        info.append(String.format("- Food Consumption: %.2f\n", mods.foodConsumption()));
                        info.append("\n");
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
