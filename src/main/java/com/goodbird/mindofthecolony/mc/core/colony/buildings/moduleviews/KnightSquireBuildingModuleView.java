package com.goodbird.mindofthecolony.mc.core.colony.buildings.moduleviews;

import com.goodbird.mindofthecolony.mc.api.colony.jobs.ModJobs;
import com.goodbird.mindofthecolony.mc.api.colony.jobs.registry.JobEntry;

/**
 *  Student module view.
 */
public class KnightSquireBuildingModuleView extends WorkerBuildingModuleView
{
    @Override
    public boolean canBeHiredAs(final JobEntry jobEntry)
    {
        return jobEntry == ModJobs.knight.get();
    }
}
