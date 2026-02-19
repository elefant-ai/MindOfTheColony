package com.goodbird.mindofthecolony.mc.core.quests.rewards;

import com.google.gson.JsonObject;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.quests.IQuestInstance;
import com.goodbird.mindofthecolony.mc.api.quests.IQuestRewardTemplate;
import com.goodbird.mindofthecolony.mc.api.util.Utils;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import static com.goodbird.mindofthecolony.mc.api.quests.QuestParseConstant.DETAILS_KEY;
import static com.goodbird.mindofthecolony.mc.api.quests.QuestParseConstant.ITEM_KEY;

/**
 * Item based quest reward.
 */
public class ItemRewardTemplate implements IQuestRewardTemplate
{
    /**
     * The stack to give to the player.
     */
    private final ItemStack item;

    /**
     * Setup the item reward.
     * @param item the item.
     */
    public ItemRewardTemplate(final ItemStack item)
    {
        this.item = item;
    }

    /**
     * Create the reward.
     * @param jsonObject the json to read from.
     * @return the reward object.
     */
    public static IQuestRewardTemplate createReward(@NotNull final HolderLookup.Provider provider, final JsonObject jsonObject)
    {
        JsonObject details = jsonObject.getAsJsonObject(DETAILS_KEY);
        final ItemStack item = Utils.deserializeCodecMessFromJson(ItemStack.CODEC, provider, details.get(ITEM_KEY));
        return new ItemRewardTemplate(item);
    }
    @Override
    public void applyReward(final IColony colony, final Player player, final IQuestInstance colonyQuest)
    {
        player.getInventory().add(item);
    }
}
