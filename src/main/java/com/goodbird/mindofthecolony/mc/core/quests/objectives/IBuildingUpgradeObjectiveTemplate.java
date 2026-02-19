package com.goodbird.mindofthecolony.mc.core.quests.objectives;

import com.goodbird.mindofthecolony.mc.api.quests.IObjectiveInstance;
import com.goodbird.mindofthecolony.mc.api.quests.IQuestInstance;

/**
 * Specific objective for building upgrade.
 */
public interface IBuildingUpgradeObjectiveTemplate
{
    /**
     * Callback for block upgrade event
     *
     * @param progressData the objective data.
     * @param colonyQuest  the quest.
     * @param level        reached lvl.
     */
    void onBuildingUpgrade(IObjectiveInstance progressData, final IQuestInstance colonyQuest, final int level);
}
