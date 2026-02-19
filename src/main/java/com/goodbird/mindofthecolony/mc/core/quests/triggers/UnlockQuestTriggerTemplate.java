package com.goodbird.mindofthecolony.mc.core.quests.triggers;

import com.google.gson.JsonObject;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.quests.IQuestTriggerTemplate;
import com.goodbird.mindofthecolony.mc.api.quests.ITriggerReturnData;
import net.minecraft.resources.ResourceLocation;

/**
 * Unlock quest trigger.
 */
public class UnlockQuestTriggerTemplate implements IQuestTriggerTemplate
{
    /**
     * Create a new instance of this trigger.
     */
    public UnlockQuestTriggerTemplate()
    {

    }

    /**
     * Create a new trigger directly from json.
     * @param ignoreJson the json associated to this trigger.
     */
    public static UnlockQuestTriggerTemplate createUnlockTrigger(final JsonObject ignoreJson)
    {
        return new UnlockQuestTriggerTemplate();
    }

    @Override
    public ITriggerReturnData canTriggerQuest(final IColony colony)
    {
        return new BooleanTriggerReturnData(false);
    }

    @Override
    public ITriggerReturnData canTriggerQuest(final ResourceLocation questId, final IColony colony)
    {
        return new BooleanTriggerReturnData(colony.getQuestManager().isUnlocked(questId));
    }
}
