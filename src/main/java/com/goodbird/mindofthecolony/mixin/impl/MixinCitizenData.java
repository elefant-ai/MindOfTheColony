package com.goodbird.mindofthecolony.mixin.impl;

import com.goodbird.mindofthecolony.CitizenAIManager;
import com.goodbird.mindofthecolony.aibridge.CitizenAIBridge;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.core.colony.CitizenData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CitizenData.class)
public class MixinCitizenData implements IExtendedCitizenData {
    @Unique
    private CompoundTag mindOfTheColony$loadedConversationHistoryNBT = null;


    @Inject(method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"), remap = false)
    public void deserializeNBT(CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("aiConversationHistory", Tag.TAG_COMPOUND)) {
            this.mindOfTheColony$loadedConversationHistoryNBT = compound.getCompound("aiConversationHistory");
        }
    }

    @Inject(method = "serializeNBT()Lnet/minecraft/nbt/CompoundTag;", at = @At("TAIL"), cancellable = true, remap = false)
    public void serializeNBT(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag compound = cir.getReturnValue();
        CitizenAIBridge aiBridge = CitizenAIManager.getInstance().getAIBridge(((CitizenData) (Object) this).getId());
        if (aiBridge != null) {
            compound.put("aiConversationHistory", aiBridge.getConversationHistory().serializeNBT());
        } else if (this.mindOfTheColony$loadedConversationHistoryNBT != null) {
            compound.put("aiConversationHistory", this.mindOfTheColony$loadedConversationHistoryNBT);
        }
        cir.setReturnValue(compound);
    }

    @Override
    public CompoundTag getLoadedConversationHistoryNBT() {
        return mindOfTheColony$loadedConversationHistoryNBT;
    }
}
