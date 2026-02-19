package com.goodbird.mindofthecolony.mc.core.entity.ai.workers.guard;

import com.goodbird.mindofthecolony.mc.api.crafting.ItemStorage;
import com.goodbird.mindofthecolony.mc.api.entity.ai.workers.util.GuardGear;
import com.goodbird.mindofthecolony.mc.api.equipment.ModEquipmentTypes;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.AbstractBuildingGuards;
import com.goodbird.mindofthecolony.mc.core.colony.jobs.JobKnight;
import com.goodbird.mindofthecolony.mc.core.entity.citizen.EntityCitizen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.goodbird.mindofthecolony.mc.api.research.util.ResearchConstants.SHIELD_USAGE;
import static com.goodbird.mindofthecolony.mc.api.util.constant.EquipmentLevelConstants.TOOL_LEVEL_MAXIMUM;
import static com.goodbird.mindofthecolony.mc.api.util.constant.EquipmentLevelConstants.TOOL_LEVEL_WOOD_OR_GOLD;
import static com.goodbird.mindofthecolony.mc.api.util.constant.GuardConstants.SHIELD_BUILDING_LEVEL_RANGE;
import static com.goodbird.mindofthecolony.mc.api.util.constant.GuardConstants.SHIELD_LEVEL_RANGE;

/**
 * Knight AI, which deals with gear specifics
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class EntityAIKnight extends AbstractEntityAIGuard<JobKnight, AbstractBuildingGuards>
{
    public EntityAIKnight(@NotNull final JobKnight job)
    {
        super(job);
        super.registerTargets();

        toolsNeeded.add(ModEquipmentTypes.sword.get());

        for (final List<GuardGear> list : itemsNeeded)
        {
            list.add(new GuardGear(ModEquipmentTypes.shield.get(),
              EquipmentSlot.OFFHAND,
              TOOL_LEVEL_WOOD_OR_GOLD,
              TOOL_LEVEL_MAXIMUM,
              SHIELD_LEVEL_RANGE,
              SHIELD_BUILDING_LEVEL_RANGE));
        }

        new KnightCombatAI((EntityCitizen) worker, getStateAI(), this);
    }

    @NotNull
    @Override
    protected List<ItemStorage> itemsNiceToHave()
    {
        final List<ItemStorage> list = super.itemsNiceToHave();
        if (worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(SHIELD_USAGE) > 0)
        {
            list.add(new ItemStorage(Items.SHIELD, 1));
        }
        return list;
    }
}
