package com.goodbird.mindofthecolony.interaction;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.goodbird.mindofthecolony.config.NpcInteractionConfig;
import com.goodbird.mindofthecolony.event.ColonyEvent;
import com.goodbird.mindofthecolony.event.EventContext;
import com.goodbird.mindofthecolony.event.EventEvaluator;
import com.minecolonies.api.colony.ICitizenData;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Evaluates citizen proximity and initiates NPC-to-NPC conversations.
 * Uses spatial hashing for efficient O(n) proximity detection.
 */
public class NpcProximityEvaluator implements EventEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(NpcProximityEvaluator.class);

    private long lastCheckTick = 0;

    @Override
    public String getEventType() {
        return "npc_interaction";
    }

    @Override
    public boolean isEnabled() {
        return NpcInteractionConfig.isEnabled();
    }

    @Override
    public List<ColonyEvent> evaluate(EventContext context) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }

        // Check if enough time has passed since last check
        var proximityConfig = NpcInteractionConfig.getProximityConfig();
        if (context.currentTick() - lastCheckTick < proximityConfig.checkIntervalTicks) {
            return Collections.emptyList();
        }
        lastCheckTick = context.currentTick();

        // Find nearby citizen pairs
        List<CitizenPair> nearbyPairs = findNearbyPairs(
            context.citizens(),
            proximityConfig.interactionRadius
        );

        if (nearbyPairs.isEmpty()) {
            return Collections.emptyList();
        }

        // Get conversation manager for this colony
        NpcConversationManager convManager = NpcConversationManager.getInstance(context.colony().getID());

        // Limit pairs to check for performance
        var perfConfig = NpcInteractionConfig.getPerformanceConfig();
        int pairsToCheck = Math.min(nearbyPairs.size(), perfConfig.maxCitizenPairsToCheck);

        // Shuffle to randomize which pairs get checked
        Collections.shuffle(nearbyPairs);

        int conversationsStarted = 0;
        var convConfig = NpcInteractionConfig.getConversationConfig();

        for (int i = 0; i < pairsToCheck && conversationsStarted < perfConfig.maxActiveConversationsPerColony; i++) {
            CitizenPair pair = nearbyPairs.get(i);

            // Check if both citizens can start a conversation
            if (!convManager.canStartConversation(pair.citizen1.getId(), context.currentTick()) ||
                !convManager.canStartConversation(pair.citizen2.getId(), context.currentTick())) {
                continue;
            }

            // Calculate conversation chance with relationship modifier
            double baseChance = convConfig.startChance;
            CitizenRelationship relationship = convManager.getRelationship(
                pair.citizen1.getId(), pair.citizen2.getId());

            double chanceModifier = 1.0;
            if (relationship != null) {
                chanceModifier = relationship.getConversationChanceModifier();
            }

            double finalChance = baseChance * chanceModifier;

            // Roll for conversation
            if (context.random().nextDouble() < finalChance) {
                // Start conversation - randomly pick initiator
                ICitizenData initiator, responder;
                if (context.random().nextBoolean()) {
                    initiator = pair.citizen1;
                    responder = pair.citizen2;
                } else {
                    initiator = pair.citizen2;
                    responder = pair.citizen1;
                }

                convManager.startConversation(initiator.getId(), responder.getId(), context.currentTick());
                conversationsStarted++;

                LOGGER.debug("Started NPC conversation between {} and {} (chance: {:.1f}%)",
                    initiator.getName(), responder.getName(), finalChance * 100);
            }
        }

        // This evaluator doesn't produce ColonyEvents - it directly manages conversations
        return Collections.emptyList();
    }

    /**
     * Find all pairs of citizens within interaction radius using spatial hashing.
     * This is O(n) instead of O(n^2) for large numbers of citizens.
     */
    private List<CitizenPair> findNearbyPairs(List<ICitizenData> citizens, double radius) {
        if (citizens.size() < 2) {
            return Collections.emptyList();
        }

        int cellSize = Math.max(1, (int) Math.ceil(radius));
        Map<Long, List<ICitizenData>> grid = new HashMap<>();

        // Assign citizens to grid cells
        for (ICitizenData citizen : citizens) {
            if (citizen.getEntity().isEmpty()) {
                continue;
            }

            BlockPos pos = citizen.getEntity().get().blockPosition();
            long cellKey = packCell(pos.getX() / cellSize, pos.getZ() / cellSize);
            grid.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(citizen);
        }

        List<CitizenPair> pairs = new ArrayList<>();
        double radiusSq = radius * radius;

        // Check each cell and adjacent cells
        Set<Long> processedCells = new HashSet<>();
        for (Map.Entry<Long, List<ICitizenData>> entry : grid.entrySet()) {
            long cellKey = entry.getKey();
            if (processedCells.contains(cellKey)) {
                continue;
            }
            processedCells.add(cellKey);

            int cx = unpackX(cellKey);
            int cz = unpackZ(cellKey);

            List<ICitizenData> cellCitizens = entry.getValue();

            // Check within this cell
            for (int i = 0; i < cellCitizens.size(); i++) {
                for (int j = i + 1; j < cellCitizens.size(); j++) {
                    ICitizenData c1 = cellCitizens.get(i);
                    ICitizenData c2 = cellCitizens.get(j);
                    if (isWithinRadius(c1, c2, radiusSq)) {
                        pairs.add(new CitizenPair(c1, c2));
                    }
                }
            }

            // Check adjacent cells (only half to avoid duplicates)
            int[][] offsets = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}};
            for (int[] offset : offsets) {
                long neighborKey = packCell(cx + offset[0], cz + offset[1]);
                List<ICitizenData> neighborCitizens = grid.get(neighborKey);
                if (neighborCitizens == null) {
                    continue;
                }

                for (ICitizenData c1 : cellCitizens) {
                    for (ICitizenData c2 : neighborCitizens) {
                        if (isWithinRadius(c1, c2, radiusSq)) {
                            pairs.add(new CitizenPair(c1, c2));
                        }
                    }
                }
            }
        }

        return pairs;
    }

    private boolean isWithinRadius(ICitizenData c1, ICitizenData c2, double radiusSq) {
        if (c1.getEntity().isEmpty() || c2.getEntity().isEmpty()) {
            return false;
        }
        return c1.getEntity().get().distanceToSqr(c2.getEntity().get()) <= radiusSq;
    }

    private long packCell(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private int unpackX(long key) {
        return (int) (key >> 32);
    }

    private int unpackZ(long key) {
        return (int) key;
    }

    /**
     * Represents a pair of citizens that are near each other.
     */
    private record CitizenPair(ICitizenData citizen1, ICitizenData citizen2) {}
}
