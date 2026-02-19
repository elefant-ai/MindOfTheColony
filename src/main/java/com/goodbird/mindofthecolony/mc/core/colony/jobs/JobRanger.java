package com.goodbird.mindofthecolony.mc.core.colony.jobs;

import net.minecraft.resources.ResourceLocation;
import com.goodbird.mindofthecolony.mc.api.client.render.modeltype.ModModelTypes;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.core.entity.ai.workers.guard.EntityAIRanger;

/**
 * The Ranger's Job class
 *
 * @author Asherslab
 */
public class JobRanger extends AbstractJobGuard<JobRanger>
{

    /**
     * The name associated with the job.
     */
    public static final String DESC = "com.goodbird.mindofthecolony.mc.coremod.job.Ranger";

    /**
     * Initialize citizen data.
     *
     * @param entity the citizen data.
     */
    public JobRanger(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public EntityAIRanger generateGuardAI()
    {
        return new EntityAIRanger(this);
    }

    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.ARCHER_GUARD_ID;
    }
}
