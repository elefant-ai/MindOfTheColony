package com.goodbird.mindofthecolony.mc.core.blocks.huts;

import com.goodbird.mindofthecolony.mc.api.blocks.AbstractBlockHut;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Hut for the StoneSmeltery. No different from {@link AbstractBlockHut}
 */
public class BlockHutStoneSmeltery extends AbstractBlockHut<BlockHutStoneSmeltery>
{
    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutstonesmeltery";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.stoneSmelter.get();
    }
}
