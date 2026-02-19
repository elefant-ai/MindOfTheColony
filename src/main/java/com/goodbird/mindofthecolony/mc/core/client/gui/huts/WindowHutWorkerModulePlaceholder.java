package com.goodbird.mindofthecolony.mc.core.client.gui.huts;

import com.goodbird.mindofthecolony.mc.api.colony.buildings.views.IBuildingView;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import com.goodbird.mindofthecolony.mc.core.client.gui.AbstractWindowWorkerModuleBuilding;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.views.AbstractBuildingView;
import net.minecraft.resources.ResourceLocation;

/**
 * BOWindow for worker. Placeholder for many different jobs.
 *
 * @param <B> Object extending {@link AbstractBuildingView}.
 */
public class WindowHutWorkerModulePlaceholder<B extends IBuildingView> extends AbstractWindowWorkerModuleBuilding<B>
{
    /**
     * BOWindow for worker placeholder. Used by buildings not listed above this file.
     *
     * @param building AbstractBuilding extending {@link AbstractBuildingView}.
     */
    public WindowHutWorkerModulePlaceholder(final B building)
    {
        super(building, new ResourceLocation(Constants.MOD_ID, "gui/windowhutworkerplaceholder.xml"));
    }
}
