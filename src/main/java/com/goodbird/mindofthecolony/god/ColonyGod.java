package com.goodbird.mindofthecolony.god;

import com.goodbird.mindofthecolony.config.ModSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.modules.IAssignsCitizen;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenSkillHandler;
import game.player2.npc.Player2NpcLib;
import game.player2.npc.dto.ChatCompletionRequest;
import game.player2.npc.dto.ChatCompletionRequest.ChatMessage;
import game.player2.npc.dto.ChatCompletionRequest.Tool;
import game.player2.npc.dto.ChatCompletionRequest.ToolCall;
import game.player2.npc.dto.ChatCompletionRequest.ToolFunction;
import game.player2.npc.dto.ChatCompletionResponse;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The Colony God: an AI overseer that manages a colony by calling /v1/chat/completions.
 * One instance per colony, managed by a static registry.
 */
public class ColonyGod {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColonyGod.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<Integer, ColonyGod> INSTANCES = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
            You are an AI colony overseer managing a MineColonies settlement in Minecraft.
            Your job is to make strategic decisions about worker assignments and construction priorities.

            When colonists join or die, you should reassign workers optimally based on their skills and the colony's needs.
            When events occur, decide what buildings should be constructed next by adding them to the build queue.

            Use the provided tools to take actions. You may call multiple tools in one response.
            Always explain your reasoning briefly before calling tools.

