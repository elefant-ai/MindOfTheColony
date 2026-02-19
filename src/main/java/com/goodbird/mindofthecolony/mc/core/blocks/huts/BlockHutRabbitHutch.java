package com.goodbird.mindofthecolony.mc.core.blocks.huts;

import com.goodbird.mindofthecolony.mc.api.blocks.AbstractBlockHut;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Hut for the rabbit hutch. No different from {@link AbstractBlockHut}
 */
public class BlockHutRabbitHutch extends AbstractBlockHut<BlockHutRabbitHutch>
{
    @NotNull
    @Override
    public String getHutName()
    {
        return "blockhutrabbithutch";
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.rabbitHutch.get();
    }
}
