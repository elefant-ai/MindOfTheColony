package com.goodbird.mindofthecolony.mc.core.colony.requestsystem.management.handlers.update.implementation;

import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.management.update.UpdateType;
import com.goodbird.mindofthecolony.mc.core.colony.Colony;
import com.goodbird.mindofthecolony.mc.core.colony.requestsystem.management.IStandardRequestManager;
import com.goodbird.mindofthecolony.mc.core.colony.requestsystem.management.handlers.update.IUpdateStep;
import org.jetbrains.annotations.NotNull;

public class InitialUpdate implements IUpdateStep
{
    @Override
    public int updatesToVersion()
    {
        return 0;
    }

    @Override
    public void update(@NotNull final UpdateType type, @NotNull final IStandardRequestManager manager)
    {
        //Noop
        final Colony colony = (Colony) manager.getColony();
        if (type == UpdateType.DATA_LOAD)
        {
            manager.reset();
        }
        colony.getServerBuildingManager().getBuildings().values().forEach(manager::onProviderAddedToColony);
    }
}
