package com.goodbird.mindofthecolony.mc.core.blocks.huts;

import com.goodbird.mindofthecolony.mc.api.blocks.AbstractBlockHut;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;

import org.jetbrains.annotations.NotNull;

/**
 * Hut for the concrete mason. No different from {@link AbstractBlockHut}
 */
public class BlockHutConcreteMixer extends AbstractBlockHut<BlockHutConcreteMixer>
{
    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutconcretemixer";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.concreteMixer.get();
    }
}
