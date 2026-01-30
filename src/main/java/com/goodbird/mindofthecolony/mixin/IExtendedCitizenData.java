package com.goodbird.mindofthecolony.mixin;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.effect.TemporaryModifier;
import com.goodbird.mindofthecolony.effect.TemporaryTrait;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public interface IExtendedCitizenData {
    CompoundTag getLoadedConversationHistoryNBT();

    CitizenBackground getCitizenBackground();

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
}
