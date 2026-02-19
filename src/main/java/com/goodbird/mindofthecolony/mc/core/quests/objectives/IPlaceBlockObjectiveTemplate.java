package com.goodbird.mindofthecolony.mc.core.quests.objectives;

import com.goodbird.mindofthecolony.mc.api.quests.IObjectiveInstance;
import com.goodbird.mindofthecolony.mc.api.quests.IQuestInstance;
import net.minecraft.world.entity.player.Player;

/**
 * Specific objective for block placing.
 */
public interface IPlaceBlockObjectiveTemplate
{
    /**
     * Callback for block place event
     *
     * @param blockPlacementProgressData the objective data.
     * @param player the involved player.
     */
    void onBlockPlace(IObjectiveInstance blockPlacementProgressData, final IQuestInstance colonyQuest, final Player player);
}
