package com.goodbird.mindofthecolony;

import com.goodbird.mindofthecolony.aibridge.CitizenAIBridge;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.ICitizenData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class CitizenAIManager {
    public static final int PLAYER_INPUT_CHAT_RADIUS = 16;
    public static final int LISTEN_CHAT_RADIUS = 16;

    private static final CitizenAIManager INSTANCE = new CitizenAIManager();
    private final Map<Integer, CitizenAIBridge> activeAIs = new ConcurrentHashMap<>();

    private final ReentrantLock llmMutex = new ReentrantLock();
    private final Queue<CitizenAIBridge> responseQueue = new ConcurrentLinkedQueue<>();

    private CitizenAIManager() {
    }

    public static CitizenAIManager getInstance() {
        return INSTANCE;
    }

    public void onCitizenLoad(ICitizenData citizenData) {
        if (citizenData == null || activeAIs.containsKey(citizenData.getId())) {
            return;
        }
        CompoundTag historyNBT = (citizenData instanceof IExtendedCitizenData) ? ((IExtendedCitizenData) citizenData).getLoadedConversationHistoryNBT() : null;
        CitizenAIBridge aiBridge = new CitizenAIBridge(citizenData, historyNBT);
        activeAIs.put(citizenData.getId(), aiBridge);
        System.out.println("AI Bridge created for citizen: " + citizenData.getName());
    }

    public void onCitizenUnload(int citizenId) {
        CitizenAIBridge removedBridge = activeAIs.remove(citizenId);
        if (removedBridge != null) {
            removedBridge.shutdown();
        }
        System.out.println("AI Bridge removed for citizen ID: " + citizenId);
    }

    @Nullable
    public CitizenAIBridge getAIBridge(int citizenId) {
        return activeAIs.get(citizenId);
    }

    public void broadcastPlayerMessage(Player player, String message) {
        activeAIs.values().forEach(bridge -> {
            if (bridge.getCitizenData().getEntity().isPresent() &&
                    bridge.getCitizenData().getEntity().get().distanceToSqr(player) < PLAYER_INPUT_CHAT_RADIUS * PLAYER_INPUT_CHAT_RADIUS) {
                bridge.addPlayerMessageToQueue(player.getName().getString(), message);
            }
        });
    }

    public void broadcastCitizenMessage(ICitizenData sender, String message) {
        if (sender.getEntity().isEmpty()) return;

        sender.getColony().getMessagePlayerEntities().stream()
                .filter(player -> player.distanceTo(sender.getEntity().get()) < LISTEN_CHAT_RADIUS)
                .forEach(player -> player.sendSystemMessage(Component.literal("<" + sender.getName() + "> " + message)));

        activeAIs.values().forEach(bridge -> {
            if (bridge.getCitizenData().getId() != sender.getId() &&
                    bridge.getCitizenData().getEntity().isPresent() &&
                    bridge.getCitizenData().getEntity().get().distanceToSqr(sender.getEntity().get()) < LISTEN_CHAT_RADIUS * LISTEN_CHAT_RADIUS) {
                bridge.addOtherCitizenMessageToContext(sender.getName(), message);
            }
        });
    }

    public void requestToSpeak(CitizenAIBridge bridge) {
        if (!responseQueue.contains(bridge)) {
            responseQueue.add(bridge);
        }
    }

    public void onServerTick() {
        for (CitizenAIBridge aiBridge : activeAIs.values()) {
            aiBridge.onTick();
        }

        if (!responseQueue.isEmpty() && llmMutex.tryLock()) {
            CitizenAIBridge nextToSpeak = responseQueue.poll();
            if (nextToSpeak != null) {
                nextToSpeak.processLlmRequest(llmMutex);
            } else {
                llmMutex.unlock();
            }
        }
    }

    public void clearAllAIs() {
        activeAIs.forEach((id, bridge) -> bridge.shutdown());
        activeAIs.clear();
    }
}