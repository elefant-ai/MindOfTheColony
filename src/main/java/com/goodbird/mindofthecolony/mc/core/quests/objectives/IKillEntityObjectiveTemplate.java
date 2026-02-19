package com.goodbird.mindofthecolony.mc.core.quests.objectives;

import com.goodbird.mindofthecolony.mc.api.quests.IQuestInstance;
import com.goodbird.mindofthecolony.mc.api.quests.IObjectiveInstance;
import net.minecraft.world.entity.player.Player;

/**
 * Specific objective for entity killing.
 */
public interface IKillEntityObjectiveTemplate
{
    /**
     * Callback for entity kill event
     *
     * @param progressData the objective data.
     * @param player the involved player.
     */
    void onEntityKill(IObjectiveInstance progressData, final IQuestInstance colonyQuest, final Player player);
}
