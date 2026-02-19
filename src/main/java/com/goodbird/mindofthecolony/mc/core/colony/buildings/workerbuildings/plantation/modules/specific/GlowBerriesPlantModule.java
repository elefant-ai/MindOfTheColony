package com.goodbird.mindofthecolony.mc.core.colony.buildings.workerbuildings.plantation.modules.specific;

import com.goodbird.mindofthecolony.mc.api.colony.buildingextensions.IBuildingExtension;
import com.goodbird.mindofthecolony.mc.api.equipment.ModEquipmentTypes;
import com.goodbird.mindofthecolony.mc.api.equipment.registry.EquipmentTypeEntry;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.workerbuildings.plantation.modules.generic.DownwardsGrowingPlantModule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.goodbird.mindofthecolony.mc.api.research.util.ResearchConstants.PLANTATION_EXOTIC;

/**
 * Planter module for growing {@link Items#GLOW_BERRIES}.
 * <br/>
 * Requirements:
 * <ol>
 *     <li>All requirements from {@link DownwardsGrowingPlantModule}</li>
 * </ol>
 */
public class GlowBerriesPlantModule extends DownwardsGrowingPlantModule
{
    /**
     * Default constructor.
     *
     * @param field    the field instance this module is working on.
     * @param fieldTag the tag of the field anchor block.
     * @param workTag  the tag of the working positions.
     * @param item     the item which is harvested.
     */
    public GlowBerriesPlantModule(final IBuildingExtension field, final String fieldTag, final String workTag, final Item item)
    {
        super(field, fieldTag, workTag, item);
    }

    @Override
    protected boolean isValidHarvestBlock(final BlockState blockState)
    {
        return blockState.getBlock() == Blocks.CAVE_VINES || blockState.getBlock() == Blocks.CAVE_VINES_PLANT;
    }

    @Override
    public ResourceLocation getRequiredResearchEffect()
    {
        return PLANTATION_EXOTIC;
    }

    @Override
    public EquipmentTypeEntry getRequiredTool()
    {
        return ModEquipmentTypes.none.get();
    }
}
