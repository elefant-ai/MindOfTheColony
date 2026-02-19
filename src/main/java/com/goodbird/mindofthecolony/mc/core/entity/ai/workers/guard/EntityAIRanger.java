package com.goodbird.mindofthecolony.mc.core.entity.ai.workers.guard;

import com.goodbird.mindofthecolony.mc.api.equipment.ModEquipmentTypes;
import com.goodbird.mindofthecolony.mc.api.util.BlockPosUtil;
import com.goodbird.mindofthecolony.mc.api.util.InventoryUtils;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.AbstractBuildingGuards;
import com.goodbird.mindofthecolony.mc.core.colony.jobs.JobRanger;
import com.goodbird.mindofthecolony.mc.core.entity.citizen.EntityCitizen;
import com.goodbird.mindofthecolony.mc.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.goodbird.mindofthecolony.mc.core.entity.pathfinding.pathjobs.PathJobWalkRandomEdge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import static com.goodbird.mindofthecolony.mc.api.entity.ai.statemachine.states.AIWorkerState.IDLE;
import static com.goodbird.mindofthecolony.mc.api.research.util.ResearchConstants.ARCHER_USE_ARROWS;

/**
 * Ranger AI class, which deals with equipment and movement specifics
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class EntityAIRanger extends AbstractEntityAIGuard<JobRanger, AbstractBuildingGuards>
{
    public static final String RENDER_META_ARROW = "arrow";

    public EntityAIRanger(@NotNull final JobRanger job)
    {
        super(job);
        toolsNeeded.add(ModEquipmentTypes.bow.get());
        new RangerCombatAI((EntityCitizen) worker, getStateAI(), this);
    }

    @Override
    protected void updateRenderMetaData()
    {
        String renderMeta = getState() == IDLE ? "" : RENDER_META_WORKING;
        if (worker.getCitizenInventoryHandler().hasItemInInventory(Items.ARROW))
        {
            renderMeta += RENDER_META_ARROW;
        }
        worker.setRenderMetadata(renderMeta);
    }

    @Override
    protected void atBuildingActions()
    {
        super.atBuildingActions();

        if (worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(ARCHER_USE_ARROWS) > 0)
        {
            // Pickup arrows and request arrows
            InventoryUtils.transferXOfFirstSlotInProviderWithIntoNextFreeSlotInItemHandler(building,
              item -> item.getItem() instanceof ArrowItem,
              64,
              worker.getInventoryCitizen());

            if (InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), item -> item.getItem() instanceof ArrowItem) < 16)
            {
                checkIfRequestForItemExistOrCreateAsync(new ItemStack(Items.ARROW), 64, 16);
            }
        }
    }

    @Override
    public void guardMovement()
    {
        if (worker.getRandom().nextInt(30) < 1)
        {
            walkToSafePos(buildingGuards.getGuardPos(worker));
            return;
        }

        if (!worker.getNavigation().isDone())
        {
            return;
        }

        final BlockPos guardPos = buildingGuards.getGuardPos(worker);
        if ((BlockPosUtil.dist(guardPos, worker.blockPosition()) <= 10 || walkToSafePos(guardPos)))
        {
            // Moves the ranger randomly to close edges, for better vision to mobs
             ((MinecoloniesAdvancedPathNavigate) worker.getNavigation()).setPathJob(
                 new PathJobWalkRandomEdge(world, guardPos, 10, worker), null, 1.0, true);
        }
    }
}
