package com.goodbird.mindofthecolony.network;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.background.TraitDefinition;
import com.goodbird.mindofthecolony.background.TraitModifierCalculator;
import com.goodbird.mindofthecolony.background.TraitModifiers;
import com.goodbird.mindofthecolony.background.TraitRegistry;
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
import java.util.Map;

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
                List<String> permanentEffects = new ArrayList<>();
                List<String> temporaryEffects = new ArrayList<>();

                // Extract background data
                if (citizenData instanceof IExtendedCitizenData extData) {
                    CitizenBackground bg = extData.getCitizenBackground();
                    if (bg != null && bg.isInitialized()) {
                        backstory = bg.getBackstory() != null ? bg.getBackstory() : "";

                        // Permanent traits with name and display text
                        for (String traitId : bg.getTraits()) {
                            TraitDefinition traitDef = TraitRegistry.getTrait(traitId);
                            if (traitDef != null) {
                                String traitName = formatTraitName(traitId);
                                permanentTraits.add("[" + traitName + "] " + traitDef.displayText());
                            } else {
                                permanentTraits.add("[" + traitId + "]");
                            }
                        }

                        // Calculate permanent trait effects only
                        TraitModifiers permMods = TraitModifierCalculator.calculate(bg.getTraits());
                        formatModifiers(permMods, permanentEffects);
                    }

                    // Temporary traits with name and display text
                    List<String> tempTraitIds = new ArrayList<>();
                    for (TemporaryTrait tempTrait : extData.getTemporaryTraits()) {
                        if (!tempTrait.isExpired(currentTick)) {
                            TraitDefinition traitDef = TraitRegistry.getTrait(tempTrait.getTraitId());
                            String traitName = formatTraitName(tempTrait.getTraitId());
                            String displayText = traitDef != null ? traitDef.displayText() : tempTrait.getTraitId();
                            int remainingMinutes = tempTrait.getRemainingTicks(currentTick) / 1200;
                            String timeStr = tempTrait.isPermanent() ? "" : String.format(" (%d min)", remainingMinutes);
                            temporaryTraits.add("[" + traitName + "] " + displayText + timeStr);
                            tempTraitIds.add(tempTrait.getTraitId());
                        }
                    }

                    // Calculate temporary trait effects only
                    if (!tempTraitIds.isEmpty()) {
                        TraitModifiers tempMods = TraitModifierCalculator.calculate(tempTraitIds);
                        formatModifiers(tempMods, temporaryEffects);
                    }
                }

                PacketDistributor.sendToPlayer(player,
                    new BackgroundResponseMessage(msg.citizenId(), backstory, permanentTraits, temporaryTraits, permanentEffects, temporaryEffects));
            }
        });
    }

    private static void formatModifiers(TraitModifiers mods, List<String> output) {
        if (mods.workSpeed() != 1.0) {
            int pct = (int)((mods.workSpeed() - 1.0) * 100);
            output.add("Work Speed: " + (pct >= 0 ? "+" : "") + pct + "%");
        }
        if (mods.foodConsumption() != 1.0) {
            int pct = (int)((mods.foodConsumption() - 1.0) * 100);
            output.add("Food Consumption: " + (pct >= 0 ? "+" : "") + pct + "%");
        }
        if (mods.diseaseRate() != 1.0) {
            int pct = (int)((mods.diseaseRate() - 1.0) * 100);
            output.add("Disease Rate: " + (pct >= 0 ? "+" : "") + pct + "%");
        }
        if (mods.contactDiseaseRate() != 1.0) {
            int pct = (int)((mods.contactDiseaseRate() - 1.0) * 100);
            output.add("Contact Disease: " + (pct >= 0 ? "+" : "") + pct + "%");
        }
        if (mods.happinessDecayRate() != 1.0) {
            int pct = (int)((mods.happinessDecayRate() - 1.0) * 100);
            output.add("Happiness Decay: " + (pct >= 0 ? "+" : "") + pct + "%");
        }
        if (mods.happinessBase() != 0.0) {
            output.add("Happiness: " + (mods.happinessBase() >= 0 ? "+" : "") +
                String.format("%.1f", mods.happinessBase()));
        }
        for (Map.Entry<String, Integer> skill : mods.skillBonuses().entrySet()) {
            if (skill.getValue() != 0) {
                String skillName = capitalize(skill.getKey());
                output.add(skillName + ": " + (skill.getValue() >= 0 ? "+" : "") + skill.getValue());
            }
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /**
     * Format a trait ID like "short_temper" into "Short Temper".
     */
    private static String formatTraitName(String traitId) {
        if (traitId == null || traitId.isEmpty()) return traitId;
        String[] words = traitId.split("_");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            result.append(capitalize(words[i]));
        }
        return result.toString();
    }
}
