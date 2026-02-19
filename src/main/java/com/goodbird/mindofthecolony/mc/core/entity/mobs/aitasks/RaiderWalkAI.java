package com.goodbird.mindofthecolony.mc.core.entity.mobs.aitasks;

import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.api.colony.colonyEvents.EventStatus;
import com.goodbird.mindofthecolony.mc.api.colony.colonyEvents.IColonyEvent;
import com.goodbird.mindofthecolony.mc.api.colony.colonyEvents.IColonyRaidEvent;
import com.goodbird.mindofthecolony.mc.api.entity.ai.IStateAI;
import com.goodbird.mindofthecolony.mc.api.entity.ai.combat.CombatAIStates;
import com.goodbird.mindofthecolony.mc.api.entity.ai.statemachine.states.IState;
import com.goodbird.mindofthecolony.mc.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.goodbird.mindofthecolony.mc.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import com.goodbird.mindofthecolony.mc.api.entity.mobs.AbstractEntityMinecoloniesRaider;
import com.goodbird.mindofthecolony.mc.api.util.Log;
import com.goodbird.mindofthecolony.mc.core.colony.events.raid.HordeRaidEvent;
import com.goodbird.mindofthecolony.mc.core.colony.events.raid.pirateEvent.ShipBasedRaiderUtils;
import com.goodbird.mindofthecolony.mc.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;

import java.util.List;

import static com.goodbird.mindofthecolony.mc.api.util.constant.Constants.TICKS_SECOND;

/**
 * AI for handling the raiders walking directions
 */
public class RaiderWalkAI implements IStateAI
{
    /**
     * The entity using this AI
     */
    private final AbstractEntityMinecoloniesRaider raider;

    /**
     * Target block we're walking to
     */
    private BlockPos targetBlock = null;

    /**
     * Campfire walk timer
     */
    private long walkTimer = 0;

    /**
     * Building the raider is checking out
     */
    private IBuilding walkInBuilding = null;

    public RaiderWalkAI(final AbstractEntityMinecoloniesRaider raider, final ITickRateStateMachine<IState> stateMachine)
    {
        this.raider = raider;
        stateMachine.addTransition(new TickingTransition<>(CombatAIStates.NO_TARGET, this::walk, () -> null, 80));
    }

    /**
     * Walk raider towards the colony or campfires
     *
     * @return
     */
    private boolean walk()
    {
        if (raider.getColony() != null)
        {
            final IColonyEvent event = raider.getColony().getEventManager().getEventByID(raider.getEventID());
            if (event == null)
            {
                return false;
            }

            if (event.getStatus() == EventStatus.PREPARING && event instanceof HordeRaidEvent)
            {
                walkToCampFire();
                return false;
            }
            raider.setTempEnvDamageImmunity(false);

            if (targetBlock == null || raider.level().getGameTime() > walkTimer)
            {
                targetBlock = raider.getColony().getRaiderManager().getRandomBuilding();
                walkTimer = raider.level().getGameTime() + TICKS_SECOND * 240;

                final List<BlockPos> wayPoints = ((IColonyRaidEvent) event).getWayPoints();
                final BlockPos moveToPos = ShipBasedRaiderUtils.chooseWaypointFor(wayPoints, raider.blockPosition(), targetBlock);
                EntityNavigationUtils.walkToPos(raider, moveToPos, 4, false, !moveToPos.equals(targetBlock) && moveToPos.distManhattan(wayPoints.get(0)) > 50 ? 1.8 : 1.1);
                walkInBuilding = null;
            }
            else if (walkInBuilding != null)
            {
                if (EntityNavigationUtils.walkToRandomPosWithin(raider, 10, 0.7, walkInBuilding.getCorners())
                    && raider.getRandom().nextDouble() < 0.25)
                {
                    walkInBuilding = null;
                    targetBlock = null;
                }

                if (raider.getNavigation().getPathResult() != null)
                {
                    raider.getNavigation().getPathResult().getJob().getPathingOptions().withCanEnterDoors(true).withCanEnterGates(true).withToggleCost(0).withNonLadderClimbableCost(0);
                }
            }
            else if (raider.blockPosition().distSqr(targetBlock) < 25)
            {
                walkTimer = raider.level().getGameTime() + TICKS_SECOND * 30;
                walkInBuilding = raider.getColony().getServerBuildingManager().getBuilding(targetBlock);
            }
            else if (raider.getNavigation().isDone())
            {
                final List<BlockPos> wayPoints = ((IColonyRaidEvent) event).getWayPoints();
                final BlockPos moveToPos = ShipBasedRaiderUtils.chooseWaypointFor(wayPoints, raider.blockPosition(), targetBlock);

                if (moveToPos.equals(BlockPos.ZERO))
                {
                    Log.getLogger().warn("Raider trying to path to zero position, target pos:" + targetBlock + " Waypoints:");
                    for (final BlockPos pos : wayPoints)
                    {
                        Log.getLogger().warn(pos.toShortString());
                    }
                }

                EntityNavigationUtils.walkToPos(raider, moveToPos, 7, true, !moveToPos.equals(targetBlock) && moveToPos.distManhattan(wayPoints.get(0)) > 50 ? 1.8 : 1.1);
            }
        }

        return false;
    }

    /**
     * Chooses and walks to a random campfire
     */
    private void walkToCampFire()
    {
        if (raider.level().getGameTime() - walkTimer < 0)
        {
            return;
        }

        final BlockPos campFire = ((HordeRaidEvent) raider.getColony().getEventManager().getEventByID(raider.getEventID())).getRandomCampfire();

        if (campFire == null)
        {
            return;
        }

        walkTimer = raider.level().getGameTime() + raider.level().random.nextInt(1000);
        EntityNavigationUtils.walkToRandomPosAround(raider, campFire, 10, 0.7);
    }
}

