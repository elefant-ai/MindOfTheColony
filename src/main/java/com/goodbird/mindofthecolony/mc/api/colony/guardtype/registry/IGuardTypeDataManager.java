package com.goodbird.mindofthecolony.mc.api.colony.guardtype.registry;

import com.goodbird.mindofthecolony.mc.api.IMinecoloniesAPI;
import com.goodbird.mindofthecolony.mc.api.colony.guardtype.GuardType;
import net.minecraft.resources.ResourceLocation;

public interface IGuardTypeDataManager
{

    static IGuardTypeDataManager getInstance()
    {
        return IMinecoloniesAPI.getInstance().getGuardTypeDataManager();
    }

    GuardType getFrom(ResourceLocation jobName);
}
