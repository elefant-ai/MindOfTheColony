package com.goodbird.mindofthecolony.mc.core.blocks.huts;

import com.goodbird.mindofthecolony.mc.api.blocks.AbstractBlockHut;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Hut for the school. No different from {@link AbstractBlockHut}
 */
public class BlockHutSchool extends AbstractBlockHut<BlockHutSchool>
{
    public BlockHutSchool()
    {
        //No different from Abstract parent
        super();
    }

    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutschool";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.school.get();
    }
}
