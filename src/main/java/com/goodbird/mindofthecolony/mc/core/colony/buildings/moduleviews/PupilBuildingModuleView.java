package com.goodbird.mindofthecolony.mc.core.colony.buildings.moduleviews;

import com.goodbird.mindofthecolony.mc.api.colony.ICitizenDataView;

/**
 *  Pupil module view.
 */
public class PupilBuildingModuleView extends WorkerBuildingModuleView
{
    @Override
    public boolean canAssign(final ICitizenDataView citizen)
    {
        return citizen.isChild() && (citizen.getWorkBuilding() == null || getAssignedCitizens().contains(citizen.getId()));
    }
}
