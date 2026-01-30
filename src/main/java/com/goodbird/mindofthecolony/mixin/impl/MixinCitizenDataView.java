package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.background.TraitModifiers;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenDataView;
import com.minecolonies.core.colony.CitizenDataView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Mixin to add trait modifiers to CitizenDataView for client-side display.
 */
@Mixin(value = CitizenDataView.class, remap = false)
public class MixinCitizenDataView implements IExtendedCitizenDataView {

    @Unique
    private TraitModifiers mindOfTheColony$traitModifiers = TraitModifiers.DEFAULT;

    @Override
    public TraitModifiers getTraitModifiers() {
        return mindOfTheColony$traitModifiers;
    }

    @Override
    public void setTraitModifiers(TraitModifiers modifiers) {
        this.mindOfTheColony$traitModifiers = modifiers;
    }

    /**
     * Deserialize trait modifiers from the network buffer.
     */
    @Inject(method = "deserialize", at = @At("TAIL"))
    private void onDeserialize(RegistryFriendlyByteBuf buf, CallbackInfo ci) {
        // Check if our mod data is present
        if (buf.isReadable() && buf.readBoolean()) {
            double diseaseRate = buf.readDouble();
            double contactDiseaseRate = buf.readDouble();
            double happinessBase = buf.readDouble();
            double happinessDecayRate = buf.readDouble();
            double workSpeed = buf.readDouble();
            double foodConsumption = buf.readDouble();

            // Read skill bonuses
            int skillCount = buf.readInt();
            Map<String, Integer> skillBonuses = new HashMap<>();
            for (int i = 0; i < skillCount; i++) {
                String skill = buf.readUtf();
                int bonus = buf.readInt();
                skillBonuses.put(skill, bonus);
            }

            this.mindOfTheColony$traitModifiers = new TraitModifiers(
                diseaseRate,
                contactDiseaseRate,
                happinessBase,
                happinessDecayRate,
                workSpeed,
                foodConsumption,
                Map.copyOf(skillBonuses)
            );
        }
    }
}
