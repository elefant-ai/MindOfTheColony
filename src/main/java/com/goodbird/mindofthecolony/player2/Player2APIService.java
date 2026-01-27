package com.goodbird.mindofthecolony.player2;

import com.goodbird.mindofthecolony.aibridge.ConversationHistory;
import com.goodbird.mindofthecolony.utils.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

import static com.goodbird.mindofthecolony.player2.HTTPUtils.sendRequest;

public class Player2APIService {

    public static JsonObject completeConversation(ConversationHistory conversationHistory) throws Exception {
        JsonObject requestBody = new JsonObject();
        JsonArray messagesArray = new JsonArray();
        for (JsonObject msg : conversationHistory.getListJSON())
            messagesArray.add(msg);
        requestBody.add("messages", messagesArray);
        Map<String, JsonElement> responseMap = sendRequest("/v1/chat/completions", true, requestBody);
        if (responseMap.containsKey("choices")) {
            JsonArray choices = responseMap.get("choices").getAsJsonArray();
            if (!choices.isEmpty()) {
                JsonObject messageObject = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                if (messageObject != null && messageObject.has("content")) {
                    String content = messageObject.get("content").getAsString();
                    return Utils.parseCleanedJson(content);
                }
            }
        }
        throw new Exception("Invalid response format: " + responseMap);
    }

    public static void sendHeartbeat() {
        try {
            System.out.println("Sending Heartbeat");
            Map<String, JsonElement> responseMap = sendRequest("/v1/health", false, null);
            if (responseMap.containsKey("client_version"))
                System.out.println("Heartbeat Successful");
        } catch (Exception e) {
            System.err.printf("Heartbeat Fail: %s", e.getMessage());
        }
    }
}

