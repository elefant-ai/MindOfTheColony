package com.goodbird.mindofthecolony.mc.api.eventbus.events.colony;

import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import org.jetbrains.annotations.NotNull;

/**
 * Colony team changed event.
 */
public final class ColonyTeamColorChangedModEvent extends AbstractColonyModEvent
{
    /**
     * Constructs a colony team changed event.
     *
     * @param colony the colony related to the event.
     */
    public ColonyTeamColorChangedModEvent(final @NotNull IColony colony)
    {
        super(colony);
    }
}
