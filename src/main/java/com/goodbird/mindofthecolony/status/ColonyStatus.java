package com.goodbird.mindofthecolony.status;

import com.goodbird.mindofthecolony.event.ColonyEventManager;
import com.minecolonies.api.colony.IColony;
import net.minecraft.server.level.ServerLevel;

public class ColonyStatus extends ObjectStatus {

    public static ColonyStatus fromColony(IColony colony) {
        ColonyStatus status = new ColonyStatus();
        if (colony == null) {
            status.add("error", "Colony data is null");
            return status;
        }

        status.add("name", colony.getName());
        status.add("id", String.valueOf(colony.getID()));
        status.add("style", colony.getStructurePack());
        status.add("location", MinecoloniesStatusUtils.formatBlockPos(colony.getCenter()));
        status.add("dimension", colony.getDimension().location().toString());
        status.add("colony_day", String.valueOf(colony.getDay()));
        status.add("is_under_attack", String.valueOf(colony.isColonyUnderAttack()));

        status.add("population", MinecoloniesStatusUtils.getPopulationString(colony));
        status.add("buildings", MinecoloniesStatusUtils.getBuildingsString(colony));

        status.add("overall_happiness", String.format("%.2f/10.0", colony.getOverallHappiness()));

        status.add("owner", colony.getPermissions().getOwnerName());
        status.add("players_in_colony", String.valueOf(colony.getPermissions().getPlayers().size()));
        status.add("mourning_info", MinecoloniesStatusUtils.getMourningStatusString(colony));

        // Add weather info if available
        if (colony.getWorld() instanceof ServerLevel level) {
            status.add("weather", getWeatherDescription(level));
        }

        // Add recent events if available
        ColonyEventManager eventManager = ColonyEventManager.getIfExists(colony.getID());
        if (eventManager != null) {
            long currentTick = colony.getWorld() != null ? colony.getWorld().getGameTime() : 0;
            String eventContext = eventManager.getEventsAsContext(5, currentTick);
            if (!eventContext.isEmpty()) {
                status.add("recent_events", eventContext);
            }
        }

        return status;
    }

    private static String getWeatherDescription(ServerLevel level) {
        if (level.isThundering()) {
            return "Thunderstorm";
        } else if (level.isRaining()) {
            return "Raining";
        } else {
            return "Clear";
        }
    }
}