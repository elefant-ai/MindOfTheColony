package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.config.DiseaseConfig;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.IBuildingWorkerModule;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenFoodHandler;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenSkillHandler;
import com.minecolonies.core.colony.CitizenData;

import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to extend CitizenData with conversation history and background persistence.
 */
@Mixin(CitizenData.class)
public abstract class MixinCitizenData implements IExtendedCitizenData {

    @Shadow public abstract IJob<?> getJob();
    @Shadow public abstract ICitizenFoodHandler getCitizenFoodHandler();
    @Shadow public abstract Optional<AbstractEntityCitizen> getEntity();
    @Shadow public abstract ICitizenSkillHandler getCitizenSkillHandler();
    @Shadow public abstract IBuilding getWorkBuilding();

    @Unique
    private CompoundTag mindOfTheColony$loadedConversationHistoryNBT = null;

    @Unique
    private CitizenBackground mindOfTheColony$citizenBackground = null;

    @Inject(method = "deserializeNBT(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"), remap = false)
    public void deserializeNBT(@NotNull HolderLookup.Provider provider, CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("aiConversationHistory", Tag.TAG_COMPOUND)) {
            this.mindOfTheColony$loadedConversationHistoryNBT = compound.getCompound("aiConversationHistory");
        }
        if (compound.contains("citizenBackground", Tag.TAG_COMPOUND)) {
            this.mindOfTheColony$citizenBackground = CitizenBackground.fromNBT(compound.getCompound("citizenBackground"));
        }
    }

    @Inject(method = "serializeNBT(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;", at = @At("TAIL"), cancellable = true, remap = false)
    public void serializeNBT(@NotNull HolderLookup.Provider provider, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag compound = cir.getReturnValue();
        if (this.mindOfTheColony$loadedConversationHistoryNBT != null) {
            compound.put("aiConversationHistory", this.mindOfTheColony$loadedConversationHistoryNBT);
        }
        if (this.mindOfTheColony$citizenBackground != null) {
            compound.put("citizenBackground", this.mindOfTheColony$citizenBackground.toNBT());
        }
        cir.setReturnValue(compound);
    }

    @Override
    public CompoundTag getLoadedConversationHistoryNBT() {
        return mindOfTheColony$loadedConversationHistoryNBT;
    }

    @Override
    public CitizenBackground getCitizenBackground() {
        return mindOfTheColony$citizenBackground;
    }

    @Override
    public void setCitizenBackground(CitizenBackground background) {
        this.mindOfTheColony$citizenBackground = background;
    }

    /**
     * @author MindOfTheColony
     * @reason Allow any job's disease modifier to be configured via TOML
     */
    @Overwrite(remap = false)
    public double getDiseaseModifier() {
        String jobId = mindOfTheColony$getJobId();

        // Nether workers are immune while invisible (vanilla behavior)
        if ("netherworker".equals(jobId) && getEntity().isPresent() && getEntity().get().isInvisible()) {
            return 0;
        }

        // Composter/Crusher use config base * skill factor (vanilla behavior)
        if ("composter".equals(jobId) || "crusher".equals(jobId)) {
            double baseModifier = DiseaseConfig.getJobModifier(jobId);
            double skillFactor = mindOfTheColony$getSkillFactor();
            return getCitizenFoodHandler().getDiseaseModifier(baseModifier * skillFactor);
        }

        double jobModifier = DiseaseConfig.getJobModifier(jobId);
        return getCitizenFoodHandler().getDiseaseModifier(jobModifier);
    }

    @Unique
    private double mindOfTheColony$getSkillFactor() {
        IBuilding building = getWorkBuilding();
        if (building == null) return 1.0;

        for (Object module : building.getModules()) {
            if (module instanceof IBuildingWorkerModule workerModule) {
                int skill = getCitizenSkillHandler().getLevel(workerModule.getPrimarySkill());
                return (int) ((100 - skill) / 25.0);
            }
        }
        return 1.0;
    }

    @Unique
    private String mindOfTheColony$getJobId() {
        IJob<?> job = getJob();
        if (job == null) {
            return "default";
        }
        String translationKey = job.getJobRegistryEntry().getTranslationKey();
        // Extract job name from "com.minecolonies.job.healer" -> "healer"
        if (translationKey.contains(".")) {
            return translationKey.substring(translationKey.lastIndexOf(".") + 1).toLowerCase();
        }
        return translationKey.toLowerCase();
    }
}
