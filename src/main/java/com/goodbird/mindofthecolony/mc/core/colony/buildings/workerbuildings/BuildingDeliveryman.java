package com.goodbird.mindofthecolony.mc.core.colony.buildings.workerbuildings;

import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.workerbuildings.IBuildingDeliveryman;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.request.IRequest;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.requestable.IRequestable;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.requestable.deliveryman.Delivery;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.AbstractBuilding;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.modules.WorkerBuildingModule;
import com.goodbird.mindofthecolony.mc.core.colony.jobs.JobDeliveryman;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import static com.goodbird.mindofthecolony.mc.api.util.constant.BuildingConstants.CONST_DEFAULT_MAX_BUILDING_LEVEL;
import static com.goodbird.mindofthecolony.mc.core.colony.buildings.modules.BuildingModules.COURIER_WORK;

/**
 * Class of the warehouse building.
 */
public class BuildingDeliveryman extends AbstractBuilding implements IBuildingDeliveryman
{

    private static final String DELIVERYMAN = "deliveryman";

    /**
     * Instantiates a new warehouse building.
     *
     * @param c the colony.
     * @param l the location
     */
    public BuildingDeliveryman(final IColony c, final BlockPos l)
    {
        super(c, l);
    }

    @NotNull
    @Override
    public String getSchematicName()
    {
        return DELIVERYMAN;
    }

    @Override
    public int getMaxBuildingLevel()
    {
        return CONST_DEFAULT_MAX_BUILDING_LEVEL;
    }

    @Override
    public boolean canEat(final ItemStack stack)
    {
        final ICitizenData citizenData = getModule(COURIER_WORK).getFirstCitizen();
        if (citizenData != null)
        {
            final JobDeliveryman job = (JobDeliveryman) citizenData.getJob();
            final IRequest<? extends IRequestable> currentTask = job.getCurrentTask();
            if (currentTask == null)
            {
                return super.canEat(stack);
            }
            final IRequestable request = currentTask.getRequest();
            if (request instanceof Delivery && ItemStack.isSameItem(((Delivery) request).getStack(), stack))
            {
                return false;
            }
        }
        return super.canEat(stack);
    }
}
