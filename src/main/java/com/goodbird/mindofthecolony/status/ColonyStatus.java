package com.goodbird.mindofthecolony.status;

import com.minecolonies.api.colony.IColony;

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

        return status;
    }
}