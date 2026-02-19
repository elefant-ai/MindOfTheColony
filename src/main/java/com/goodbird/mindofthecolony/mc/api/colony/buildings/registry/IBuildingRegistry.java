package com.goodbird.mindofthecolony.mc.api.colony.buildings.registry;

import com.goodbird.mindofthecolony.mc.api.IMinecoloniesAPI;
import net.minecraft.core.Registry;

public interface IBuildingRegistry
{

    static Registry<BuildingEntry> getInstance()
    {
        return IMinecoloniesAPI.getInstance().getBuildingRegistry();
    }
}
