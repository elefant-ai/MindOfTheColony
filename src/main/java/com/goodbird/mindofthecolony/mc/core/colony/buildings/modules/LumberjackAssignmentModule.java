package com.goodbird.mindofthecolony.mc.core.colony.buildings.modules;

import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuildingWorkerModule;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.modules.*;
import com.goodbird.mindofthecolony.mc.api.colony.jobs.registry.JobEntry;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.AbstractEntityCitizen;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.Skill;
import com.goodbird.mindofthecolony.mc.core.util.AttributeModifierUtils;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Optional;
import java.util.function.Function;

import static com.goodbird.mindofthecolony.mc.api.util.constant.CitizenConstants.SKILL_BONUS_ADD;
import static com.goodbird.mindofthecolony.mc.api.util.constant.CitizenConstants.SKILL_BONUS_ADD_NAME;

/**
 * Assignment module for foresters.
 */
public class LumberjackAssignmentModule extends CraftingWorkerBuildingModule implements IBuildingEventsModule, ITickingModule, IPersistentModule, IBuildingWorkerModule, ICreatesResolversModule
{
    public LumberjackAssignmentModule(final JobEntry entry,
      final Skill primary,
      final Skill secondary,
      final boolean canWorkingDuringRain,
      final Function<IBuilding, Integer> sizeLimit,
      final Skill craftingSpeedSkill,
      final Skill recipeImprovementSkill)
    {
        super(entry, primary, secondary, canWorkingDuringRain, sizeLimit, craftingSpeedSkill, recipeImprovementSkill);
    }

    @Override
    void onRemoval(final ICitizenData citizen)
    {
        super.onRemoval(citizen);
        final Optional<AbstractEntityCitizen> optCitizen = citizen.getEntity();
        optCitizen.ifPresent(entityCitizen -> AttributeModifierUtils.removeModifier(entityCitizen, SKILL_BONUS_ADD_NAME, Attributes.MOVEMENT_SPEED));
    }
}
