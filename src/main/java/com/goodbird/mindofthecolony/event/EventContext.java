package com.goodbird.mindofthecolony.event;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Context data passed to event evaluators.
 * Contains all information needed to evaluate whether events should trigger.
 *
 * @param colony      The colony being evaluated
 * @param level       The server level for weather and other checks
 * @param weather     Weather state tracker
 * @param citizens    All citizens in the colony
 * @param currentTick Current game tick
 * @param random      Random source for probabilistic events
 */
public record EventContext(
    IColony colony,
    ServerLevel level,
    WeatherState weather,
    List<ICitizenData> citizens,
    long currentTick,
    RandomSource random
) {}
