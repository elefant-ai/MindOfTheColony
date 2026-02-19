package com.goodbird.mindofthecolony.mc.core.items;

import static com.goodbird.mindofthecolony.mc.api.util.constant.Constants.STACKSIZE;

/**
 * Class handling Cookie Dough.
 */
public class ItemCookieDough extends AbstractItemMinecolonies
{
    /**
     * Sets the name, creative tab, and registers the Cookie Dough item.
     *
     * @param properties the properties.
     */
    public ItemCookieDough(final Properties properties)
    {
        super("cookie_dough", properties.stacksTo(STACKSIZE));
    }
}
