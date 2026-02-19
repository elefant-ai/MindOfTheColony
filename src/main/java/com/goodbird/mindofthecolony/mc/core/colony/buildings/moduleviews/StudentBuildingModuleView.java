package com.goodbird.mindofthecolony.mc.core.colony.buildings.moduleviews;

import com.goodbird.mindofthecolony.mc.api.colony.jobs.registry.JobEntry;

/**
 *  Student module view.
 */
public class StudentBuildingModuleView extends WorkerBuildingModuleView
{
    @Override
    public boolean canBeHiredAs(final JobEntry jobEntry)
    {
        return true;
    }
}
