package com.goodbird.mindofthecolony.interaction;

import com.goodbird.mindofthecolony.config.NpcInteractionConfig;

/**
 * Represents an active conversation between two NPCs.
 * Manages turn-taking and conversation state.
 */
public class NpcConversation {
    private final int initiatorId;
    private final int responderId;
    private final int colonyId;
    private final long startTick;
    private final int maxTurns;

    private int currentTurn = 0;
    private int currentSpeakerId;
    private long nextTurnTick;
    private long lastRequestTick;
    private boolean waitingForResponse = false;
    private ConversationState state = ConversationState.STARTING;
    private String lastMessage = null;

    public enum ConversationState {
        STARTING,      // Initial greeting phase
        ACTIVE,        // Mid-conversation
        ENDING,        // Wrapping up
        COMPLETED,     // Finished normally
        ABORTED        // Ended due to distance/timeout/interruption
    }

    public NpcConversation(int initiatorId, int responderId, int colonyId, long startTick) {
        this.initiatorId = initiatorId;
        this.responderId = responderId;
        this.colonyId = colonyId;
        this.startTick = startTick;

        // Initiator speaks first
        this.currentSpeakerId = initiatorId;
        this.nextTurnTick = startTick;

        // Randomize max turns within config range
        var config = NpcInteractionConfig.getConversationConfig();
        this.maxTurns = config.minTurns +
            (int) (Math.random() * (config.maxTurns - config.minTurns + 1));
    }

    public int getInitiatorId() {
        return initiatorId;
    }

    public int getResponderId() {
        return responderId;
    }

    public int getColonyId() {
        return colonyId;
    }

    public long getStartTick() {
        return startTick;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public int getMaxTurns() {
        return maxTurns;
    }

    public int getCurrentSpeakerId() {
        return currentSpeakerId;
    }

    public int getListenerId() {
        return currentSpeakerId == initiatorId ? responderId : initiatorId;
    }

    public long getNextTurnTick() {
        return nextTurnTick;
    }

    public boolean isWaitingForResponse() {
        return waitingForResponse;
    }

    public long getLastRequestTick() {
        return lastRequestTick;
    }

    public ConversationState getState() {
        return state;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    /**
     * Checks if either citizen is involved in this conversation.
     */
    public boolean involvesCitizen(int citizenId) {
        return citizenId == initiatorId || citizenId == responderId;
    }

    /**
     * Checks if it's time for the next turn.
     */
    public boolean isReadyForNextTurn(long currentTick) {
        return !waitingForResponse && currentTick >= nextTurnTick;
    }

    /**
     * Checks if the conversation has timed out waiting for response.
     */
    public boolean hasTimedOut(long currentTick) {
        if (!waitingForResponse) {
            return false;
        }
        var config = NpcInteractionConfig.getConversationConfig();
        return currentTick - lastRequestTick > config.responseTimeoutTicks;
    }

    /**
     * Checks if the conversation should end naturally.
     */
    public boolean shouldEnd() {
        return currentTurn >= maxTurns || state == ConversationState.ENDING;
    }

    /**
     * Checks if the conversation is finished (completed or aborted).
     */
    public boolean isFinished() {
        return state == ConversationState.COMPLETED || state == ConversationState.ABORTED;
    }

    /**
     * Marks that we're waiting for an NPC response.
     */
    public void markWaitingForResponse(long currentTick) {
        this.waitingForResponse = true;
        this.lastRequestTick = currentTick;
    }

    /**
     * Handles receiving a response from the current speaker.
     */
    public void handleResponse(String message, long currentTick) {
        this.lastMessage = message;
        this.waitingForResponse = false;
        this.currentTurn++;

        // Update state based on turn count
        if (currentTurn >= maxTurns) {
            this.state = ConversationState.ENDING;
        } else if (state == ConversationState.STARTING) {
            this.state = ConversationState.ACTIVE;
        }

        // Switch speaker
        this.currentSpeakerId = getListenerId();

        // Schedule next turn with delay
        var config = NpcInteractionConfig.getConversationConfig();
        this.nextTurnTick = currentTick + config.turnDelayTicks;
    }

    /**
     * Marks the conversation as ending (will wrap up on next turn).
     */
    public void markEnding() {
        this.state = ConversationState.ENDING;
    }

    /**
     * Marks the conversation as completed successfully.
     */
    public void complete() {
        this.state = ConversationState.COMPLETED;
        this.waitingForResponse = false;
    }

    /**
     * Aborts the conversation due to an issue.
     */
    public void abort(String reason) {
        this.state = ConversationState.ABORTED;
        this.waitingForResponse = false;
    }

    @Override
    public String toString() {
        return "NpcConversation{" +
            "citizens=" + initiatorId + "<->" + responderId +
            ", colony=" + colonyId +
            ", turn=" + currentTurn + "/" + maxTurns +
            ", state=" + state +
            ", waiting=" + waitingForResponse +
            '}';
    }
}
