package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.client.TabbedDebugWindow;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.AbstractWindowSkeleton;
import com.minecolonies.core.client.gui.citizen.AbstractWindowCitizen;
import com.minecolonies.core.debug.DebugPlayerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to redirect debug tab to our TabbedDebugWindow.
 */
@Mixin(AbstractWindowCitizen.class)
public abstract class MixinAbstractWindowCitizen extends AbstractWindowSkeleton {

    @Shadow @Final protected ICitizenDataView citizen;

    // Constructor required by AbstractWindowSkeleton but never called for mixin
    protected MixinAbstractWindowCitizen(ResourceLocation resource) {
        super(resource);
    }

    /**
     * Override the debug button registration to use TabbedDebugWindow.
     * Injected at the end of constructor to re-register buttons after MineColonies does.
     */
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void onInitTail(ICitizenDataView citizen, ResourceLocation ui, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (DebugPlayerManager.hasDebugEnabled(mc.player)) {
            // Re-register the debug buttons to open our TabbedDebugWindow instead
            // The registerButton method is inherited from AbstractWindowSkeleton
            this.registerButton("debugTab", () -> new TabbedDebugWindow(citizen).open());
            this.registerButton("debugIcon", () -> new TabbedDebugWindow(citizen).open());
        }
    }
}
