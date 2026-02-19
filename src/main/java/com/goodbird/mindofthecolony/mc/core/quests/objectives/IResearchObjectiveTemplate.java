package com.goodbird.mindofthecolony.mc.core.quests.objectives;

import com.goodbird.mindofthecolony.mc.api.quests.IQuestInstance;

/**
 * Specific objective for research tracking.
 */
public interface IResearchObjectiveTemplate
{
    /**
     * Callback for research completion event.
     * @param questInstance the quest instance.
     */
    void onResearchCompletion(final IQuestInstance questInstance);
}
