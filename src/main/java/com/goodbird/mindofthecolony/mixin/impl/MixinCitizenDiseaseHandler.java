package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.config.DiseaseConfig;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.datalistener.model.Disease;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenDiseaseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static com.minecolonies.api.research.util.ResearchConstants.MASKS;
import static com.minecolonies.api.util.constant.Constants.ONE_HUNDRED_PERCENT;

/**
 * Mixin to modify disease handling.
 * - Removes hardcoded healer immunity (uses configurable modifier instead)
 * - Applies job-specific contact spread modifiers
 */
@Mixin(value = CitizenDiseaseHandler.class, remap = false)
public abstract class MixinCitizenDiseaseHandler {

    @Shadow
    @Final
    private ICitizenData citizenData;

    @Shadow
    private Disease disease;

    @Shadow
    private int immunityTicks;

    @Shadow
    public abstract boolean isSick();

    /**
     * Initial citizen count from config.
     */
    private static final int initialCitizenCount = IMinecoloniesAPI.getInstance()
            .getConfig()
            .getServer().initialCitizenAmount.get();

    /**
     * @author MindOfTheColony
     * @reason Remove hardcoded healer immunity - use configurable modifier instead
     */
    @Overwrite
    private boolean canBecomeSick() {
        // Removed: && !(citizenData.getJob() instanceof JobHealer)
        // Healers now use a very low modifier (e.g., 0.05) instead of being immune
        return !isSick()
                && citizenData.getEntity().isPresent()
                && citizenData.getColony().isActive()
                && immunityTicks <= 0
                && citizenData.getColony().getCitizenManager().getCurrentCitizenCount() > initialCitizenCount;
    }

    /**
     * @author MindOfTheColony
     * @reason Apply job and trait contact spread modifiers
     */
    @Overwrite
    public void onCollission(final ICitizenData citizen) {
        if (citizen.getCitizenDiseaseHandler().isSick()
                && canBecomeSick()) {

            // Get the job-specific contact modifier
            String jobId = getJobId();
            double contactModifier = DiseaseConfig.getContactModifier(jobId);

            // Apply trait-based contact modifier
            if (citizenData instanceof IExtendedCitizenData extData) {
                CitizenBackground bg = extData.getCitizenBackground();
                if (bg != null) {
                    contactModifier *= bg.getContactDiseaseRateModifier();
                }
            }

            // Base chance is 1% (1 in 100), apply contact modifier
            double effectiveChance = contactModifier;

            if (citizen.getRandom().nextDouble() * ONE_HUNDRED_PERCENT < effectiveChance) {
                if (citizen.getColony().getResearchManager().getResearchEffects().getEffectStrength(MASKS) <= 0
                        || citizen.getRandom().nextBoolean()) {
                    this.disease = citizen.getCitizenDiseaseHandler().getDisease();
                }
            }
        }
    }

    /**
     * Get the job ID for the citizen, or "default" if no job.
     */
    private String getJobId() {
        if (citizenData.getJob() != null) {
            String jobName = citizenData.getJob().getJobRegistryEntry().getTranslationKey();
            // Extract job name from translation key like "com.minecolonies.job.healer"
            if (jobName.contains(".")) {
                return jobName.substring(jobName.lastIndexOf(".") + 1).toLowerCase();
            }
            return jobName.toLowerCase();
        }
        return "default";
    }
}
