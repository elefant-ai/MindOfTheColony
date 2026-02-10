package com.goodbird.mindofthecolony.interaction;

import com.goodbird.mindofthecolony.config.NpcInteractionConfig;
import net.minecraft.nbt.CompoundTag;

/**
 * Tracks the relationship between two citizens.
 * Citizen IDs are stored in sorted order to ensure consistent key generation.
 */
public class CitizenRelationship {
    private final int citizen1Id;
    private final int citizen2Id;
    private int conversationCount;
    private long lastConversationTick;
    private float affinity;  // -1.0 (hostile) to 1.0 (friendly)

    public CitizenRelationship(int citizenA, int citizenB) {
        // Store IDs in sorted order for consistent lookup
        if (citizenA <= citizenB) {
            this.citizen1Id = citizenA;
            this.citizen2Id = citizenB;
        } else {
            this.citizen1Id = citizenB;
            this.citizen2Id = citizenA;
        }
        this.conversationCount = 0;
        this.lastConversationTick = 0;
        this.affinity = 0.0f;  // Start as strangers
    }

    private CitizenRelationship(int citizen1Id, int citizen2Id, int conversationCount,
                                long lastConversationTick, float affinity) {
        this.citizen1Id = citizen1Id;
        this.citizen2Id = citizen2Id;
        this.conversationCount = conversationCount;
        this.lastConversationTick = lastConversationTick;
        this.affinity = affinity;
    }

    /**
     * Generates a unique key for this relationship pair.
     */
    public static long makeKey(int citizenA, int citizenB) {
        int low = Math.min(citizenA, citizenB);
        int high = Math.max(citizenA, citizenB);
        return ((long) low << 32) | (high & 0xFFFFFFFFL);
    }

    public long getKey() {
        return makeKey(citizen1Id, citizen2Id);
    }

    public int getCitizen1Id() {
        return citizen1Id;
    }

    public int getCitizen2Id() {
        return citizen2Id;
    }

    public int getConversationCount() {
        return conversationCount;
    }

    public long getLastConversationTick() {
        return lastConversationTick;
    }

    public float getAffinity() {
        return affinity;
    }

    /**
     * Returns relationship level description for prompts.
     */
    public String getRelationshipLevel() {
        var config = NpcInteractionConfig.getRelationshipConfig();
        if (affinity >= config.friendlyThreshold) {
            return "friend";
        } else if (affinity >= config.acquaintanceThreshold) {
            return "acquaintance";
        } else if (affinity <= -config.friendlyThreshold) {
            return "rival";
        } else {
            return "stranger";
        }
    }

    /**
     * Records that a conversation happened between these citizens.
     */
    public void recordConversation(long currentTick, boolean completedSuccessfully) {
        this.conversationCount++;
        this.lastConversationTick = currentTick;

        var config = NpcInteractionConfig.getRelationshipConfig();
        if (completedSuccessfully) {
            adjustAffinity(config.affinityGainOnSuccess);
        } else {
            adjustAffinity(config.affinityGainOnInterrupt);
        }
    }

    /**
     * Records an argument or negative interaction.
     */
    public void recordArgument() {
        var config = NpcInteractionConfig.getRelationshipConfig();
        adjustAffinity(-config.affinityLossOnArgument);
    }

    /**
     * Adjusts affinity within bounds.
     */
    private void adjustAffinity(float delta) {
        this.affinity = Math.max(-1.0f, Math.min(1.0f, this.affinity + delta));
    }

    /**
     * Returns the modifier for conversation start chance based on affinity.
     * Higher affinity = more likely to start conversations.
     */
    public float getConversationChanceModifier() {
        // Strangers: 1.0, Friends: up to 2.0, Rivals: 0.5
        return 1.0f + (affinity * 0.5f);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("citizen1Id", citizen1Id);
        tag.putInt("citizen2Id", citizen2Id);
        tag.putInt("conversationCount", conversationCount);
        tag.putLong("lastConversationTick", lastConversationTick);
        tag.putFloat("affinity", affinity);
        return tag;
    }

    public static CitizenRelationship fromNBT(CompoundTag tag) {
        return new CitizenRelationship(
            tag.getInt("citizen1Id"),
            tag.getInt("citizen2Id"),
            tag.getInt("conversationCount"),
            tag.getLong("lastConversationTick"),
            tag.getFloat("affinity")
        );
    }

    @Override
    public String toString() {
        return "CitizenRelationship{" +
            "citizens=" + citizen1Id + "<->" + citizen2Id +
            ", count=" + conversationCount +
            ", affinity=" + String.format("%.2f", affinity) +
            ", level=" + getRelationshipLevel() +
            '}';
    }
}
