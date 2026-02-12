package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.goodbird.mindofthecolony.background.BackgroundGenerationService;
import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.background.TraitDefinition;
import com.goodbird.mindofthecolony.background.TraitModifierCalculator;
import com.goodbird.mindofthecolony.background.TraitModifiers;
import com.goodbird.mindofthecolony.background.TraitRegistry;
import com.goodbird.mindofthecolony.config.DiseaseConfig;
import com.goodbird.mindofthecolony.effect.TemporaryModifier;
import com.goodbird.mindofthecolony.effect.TemporaryTrait;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenSkillHandler;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.IBuildingWorkerModule;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenFoodHandler;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenSkillHandler;
import com.minecolonies.core.colony.CitizenData;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.UUID;

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
    @Shadow public abstract IColony getColony();

    @Unique
    private CompoundTag mindOfTheColony$loadedConversationHistoryNBT = null;

    @Unique
    private CitizenBackground mindOfTheColony$citizenBackground = null;

    @Unique
    private UUID mindOfTheColony$npcId = null;

    @Unique
    private final List<TemporaryModifier> mindOfTheColony$temporaryModifiers = new ArrayList<>();

    @Unique
    private final List<TemporaryTrait> mindOfTheColony$temporaryTraits = new ArrayList<>();

    @Unique
    private String mindOfTheColony$voiceId = null;

    @Inject(method = "deserializeNBT", at = @At("TAIL"), remap = false)
    private void onDeserializeNBT(HolderLookup.Provider provider, CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("aiConversationHistory", Tag.TAG_COMPOUND)) {
            this.mindOfTheColony$loadedConversationHistoryNBT = compound.getCompound("aiConversationHistory");
        }
        if (compound.contains("aiNpcId", Tag.TAG_STRING)) {
            String npcIdStr = compound.getString("aiNpcId");
            try {
                this.mindOfTheColony$npcId = UUID.fromString(npcIdStr);
            } catch (IllegalArgumentException e) {
                this.mindOfTheColony$npcId = null;
            }
        }
        if (compound.contains("aiVoiceId", Tag.TAG_STRING)) {
            this.mindOfTheColony$voiceId = compound.getString("aiVoiceId");
        }
        if (compound.contains("citizenBackground", Tag.TAG_COMPOUND)) {
            this.mindOfTheColony$citizenBackground = CitizenBackground.fromNBT(compound.getCompound("citizenBackground"));
        }
        // Load temporary modifiers
        this.mindOfTheColony$temporaryModifiers.clear();
        if (compound.contains("temporaryModifiers", Tag.TAG_LIST)) {
            ListTag modifierList = compound.getList("temporaryModifiers", Tag.TAG_COMPOUND);
            for (int i = 0; i < modifierList.size(); i++) {
                this.mindOfTheColony$temporaryModifiers.add(TemporaryModifier.fromNBT(modifierList.getCompound(i)));
            }
        }
        // Load temporary traits
        this.mindOfTheColony$temporaryTraits.clear();
        if (compound.contains("temporaryTraits", Tag.TAG_LIST)) {
            ListTag traitList = compound.getList("temporaryTraits", Tag.TAG_COMPOUND);
            for (int i = 0; i < traitList.size(); i++) {
                this.mindOfTheColony$temporaryTraits.add(TemporaryTrait.fromNBT(traitList.getCompound(i)));
            }
        }

        // Set citizen reference on skill handler for trait bonuses
        mindOfTheColony$setSkillHandlerCitizen();

        // If background is missing or uninitialized, trigger generation
        // This catches citizens saved before async generation completed
        if (mindOfTheColony$citizenBackground == null || !mindOfTheColony$citizenBackground.isInitialized()) {
            CitizenData self = (CitizenData)(Object)this;
            String gameId = CitizenNpcManager.getInstance().getGameId();
            if (gameId != null) {
                org.slf4j.LoggerFactory.getLogger(MixinCitizenData.class)
                    .info("Citizen {} loaded with uninitialized background, triggering generation", self.getName());
                BackgroundGenerationService.getInstance().generateBackground(self, gameId)
                    .thenAccept(background -> {
                        setCitizenBackground(background);
                        org.slf4j.LoggerFactory.getLogger(MixinCitizenData.class)
                            .info("Generated background for citizen {} on load", self.getName());
                    })
                    .exceptionally(ex -> {
                        org.slf4j.LoggerFactory.getLogger(MixinCitizenData.class)
                            .error("Failed to generate background for citizen {} on load: {}",
                                self.getName(), ex.getMessage());
                        // Broadcast error to all players
                        IColony colony = getColony();
                        if (colony != null && colony.getWorld() != null) {
                            String errorMsg = "§c[MindOfTheColony] Failed to generate background for " + self.getName() + ": " + ex.getMessage();
                            for (net.minecraft.world.entity.player.Player player : colony.getWorld().players()) {
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(errorMsg));
                            }
                        }
                        return null;
                    });
            }
        }
    }

    /**
     * Set the citizen reference on the skill handler.
     */
    @Unique
    private void mindOfTheColony$setSkillHandlerCitizen() {
        CitizenData self = (CitizenData)(Object)this;
        if (getCitizenSkillHandler() instanceof IExtendedCitizenSkillHandler extHandler) {
            extHandler.mindOfTheColony$setCitizen(self);
        }
    }

    /**
     * Also set citizen reference when initStats is called (for newly created citizens).
     */
    @Inject(method = "initStats", at = @At("TAIL"), remap = false)
    private void onInitStats(CallbackInfo ci) {
        mindOfTheColony$setSkillHandlerCitizen();
    }

    /**
     * Hook into initForNewCivilian to trigger background generation for newly created citizens.
     * This catches babies born mid-game and newly recruited citizens.
     */
    @Inject(method = "initForNewCivilian", at = @At("TAIL"), remap = false)
    private void onInitForNewCivilian(CallbackInfo ci) {
        CitizenData self = (CitizenData)(Object)this;
        // Trigger background generation via CitizenNpcManager
        CitizenNpcManager.getInstance().onCitizenLoad(self);
    }

    @Inject(method = "serializeNBT", at = @At("RETURN"), remap = false)
    private void onSerializeNBT(HolderLookup.Provider provider, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag compound = cir.getReturnValue();
        if (this.mindOfTheColony$loadedConversationHistoryNBT != null) {
            compound.put("aiConversationHistory", this.mindOfTheColony$loadedConversationHistoryNBT);
        }
        if (this.mindOfTheColony$citizenBackground != null) {
            compound.put("citizenBackground", this.mindOfTheColony$citizenBackground.toNBT());
        }
        // Save NPC ID for restoring memories on reload
        if (this.mindOfTheColony$npcId != null) {
            compound.putString("aiNpcId", this.mindOfTheColony$npcId.toString());
        }
        if (this.mindOfTheColony$voiceId != null) {
            compound.putString("aiVoiceId", this.mindOfTheColony$voiceId);
        }
        // Save temporary modifiers
        if (!this.mindOfTheColony$temporaryModifiers.isEmpty()) {
            ListTag modifierList = new ListTag();
            for (TemporaryModifier mod : this.mindOfTheColony$temporaryModifiers) {
                modifierList.add(mod.toNBT());
            }
            compound.put("temporaryModifiers", modifierList);
        }
        // Save temporary traits
        if (!this.mindOfTheColony$temporaryTraits.isEmpty()) {
            ListTag traitList = new ListTag();
            for (TemporaryTrait trait : this.mindOfTheColony$temporaryTraits) {
                traitList.add(trait.toNBT());
            }
            compound.put("temporaryTraits", traitList);
        }
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

        // Apply skill bonuses from permanent traits
        if (background != null && background.isInitialized()) {
            for (String traitId : background.getTraits()) {
                mindOfTheColony$applyTraitSkillBonuses(traitId);
            }
        }

        // Mark dirty to ensure the background is saved
        CitizenData self = (CitizenData)(Object)this;
        self.markDirty(0);
    }

    @Override
    public List<TemporaryModifier> getTemporaryModifiers() {
        return new ArrayList<>(mindOfTheColony$temporaryModifiers);
    }

    @Override
    public void addTemporaryModifier(TemporaryModifier modifier) {
        mindOfTheColony$temporaryModifiers.add(modifier);
    }

    @Override
    public void removeExpiredModifiers(long currentTick) {
        mindOfTheColony$temporaryModifiers.removeIf(mod -> mod.isExpired(currentTick));
    }

    @Override
    public List<TemporaryTrait> getTemporaryTraits() {
        return new ArrayList<>(mindOfTheColony$temporaryTraits);
    }

    @Override
    public void addTemporaryTrait(TemporaryTrait trait) {
        // Remove existing trait with same ID if present (and its skill bonuses)
        TemporaryTrait existing = mindOfTheColony$temporaryTraits.stream()
            .filter(t -> t.getTraitId().equals(trait.getTraitId()))
            .findFirst()
            .orElse(null);
        if (existing != null) {
            mindOfTheColony$removeTraitSkillBonuses(existing.getTraitId());
            mindOfTheColony$temporaryTraits.remove(existing);
        }

        mindOfTheColony$temporaryTraits.add(trait);

        // Apply skill bonuses from the new trait
        mindOfTheColony$applyTraitSkillBonuses(trait.getTraitId());
    }

    @Override
    public void removeTemporaryTrait(String traitId) {
        boolean removed = mindOfTheColony$temporaryTraits.removeIf(t -> t.getTraitId().equals(traitId));
        if (removed) {
            mindOfTheColony$removeTraitSkillBonuses(traitId);
        }
    }

    @Override
    public void removeExpiredTraits(long currentTick) {
        // Collect expired traits first so we can remove their bonuses
        List<TemporaryTrait> expired = mindOfTheColony$temporaryTraits.stream()
            .filter(trait -> trait.isExpired(currentTick))
            .toList();

        for (TemporaryTrait trait : expired) {
            mindOfTheColony$removeTraitSkillBonuses(trait.getTraitId());
        }

        mindOfTheColony$temporaryTraits.removeIf(trait -> trait.isExpired(currentTick));
    }

    /**
     * Apply skill bonuses from a trait to the citizen's skill handler.
     */
    @Unique
    private void mindOfTheColony$applyTraitSkillBonuses(String traitId) {
        TraitDefinition trait = TraitRegistry.getTrait(traitId);
        if (trait == null || trait.modifiers() == null) {
            return;
        }

        boolean appliedAny = false;
        if (getCitizenSkillHandler() instanceof IExtendedCitizenSkillHandler extHandler) {
            for (Map.Entry<String, Double> entry : trait.modifiers().entrySet()) {
                String key = entry.getKey().toLowerCase();
                if (TraitModifierCalculator.isSkillModifier(key)) {
                    int bonus = entry.getValue().intValue();
                    extHandler.mindOfTheColony$applySkillBonus(key, bonus);
                    appliedAny = true;
                }
            }
        }

        // Trigger sync to client
        if (appliedAny) {
            CitizenData self = (CitizenData)(Object)this;
            self.markDirty(0);
        }
    }

    /**
     * Remove skill bonuses from a trait from the citizen's skill handler.
     */
    @Unique
    private void mindOfTheColony$removeTraitSkillBonuses(String traitId) {
        TraitDefinition trait = TraitRegistry.getTrait(traitId);
        if (trait == null || trait.modifiers() == null) {
            return;
        }

        boolean removedAny = false;
        if (getCitizenSkillHandler() instanceof IExtendedCitizenSkillHandler extHandler) {
            for (Map.Entry<String, Double> entry : trait.modifiers().entrySet()) {
                String key = entry.getKey().toLowerCase();
                if (TraitModifierCalculator.isSkillModifier(key)) {
                    int bonus = entry.getValue().intValue();
                    extHandler.mindOfTheColony$applySkillBonus(key, -bonus); // Negative to remove
                    removedAny = true;
                }
            }
        }

        // Trigger sync to client
        if (removedAny) {
            CitizenData self = (CitizenData)(Object)this;
            self.markDirty(0);
        }
    }

    @Override
    public boolean hasTemporaryTrait(String traitId) {
        return mindOfTheColony$temporaryTraits.stream().anyMatch(t -> t.getTraitId().equals(traitId));
    }

    /**
     * @author MindOfTheColony
     * @reason Allow disease modifier from job config and trait modifiers
     */
    @Overwrite(remap = false)
    public double getDiseaseModifier() {
        String jobId = mindOfTheColony$getJobId();

        // Nether workers are immune while invisible (vanilla behavior)
        if ("netherworker".equals(jobId) && getEntity().isPresent() && getEntity().get().isInvisible()) {
            return 0;
        }

        // Get job-based modifier
        double jobModifier = DiseaseConfig.getJobModifier(jobId);

        // Composter/Crusher use config base * skill factor (vanilla behavior)
        if ("composter".equals(jobId) || "crusher".equals(jobId)) {
            jobModifier *= mindOfTheColony$getSkillFactor();
        }

        // Apply trait-based modifier
        if (mindOfTheColony$citizenBackground != null) {
            jobModifier *= mindOfTheColony$citizenBackground.getDiseaseRateModifier();
        }

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

    /**
     * Apply foodConsumption modifier after saturation decrease.
     * Higher foodConsumption = more hunger (additional saturation decrease).
     */
    @Inject(method = "decreaseSaturation", at = @At("TAIL"), remap = false)
    private void applyFoodConsumptionModifier(double extraSaturation, CallbackInfo ci) {
        if (mindOfTheColony$citizenBackground != null) {
            long currentTick = 0;
            IColony colony = getColony();
            if (colony != null && colony.getWorld() != null) {
                currentTick = colony.getWorld().getGameTime();
            }

            TraitModifiers modifiers = mindOfTheColony$citizenBackground.getModifiers(
                mindOfTheColony$temporaryTraits,
                mindOfTheColony$temporaryModifiers,
                currentTick
            );
            double foodConsumption = modifiers.foodConsumption();
            if (foodConsumption > 0 && foodConsumption != 1.0) {
                // Apply additional decrease based on trait modifier
                // If foodConsumption is 1.3, we need to decrease by an extra 30%
                // If foodConsumption is 0.75, we need to add back 25%
                CitizenData self = (CitizenData)(Object)this;
                double configModifier = com.minecolonies.core.MineColonies.getConfig().getServer().foodModifier.get();
                double baseDecrease = Math.abs(extraSaturation * configModifier);
                double additionalChange = baseDecrease * (foodConsumption - 1.0);

                // Access saturation field via reflection or direct field access
                // Since we're in a mixin, we can shadow the field
                mindOfTheColony$adjustSaturation(-additionalChange);
            }
        }
    }

    @Unique
    private void mindOfTheColony$adjustSaturation(double amount) {
        CitizenData self = (CitizenData)(Object)this;
        // We need to access the saturation field - let's use the increase/decrease methods
        if (amount > 0) {
            self.increaseSaturation(amount);
        } else if (amount < 0) {
            // For negative, we can't directly decrease again without looping
            // Let's just use reflection to access the field
            try {
                java.lang.reflect.Field satField = CitizenData.class.getDeclaredField("saturation");
                satField.setAccessible(true);
                double currentSat = satField.getDouble(self);
                satField.setDouble(self, Math.max(0, currentSat + amount));
            } catch (Exception e) {
                // Ignore - not critical
            }
        }
    }

    /**
     * Serialize trait modifiers to network for client-side display.
     */
    @Inject(method = "serializeViewNetworkData", at = @At("TAIL"), remap = false)
    private void onSerializeViewNetworkData(RegistryFriendlyByteBuf buf, CallbackInfo ci) {
        // Send modifiers if we have a background OR temporary traits/modifiers
        boolean hasData = mindOfTheColony$citizenBackground != null ||
                          !mindOfTheColony$temporaryTraits.isEmpty() ||
                          !mindOfTheColony$temporaryModifiers.isEmpty();

        if (hasData) {
            buf.writeBoolean(true);

            long currentTick = 0;
            IColony colony = getColony();
            if (colony != null && colony.getWorld() != null) {
                currentTick = colony.getWorld().getGameTime();
            }

            TraitModifiers modifiers;
            if (mindOfTheColony$citizenBackground != null) {
                modifiers = mindOfTheColony$citizenBackground.getModifiers(
                    mindOfTheColony$temporaryTraits,
                    mindOfTheColony$temporaryModifiers,
                    currentTick
                );
            } else {
                // No permanent background, but calculate from temporary traits/modifiers only
                modifiers = com.goodbird.mindofthecolony.background.TraitModifierCalculator.calculate(
                    java.util.List.of(),  // No permanent traits
                    mindOfTheColony$temporaryTraits,
                    mindOfTheColony$temporaryModifiers,
                    currentTick
                );
            }

            buf.writeDouble(modifiers.diseaseRate());
            buf.writeDouble(modifiers.contactDiseaseRate());
            buf.writeDouble(modifiers.happinessBase());
            buf.writeDouble(modifiers.happinessDecayRate());
            buf.writeDouble(modifiers.workSpeed());
            buf.writeDouble(modifiers.foodConsumption());

            // Write skill bonuses
            var skillBonuses = modifiers.skillBonuses();
            buf.writeInt(skillBonuses.size());
            for (var entry : skillBonuses.entrySet()) {
                buf.writeUtf(entry.getKey());
                buf.writeInt(entry.getValue());
            }
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    @Nullable
    public UUID getNpcId() {
        return mindOfTheColony$npcId;
    }

    @Override
    public void setNpcId(@Nullable UUID npcId) {
        this.mindOfTheColony$npcId = npcId;
    }

    @Override
    @Nullable
    public String getVoiceId() {
        return mindOfTheColony$voiceId;
    }

    @Override
    public void setVoiceId(@Nullable String voiceId) {
        this.mindOfTheColony$voiceId = voiceId;
        CitizenData self = (CitizenData)(Object)this;
        self.markDirty(0);
    }
}
