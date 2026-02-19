package com.goodbird.mindofthecolony.mc.core.quests.rewards;

import com.google.gson.JsonObject;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.quests.IQuestInstance;
import com.goodbird.mindofthecolony.mc.api.quests.IQuestRewardTemplate;
import net.minecraft.world.entity.player.Player;

import static com.goodbird.mindofthecolony.mc.api.quests.QuestParseConstant.*;

/**
 * Quest reputation reward template.
 */
public class QuestReputationRewardTemplate implements IQuestRewardTemplate
{
    /**
     * The reputation quantity.
     */
    private final double quantity;

    /**
     * Setup the quest reputation reward.
     */
    public QuestReputationRewardTemplate(final double quantity)
    {
        this.quantity = quantity;
    }

    /**
     * Create the reward.
     * @param jsonObject the json to read from.
     * @return the reward object.
     */
    public static IQuestRewardTemplate createReward(final JsonObject jsonObject)
    {
        JsonObject details = jsonObject.getAsJsonObject(DETAILS_KEY);
        final double qty = details.get(QUANTITY_KEY).getAsDouble();
        return new QuestReputationRewardTemplate(qty);
    }

    @Override
    public void applyReward(final IColony colony, final Player player, final IQuestInstance colonyQuest)
    {
        colony.getQuestManager().alterReputation(this.quantity);
    }
}
