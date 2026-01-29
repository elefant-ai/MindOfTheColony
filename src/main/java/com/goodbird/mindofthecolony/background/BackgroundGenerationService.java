package com.goodbird.mindofthecolony.background;

import com.goodbird.mindofthecolony.config.ModSettings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.minecolonies.api.colony.ICitizenData;
import game.player2.npc.Player2NpcLib;
import game.player2.npc.api.NpcHandle;
import game.player2.npc.event.NpcMessageEvent;
import game.player2.npc.event.Player2EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service that uses AI to generate backstories and select appropriate traits for citizens.
 * Spawns a temporary NPC to generate the background, then parses the response.
 */
public class BackgroundGenerationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundGenerationService.class);
    private static final Gson GSON = new Gson();

    private static final BackgroundGenerationService INSTANCE = new BackgroundGenerationService();

    // Pending generation requests: npcId -> CompletableFuture
    private final ConcurrentHashMap<UUID, CompletableFuture<CitizenBackground>> pendingRequests = new ConcurrentHashMap<>();

    // Track NPC handles for cleanup
    private final ConcurrentHashMap<UUID, NpcHandle> generatorNpcs = new ConcurrentHashMap<>();

    private Player2EventListener listener;
    private boolean listenerRegistered = false;

    private BackgroundGenerationService() {}

    public static BackgroundGenerationService getInstance() {
        return INSTANCE;
    }

    /**
     * Generates a background for a citizen using AI.
     * The AI will create a backstory and select appropriate traits.
     *
     * @param citizenData The citizen to generate a background for
     * @param gameId The game session ID
     * @return CompletableFuture that completes with the generated background
     */
    public CompletableFuture<CitizenBackground> generateBackground(ICitizenData citizenData, String gameId) {
        ensureListenerRegistered();

        String citizenName = citizenData.getName();
        String gender = citizenData.isFemale() ? "female" : "male";
        String age = citizenData.isChild() ? "child" : "adult";
        String job = citizenData.getJob() != null
            ? citizenData.getJob().getJobRegistryEntry().getKey().getPath()
            : "unemployed";

        CompletableFuture<CitizenBackground> future = new CompletableFuture<>();

        // Build the system prompt for the background generator
        String systemPrompt = buildGeneratorSystemPrompt();

        // Build the user message with citizen details and available traits
        String userMessage = buildGeneratorUserMessage(citizenName, gender, age, job);

        // Spawn a temporary NPC for generation
        String shortName = "bg_gen_" + citizenData.getId() + "_" + System.currentTimeMillis();

        Player2NpcLib.builder(shortName)
            .name("Background Generator")
            .description("Temporary NPC for generating citizen backgrounds")
            .systemPrompt(systemPrompt)
            .keepGameState(false)
            .spawn(gameId)
            .thenAccept(handle -> {
                UUID npcId = handle.getId();
                pendingRequests.put(npcId, future);
                generatorNpcs.put(npcId, handle);

                LOGGER.debug("Spawned background generator NPC {} for citizen {}", npcId, citizenName);

                // Send the generation request
                handle.chat("System", userMessage, "");
            })
            .exceptionally(ex -> {
                LOGGER.error("Failed to spawn background generator for citizen {}", citizenName, ex);
                future.completeExceptionally(ex);
                return null;
            });

        // Timeout after 30 seconds and fall back to random generation
        return future.orTimeout(30, TimeUnit.SECONDS)
            .exceptionally(ex -> {
                LOGGER.warn("Background generation timed out for {}, using fallback", citizenName);
                return BackgroundGenerator.generate();
            });
    }

    private void ensureListenerRegistered() {
        if (!listenerRegistered) {
            listener = new Player2EventListener() {
                @Override
                public boolean onMessageEvent(NpcMessageEvent event) {
                    return handleGeneratorResponse(event);
                }
            };
            Player2NpcLib.addListener(listener);
            listenerRegistered = true;
            LOGGER.debug("Background generation listener registered");
        }
    }

    private boolean handleGeneratorResponse(NpcMessageEvent event) {
        UUID npcId = event.getNpcId();
        CompletableFuture<CitizenBackground> future = pendingRequests.remove(npcId);

        if (future == null) {
            // Not a background generator response
            return false;
        }

        String response = event.getMessage();
        LOGGER.debug("Received background generation response: {}", response);

        try {
            CitizenBackground background = parseGeneratorResponse(response);
            future.complete(background);
            LOGGER.info("Successfully generated AI background with {} traits", background.getTraits().size());
        } catch (Exception e) {
            LOGGER.warn("Failed to parse AI background response, using fallback: {}", e.getMessage());
            future.complete(BackgroundGenerator.generate());
        }

        // Clean up the temporary NPC
        NpcHandle handle = generatorNpcs.remove(npcId);
        if (handle != null && handle.isAlive()) {
            handle.kill();
        }

        return true; // Consume this event
    }

    /**
     * Parses the AI response to extract backstory and traits.
     * Expected format:
     * {
     *   "backstory": "A detailed backstory...",
     *   "traits": ["trait_id_1", "trait_id_2", "trait_id_3"]
     * }
     */
    private CitizenBackground parseGeneratorResponse(String response) {
        // Try to extract JSON from the response (AI might include extra text)
        int jsonStart = response.indexOf('{');
        int jsonEnd = response.lastIndexOf('}');

        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
            throw new IllegalArgumentException("No JSON object found in response");
        }

        String jsonStr = response.substring(jsonStart, jsonEnd + 1);
        JsonObject json = GSON.fromJson(jsonStr, JsonObject.class);

        CitizenBackground bg = new CitizenBackground();

        // Extract backstory
        if (json.has("backstory") && !json.get("backstory").isJsonNull()) {
            bg.setBackstory(json.get("backstory").getAsString());
        } else {
            bg.setBackstory("A citizen with an unremarkable past.");
        }

        // Extract traits
        if (json.has("traits") && json.get("traits").isJsonArray()) {
            JsonArray traitsArray = json.getAsJsonArray("traits");
            List<String> validTraits = new ArrayList<>();

            for (int i = 0; i < traitsArray.size(); i++) {
                String traitId = traitsArray.get(i).getAsString();
                // Validate that the trait exists
                if (TraitRegistry.getTrait(traitId) != null) {
                    validTraits.add(traitId);
                } else {
                    LOGGER.warn("AI selected unknown trait: {}", traitId);
                }
            }

            // Ensure we have at least some traits
            if (validTraits.isEmpty()) {
                LOGGER.warn("No valid traits in AI response, adding random traits");
                // Fall back to random trait selection
                int minTraits = ModSettings.MIN_TRAITS.get();
                int maxTraits = ModSettings.MAX_TRAITS.get();
                CitizenBackground fallback = BackgroundGenerator.generate();
                validTraits.addAll(fallback.getTraits());
            }

            // Limit to max traits
            int maxTraits = ModSettings.MAX_TRAITS.get();
            for (int i = 0; i < Math.min(validTraits.size(), maxTraits); i++) {
                bg.addTrait(validTraits.get(i));
            }
        }

        // Ensure minimum traits
        int minTraits = ModSettings.MIN_TRAITS.get();
        while (bg.getTraits().size() < minTraits) {
            // Add random traits until we meet minimum
            CitizenBackground fallback = BackgroundGenerator.generate();
            for (String trait : fallback.getTraits()) {
                if (!bg.getTraits().contains(trait)) {
                    bg.addTrait(trait);
                    if (bg.getTraits().size() >= minTraits) break;
                }
            }
        }

        return bg;
    }

    private String buildGeneratorSystemPrompt() {
        return """
            You are a creative writer generating backgrounds for medieval colony simulation characters.
            Your task is to create a unique backstory and select appropriate personality traits.

            IMPORTANT: You must respond with ONLY a JSON object in this exact format:
            {
              "backstory": "A 2-3 sentence backstory describing the character's past and how they came to the colony.",
              "traits": ["trait_id_1", "trait_id_2", "trait_id_3"]
            }

            Guidelines for backstories:
            - Keep it concise (2-3 sentences)
            - Include where they came from and why they joined the colony
            - Reference their personality traits naturally
            - Make it feel medieval/fantasy appropriate

            Guidelines for trait selection:
            - Select 2-4 traits that work well together
            - The backstory should justify the traits
            - Mix positive and negative traits for interesting characters
            - Use ONLY the exact trait IDs provided in the list

            Do NOT include any text outside the JSON object.
            """;
    }

    private String buildGeneratorUserMessage(String name, String gender, String age, String job) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate a background for this citizen:\n");
        sb.append("Name: ").append(name).append("\n");
        sb.append("Gender: ").append(gender).append("\n");
        sb.append("Age: ").append(age).append("\n");
        sb.append("Job: ").append(job).append("\n\n");

        sb.append("Available traits (use these exact IDs):\n");

        for (TraitDefinition trait : TraitRegistry.getAllTraits()) {
            sb.append("- ").append(trait.id()).append(": ").append(trait.displayText());
            if (!trait.modifiers().isEmpty()) {
                sb.append(" [Effects: ");
                List<String> effects = new ArrayList<>();
                trait.modifiers().forEach((key, value) -> {
                    String effect = key + "=" + String.format("%.1f", value);
                    effects.add(effect);
                });
                sb.append(String.join(", ", effects));
                sb.append("]");
            }
            sb.append("\n");
        }

        sb.append("\nRespond with ONLY the JSON object, no other text.");
        return sb.toString();
    }

    /**
     * Cleans up any pending requests and listener.
     */
    public void shutdown() {
        // Complete all pending futures exceptionally
        pendingRequests.forEach((id, future) -> {
            if (!future.isDone()) {
                future.complete(BackgroundGenerator.generate());
            }
        });
        pendingRequests.clear();

        // Kill any remaining generator NPCs
        generatorNpcs.values().forEach(handle -> {
            if (handle.isAlive()) {
                handle.kill();
            }
        });
        generatorNpcs.clear();

        if (listener != null && listenerRegistered) {
            Player2NpcLib.removeListener(listener);
            listenerRegistered = false;
        }
    }
}
