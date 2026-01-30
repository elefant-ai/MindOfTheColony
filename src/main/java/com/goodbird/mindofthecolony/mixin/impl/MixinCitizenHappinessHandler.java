package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.background.TraitModifiers;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenHappinessHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to apply happinessBase modifier from traits to citizen happiness.
 * Adds a flat bonus/penalty to the final happiness value.
 */
@Mixin(value = CitizenHappinessHandler.class, remap = false)
public class MixinCitizenHappinessHandler {

    /**
     * Apply happinessBase modifier after the happiness calculation.
     * This adds a flat bonus/penalty to the final happiness value.
     */
    @Inject(method = "getHappiness", at = @At("RETURN"), cancellable = true)
    private void applyHappinessBaseModifier(IColony colony, ICitizenData citizenData,
                                            CallbackInfoReturnable<Double> cir) {
        if (citizenData instanceof IExtendedCitizenData extData) {
            CitizenBackground bg = extData.getCitizenBackground();
            if (bg != null) {
                long currentTick = 0;
                if (colony != null && colony.getWorld() != null) {
                    currentTick = colony.getWorld().getGameTime();
                }

                TraitModifiers modifiers = bg.getModifiers(
                    extData.getTemporaryTraits(),
                    extData.getTemporaryModifiers(),
                    currentTick
                );

                double happinessBase = modifiers.happinessBase();
                if (happinessBase != 0.0) {
                    double currentHappiness = cir.getReturnValue();
                    // Apply additive modifier and clamp to [0, 10]
                    double newHappiness = Math.max(0, Math.min(10, currentHappiness + happinessBase));
                    cir.setReturnValue(newHappiness);
                }
            }
        }
    }
}
