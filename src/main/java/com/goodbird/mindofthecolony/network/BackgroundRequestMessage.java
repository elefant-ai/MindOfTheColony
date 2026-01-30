package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.background.TraitDefinition;
import com.goodbird.mindofthecolony.background.TraitRegistry;
import com.goodbird.mindofthecolony.effect.TemporaryModifier;
import com.goodbird.mindofthecolony.effect.TemporaryTrait;
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

import java.util.ArrayList;
import java.util.List;

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

                long currentTick = player.level().getGameTime();
                String backstory = "";
                List<String> permanentTraits = new ArrayList<>();
                List<String> temporaryTraits = new ArrayList<>();
                List<String> activeModifiers = new ArrayList<>();

                // Extract background data
                if (citizenData instanceof IExtendedCitizenData extData) {
                    CitizenBackground bg = extData.getCitizenBackground();
                    if (bg != null && bg.isInitialized()) {
                        backstory = bg.getBackstory() != null ? bg.getBackstory() : "";

                        // Permanent traits with display text
                        for (String traitId : bg.getTraits()) {
                            TraitDefinition traitDef = TraitRegistry.getTrait(traitId);
                            if (traitDef != null) {
                                permanentTraits.add(traitDef.displayText());
                            } else {
                                permanentTraits.add(traitId);
                            }
                        }
                    }

                    // Temporary traits
                    for (TemporaryTrait tempTrait : extData.getTemporaryTraits()) {
                        if (!tempTrait.isExpired(currentTick)) {
                            TraitDefinition traitDef = TraitRegistry.getTrait(tempTrait.getTraitId());
                            String displayText = traitDef != null ? traitDef.displayText() : tempTrait.getTraitId();
                            int remainingMinutes = tempTrait.getRemainingTicks(currentTick) / 1200;
                            String timeStr = tempTrait.isPermanent() ? "" : String.format(" (%d min)", remainingMinutes);
                            temporaryTraits.add(displayText + timeStr);
                        }
                    }

                    // Active modifiers
                    for (TemporaryModifier mod : extData.getTemporaryModifiers()) {
                        if (!mod.isExpired(currentTick)) {
                            double currentValue = mod.getCurrentValue(currentTick);
                            int remainingMinutes = mod.getRemainingTicks(currentTick) / 1200;
                            String valueStr;
                            if (mod.getModifierType().equals("happinessBase")) {
                                valueStr = String.format("%+.1f", currentValue);
                            } else {
                                valueStr = String.format("%.0f%%", currentValue * 100);
                            }
                            activeModifiers.add(String.format("%s: %s from %s (%d min)",
                                formatModifierType(mod.getModifierType()),
                                valueStr,
                                mod.getSource(),
                                remainingMinutes));
                        }
                    }
                }

                PacketDistributor.sendToPlayer(player,
                    new BackgroundResponseMessage(msg.citizenId(), backstory, permanentTraits, temporaryTraits, activeModifiers));
            }
        });
    }

    private static String formatModifierType(String type) {
        return switch (type) {
            case "diseaseRate" -> "Disease Rate";
            case "contactDiseaseRate" -> "Contact Disease";
            case "happinessBase" -> "Happiness";
            case "happinessDecayRate" -> "Happiness Decay";
            case "workSpeed" -> "Work Speed";
            case "foodConsumption" -> "Food Consumption";
            default -> type;
        };
    }
}
