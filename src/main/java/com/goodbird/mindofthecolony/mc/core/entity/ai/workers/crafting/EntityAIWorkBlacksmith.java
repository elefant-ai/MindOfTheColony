package com.goodbird.mindofthecolony.mc.core.entity.ai.workers.crafting;

import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.request.IRequest;
import com.goodbird.mindofthecolony.mc.api.crafting.IRecipeStorage;
import com.goodbird.mindofthecolony.mc.api.util.StatsUtil;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.workerbuildings.BuildingBlacksmith;
import com.goodbird.mindofthecolony.mc.core.colony.jobs.JobBlacksmith;
import org.jetbrains.annotations.NotNull;

import static com.goodbird.mindofthecolony.mc.api.util.constant.StatisticsConstants.ITEMS_CRAFTED_DETAIL;

/**
 * Crafts tools and armour.
 */
public class EntityAIWorkBlacksmith extends AbstractEntityAICrafting<JobBlacksmith, BuildingBlacksmith>
{
    /**
     * Initialize the blacksmith and add all his tasks.
     *
     * @param blacksmith the job he has.
     */
    public EntityAIWorkBlacksmith(@NotNull final JobBlacksmith blacksmith)
    {
        super(blacksmith);
    }

    @Override
    public Class<BuildingBlacksmith> getExpectedBuildingClass()
    {
        return BuildingBlacksmith.class;
    }

}
