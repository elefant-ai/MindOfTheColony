package com.goodbird.mindofthecolony.mc.core.client.gui.townhall;

import com.ldtteam.blockui.Color;
import com.ldtteam.blockui.controls.ButtonImage;
import com.ldtteam.blockui.controls.Image;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.workerbuildings.ITownHallView;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import com.goodbird.mindofthecolony.mc.core.client.gui.AbstractBuildingMainWindow;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.workerbuildings.BuildingTownHall;
import net.minecraft.resources.ResourceLocation;

import static com.goodbird.mindofthecolony.mc.api.util.constant.WindowConstants.*;

/**
 * BOWindow for the town hall.
 */
public abstract class AbstractWindowTownHall extends AbstractBuildingMainWindow<ITownHallView>
{
    /**
     * Color constants for builder list.
     */
    public static final int RED       = Color.getByName("red", 0);
    public static final int DARKGREEN = Color.getByName("darkgreen", 0);
    public static final int ORANGE    = Color.getByName("orange", 0);
    public static final int YELLOW = Color.getByName("yellow", 0);

    /**
     * Constructor for the town hall window.
     *
     * @param townHall {@link BuildingTownHall.View}.
     */
    public AbstractWindowTownHall(final BuildingTownHall.View townHall, final String page)
    {
        super(townHall, new ResourceLocation(Constants.MOD_ID, "gui/townhall/").withSuffix(page));

        registerButton(BUTTON_ACTIONS, () -> new WindowMainPage(townHall).open());
        registerButton(BUTTON_INFOPAGE, () -> new WindowInfoPage(townHall).open());
        registerButton(BUTTON_PERMISSIONS, () -> new WindowPermissionsPage(townHall).open());
        registerButton(BUTTON_CITIZENS, () -> new WindowCitizenPage(townHall).open());
        registerButton(BUTTON_STATS, () -> new WindowStatsPage(townHall).open());
        registerButton(BUTTON_SETTINGS, () -> new WindowSettings(townHall).open());
        registerButton(BUTTON_ALLIANCE, () -> new WindowAlliancePage(townHall).open());

        findPaneOfTypeByID(getWindowId() + "0", Image.class).hide();
        findPaneOfTypeByID(getWindowId(), ButtonImage.class).hide();

        findPaneOfTypeByID(getWindowId() + "1", ButtonImage.class).show();
    }

    /**
     * Get the id that identifies the window.
     * @return the string id.
     */
    protected abstract String getWindowId();

    @Override
    protected boolean shouldRenderDefaultSidebar()
    {
        return false;
    }
}
