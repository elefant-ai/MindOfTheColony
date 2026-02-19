package com.goodbird.mindofthecolony.mc.api.colony.guardtype.registry;

import com.goodbird.mindofthecolony.mc.api.IMinecoloniesAPI;
import com.goodbird.mindofthecolony.mc.api.colony.guardtype.GuardType;
import net.minecraft.core.Registry;

public interface IGuardTypeRegistry
{

    static Registry<GuardType> getInstance()
    {
        return IMinecoloniesAPI.getInstance().getGuardTypeRegistry();
    }
}
