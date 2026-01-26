package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.AbstractCivilianEntity;
import com.minecolonies.core.colony.managers.CitizenManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Mixin to hook into citizen lifecycle events.
 * Creates/destroys AI bridges when citizens are loaded/unloaded.
 */
@Mixin(CitizenManager.class)
public abstract class MixinCitizenManager {

    @Shadow @Final private @NotNull Map<Integer, ICitizenData> citizens;

    @Inject(method = "registerCivilian", at = @At("RETURN"), remap = false)
    private void onRegisterCivilian(AbstractCivilianEntity entity, CallbackInfo ci) {
        if (entity != null && entity.getCivilianID() != 0) {
            ICitizenData data = this.citizens.get(entity.getCivilianID());
            CitizenNpcManager.getInstance().onCitizenLoad(data);
        }
    }

    @Inject(method = "unregisterCivilian", at = @At("HEAD"), remap = false)
    private void onRemoveCivilian(AbstractCivilianEntity entity, CallbackInfo ci) {
        if (entity != null) {
            CitizenNpcManager.getInstance().onCitizenUnload(entity.getCivilianID());
        }
    }
}
