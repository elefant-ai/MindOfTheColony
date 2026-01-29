package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.core.colony.CitizenData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to extend CitizenData with conversation history and background persistence.
 */
@Mixin(CitizenData.class)
public class MixinCitizenData implements IExtendedCitizenData {
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
}
