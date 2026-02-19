package com.goodbird.mindofthecolony.mc.core.blocks.huts;

import com.goodbird.mindofthecolony.mc.api.blocks.AbstractBlockHut;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Hut for the miner. No different from {@link AbstractBlockHut}
 */

public class BlockHutMiner extends AbstractBlockHut<BlockHutMiner>
{
    public BlockHutMiner()
    {
        //No different from Abstract parent
        super();
    }

    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutminer";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.miner.get();
    }
}
