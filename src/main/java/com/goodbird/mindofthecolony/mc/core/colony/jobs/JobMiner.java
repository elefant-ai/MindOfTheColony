package com.goodbird.mindofthecolony.mc.core.colony.jobs;

import com.goodbird.mindofthecolony.mc.api.client.render.modeltype.ModModelTypes;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.core.entity.ai.workers.production.EntityAIStructureMiner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import org.jetbrains.annotations.NotNull;

import static com.goodbird.mindofthecolony.mc.api.research.util.ResearchConstants.FIRE_DAMAGE_PREDICATE;
import static com.goodbird.mindofthecolony.mc.api.research.util.ResearchConstants.FIRE_RES;

/**
 * Special miner job. Defines miner model and specialized job behaviour.
 */
public class JobMiner extends AbstractJobStructure<EntityAIStructureMiner, JobMiner>
{
    /**
     * Creates a new instance of the miner job.
     *
     * @param entity the entity to add the job to.
     */
    public JobMiner(final ICitizenData entity)
    {
        super(entity);
    }

    @NotNull
    @Override
    public ResourceLocation getModel()
    {
        return ModModelTypes.MINER_ID;
    }

    /**
     * Generate your AI class to register.
     *
     * @return your personal AI instance.
     */
    @NotNull
    @Override
    public EntityAIStructureMiner generateAI()
    {
        return new EntityAIStructureMiner(this);
    }

    @Override
    public double getDiseaseModifier()
    {
        return 2;
    }

    @Override
    public boolean ignoresDamage(@NotNull final DamageSource damageSource)
    {
        if (damageSource.typeHolder().is(FIRE_DAMAGE_PREDICATE))
        {
            return getColony().getResearchManager().getResearchEffects().getEffectStrength(FIRE_RES) > 0;
        }

        return super.ignoresDamage(damageSource);
    }

    @Override
    public double getSaturationFactor()
    {
        return 1.2;
    }
}
