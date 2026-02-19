package com.goodbird.mindofthecolony.mc.api.colony.buildingextensions.modules;

import com.goodbird.mindofthecolony.mc.api.colony.buildingextensions.IBuildingExtension;

/**
 * Default interface for all building extension modules.
 */
public interface IBuildingExtensionModule
{
    /**
     * Get the building extension of the module.
     *
     * @return the building extension.
     */
    IBuildingExtension getExtension();
}
