package com.goodbird.mindofthecolony.mc.api.colony.jobs.registry;

import com.goodbird.mindofthecolony.mc.api.IMinecoloniesAPI;
import net.minecraft.core.Registry;

public interface IJobRegistry
{
    static Registry<JobEntry> getInstance()
    {
        return IMinecoloniesAPI.getInstance().getJobRegistry();
    }
}
