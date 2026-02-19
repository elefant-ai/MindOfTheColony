package com.goodbird.mindofthecolony.mc.core.items;

import static com.goodbird.mindofthecolony.mc.api.util.constant.Constants.STACKSIZE;

/**
 * Class handling Bread Dough.
 */
public class ItemBreadDough extends AbstractItemMinecolonies
{
    /**
     * Sets the name, creative tab, and registers the Bread Dough item.
     *
     * @param properties the properties.
     */
    public ItemBreadDough(final Properties properties)
    {
        super("bread_dough", properties.stacksTo(STACKSIZE));
    }
}
