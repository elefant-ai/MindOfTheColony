package com.goodbird.mindofthecolony.status;

import com.goodbird.mindofthecolony.events.ColonyEventLog;
import com.goodbird.mindofthecolony.events.ColonyEventManager;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;

import java.util.stream.Collectors;

public class AgentStatus extends ObjectStatus {

    public static AgentStatus fromCitizenAndColony(ICitizenData citizenData) {
        AgentStatus agentStatus = new AgentStatus();
        IColony colony = citizenData.getColony();

        agentStatus.add("agentStatus", CitizenStatus.fromCitizen(citizenData).toString());

        if (colony != null) {
            agentStatus.add("colonyStatus", ColonyStatus.fromColony(colony).toString());

            ColonyEventLog log = ColonyEventManager.getInstance().getLog(colony.getID());
            if (log != null && !log.getActiveEvents().isEmpty()) {
                agentStatus.add("recentColonyEvents", log.toContextString());
            }
        } else {
            agentStatus.add("colonyStatus", "{\"error\": \"Colony data not available\"}");
        }

        return agentStatus;
    }

    @Override
    public String toString() {
        if (fields.isEmpty()) {
            return "{}";
        }
        return "{\n" +
                fields.entrySet().stream()
                        .map(entry -> {
                            String value = entry.getValue();
                            if (value.trim().startsWith("{") && value.trim().endsWith("}")) {
                                return "  \"" + entry.getKey() + "\": " + value.replace("\n", "\n  ");
                            } else {
                                return "  \"" + entry.getKey() + "\": \"" + value + "\"";
                            }
                        })
                        .collect(Collectors.joining(",\n")) +
                "\n}";
    }
}