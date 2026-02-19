package com.goodbird.mindofthecolony.mc.core.entity.mobs.aitasks;

import com.goodbird.mindofthecolony.mc.api.entity.ai.IStateAI;
import com.goodbird.mindofthecolony.mc.api.entity.ai.combat.CombatAIStates;
import com.goodbird.mindofthecolony.mc.api.entity.ai.statemachine.states.IState;
import com.goodbird.mindofthecolony.mc.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.goodbird.mindofthecolony.mc.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import com.goodbird.mindofthecolony.mc.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.goodbird.mindofthecolony.mc.api.entity.pathfinding.IPathJob;
import com.goodbird.mindofthecolony.mc.core.entity.pathfinding.navigation.EntityNavigationUtils;
import com.goodbird.mindofthecolony.mc.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;

import static com.goodbird.mindofthecolony.mc.api.util.constant.Constants.TICKS_SECOND;

/**
 * AI for handling the raiders walking directions
 */
public class CampWalkAI implements IStateAI
{
    /**
     * The entity using this AI
     */
    private final AbstractEntityMinecoloniesMonster entity;

    /**
     * Random path result.
     */
    private PathResult<? extends IPathJob> randomPathResult;

    /**
     * Spawn center box cache.
     */
    private Tuple<BlockPos, BlockPos> spawnCenterBoxCache = null;

    public CampWalkAI(final AbstractEntityMinecoloniesMonster raider, final ITickRateStateMachine<IState> stateMachine)
    {
        this.entity = raider;
        stateMachine.addTransition(new TickingTransition<>(CombatAIStates.NO_TARGET, this::walk, () -> null, TICKS_SECOND * 30));
    }

    /**
     * Walk camp mob randomly
     */
    private boolean walk()
    {
        if (spawnCenterBoxCache == null)
        {
            final BlockPos startPos = entity.getSpawnPos() == null ? entity.blockPosition() : entity.getSpawnPos();
            spawnCenterBoxCache = new Tuple<>(startPos.offset(-10, -5, -10), startPos.offset(10, 5, 10));
        }

        EntityNavigationUtils.walkToRandomPosWithin(entity, 10, 0.6, spawnCenterBoxCache);
        return false;
    }
}
