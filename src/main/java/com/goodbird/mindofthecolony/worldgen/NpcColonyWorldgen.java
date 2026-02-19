package com.goodbird.mindofthecolony.worldgen;

import com.goodbird.mindofthecolony.god.ColonyGod;
import com.goodbird.mindofthecolony.god.NpcColonyRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.savedata.IServerColonySaveData;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.structures.EmptyColonyStructure;
import com.minecolonies.core.util.ChunkDataHelper;
import game.player2.npc.Player2NpcLib;
import game.player2.npc.dto.ChatCompletionRequest;
import game.player2.npc.dto.ChatCompletionRequest.ChatMessage;
import game.player2.npc.dto.ChatCompletionResponse;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects MineColonies EmptyColonyStructure generation via chunk loading
 * and automatically creates NPC-managed colonies at those locations.
 */
public class NpcColonyWorldgen {
    private static final Logger LOGGER = LoggerFactory.getLogger(NpcColonyWorldgen.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PROCESSED_PATH = Paths.get("config", "mindofthecolony", "processed_structures.json");

    private static final Set<String> processedPositions = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;

        for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
            Structure structure = entry.getKey();
            StructureStart start = entry.getValue();

            if (!(structure instanceof EmptyColonyStructure) || !start.isValid()) continue;

            BlockPos centerPos = start.getBoundingBox().getCenter();
            String posKey = centerPos.getX() + "," + centerPos.getY() + "," + centerPos.getZ();

            if (processedPositions.contains(posKey)) continue;
            processedPositions.add(posKey);
            saveProcessedPositions();

            LOGGER.info("Detected EmptyColonyStructure at {} — creating NPC colony", posKey);

            // Defer colony creation to next server tick to ensure blocks are loaded
            BoundingBox bounds = start.getBoundingBox();
            level.getServer().execute(() -> createNpcColony(level, centerPos, bounds));
        }
    }

    private static void createNpcColony(ServerLevel level, BlockPos centerPos, BoundingBox bounds) {
        try {
            // Generate a name via /chat/completions
            String colonyName = generateColonyName(centerPos);

            // Create the colony
            IColony colony = IServerColonySaveData.getOrComputeSaveData(level)
                    .createColony(level, colonyName, centerPos);

            if (colony == null) {
                LOGGER.error("Failed to create colony at {}", centerPos);
                return;
            }

            LOGGER.info("Created NPC colony '{}' (ID {}) at {}", colony.getName(), colony.getID(), centerPos);

            // Set as abandoned (no real player owner)
            colony.getPermissions().setOwnerAbandoned();

            // Claim chunks around the colony center
            if (colony instanceof Colony concreteColony) {
                ChunkDataHelper.claimColonyChunks(level, true, concreteColony, centerPos);
                LOGGER.info("Claimed chunks for colony '{}'", colony.getName());
            }

            // Scan the structure bounds for building tile entities and register them
            int buildingsRegistered = scanAndRegisterBuildings(level, colony, bounds);
            LOGGER.info("Registered {} buildings for colony '{}'", buildingsRegistered, colony.getName());

            // Mark as NPC-managed and start the Colony God
            NpcColonyRegistry.register(colony.getID());
            ColonyGod.getOrCreate(colony);

            LOGGER.info("NPC colony '{}' (ID {}) fully initialized", colony.getName(), colony.getID());
        } catch (Exception e) {
            LOGGER.error("Failed to create NPC colony at {}", centerPos, e);
        }
    }

    private static int scanAndRegisterBuildings(ServerLevel level, IColony colony, BoundingBox bounds) {
        int count = 0;

        for (int x = bounds.minX(); x <= bounds.maxX(); x += 16) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z += 16) {
                LevelChunk chunk = level.getChunkAt(new BlockPos(x, 0, z));
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    BlockEntity be = entry.getValue();

                    if (!(be instanceof AbstractTileEntityColonyBuilding buildingTe)) continue;
                    if (!bounds.isInside(pos)) continue;

                    try {
                        colony.getBuildingManager().addNewBuilding(buildingTe, level);
                        count++;
                        LOGGER.debug("Registered building at {} for colony '{}'", pos, colony.getName());
                    } catch (Exception e) {
                        LOGGER.warn("Failed to register building at {} for colony '{}': {}",
                                pos, colony.getName(), e.getMessage());
                    }
                }
            }
        }

        return count;
    }

    /**
     * Generates a colony name by calling /v1/chat/completions.
     * Falls back to a position-based name if the API call fails.
     */
    private static String generateColonyName(BlockPos pos) {
        try {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .addMessage(ChatMessage.system(
                            "You are a fantasy world name generator. "
                            + "When asked, generate a single short, evocative settlement name (1-3 words). "
                            + "Reply with ONLY the name, nothing else. No quotes, no explanation."))
                    .addMessage(ChatMessage.user(
                            "Generate a name for an abandoned medieval colony settlement that was recently rediscovered."))
                    .temperature(1.0f)
                    .maxTokens(20)
                    .stream(false)
                    .build();

            ChatCompletionResponse response = Player2NpcLib.getChatCompletionsClient()
                    .complete(request)
                    .join(); // blocking is fine here since we're already on server thread deferred

            String name = response.getContent();
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to generate colony name via chat completions, using fallback", e);
        }

        return "Settlement " + pos.getX() + "," + pos.getZ();
    }

    // ── Persistence ──

    public static void loadProcessedPositions() {
        try {
            if (!Files.exists(PROCESSED_PATH)) {
                LOGGER.debug("No processed structures file found, starting empty");
                return;
            }

            String json = Files.readString(PROCESSED_PATH);
            Type type = new TypeToken<ProcessedData>() {}.getType();
            ProcessedData data = GSON.fromJson(json, type);

            processedPositions.clear();
            if (data != null && data.positions != null) {
                processedPositions.addAll(data.positions);
            }

            LOGGER.info("Loaded {} processed structure positions", processedPositions.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load processed structures", e);
        }
    }

    public static void saveProcessedPositions() {
        try {
            Files.createDirectories(PROCESSED_PATH.getParent());

            ProcessedData data = new ProcessedData();
            data.positions = new HashSet<>(processedPositions);

            Files.writeString(PROCESSED_PATH, GSON.toJson(data));
        } catch (IOException e) {
            LOGGER.error("Failed to save processed structures", e);
        }
    }

    private static class ProcessedData {
        Set<String> positions;
    }
}
