package com.goodbird.mindofthecolony.mixin;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

public interface IExtendedCitizenData {
    CompoundTag getLoadedConversationHistoryNBT();

    @Nullable
    UUID getNpcId();

    CitizenBackground getCitizenBackground();

    void setNpcId(@Nullable UUID npcId);

    void setCitizenBackground(CitizenBackground background);
}
