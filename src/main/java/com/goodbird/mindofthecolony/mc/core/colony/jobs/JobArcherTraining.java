package com.goodbird.mindofthecolony.mc.core.colony.jobs;

import net.minecraft.resources.ResourceLocation;
import com.goodbird.mindofthecolony.mc.api.client.render.modeltype.ModModelTypes;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.core.entity.ai.workers.guard.training.EntityAIArcherTraining;

/**
 * The Archers's Training Job class
 */
public class JobArcherTraining extends AbstractJob<EntityAIArcherTraining, JobArcherTraining>
{
    /**
     * Initialize citizen data.
     *
     * @param entity the citizen data.
     */
    public JobArcherTraining(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.ARCHER_GUARD_ID;
    }

    @Override
    public EntityAIArcherTraining generateAI()
    {
        return new EntityAIArcherTraining(this);
    }
}
