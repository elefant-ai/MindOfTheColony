package com.goodbird.mindofthecolony.aibridge;

import com.goodbird.mindofthecolony.status.ObjectStatus;
import com.goodbird.mindofthecolony.utils.Utils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class ConversationHistory {
    private final List<JsonObject> conversationHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 64;

    private final String citizenName;

    public ConversationHistory(String initialSystemPrompt, String citizenName) {
        this.citizenName = citizenName;
        setBaseSystemPrompt(initialSystemPrompt);
    }

    public ConversationHistory(String initialSystemPrompt, String citizenName, CompoundTag nbt) {
        this.citizenName = citizenName;
        deserializeNBT(nbt);
        setBaseSystemPrompt(initialSystemPrompt);
    }

    private ConversationHistory(String citizenName) {
        this.citizenName = citizenName;
    }

    public void setBaseSystemPrompt(String newPrompt) {
        if (!this.conversationHistory.isEmpty() && "system".equals(this.conversationHistory.get(0).get("role").getAsString())) {
            this.conversationHistory.get(0).addProperty("content", newPrompt);
        } else {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", newPrompt);
            this.conversationHistory.add(0, systemMessage);
        }
    }

    public void addUserMessage(String userText) {
        addHistoryEntry("user", userText);
    }

    public void addAssistantMessage(String messageText) {
        addHistoryEntry("assistant", messageText);
    }

    private void addHistoryEntry(String role, String content) {
        JsonObject entry = new JsonObject();
        entry.addProperty("role", role);
        entry.addProperty("content", content);
        this.conversationHistory.add(entry);

        while (this.conversationHistory.size() > MAX_HISTORY) {
            this.conversationHistory.remove(1);
        }
    }

    public ConversationHistory copyThenWrapLatestWithStatus(String colonyStatusString, String agentStatusString, String systemMessagesString) {
        ConversationHistory copy = new ConversationHistory(this.citizenName);

        if (this.conversationHistory.isEmpty()) {
            return copy;
        }

        for (int i = 0; i < this.conversationHistory.size() - 1; i++) {
            copy.conversationHistory.add(Utils.deepCopy(this.conversationHistory.get(i)));
        }

        if (this.conversationHistory.size() > 1) {
            JsonObject lastMessage = this.conversationHistory.get(this.conversationHistory.size() - 1);
            JsonObject lastMessageCopy = Utils.deepCopy(lastMessage);

            if ("user".equals(lastMessageCopy.get("role").getAsString())) {
                String originalUserMessage = lastMessageCopy.get("content").getAsString();

                ObjectStatus contentWrapper = new ObjectStatus();
                contentWrapper.add("userMessage", originalUserMessage);
                contentWrapper.add("agentStatus", agentStatusString);
                contentWrapper.add("colonyStatus", colonyStatusString);
                contentWrapper.add("systemMessages", systemMessagesString);

                lastMessageCopy.addProperty("content", contentWrapper.toString());
            }

            copy.conversationHistory.add(lastMessageCopy);
        } else {
            copy.conversationHistory.add(Utils.deepCopy(this.conversationHistory.get(0)));
        }

        return copy;
    }


    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        ListTag historyList = new ListTag();
        for (JsonObject msg : conversationHistory) {
            historyList.add(StringTag.valueOf(msg.toString()));
        }
        nbt.put("history", historyList);
        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        conversationHistory.clear();
        if (nbt != null && nbt.contains("history", Tag.TAG_LIST)) {
            ListTag historyList = nbt.getList("history", Tag.TAG_STRING);
            for (Tag tag : historyList) {
                try {
                    JsonObject obj = JsonParser.parseString(tag.getAsString()).getAsJsonObject();
                    conversationHistory.add(obj);
                } catch (Exception e) {
                    System.err.println("Failed to parse conversation history entry for citizen " + citizenName + ": " + tag.getAsString());
                }
            }
        }
    }

    public List<JsonObject> getListJSON() {
        return conversationHistory;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConversationHistory for ").append(citizenName).append(" {\n");
        for (JsonObject message : this.conversationHistory) {
            String role = message.has("role") ? message.get("role").getAsString() : "unknown";
            String content = message.has("content") ? message.get("content").getAsString() : "";
            sb.append("  [").append(role).append("] ").append(content, 0, Math.min(content.length(), 150)).append("...\n");
        }
        sb.append("}");
        return sb.toString();
    }
}