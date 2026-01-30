package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.background.TraitModifiers;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenSkillHandler;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenSkillHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to apply skill bonuses from traits to citizen skill levels.
 */
@Mixin(value = CitizenSkillHandler.class, remap = false)
public class MixinCitizenSkillHandler implements IExtendedCitizenSkillHandler {

    @Unique
    private ICitizenData mindOfTheColony$citizen;

    @Override
    public void mindOfTheColony$setCitizen(ICitizenData citizen) {
        this.mindOfTheColony$citizen = citizen;
    }

    @Override
    public ICitizenData mindOfTheColony$getCitizen() {
        return mindOfTheColony$citizen;
    }

    /**
     * Apply trait skill bonus to the returned level.
     */
    @Inject(method = "getLevel", at = @At("RETURN"), cancellable = true)
    private void applyTraitSkillBonus(Skill skill, CallbackInfoReturnable<Integer> cir) {
        if (mindOfTheColony$citizen == null) return;

        if (mindOfTheColony$citizen instanceof IExtendedCitizenData extData) {
            CitizenBackground bg = extData.getCitizenBackground();
            if (bg != null) {
                long currentTick = 0;
                if (mindOfTheColony$citizen.getColony() != null &&
                    mindOfTheColony$citizen.getColony().getWorld() != null) {
                    currentTick = mindOfTheColony$citizen.getColony().getWorld().getGameTime();
                }

                TraitModifiers modifiers = bg.getModifiers(
                    extData.getTemporaryTraits(),
                    extData.getTemporaryModifiers(),
                    currentTick
                );

                int traitBonus = modifiers.getSkillBonus(skill.name());
                if (traitBonus != 0) {
                    int baseLevel = cir.getReturnValue();
                    // Apply bonus but ensure minimum level of 1
                    cir.setReturnValue(Math.max(1, baseLevel + traitBonus));
                }
            }
        }
    }
}
