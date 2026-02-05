package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.background.TraitModifiers;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to apply work speed modifier from traits to worker delays.
 * Higher workSpeed = shorter delays = faster work.
 */
@Mixin(value = AbstractEntityAIBasic.class, remap = false)
public abstract class MixinAbstractEntityAIBasic {

    /**
     * Modify the delay timeout based on workSpeed modifier.
     * Higher workSpeed = shorter delay (faster work).
     */
    @ModifyVariable(method = "setDelay", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int modifyDelayTimeout(int timeout) {
        // Access worker field from parent class via casting
        AbstractEntityAIBasic<?, ?> self = (AbstractEntityAIBasic<?, ?>) (Object) this;
        AbstractEntityCitizen worker = getWorkerFromAI(self);

        if (worker == null) return timeout;

        ICitizenData citizenData = worker.getCitizenData();
        if (citizenData == null) return timeout;

        if (citizenData instanceof IExtendedCitizenData extData) {
            CitizenBackground bg = extData.getCitizenBackground();
            if (bg != null) {
                long currentTick = worker.level().getGameTime();
                TraitModifiers modifiers = bg.getModifiers(
                    extData.getTemporaryTraits(),
                    extData.getTemporaryModifiers(),
                    currentTick
                );
                double workSpeed = modifiers.workSpeed();
                if (workSpeed > 0 && workSpeed != 1.0) {
                    // Faster workSpeed = shorter delay
                    return (int) Math.max(1, timeout / workSpeed);
                }
            }
        }
        return timeout;
    }

    /**
     * Get worker from AI via reflection since it's in a parent class.
     */
    private static AbstractEntityCitizen getWorkerFromAI(AbstractEntityAIBasic<?, ?> ai) {
        try {
            java.lang.reflect.Field workerField = findWorkerField(ai.getClass());
            if (workerField != null) {
                workerField.setAccessible(true);
                return (AbstractEntityCitizen) workerField.get(ai);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private static java.lang.reflect.Field findWorkerField(Class<?> clazz) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField("worker");
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
