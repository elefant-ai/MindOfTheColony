package com.goodbird.mindofthecolony.voice;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.minecolonies.api.colony.ICitizenData;
import game.player2.npc.Player2NpcLib;
import game.player2.npc.config.Player2Config;
import game.player2.npc.dto.TtsVoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selects a TTS voice for each citizen using the /v1/completions endpoint.
 * Fetches available voices, sends citizen details to the LLM, and returns the best matching voice ID.
 */
public class VoiceSelectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceSelectionService.class);
    private static final Gson GSON = new Gson();
    private static final VoiceSelectionService INSTANCE = new VoiceSelectionService();

    private volatile List<TtsVoice> cachedVoices = null;
    private final Object voiceCacheLock = new Object();

    // Prevent duplicate in-flight requests per citizen
    private final Set<Integer> inFlightRequests = ConcurrentHashMap.newKeySet();

    private volatile HttpClient httpClient;

    private VoiceSelectionService() {}

    public static VoiceSelectionService getInstance() {
        return INSTANCE;
    }

    /**
     * Selects a voice for the given citizen using the /v1/completions endpoint.
     * @return CompletableFuture with the selected voice ID, or null on failure.
     */
    public CompletableFuture<String> selectVoice(ICitizenData citizenData) {
        int citizenId = citizenData.getId();

        if (!inFlightRequests.add(citizenId)) {
            LOGGER.debug("Voice selection already in progress for citizen {}", citizenData.getName());
            return CompletableFuture.completedFuture(null);
        }

        String citizenGender = citizenData.isFemale() ? "female" : "male";

        return getOrFetchVoices()
            .thenCompose(voices -> {
                if (voices == null || voices.isEmpty()) {
                    LOGGER.warn("No TTS voices available, cannot select voice for {}", citizenData.getName());
                    return CompletableFuture.completedFuture(null);
                }

                // Filter voices to match the citizen's gender
                List<TtsVoice> genderFiltered = voices.stream()
                    .filter(v -> v.getGender() != null && v.getGender().equalsIgnoreCase(citizenGender))
                    .toList();

                // Fall back to all voices if no gender match found
                List<TtsVoice> candidates = genderFiltered.isEmpty() ? voices : genderFiltered;

                return callCompletionsEndpoint(citizenData, candidates);
            })
            .whenComplete((result, ex) -> {
                inFlightRequests.remove(citizenId);
                if (ex != null) {
                    LOGGER.error("Voice selection failed for {}: {}", citizenData.getName(), ex.getMessage());
                }
            });
    }

    private CompletableFuture<List<TtsVoice>> getOrFetchVoices() {
        if (cachedVoices != null) {
            return CompletableFuture.completedFuture(cachedVoices);
        }

        return Player2NpcLib.ttsGetVoices()
            .thenApply(voices -> {
                synchronized (voiceCacheLock) {
                    cachedVoices = voices;
                }
                LOGGER.info("Cached {} TTS voices", voices.size());
                return voices;
            })
            .exceptionally(ex -> {
                LOGGER.error("Failed to fetch TTS voices: {}", ex.getMessage());
                return null;
            });
    }

    private CompletableFuture<String> callCompletionsEndpoint(ICitizenData citizenData, List<TtsVoice> voices) {
        String prompt = buildPrompt(citizenData, voices);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("prompt", prompt);
        requestBody.addProperty("max_tokens", 50);
        requestBody.addProperty("temperature", 0.3);

        String url = Player2Config.getApiBaseUrl() + "/v1/completions";
        String body = GSON.toJson(requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json; charset=utf-8")
            .header("player2-game-key", Player2Config.getGameKey())
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(15))
            .build();

        return getOrCreateHttpClient()
            .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    LOGGER.error("Completions API returned HTTP {}: {}", response.statusCode(), response.body());
                    return null;
                }
                return parseVoiceIdFromResponse(response.body(), voices);
            })
            .exceptionally(ex -> {
                LOGGER.error("Failed to call completions API: {}", ex.getMessage());
                return null;
            });
    }

    private String buildPrompt(ICitizenData citizenData, List<TtsVoice> voices) {
        String name = citizenData.getName();
        String gender = citizenData.isFemale() ? "female" : "male";
        String age = citizenData.isChild() ? "child" : "adult";
        String job = citizenData.getJob() != null
            ? citizenData.getJob().getJobRegistryEntry().getKey().getPath()
            : "unemployed";

        String backstory = "";
        String traits = "";
        if (citizenData instanceof IExtendedCitizenData extData) {
            CitizenBackground bg = extData.getCitizenBackground();
            if (bg != null && bg.isInitialized()) {
                backstory = bg.getBackstory() != null ? bg.getBackstory() : "";
                traits = String.join(", ", bg.getTraits());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Select the most appropriate TTS voice for this character.\n\n");
        sb.append("Character:\n");
        sb.append("- Name: ").append(name).append("\n");
        sb.append("- Gender: ").append(gender).append("\n");
        sb.append("- Age: ").append(age).append("\n");
        sb.append("- Job: ").append(job).append("\n");
        if (!backstory.isEmpty()) {
            sb.append("- Backstory: ").append(backstory).append("\n");
        }
        if (!traits.isEmpty()) {
            sb.append("- Personality traits: ").append(traits).append("\n");
        }

        sb.append("\nAvailable voices:\n");
        for (TtsVoice voice : voices) {
            sb.append("- ID: ").append(voice.getId());
            sb.append(", Name: ").append(voice.getName());
            if (voice.getLanguage() != null) {
                sb.append(", Language: ").append(voice.getLanguage());
            }
            if (voice.getGender() != null) {
                sb.append(", Gender: ").append(voice.getGender());
            }
            sb.append("\n");
        }

        sb.append("\nRespond with ONLY the voice ID that best matches this character. ");
        sb.append("Consider gender, age, job, and personality when choosing. ");
        sb.append("Output just the ID, nothing else.");

        return sb.toString();
    }

    /**
     * Parses the /v1/completions response (OpenAI-compatible format).
     * Expected: { "choices": [{ "text": "voice-id-here" }] }
     */
    @Nullable
    private String parseVoiceIdFromResponse(String responseBody, List<TtsVoice> voices) {
        try {
            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                LOGGER.warn("No choices in completions response");
                return null;
            }

            String rawText = choices.get(0).getAsJsonObject().get("text").getAsString().trim();

            // Clean up whitespace and quotes
            String candidateId = rawText.replaceAll("[\"'\\s]", "");

            // Exact match against available voices
            for (TtsVoice voice : voices) {
                if (voice.getId().equals(candidateId)) {
                    LOGGER.info("AI selected voice '{}' ({})", voice.getId(), voice.getName());
                    return voice.getId();
                }
            }

            // Fuzzy match: check if the response contains any voice ID
            for (TtsVoice voice : voices) {
                if (rawText.contains(voice.getId())) {
                    LOGGER.info("AI selected voice '{}' ({}) via fuzzy match from: {}",
                        voice.getId(), voice.getName(), rawText);
                    return voice.getId();
                }
            }

            LOGGER.warn("AI returned unrecognized voice ID '{}', available: {}",
                rawText, voices.stream().map(TtsVoice::getId).toList());
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to parse completions response: {}", e.getMessage());
            return null;
        }
    }

    private HttpClient getOrCreateHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        }
        return httpClient;
    }

    public void shutdown() {
        cachedVoices = null;
        inFlightRequests.clear();
        LOGGER.info("VoiceSelectionService shut down");
    }
}
