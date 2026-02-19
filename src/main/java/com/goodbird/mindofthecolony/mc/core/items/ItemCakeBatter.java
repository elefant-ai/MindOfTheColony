package com.goodbird.mindofthecolony.mc.core.items;

import static com.goodbird.mindofthecolony.mc.api.util.constant.Constants.STACKSIZE;

/**
 * Class handling Cake Batter.
 */
public class ItemCakeBatter extends AbstractItemMinecolonies
{
    /**
     * Sets the name, creative tab, and registers the Cake Batter item.
     *
     * @param properties the properties.
     */
    public ItemCakeBatter(final Properties properties)
    {
        super("cake_batter", properties.stacksTo(STACKSIZE));
    }
}
