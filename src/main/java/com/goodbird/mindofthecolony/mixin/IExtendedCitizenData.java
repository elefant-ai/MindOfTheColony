package com.goodbird.mindofthecolony.mixin;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.effect.TemporaryModifier;
import com.goodbird.mindofthecolony.effect.TemporaryTrait;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public interface IExtendedCitizenData {
    CompoundTag getLoadedConversationHistoryNBT();

    @Nullable
    UUID getNpcId();

    CitizenBackground getCitizenBackground();

    void setNpcId(@Nullable UUID npcId);

    void setCitizenBackground(CitizenBackground background);

    // Temporary effects
    List<TemporaryModifier> getTemporaryModifiers();

    void addTemporaryModifier(TemporaryModifier modifier);

    void removeExpiredModifiers(long currentTick);

    List<TemporaryTrait> getTemporaryTraits();

    void addTemporaryTrait(TemporaryTrait trait);

    void removeTemporaryTrait(String traitId);

    void removeExpiredTraits(long currentTick);

    /**
     * Check if this citizen has a specific temporary trait.
     */
    boolean hasTemporaryTrait(String traitId);

    @Nullable
    String getVoiceId();

    void setVoiceId(@Nullable String voiceId);
}
