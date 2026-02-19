package com.goodbird.mindofthecolony.mc.core.quests.rewards;

import com.google.gson.JsonObject;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.happiness.ExpirationBasedHappinessModifier;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.happiness.StaticHappinessSupplier;
import com.goodbird.mindofthecolony.mc.api.quests.IQuestInstance;
import com.goodbird.mindofthecolony.mc.api.quests.IQuestRewardTemplate;
import com.goodbird.mindofthecolony.mc.core.colony.CitizenData;
import net.minecraft.world.entity.player.Player;

import static com.goodbird.mindofthecolony.mc.api.util.constant.HappinessConstants.QUEST;
import static com.goodbird.mindofthecolony.mc.api.quests.QuestParseConstant.*;

/**
 * Happiness inducing reward.
 */
public class HappinessRewardTemplate implements IQuestRewardTemplate
{
    /**
     * Happiness boost.
     */
    private final int qty;

    /**
     * Days to hold.
     */
    private final int days;

    /**
     * Target to affect.
     */
    private final int target;

    /**
     * Setup the research reward.
     */
    public HappinessRewardTemplate(final int target, final int qty, final int days)
    {
        this.target = target;
        this.qty = qty;
        this.days = days;
    }

    /**
     * Create the reward.
     * @param jsonObject the json to read from.
     * @return the reward object.
     */
    public static IQuestRewardTemplate createReward(final JsonObject jsonObject)
    {
        JsonObject details = jsonObject.getAsJsonObject(DETAILS_KEY);
        final int target = details.get(TARGET_KEY).getAsInt();
        final int qty = details.get(QUANTITY_KEY).getAsInt();
        final int days = details.get(DAYS_KEY).getAsInt();

        return new HappinessRewardTemplate(target, qty, days);
    }

    @Override
    public void applyReward(final IColony colony, final Player player, final IQuestInstance colonyQuest)
    {
        if (this.target == 0)
        {
            ((CitizenData) colonyQuest.getQuestGiver()).getCitizenHappinessHandler().addModifier(new ExpirationBasedHappinessModifier(QUEST, 2.0, new StaticHappinessSupplier(qty), days));
        }
        else
        {
            ((CitizenData) colonyQuest.getParticipant(this.target)).getCitizenHappinessHandler().addModifier(new ExpirationBasedHappinessModifier(QUEST, 2.0, new StaticHappinessSupplier(qty), days));
        }
    }
}
