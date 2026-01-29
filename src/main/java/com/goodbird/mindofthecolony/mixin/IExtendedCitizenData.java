package com.goodbird.mindofthecolony.mixin;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import net.minecraft.nbt.CompoundTag;

public interface IExtendedCitizenData {
    CompoundTag getLoadedConversationHistoryNBT();

    CitizenBackground getCitizenBackground();

    void setCitizenBackground(CitizenBackground background);
}
