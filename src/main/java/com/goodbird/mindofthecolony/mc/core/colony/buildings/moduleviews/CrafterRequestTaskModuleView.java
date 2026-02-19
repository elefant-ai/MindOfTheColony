package com.goodbird.mindofthecolony.mc.core.colony.buildings.moduleviews;

import com.goodbird.mindofthecolony.mc.api.colony.ICitizenDataView;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.token.IToken;
import com.goodbird.mindofthecolony.mc.core.colony.jobs.views.CrafterJobView;

import java.util.ArrayList;
import java.util.List;

/**
 * Crafter task module to display tasks in the UI.
 */
public class CrafterRequestTaskModuleView extends RequestTaskModuleView
{
    @Override
    public List<IToken<?>> getTasks()
    {
        final List<IToken<?>> tasks = new ArrayList<>();
        for (final WorkerBuildingModuleView moduleView : buildingView.getModuleViews(WorkerBuildingModuleView.class))
        {
            for (final int citizenId : moduleView.getAssignedCitizens())
            {
                ICitizenDataView citizen = buildingView.getColony().getCitizen(citizenId);
                if (citizen != null && citizen.getJobView() instanceof CrafterJobView)
                {
                    tasks.addAll(((CrafterJobView) citizen.getJobView()).getDataStore().getQueue());
                }
            }
        }

        return tasks;
    }
}
