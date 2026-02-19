package com.goodbird.mindofthecolony.mc.core.colony.jobs;

import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.AbstractEntityCitizen;
import com.goodbird.mindofthecolony.mc.core.MineColonies;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.AbstractBuildingGuards;
import com.goodbird.mindofthecolony.mc.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.goodbird.mindofthecolony.mc.core.util.AttributeModifierUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import static com.goodbird.mindofthecolony.mc.api.entity.ai.statemachine.states.AIWorkerState.GUARD_SLEEP;
import static com.goodbird.mindofthecolony.mc.api.util.constant.CitizenConstants.GUARD_HEALTH_MOD_BUILDING_NAME;
import static com.goodbird.mindofthecolony.mc.api.util.constant.CitizenConstants.GUARD_HEALTH_MOD_CONFIG_NAME;

/**
 * Abstract Class for Guard Jobs.
 */
public abstract class AbstractJobGuard<J extends AbstractJobGuard<J>> extends AbstractJob<AbstractEntityAIGuard<J, ? extends AbstractBuildingGuards>, J>
{
    /**
     * Initialize citizen data.
     *
     * @param entity the citizen data.
     */
    public AbstractJobGuard(final ICitizenData entity)
    {
        super(entity);
    }

    protected abstract AbstractEntityAIGuard<J, ? extends AbstractBuildingGuards> generateGuardAI();

    @Override
    public AbstractEntityAIGuard<J, ? extends AbstractBuildingGuards> generateAI()
    {
        return generateGuardAI();
    }

    @Override
    public void triggerDeathAchievement(final DamageSource source, final AbstractEntityCitizen citizen)
    {
        super.triggerDeathAchievement(source, citizen);
    }

    @Override
    public boolean allowsAvoidance()
    {
        return false;
    }

    @Override
    public boolean isGuard()
    {
        return true;
    }

    /**
     * Whether the guard is asleep.
     *
     * @return true if sleeping
     */
    public boolean isAsleep()
    {
        return getWorkerAI() != null && getWorkerAI().getState() == GUARD_SLEEP;
    }

    @Override
    public void initEntityValues(AbstractEntityCitizen citizen)
    {
        super.initEntityValues(citizen);

        final IBuilding workBuilding = citizen.getCitizenData().getWorkBuilding();
        if (workBuilding instanceof AbstractBuildingGuards)
        {
            AttributeModifierUtils.addHealthModifier(citizen,
              new AttributeModifier(GUARD_HEALTH_MOD_BUILDING_NAME, ((AbstractBuildingGuards) workBuilding).getBonusHealth(), AttributeModifier.Operation.ADD_VALUE));
            AttributeModifierUtils.addHealthModifier(citizen,
              new AttributeModifier(GUARD_HEALTH_MOD_CONFIG_NAME,
                MineColonies.getConfig().getServer().guardHealthMult.get() - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @Override
    public double getSaturationFactor()
    {
        return 1.2;
    }
}
