package com.goodbird.mindofthecolony.mixin;

import net.minecraft.nbt.CompoundTag;

public interface IExtendedCitizenData {
    CompoundTag getLoadedConversationHistoryNBT();
}
