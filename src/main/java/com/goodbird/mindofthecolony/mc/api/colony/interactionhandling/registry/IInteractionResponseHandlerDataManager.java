package com.goodbird.mindofthecolony.mc.api.colony.interactionhandling.registry;

import com.goodbird.mindofthecolony.mc.api.IMinecoloniesAPI;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizen;
import com.goodbird.mindofthecolony.mc.api.colony.interactionhandling.IInteractionResponseHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The data manager of the interaction handler.
 */
public interface IInteractionResponseHandlerDataManager
{

    static IInteractionResponseHandlerDataManager getInstance()
    {
        return IMinecoloniesAPI.getInstance().getInteractionResponseHandlerDataManager();
    }

    /**
     * Create an interactionResponseHandler from saved CompoundTag data.
     *
     * @param citizen  The citizen that owns the interaction response handler..
     * @param compound The CompoundTag containing the saved interaction data.
     * @return New InteractionResponseHandler created from the data, or null.
     */
    @Nullable
    IInteractionResponseHandler createFrom(@NotNull final HolderLookup.Provider provider, @NotNull ICitizen citizen, @NotNull CompoundTag compound);
}
