package com.goodbird.mindofthecolony.mc.core.blocks.huts;

import com.goodbird.mindofthecolony.mc.api.blocks.AbstractBlockHut;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Hut for the university. No different from {@link AbstractBlockHut}
 */
public class BlockHutUniversity extends AbstractBlockHut<BlockHutUniversity>
{
    public BlockHutUniversity()
    {
        //No different from Abstract parent
        super();
    }

    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutuniversity";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.university.get();
    }
}
