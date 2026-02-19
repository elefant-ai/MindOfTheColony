package com.goodbird.mindofthecolony.mc.core.colony.buildings.modules;

import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuildingWorkerModule;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.modules.ICreatesResolversModule;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.modules.IPersistentModule;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.modules.ITickingModule;
import com.goodbird.mindofthecolony.mc.api.colony.jobs.registry.JobEntry;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.Skill;

import java.util.function.Function;

/**
 * Assignment module for miners.
 */
public class MinerBuildingModule extends WorkerBuildingModule implements ITickingModule, IPersistentModule, IBuildingWorkerModule, ICreatesResolversModule
{
    public MinerBuildingModule(
      final JobEntry entry,
      final Skill primary,
      final Skill secondary,
      final boolean canWorkingDuringRain,
      final Function<IBuilding, Integer> sizeLimit)
    {
        super(entry, primary, secondary, canWorkingDuringRain, sizeLimit);
    }

    @Override
    public boolean isFull()
    {
        return building.getAllAssignedCitizen().size() >= getModuleMax();
    }
}
