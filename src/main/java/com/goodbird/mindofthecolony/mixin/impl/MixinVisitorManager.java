package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.goodbird.mindofthecolony.background.BackgroundGenerationService;
import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.IVisitorData;
import com.minecolonies.core.colony.managers.VisitorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to generate backgrounds for visitors when they are created.
 */
@Mixin(VisitorManager.class)
public class MixinVisitorManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MixinVisitorManager.class);

    @Inject(method = "createAndRegisterCivilianData", at = @At("RETURN"), remap = false)
    private void onVisitorCreated(CallbackInfoReturnable<IVisitorData> cir) {
        IVisitorData visitorData = cir.getReturnValue();

        if (visitorData instanceof IExtendedCitizenData extData) {
            CitizenBackground existing = extData.getCitizenBackground();

            // Only generate if no background exists
            if (existing == null || !existing.isInitialized()) {
                String gameId = CitizenNpcManager.getInstance().getGameId();

                if (gameId != null) {
                    LOGGER.info("Generating AI background for new visitor: {}", visitorData.getName());

                    BackgroundGenerationService.getInstance().generateBackground(visitorData, gameId)
                        .thenAccept(background -> {
                            extData.setCitizenBackground(background);
                            LOGGER.info("Generated background for visitor {}: backstory='{}...', traits={}",
                                visitorData.getName(),
                                background.getBackstory() != null
                                    ? background.getBackstory().substring(0, Math.min(50, background.getBackstory().length()))
                                    : "none",
                                background.getTraits());
                        })
                        .exceptionally(ex -> {
                            LOGGER.error("Failed to generate background for visitor {}: {}",
                                visitorData.getName(), ex.getMessage());
                            return null;
                        });
                } else {
                    LOGGER.warn("Cannot generate background for visitor {} - NPC system not initialized",
                        visitorData.getName());
                }
            }
        }
    }
}
