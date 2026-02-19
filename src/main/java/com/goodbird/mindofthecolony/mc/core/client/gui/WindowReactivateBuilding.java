package com.goodbird.mindofthecolony.mc.core.client.gui;

import com.ldtteam.blockui.controls.Text;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.ModBuildings;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.BuildingEntry;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.registry.IBuildingRegistry;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import com.goodbird.mindofthecolony.mc.core.network.messages.server.ReactivateBuildingMessage;
import com.goodbird.mindofthecolony.mc.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static com.goodbird.mindofthecolony.mc.api.util.constant.WindowConstants.*;

/**
 * Window to reactivate a building.
 */
public class WindowReactivateBuilding extends AbstractWindowSkeleton
{
    /*
     * Building the worker is trying to place.
     */
    @NotNull
    private final BlockPos pos;

    /**
     * Creates a new instance of this window.
     * @param pos the position of the building.
     */
    public WindowReactivateBuilding(@NotNull final BlockPos pos)
    {
        super(new ResourceLocation(Constants.MOD_ID, "gui/windowreactivatebuilding.xml"));
        this.pos = pos;
        registerButton(BUTTON_REACTIVATE, this::reactivateClicked);
        registerButton(BUTTON_CANCEL, this::cancelClicked);


        if (Minecraft.getInstance().level.getBlockEntity(pos) instanceof TileEntityColonyBuilding tileEntityColonyBuilding)
        {
            final BuildingEntry buildingEntry = IBuildingRegistry.getInstance().get(tileEntityColonyBuilding.getBuildingName());
            if (buildingEntry == ModBuildings.home.get() || buildingEntry == ModBuildings.tavern.get())
            {
                findPaneOfTypeByID("text", Text.class).setText(Component.translatable("com.goodbird.mindofthecolony.mc.core.gui.reactivate.message.living", Component.translatable(buildingEntry.getTranslationKey())));
            }
            else if (buildingEntry != null)
            {
                findPaneOfTypeByID("text", Text.class).setText(Component.translatable("com.goodbird.mindofthecolony.mc.core.gui.reactivate.message.working", Component.translatable(buildingEntry.getTranslationKey())));
            }
        }
    }

    /**
     * Reactivate the building.
     */
    private void reactivateClicked()
    {
        new ReactivateBuildingMessage(pos).sendToServer();
        close();
    }


    /**
     * Cancel reactivation.
     */
    private void cancelClicked()
    {
        close();
    }
}
