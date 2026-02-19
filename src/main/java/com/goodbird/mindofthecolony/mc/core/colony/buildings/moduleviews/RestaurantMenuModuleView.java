package com.goodbird.mindofthecolony.mc.core.colony.buildings.moduleviews;

import com.ldtteam.blockui.views.BOWindow;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.goodbird.mindofthecolony.mc.api.crafting.ItemStorage;
import com.goodbird.mindofthecolony.mc.api.util.Utils;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import com.goodbird.mindofthecolony.mc.api.util.constant.translation.RequestSystemTranslationConstants;
import com.goodbird.mindofthecolony.mc.core.client.gui.modules.building.RestaurantMenuModuleWindow;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.goodbird.mindofthecolony.mc.core.colony.buildings.modules.RestaurantMenuModule.STOCK_PER_LEVEL;
import static com.goodbird.mindofthecolony.mc.core.colony.buildings.workerbuildings.BuildingCook.FOOD_EXCLUSION_LIST;

/**
 * Client side version of food menu.
 */
public class RestaurantMenuModuleView extends AbstractBuildingModuleView
{
    /**
     * The menu.
     */
    private final List<ItemStorage> menu = new ArrayList<>();

    @Override
    public void deserialize(final @NotNull RegistryFriendlyByteBuf buf)
    {
        menu.clear();
        final int size = buf.readInt();
        for (int i = 0; i < size; i++)
        {
            menu.add(new ItemStorage(Utils.deserializeCodecMess(buf)));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public BOWindow getWindow()
    {
        return new RestaurantMenuModuleWindow(this);
    }

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return new ResourceLocation(Constants.MOD_ID, "textures/gui/modules/" + FOOD_EXCLUSION_LIST + ".png");
    }

    @Override
    public Component getDesc()
    {
        return Component.translatable(RequestSystemTranslationConstants.REQUESTS_TYPE_FOOD);
    }

    /**
     * Get the menu for the restaurant.
     * @return the menu.
     */
    public List<ItemStorage> getMenu()
    {
        return menu;
    }

    public boolean hasReachedLimit()
    {
        return menu.size() >= buildingView.getBuildingLevel() * STOCK_PER_LEVEL;
    }
}
