package com.goodbird.mindofthecolony.god;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.modules.IAssignsCitizen;
import game.player2.npc.dto.ChatCompletionRequest;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes tool calls returned by the LLM against the colony.
 */
public class GodActionExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GodActionExecutor.class);
    private static final Gson GSON = new Gson();

    private final IColony colony;
    private final List<BuildTodo> buildsTodo;

    public GodActionExecutor(IColony colony, List<BuildTodo> buildsTodo) {
        this.colony = colony;
        this.buildsTodo = buildsTodo;
    }

    /**
     * Executes a tool call and returns the result string.
     */
    public String execute(ChatCompletionRequest.ToolCall toolCall) {
        String functionName = toolCall.getFunction().getName();
        String arguments = toolCall.getFunction().getArguments();

        LOGGER.info("Executing god tool: {}({})", functionName, arguments);

        try {
            return switch (functionName) {
                case "assign_worker" -> executeAssignWorker(arguments);
                case "remove_worker" -> executeRemoveWorker(arguments);
                case "queue_build" -> executeQueueBuild(arguments);
                case "get_colony_status" -> executeGetColonyStatus();
                default -> "Unknown tool: " + functionName;
            };
        } catch (Exception e) {
            LOGGER.error("Error executing tool {}", functionName, e);
            return "Error: " + e.getMessage();
        }
    }

    private String executeAssignWorker(String arguments) {
        JsonObject args = GSON.fromJson(arguments, JsonObject.class);
        int citizenId = args.get("citizen_id").getAsInt();
        String posStr = args.get("building_position").getAsString();

        BlockPos pos = parseBlockPos(posStr);
        if (pos == null) {
            return "Error: Invalid building position format '" + posStr + "'. Expected 'x,y,z'.";
        }

        ICitizenData citizenData = colony.getCitizenManager().getCivilian(citizenId);
        if (citizenData == null) {
            return "Error: Citizen with ID " + citizenId + " not found.";
        }

        IBuilding building = colony.getBuildingManager().getBuilding(pos);
        if (building == null) {
            return "Error: No building found at position " + posStr + ".";
        }

        // Remove citizen from current work building if they have one
        IBuilding currentWork = citizenData.getWorkBuilding();
        if (currentWork != null) {
            IAssignsCitizen currentModule = currentWork.getModule(IAssignsCitizen.class);
            if (currentModule != null) {
                currentModule.removeCitizen(citizenData);
                LOGGER.info("Removed {} from {}", citizenData.getName(),
                        currentWork.getBuildingType().getRegistryName().getPath());
            }
        }

        // Assign to new building
        IAssignsCitizen module = building.getModule(IAssignsCitizen.class);
        if (module == null) {
            return "Error: Building at " + posStr + " does not accept worker assignments.";
        }

        if (module.isFull()) {
            return "Error: Building at " + posStr + " is already full (" + module.getAssignedCitizen().size() + "/" + module.getModuleMax() + ").";
        }

        boolean success = module.assignCitizen(citizenData);
        if (success) {
            String buildingName = building.getBuildingType().getRegistryName().getPath();
            LOGGER.info("Assigned {} to {} at {}", citizenData.getName(), buildingName, posStr);
            return "Success: Assigned " + citizenData.getName() + " to " + buildingName + " at " + posStr + ".";
        } else {
            return "Error: Failed to assign " + citizenData.getName() + " to building at " + posStr + ".";
        }
    }

    private String executeRemoveWorker(String arguments) {
        JsonObject args = GSON.fromJson(arguments, JsonObject.class);
        int citizenId = args.get("citizen_id").getAsInt();

        ICitizenData citizenData = colony.getCitizenManager().getCivilian(citizenId);
        if (citizenData == null) {
            return "Error: Citizen with ID " + citizenId + " not found.";
        }

        IBuilding workBuilding = citizenData.getWorkBuilding();
        if (workBuilding == null) {
            return "Error: " + citizenData.getName() + " is not assigned to any building.";
        }

        IAssignsCitizen module = workBuilding.getModule(IAssignsCitizen.class);
        if (module == null) {
            return "Error: Building does not have an assignment module.";
        }

        boolean success = module.removeCitizen(citizenData);
        if (success) {
            String buildingName = workBuilding.getBuildingType().getRegistryName().getPath();
            LOGGER.info("Removed {} from {}", citizenData.getName(), buildingName);
            return "Success: Removed " + citizenData.getName() + " from " + buildingName + ".";
        } else {
            return "Error: Failed to remove " + citizenData.getName() + " from their building.";
        }
    }

    private String executeQueueBuild(String arguments) {
        JsonObject args = GSON.fromJson(arguments, JsonObject.class);
        String buildingType = args.get("building_type").getAsString();
        int priority = args.has("priority") ? args.get("priority").getAsInt() : 5;
        String reason = args.has("reason") ? args.get("reason").getAsString() : "";

        BuildTodo todo = new BuildTodo(buildingType, priority, reason);

        // Insert sorted by priority (higher priority first)
        int insertIdx = 0;
        for (int i = 0; i < buildsTodo.size(); i++) {
            if (buildsTodo.get(i).getPriority() < priority) {
                break;
            }
            insertIdx = i + 1;
        }
        buildsTodo.add(insertIdx, todo);

        LOGGER.info("Queued build: {} (priority {}): {}", buildingType, priority, reason);
        return "Success: Added '" + buildingType + "' to build queue at position " + (insertIdx + 1) +
                " (priority " + priority + "). Queue now has " + buildsTodo.size() + " items.";
    }

    private String executeGetColonyStatus() {
        return ColonyGod.buildColonyStateString(colony, buildsTodo);
    }

    private static BlockPos parseBlockPos(String posStr) {
        try {
            String[] parts = posStr.split(",");
            if (parts.length != 3) return null;
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());
            return new BlockPos(x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
