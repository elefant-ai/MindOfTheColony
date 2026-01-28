package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.client.ClientBackgroundCache;
import com.goodbird.mindofthecolony.network.BackgroundRequestMessage;
import com.ldtteam.blockui.controls.Text;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.debug.gui.DebugWindowCitizen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to display citizen background info in the debug window.
 */
@Mixin(DebugWindowCitizen.class)
public abstract class MixinDebugWindowCitizen {

    @Unique
    private int mindOfTheColony$citizenId = -1;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void onInit(ICitizenDataView citizen, CallbackInfo ci) {
        mindOfTheColony$citizenId = citizen.getId();

        // Request background data from server
        PacketDistributor.sendToServer(
            new BackgroundRequestMessage(citizen.getColonyId(), citizen.getId()));
    }

    @Inject(method = "onUpdate", at = @At("TAIL"), remap = false)
    private void onUpdateInject(CallbackInfo ci) {
        String bgInfo = ClientBackgroundCache.get(mindOfTheColony$citizenId);
        if (bgInfo != null) {
            DebugWindowCitizen self = (DebugWindowCitizen)(Object) this;
            Text output = self.findPaneOfTypeByID("output", Text.class);
            MutableComponent combined = Component.literal(bgInfo + "\n")
                .append(DebugWindowCitizen.outputMessage);
            output.setText(combined);
        }
    }
}
