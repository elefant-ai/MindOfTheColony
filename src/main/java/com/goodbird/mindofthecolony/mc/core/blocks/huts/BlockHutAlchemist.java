package com.goodbird.mindofthecolony.mc.core.blocks.huts;

import com.goodbird.mindofthecolony.mc.api.blocks.AbstractBlockHut;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Alchemist hut block.
 */
public class BlockHutAlchemist extends AbstractBlockHut<BlockHutAlchemist>
{
    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutalchemist";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.alchemist.get();
    }
}
