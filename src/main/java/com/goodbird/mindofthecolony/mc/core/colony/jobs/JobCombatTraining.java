package com.goodbird.mindofthecolony.mc.core.colony.jobs;

import net.minecraft.resources.ResourceLocation;
import com.goodbird.mindofthecolony.mc.api.client.render.modeltype.ModModelTypes;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.core.entity.ai.workers.guard.training.EntityAICombatTraining;

/**
 * The Knight's Training Job class
 */
public class JobCombatTraining extends AbstractJob<EntityAICombatTraining, JobCombatTraining>
{
    /**
     * Initialize citizen data.
     *
     * @param entity the citizen data.
     */
    public JobCombatTraining(final ICitizenData entity)
    {
        super(entity);
    }


    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.KNIGHT_GUARD_ID;
    }

    @Override
    public EntityAICombatTraining generateAI()
    {
        return new EntityAICombatTraining(this);
    }
}
