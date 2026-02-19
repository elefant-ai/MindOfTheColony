package com.goodbird.mindofthecolony.mc.core.blocks.huts;

import com.goodbird.mindofthecolony.mc.api.blocks.AbstractBlockHut;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Hut for the Large quarry. No different from {@link AbstractBlockHut}
 */
public class LargeQuarry extends AbstractBlockHut<LargeQuarry>
{
    public LargeQuarry()
    {
        //No different from Abstract parent
        super();
    }

    @NotNull
    @Override
    public String getHutName()
    {
        return "largequarry";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return null;//ModBuildings.largeQuarry;
    }
}
