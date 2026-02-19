package com.goodbird.mindofthecolony.mc.core.blocks.huts;

import com.goodbird.mindofthecolony.mc.api.blocks.AbstractBlockHut;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.colony.IColonyManager;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;
import com.goodbird.mindofthecolony.mc.api.util.MessageUtils;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.modules.BuildingModules;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.NotNull;

import static com.goodbird.mindofthecolony.mc.api.util.constant.TranslationConstants.WARNING_DUPLICATE_TAVERN;

/**
 * HutBlock for the Tavern
 */
public class BlockHutTavern extends AbstractBlockHut<com.goodbird.mindofthecolony.mc.core.blocks.huts.BlockHutTavern>
{
    /**
     * Block name
     */
    public static final String BLOCKHUT_TAVERN = "blockhuttavern";

    @NotNull
    @Override
    public String getHutName()
    {
        return BLOCKHUT_TAVERN;
    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.tavern.get();
    }

    /**
     * Check if the block can be placed at the given position by the player.
     *
     * @param pos the position to check.
     * @param player the player trying to place the block.
     * @return true if the block can be placed.
     */
    @Override
    public boolean canPlaceAt(final BlockPos pos, final Player player)
    {
        IColony colony = IColonyManager.getInstance().getIColony(player.level(), pos);
        
        for (final IBuilding building : colony.getServerBuildingManager().getBuildings().values())
        {
            if (colony.getWorld() != null && !colony.getWorld().isClientSide && building.hasModule(BuildingModules.TAVERN_VISITOR))
            {
                MessageUtils.format(WARNING_DUPLICATE_TAVERN, building.getPosition().toShortString()).sendTo(player);
                return false;
            }
        }

        return true;
    }
}
