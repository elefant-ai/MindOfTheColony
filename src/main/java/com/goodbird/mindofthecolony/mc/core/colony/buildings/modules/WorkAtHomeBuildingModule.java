package com.goodbird.mindofthecolony.mc.core.colony.buildings.modules;

import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.HiringMode;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuildingWorkerModule;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.modules.*;
import com.goodbird.mindofthecolony.mc.api.colony.jobs.registry.JobEntry;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.Skill;
import com.goodbird.mindofthecolony.mc.api.util.BlockPosUtil;
import com.goodbird.mindofthecolony.mc.core.colony.buildings.workerbuildings.BuildingTownHall;
import net.minecraft.network.chat.MutableComponent;
import com.goodbird.mindofthecolony.mc.api.util.MessageUtils;

import java.util.function.Function;

import static com.goodbird.mindofthecolony.mc.core.colony.buildings.modules.BuildingModules.LIVING;

/**
 * Assignment module for jobs that have to live at the work place mandatorily.
 */
public class WorkAtHomeBuildingModule extends WorkerBuildingModule implements IAssignsCitizen, IBuildingEventsModule, ITickingModule, IPersistentModule, IBuildingWorkerModule, ICreatesResolversModule
{
    public WorkAtHomeBuildingModule(final JobEntry entry,
      final Skill primary,
      final Skill secondary,
      final boolean canWorkingDuringRain,
      final Function<IBuilding, Integer> sizeLimit)
    {
        super(entry, primary, secondary, canWorkingDuringRain, sizeLimit);
    }

    @Override
    public boolean assignCitizen(final ICitizenData citizen)
    {
        if (super.assignCitizen(citizen) && citizen != null)
        {
            // Set new home, since guards are housed at their workerbuilding.
            final IBuilding oldHome = citizen.getHomeBuilding();
            if (oldHome != null && !oldHome.getID().equals(building.getID()))
            {
                if (oldHome.hasModule(LIVING) && !oldHome.hasModule(WorkAtHomeBuildingModule.class))
                {
                    final LivingBuildingModule livingBuildingModule = oldHome.getModule(LIVING);
                    if (livingBuildingModule.getHiringMode() == HiringMode.MANUAL || (livingBuildingModule.getHiringMode() == HiringMode.DEFAULT
                        && !building.getColony().getSettings().getSetting(BuildingTownHall.AUTO_HIRING_MODE).getValue()))
                    {
                        final MutableComponent jobComponent = MessageUtils.format(citizen.getJob().getJobRegistryEntry().getTranslationKey()).create();
                        final MutableComponent buildingComponent = MessageUtils.format(oldHome.getBuildingDisplayName()).create();
                        MessageUtils.format("com.goodbird.mindofthecolony.mc.coremod.gui.workerhuts.assignedbed",
                            citizen.getName(),
                            jobComponent,
                            buildingComponent,
                            BlockPosUtil.getString(oldHome.getID()))
                          .sendTo(oldHome.getColony()).forAllPlayers();
                    }
                }
                oldHome.getFirstModuleOccurance(LivingBuildingModule.class).removeCitizen(citizen);
            }
            citizen.setHomeBuilding(building);
            return true;
        }
        return false;
    }

    @Override
    void onRemoval(final ICitizenData citizen)
    {
        super.onRemoval(citizen);
        citizen.setHomeBuilding(null);
    }
}
