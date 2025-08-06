package com.goodbird.mindofthecolony.aibridge;

import com.goodbird.mindofthecolony.CitizenAIManager;
import com.goodbird.mindofthecolony.player2.Player2APIService;
import com.goodbird.mindofthecolony.status.AgentStatus;
import com.goodbird.mindofthecolony.status.ColonyStatus;
import com.goodbird.mindofthecolony.utils.MessageBuffer;
import com.goodbird.mindofthecolony.utils.Utils;
import com.google.gson.JsonObject;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.jobs.IJob;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class CitizenAIBridge {
    private final ICitizenData citizenData;
    private final ConversationHistory conversationHistory;
    private final MessageBuffer systemMessageBuffer = new MessageBuffer(10);
    private final Queue<String> messageQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService llmThread = Executors.newSingleThreadExecutor();
    private static long lastHeartbeatTime = System.nanoTime();

    private boolean isWaitingToSpeak = false;

    private static final String INITIAL_PROMPT_TEMPLATE =
            """
                    General Instructions:
                    You are an AI-NPC, a resident of a Minecraft colony from the MineColonies mod. You have a distinct personality and job. You can provide information about your life, your colony, and Minecraft in general from your character's perspective. You can also chat as a friend with the player.
                    You take the personality of the following character:
                    Your name is {{citizenName}}.
                    {{citizenDescription}}
                    User Message Format:
                    The user's message will be part of a JSON object with additional context about your status and the colony's status:
                    {
                        "userMessage": "The message sent to you. It could be from a player or another citizen.",
                        "agentStatus": "Your current status as a citizen.",
                        "colonyStatus": "The status of your colony.",
                        "systemMessages": "Recent events or system logs relevant to you."
                    }
                    Response Format:
                    Respond with a JSON object containing `reason` and `message` strings.
                    {
                      "reason": "Provide step-by-step reasoning for your response based on your character, current status, and the conversation history.",
                      "message": "Create a natural, conversational message that aligns with your reasoning and character. Keep it concise (under 250 characters)."
                    }
                    Additional Guidelines:
                    Stay in character. Your responses should reflect your job, skills, and mood. Be aware of your surroundings (agentStatus, colonyStatus) and incorporate them into your conversation. If you have nothing to say in response to what you heard, provide an empty string for the "message" field. If the player asks another colonist by name, don't reply.
                    """;


    public CitizenAIBridge(ICitizenData citizenData, @Nullable CompoundTag historyNBT) {
        this.citizenData = citizenData;
        String citizenDescription = generateCitizenDescription();
        String prompt = Utils.replacePlaceholders(INITIAL_PROMPT_TEMPLATE, Map.of(
                "citizenName", citizenData.getName(),
                "citizenDescription", citizenDescription
        ));
        this.conversationHistory = (historyNBT != null)
                ? new ConversationHistory(prompt, citizenData.getName(), historyNBT)
                : new ConversationHistory(prompt, citizenData.getName());
    }

    private String generateCitizenDescription() {
        IJob<?> job = citizenData.getJob();
        String jobName = (job != null) ? job.getJobRegistryEntry().getKey().getPath() : "unemployed";
        return String.format("You are a %s in the colony of %s. You are currently feeling %s.",
                jobName,
                citizenData.getColony().getName(),
                getMood());
    }

    private String getMood() {
        double happiness = citizenData.getCitizenHappinessHandler().getHappiness(citizenData.getColony(), citizenData);
        if (happiness > 8) return "very happy";
        if (happiness > 6) return "content";
        if (happiness > 4) return "a bit down";
        return "unhappy";
    }

    public void addSystemMessage(String message) {
        systemMessageBuffer.addMsg(message);
    }

    public void addPlayerMessageToQueue(String playerName, String message) {
        messageQueue.offer(String.format("[%s]: %s", playerName, message));
    }

    public void addOtherCitizenMessageToContext(String senderName, String message) {
        conversationHistory.addUserMessage(String.format("[%s]: %s", senderName, message));
    }


    public void onTick() {
        if (!messageQueue.isEmpty() && !isWaitingToSpeak) {
            isWaitingToSpeak = true;
            CitizenAIManager.getInstance().requestToSpeak(this);
        }

        long now = System.nanoTime();
        if (now - lastHeartbeatTime > 60_000_000_000L) {
            sendHeartbeat();
            lastHeartbeatTime = now;
        }
    }

    public void processLlmRequest(ReentrantLock lock) {
        if (messageQueue.isEmpty()) {
            isWaitingToSpeak = false;
            lock.unlock();
            return;
        }

        String message = messageQueue.poll();
        conversationHistory.addUserMessage(message);

        llmThread.submit(() -> {
            try {
                String agentStatus = AgentStatus.fromCitizenAndColony(citizenData).toString();
                String colonyStatus = ColonyStatus.fromColony(citizenData.getColony()).toString();
                String systemMessages = systemMessageBuffer.dumpAndGetString();

                ConversationHistory historyWithStatus = conversationHistory.copyThenWrapLatestWithStatus(colonyStatus, agentStatus, systemMessages);

                JsonObject response = Player2APIService.completeConversation(historyWithStatus);
                String responseAsString = response.toString();
                conversationHistory.addAssistantMessage(responseAsString);

                String llmMessage = Utils.getStringJsonSafely(response, "message");
                if (llmMessage != null && !llmMessage.isEmpty()) {
                    CitizenAIManager.getInstance().broadcastCitizenMessage(citizenData, llmMessage);
                }
            } catch (Exception e) {
                System.err.println("Error communicating with AI API for citizen " + citizenData.getName() + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                isWaitingToSpeak = false;
                lock.unlock();
            }
        });
    }

    public ConversationHistory getConversationHistory() {
        return conversationHistory;
    }

    public ICitizenData getCitizenData() {
        return citizenData;
    }

    public void shutdown() {
        llmThread.shutdownNow();
    }

    public void sendHeartbeat() {
        llmThread.submit(Player2APIService::sendHeartbeat);
    }
}