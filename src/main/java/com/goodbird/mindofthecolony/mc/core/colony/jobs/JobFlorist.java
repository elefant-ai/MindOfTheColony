package com.goodbird.mindofthecolony.mc.core.colony.jobs;

import net.minecraft.resources.ResourceLocation;
import com.goodbird.mindofthecolony.mc.api.client.render.modeltype.ModModelTypes;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.core.entity.ai.workers.production.agriculture.EntityAIWorkFlorist;
import org.jetbrains.annotations.NotNull;

public class JobFlorist extends AbstractJob<EntityAIWorkFlorist, JobFlorist>
{
    /**
     * Initialize citizen data.
     *
     * @param entity the citizen data.
     */
    public JobFlorist(final ICitizenData entity)
    {
        super(entity);
    }

    @NotNull
    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.FLORIST_ID;
    }

    @Override
    public EntityAIWorkFlorist generateAI()
    {
        return new EntityAIWorkFlorist(this);
    }
}
