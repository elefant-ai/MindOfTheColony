package com.goodbird.mindofthecolony.mc.core.colony.jobs;

import com.ldtteam.structurize.blockentities.interfaces.IBlueprintDataProviderBE;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.storage.StructurePacks;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.api.colony.workorders.IBuilderWorkOrder;
import com.goodbird.mindofthecolony.mc.api.colony.workorders.IWorkOrder;
import com.goodbird.mindofthecolony.mc.api.util.Log;
import com.goodbird.mindofthecolony.mc.api.util.Utils;
import com.goodbird.mindofthecolony.mc.api.util.constant.NbtTagConstants;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.goodbird.mindofthecolony.mc.core.entity.ai.workers.AbstractAISkeleton;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.ldtteam.structurize.blockentities.interfaces.IBlueprintDataProviderBE.TAG_BLUEPRINTDATA;
import static com.ldtteam.structurize.blockentities.interfaces.IBlueprintDataProviderBE.TAG_SCHEMATIC_NAME;

/**
 * Common job object for all structure AIs.
 */
public abstract class AbstractJobStructure<AI extends AbstractAISkeleton<J>, J extends AbstractJobStructure<AI, J>> extends AbstractJob<AI, J>
{
    /**
     * Tag to store the workOrder id.
     */
    public static final String TAG_WORK_ORDER = "workorder";

    /**
     * Initialize citizen data.
     *
     * @param entity the citizen data.
     */
    public AbstractJobStructure(final ICitizenData entity)
    {
        super(entity);
    }


    @Override
    public void deserializeNBT(@NotNull final HolderLookup.Provider provider, final CompoundTag compound)
    {
        super.deserializeNBT(provider, compound);
        if (compound.contains(TAG_WORK_ORDER) && workBuilding instanceof AbstractBuildingStructureBuilder abstractBuildingStructureBuilder)
        {
            abstractBuildingStructureBuilder.setWorkOrderId(compound.getInt(TAG_WORK_ORDER));
        }
    }
}
