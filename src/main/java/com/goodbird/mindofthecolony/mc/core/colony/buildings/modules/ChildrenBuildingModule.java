package com.goodbird.mindofthecolony.mc.core.colony.buildings.modules;

import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuildingWorkerModule;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.modules.IBuildingEventsModule;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.modules.ICreatesResolversModule;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.modules.IPersistentModule;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.modules.ITickingModule;
import com.goodbird.mindofthecolony.mc.api.colony.jobs.registry.JobEntry;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.Skill;
import com.goodbird.mindofthecolony.mc.core.util.BuildingUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * Assignment module for pupils.
 */
public class ChildrenBuildingModule extends WorkerBuildingModule implements IBuildingEventsModule, ITickingModule, IPersistentModule, IBuildingWorkerModule, ICreatesResolversModule
{
    public ChildrenBuildingModule(final JobEntry entry,
      final Skill primary,
      final Skill secondary,
      final boolean canWorkingDuringRain,
      final Function<IBuilding, Integer> sizeLimit)
    {
        super(entry, primary, secondary, canWorkingDuringRain, sizeLimit);

    }

    @Override
    public void onColonyTick(@NotNull final IColony colony)
    {
        // If we have no active worker, grab one from the Colony
        if (!isFull() && BuildingUtils.canAutoHire(building, getHiringMode(), null))
        {
            for (final ICitizenData data : colony.getCitizenManager().getCitizens())
            {
                if (data.isChild() && data.getWorkBuilding() == null)
                {
                    assignCitizen(data);
                }
            }
        }

        for (final ICitizenData citizenData : new ArrayList<>(getAssignedCitizen()))
        {
            if (!citizenData.isChild())
            {
                removeCitizen(citizenData);
            }
        }
    }
}
