package com.goodbird.mindofthecolony.mc.core.colony.interactionhandling.registry;

import com.goodbird.mindofthecolony.mc.api.IMinecoloniesAPI;
import com.goodbird.mindofthecolony.mc.api.colony.interactionhandling.registry.InteractionResponseHandlerEntry;
import net.minecraft.core.Registry;

public interface IInteractionResponseHandlerRegistry
{
    static Registry<InteractionResponseHandlerEntry> getInstance()
    {
        return IMinecoloniesAPI.getInstance().getInteractionResponseHandlerRegistry();
    }
}