            Important notes:
            - Building positions are in "x,y,z" format
            - Citizens have skills that affect their job performance — try to match citizens to jobs that suit their strengths
            - Higher priority builds (higher number) are done first
            - When a citizen dies, their job slot opens up — consider if it needs to be filled
            - When a citizen joins, find them the best available job based on their skills
            """;

    private final IColony colony;
    private final ConcurrentLinkedQueue<ColonyEvent> pendingEvents = new ConcurrentLinkedQueue<>();
    private final List<BuildTodo> buildsTodo = new ArrayList<>();
    private final List<ChatMessage> conversationHistory = new ArrayList<>();

    private volatile boolean processing = false;
    private int tickCounter = 0;

    private ColonyGod(IColony colony) {
        this.colony = colony;
        loadState();
        LOGGER.info("ColonyGod initialized for colony '{}' (ID {})", colony.getName(), colony.getID());
    }

    // ── Static registry ──

    /**
     * Gets an existing ColonyGod or creates one — but only if the colony is NPC-managed.
     * Returns null if the colony is not in the NPC registry.
     */
    public static ColonyGod getOrCreate(Object colonyObj) {
        IColony colony = (IColony) colonyObj;
        if (!NpcColonyRegistry.isNpcColony(colony.getID())) {
            return null;
        }
        return INSTANCES.computeIfAbsent(colony.getID(), id -> new ColonyGod(colony));
    }

    public static void remove(int colonyId) {
        ColonyGod god = INSTANCES.remove(colonyId);
        if (god != null) {
            god.saveState();
            LOGGER.info("ColonyGod removed for colony ID {}", colonyId);
        }
    }

    public static void tickAll() {
        if (!ModSettings.GOD_ENABLED.get()) return;
        INSTANCES.values().forEach(ColonyGod::tick);
    }

    public static void shutdownAll() {
        INSTANCES.values().forEach(ColonyGod::saveState);
        INSTANCES.clear();
        LOGGER.info("All ColonyGod instances shut down");
    }

    // ── Event posting ──

    public void postEvent(ColonyEvent event) {
        pendingEvents.add(event);
        LOGGER.info("ColonyGod event for colony '{}': {}", colony.getName(), event);
    }

    // ── Tick loop ──

    private void tick() {
        tickCounter++;

        boolean hasEvents = !pendingEvents.isEmpty();
        int interval = ModSettings.GOD_TICK_INTERVAL.get();

        // Only process if we have events or it's time for a periodic check
        if (!hasEvents && tickCounter < interval) {
            return;
        }

        // Don't process if already handling a request
        if (processing) {
            return;
        }

        // If no events and it's just a periodic tick with nothing to do, skip
        if (!hasEvents) {
            tickCounter = 0;
            return;
        }

        processing = true;
        tickCounter = 0;

        try {
            processEvents();
        } catch (Exception e) {
            LOGGER.error("Error in ColonyGod tick for colony '{}'", colony.getName(), e);
            processing = false;
        }
    }

    private void processEvents() {
        // Drain all pending events
        List<ColonyEvent> events = new ArrayList<>();
        ColonyEvent event;
        while ((event = pendingEvents.poll()) != null) {
            events.add(event);
        }

        if (events.isEmpty()) {
            processing = false;
            return;
        }

        // Build the user message
        StringBuilder userMessage = new StringBuilder();
        userMessage.append("## Events\n");
        for (ColonyEvent e : events) {
            userMessage.append("- ").append(e.toString()).append("\n");
        }
        userMessage.append("\n## Current Colony State\n");
        userMessage.append(buildColonyStateString(colony, buildsTodo));

        ChatMessage newUserMsg = ChatMessage.user(userMessage.toString());

        // Build the full request
        ChatCompletionRequest.Builder requestBuilder = ChatCompletionRequest.builder()
                .addMessage(ChatMessage.system(SYSTEM_PROMPT))
                .messages(conversationHistory)
                .addMessage(newUserMsg)
                .tools(buildToolDefinitions())
                .toolChoice("auto")
                .temperature(0.7f)
                .maxTokens(1000)
                .stream(false);

        ChatCompletionRequest request = requestBuilder.build();

        // Send async
        Player2NpcLib.getChatCompletionsClient().complete(request)
                .thenAccept(response -> handleResponse(response, newUserMsg))
                .exceptionally(ex -> {
                    LOGGER.error("Chat completion failed for colony '{}'", colony.getName(), ex);
                    processing = false;
                    return null;
                });
    }

    private void handleResponse(ChatCompletionResponse response, ChatMessage userMessage) {
        try {
            // Add user message to history
            conversationHistory.add(userMessage);

            String content = response.getContent();
            List<ToolCall> toolCalls = response.getToolCalls();

            if (content != null && !content.isEmpty()) {
                LOGGER.info("ColonyGod for '{}' says: {}", colony.getName(), content);
            }

            if (toolCalls != null && !toolCalls.isEmpty()) {
                // Add assistant message with tool calls to history
                conversationHistory.add(ChatMessage.assistantWithToolCalls(content, toolCalls));

                // Execute each tool call
                GodActionExecutor executor = new GodActionExecutor(colony, buildsTodo);
                List<ChatMessage> toolResults = new ArrayList<>();
                for (ToolCall tc : toolCalls) {
                    String result = executor.execute(tc);
                    LOGGER.info("Tool {} result: {}", tc.getFunction().getName(), result);
                    toolResults.add(ChatMessage.toolResult(tc.getId(), result));
                }

                // Add tool results to history
                conversationHistory.addAll(toolResults);

                // Send follow-up to let the model see tool results
                sendFollowUp(toolResults);
            } else {
                // No tool calls — just add assistant message to history
                conversationHistory.add(ChatMessage.assistant(content != null ? content : ""));
                trimHistory();
                saveState();
                processing = false;
            }
        } catch (Exception e) {
            LOGGER.error("Error handling response for colony '{}'", colony.getName(), e);
            processing = false;
        }
    }

    private void sendFollowUp(List<ChatMessage> toolResults) {
        ChatCompletionRequest.Builder requestBuilder = ChatCompletionRequest.builder()
                .addMessage(ChatMessage.system(SYSTEM_PROMPT))
                .messages(conversationHistory)
                .tools(buildToolDefinitions())
                .toolChoice("auto")
                .temperature(0.7f)
                .maxTokens(500)
                .stream(false);

        Player2NpcLib.getChatCompletionsClient().complete(requestBuilder.build())
                .thenAccept(response -> {
                    String content = response.getContent();
                    if (content != null && !content.isEmpty()) {
                        LOGGER.info("ColonyGod follow-up for '{}': {}", colony.getName(), content);
                    }
                    conversationHistory.add(ChatMessage.assistant(content != null ? content : ""));
                    trimHistory();
                    saveState();
                    processing = false;
                })
                .exceptionally(ex -> {
                    LOGGER.error("Follow-up completion failed for colony '{}'", colony.getName(), ex);
                    trimHistory();
                    saveState();
                    processing = false;
                    return null;
                });
    }

    // ── Tool definitions ──

    private static List<Tool> buildToolDefinitions() {
        List<Tool> tools = new ArrayList<>();

        // assign_worker
        JsonObject assignParams = new JsonObject();
        assignParams.addProperty("type", "object");
        JsonObject assignProps = new JsonObject();
        JsonObject citizenIdProp = new JsonObject();
        citizenIdProp.addProperty("type", "integer");
        citizenIdProp.addProperty("description", "The ID of the citizen to assign");
        assignProps.add("citizen_id", citizenIdProp);
        JsonObject buildingPosProp = new JsonObject();
        buildingPosProp.addProperty("type", "string");
        buildingPosProp.addProperty("description", "The building position in 'x,y,z' format");
        assignProps.add("building_position", buildingPosProp);
        assignParams.add("properties", assignProps);
        JsonObject assignRequired = new JsonObject();
        com.google.gson.JsonArray assignReqArr = new com.google.gson.JsonArray();
        assignReqArr.add("citizen_id");
        assignReqArr.add("building_position");
        assignParams.add("required", assignReqArr);
        tools.add(new Tool(new ToolFunction("assign_worker",
                "Assign a citizen to work at a specific building. The citizen will be removed from their current job if they have one.",
                assignParams)));

        // remove_worker
        JsonObject removeParams = new JsonObject();
        removeParams.addProperty("type", "object");
        JsonObject removeProps = new JsonObject();
        JsonObject removeCitizenProp = new JsonObject();
        removeCitizenProp.addProperty("type", "integer");
        removeCitizenProp.addProperty("description", "The ID of the citizen to remove from their job");
        removeProps.add("citizen_id", removeCitizenProp);
        removeParams.add("properties", removeProps);
        com.google.gson.JsonArray removeReqArr = new com.google.gson.JsonArray();
        removeReqArr.add("citizen_id");
        removeParams.add("required", removeReqArr);
        tools.add(new Tool(new ToolFunction("remove_worker",
                "Remove a citizen from their current job, making them unemployed.",
                removeParams)));

        // queue_build
        JsonObject buildParams = new JsonObject();
        buildParams.addProperty("type", "object");
        JsonObject buildProps = new JsonObject();
        JsonObject buildTypeProp = new JsonObject();
        buildTypeProp.addProperty("type", "string");
        buildTypeProp.addProperty("description", "The type of building to construct (e.g., 'miner', 'farmer', 'guardtower', 'warehouse', 'residence')");
        buildProps.add("building_type", buildTypeProp);
        JsonObject priorityProp = new JsonObject();
        priorityProp.addProperty("type", "integer");
        priorityProp.addProperty("description", "Priority level (1-10, higher = more urgent). Default is 5.");
        buildProps.add("priority", priorityProp);
        JsonObject reasonProp = new JsonObject();
        reasonProp.addProperty("type", "string");
        reasonProp.addProperty("description", "Brief reason for why this building should be constructed");
        buildProps.add("reason", reasonProp);
        buildParams.add("properties", buildProps);
        com.google.gson.JsonArray buildReqArr = new com.google.gson.JsonArray();
        buildReqArr.add("building_type");
        buildParams.add("required", buildReqArr);
        tools.add(new Tool(new ToolFunction("queue_build",
                "Add a building to the construction queue. The builder will construct these in priority order.",
                buildParams)));

        // get_colony_status
        tools.add(new Tool(new ToolFunction("get_colony_status",
                "Get the current detailed status of the colony including all buildings, citizens, and the build queue.",
                null)));

        return tools;
    }

    // ── Colony state formatting ──

    static String buildColonyStateString(IColony colony, List<BuildTodo> buildsTodo) {
        StringBuilder sb = new StringBuilder();

        sb.append("Colony: ").append(colony.getName())
                .append(" (Day ").append(colony.getDay()).append(")\n");
        sb.append("Under attack: ").append(colony.isColonyUnderAttack()).append("\n");
        sb.append("Overall happiness: ").append(String.format("%.1f", colony.getOverallHappiness())).append("/10\n");
        sb.append("Population: ").append(colony.getCitizenManager().getCurrentCitizenCount())
                .append("/").append(colony.getCitizenManager().getMaxCitizens()).append("\n\n");

        // Buildings
        sb.append("### Buildings\n");
        for (IBuilding building : colony.getBuildingManager().getBuildings().values()) {
            BlockPos pos = building.getPosition();
            String name = building.getBuildingType().getRegistryName().getPath();
            sb.append("- ").append(name)
                    .append(" (Level ").append(building.getBuildingLevel()).append(")")
                    .append(" at ").append(pos.getX()).append(",").append(pos.getY()).append(",").append(pos.getZ());

            // Show assigned workers
            IAssignsCitizen module = building.getModule(IAssignsCitizen.class);
            if (module != null) {
                List<ICitizenData> assigned = module.getAssignedCitizen();
                int max = module.getModuleMax();
                sb.append(" [").append(assigned.size()).append("/").append(max).append(" workers");
                if (!assigned.isEmpty()) {
                    sb.append(": ");
                    for (int i = 0; i < assigned.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(assigned.get(i).getName()).append(" (ID ").append(assigned.get(i).getId()).append(")");
                    }
                }
                sb.append("]");
            }
            sb.append("\n");
        }

        // Citizens
        sb.append("\n### Citizens\n");
        for (ICitizenData citizen : colony.getCitizenManager().getCitizens()) {
            IJob<?> job = citizen.getJob();
            sb.append("- ").append(citizen.getName())
                    .append(" (ID ").append(citizen.getId()).append(")")
                    .append(" — ").append(job != null ? job.getJobRegistryEntry().getKey().getPath() : "unemployed");

            // Top skills
            Map<Skill, CitizenSkillHandler.SkillData> skills = citizen.getCitizenSkillHandler().getSkills();
            List<String> topSkills = new ArrayList<>();
            skills.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().getLevel(), a.getValue().getLevel()))
                    .limit(3)
                    .forEach(e -> topSkills.add(e.getKey().name().toLowerCase() + ":" + e.getValue().getLevel()));
            if (!topSkills.isEmpty()) {
                sb.append(" [skills: ").append(String.join(", ", topSkills)).append("]");
            }
            sb.append("\n");
        }

        // Build queue
        sb.append("\n### Build Queue\n");
        if (buildsTodo.isEmpty()) {
            sb.append("(empty)\n");
        } else {
            for (int i = 0; i < buildsTodo.size(); i++) {
                BuildTodo todo = buildsTodo.get(i);
                sb.append(i + 1).append(". ").append(todo.toString()).append("\n");
            }
        }

        return sb.toString();
    }

    // ── History management ──

    private void trimHistory() {
        int maxHistory = ModSettings.GOD_MAX_HISTORY.get();
        while (conversationHistory.size() > maxHistory) {
            conversationHistory.remove(0);
        }
    }

    // ── Persistence ──

    private Path getStatePath() {
        return Paths.get("config", "mindofthecolony", "god_colony_" + colony.getID() + ".json");
    }

    private void saveState() {
        try {
            Path path = getStatePath();
            Files.createDirectories(path.getParent());

            GodState state = new GodState();
            state.buildsTodo = buildsTodo;
            state.conversationHistory = conversationHistory;

            Files.writeString(path, GSON.toJson(state));
            LOGGER.debug("Saved ColonyGod state for colony {}", colony.getID());
        } catch (IOException e) {
            LOGGER.error("Failed to save ColonyGod state for colony {}", colony.getID(), e);
        }
    }

    private void loadState() {
        try {
            Path path = getStatePath();
            if (!Files.exists(path)) {
                return;
            }

            String json = Files.readString(path);
            GodState state = GSON.fromJson(json, GodState.class);

            if (state.buildsTodo != null) {
                buildsTodo.clear();
                buildsTodo.addAll(state.buildsTodo);
            }
            if (state.conversationHistory != null) {
                conversationHistory.clear();
                conversationHistory.addAll(state.conversationHistory);
            }

            LOGGER.info("Loaded ColonyGod state for colony {} ({} builds, {} history messages)",
                    colony.getID(), buildsTodo.size(), conversationHistory.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load ColonyGod state for colony {}", colony.getID(), e);
        }
    }

    /**
     * State container for JSON persistence.
     */
    private static class GodState {
        List<BuildTodo> buildsTodo;
        List<ChatMessage> conversationHistory;
    }

    public List<BuildTodo> getBuildsTodo() {
        return buildsTodo;
    }
}
