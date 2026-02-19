package com.goodbird.mindofthecolony.mc.core.blocks.huts;

import com.goodbird.mindofthecolony.mc.api.blocks.AbstractBlockHut;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Hut for the Smeltery. No different from {@link AbstractBlockHut}
 */
public class BlockHutSmeltery extends AbstractBlockHut<BlockHutSmeltery>
{
    public BlockHutSmeltery()
    {
        //No different from Abstract parent
        super();
    }

    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutsmeltery";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.smeltery.get();
    }
}
