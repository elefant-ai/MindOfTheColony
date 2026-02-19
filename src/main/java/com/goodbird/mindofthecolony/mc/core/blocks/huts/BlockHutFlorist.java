package com.goodbird.mindofthecolony.mc.core.blocks.huts;

import com.goodbird.mindofthecolony.mc.api.blocks.AbstractBlockHut;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

public class BlockHutFlorist extends AbstractBlockHut<BlockHutFlorist>
{
    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutflorist";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.florist.get();
    }
}
