package com.goodbird.mindofthecolony.mc.core.colony.buildings.registry;

import com.goodbird.mindofthecolony.mc.api.colony.guardtype.GuardType;
import com.goodbird.mindofthecolony.mc.api.colony.guardtype.registry.IGuardTypeDataManager;
import com.goodbird.mindofthecolony.mc.api.colony.guardtype.registry.IGuardTypeRegistry;
import net.minecraft.resources.ResourceLocation;

public final class GuardTypeDataManager implements IGuardTypeDataManager
{
    @Override
    public GuardType getFrom(final ResourceLocation jobName)
    {
        if (jobName == null)
        {
            return null;
        }

        return IGuardTypeRegistry.getInstance().get(jobName);
    }
}
