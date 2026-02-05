package com.goodbird.mindofthecolony.event;

import com.goodbird.mindofthecolony.config.EventConfig;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages colony events - scheduling, evaluation, and history.
 * One manager instance per colony.
 */
public class ColonyEventManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColonyEventManager.class);

    // Manager instances per colony
    private static final Map<Integer, ColonyEventManager> managers = new ConcurrentHashMap<>();

    private final int colonyId;
    private final List<ColonyEvent> eventHistory = new ArrayList<>();
    private final List<EventEvaluator> evaluators = new ArrayList<>();
    private final WeatherState weatherState = new WeatherState();
    private final RandomSource random = RandomSource.create();

    private long lastEventCheckTick = 0;
    private long nextEventCheckTick = 0;

    private ColonyEventManager(int colonyId) {
        this.colonyId = colonyId;
    }

    /**
     * Get or create manager for a colony.
     */
    public static ColonyEventManager getInstance(int colonyId) {
        return managers.computeIfAbsent(colonyId, ColonyEventManager::new);
    }

    /**
     * Get manager if it exists, null otherwise.
     */
    public static ColonyEventManager getIfExists(int colonyId) {
        return managers.get(colonyId);
    }

    /**
     * Remove manager for a colony (e.g., when colony is deleted).
     */
    public static void removeInstance(int colonyId) {
        managers.remove(colonyId);
    }

    /**
     * Register an event evaluator.
     */
    public void registerEvaluator(EventEvaluator evaluator) {
        evaluators.add(evaluator);
        LOGGER.debug("Registered event evaluator: {}", evaluator.getEventType());
    }

    /**
     * Called each tick to update weather and potentially evaluate events.
     *
     * @param colony The colony to check
     * @param level  The server level
     * @param currentTick Current game tick
     */
    public void onTick(IColony colony, ServerLevel level, long currentTick) {
        // Update weather state every tick
        int ticksElapsed = (int) (currentTick - weatherState.getLastUpdateTick());
        if (ticksElapsed > 0) {
            weatherState.update(level, currentTick, ticksElapsed);
        }

        // Check if it's time to evaluate events
        if (currentTick >= nextEventCheckTick) {
            evaluateEvents(colony, level, currentTick);

            // Schedule next check
            int minTicks = EventConfig.getCheckIntervalMinTicks();
            int maxTicks = EventConfig.getCheckIntervalMaxTicks();
            int interval = minTicks + random.nextInt(maxTicks - minTicks + 1);
            lastEventCheckTick = currentTick;
            nextEventCheckTick = currentTick + interval;

            LOGGER.debug("Colony {} event check complete, next check in {} ticks", colonyId, interval);
        }
    }

    /**
     * Force an immediate event evaluation.
     */
    public void evaluateEvents(IColony colony, ServerLevel level, long currentTick) {
        if (evaluators.isEmpty()) {
            LOGGER.debug("No event evaluators registered for colony {}", colonyId);
            return;
        }

        // Build context
        List<ICitizenData> citizens = new ArrayList<>(colony.getCitizenManager().getCitizens());
        EventContext context = new EventContext(
            colony,
            level,
            weatherState,
            citizens,
            currentTick,
            random
        );

        // Run all evaluators
        List<ColonyEvent> newEvents = new ArrayList<>();
        for (EventEvaluator evaluator : evaluators) {
            if (!evaluator.isEnabled()) continue;

            try {
                List<ColonyEvent> events = evaluator.evaluate(context);
                newEvents.addAll(events);
            } catch (Exception e) {
                LOGGER.error("Error in event evaluator {}: {}", evaluator.getEventType(), e.getMessage());
            }
        }

        // Add new events to history
        for (ColonyEvent event : newEvents) {
            addEvent(event);
            LOGGER.info("Colony {} event triggered: {}", colonyId, event.getDescription());
        }
    }

    /**
     * Add an event to history.
     */
    public void addEvent(ColonyEvent event) {
        eventHistory.add(event);

        // Trim history if too large
        int maxHistory = EventConfig.getMaxEventHistory();
        while (eventHistory.size() > maxHistory) {
            eventHistory.remove(0);
        }
    }

    /**
     * Get recent events for LLM context.
     *
     * @param count Maximum number of events to return
     */
    public List<ColonyEvent> getRecentEvents(int count) {
        int start = Math.max(0, eventHistory.size() - count);
        return new ArrayList<>(eventHistory.subList(start, eventHistory.size()));
    }

    /**
     * Get all events in history.
     */
    public List<ColonyEvent> getAllEvents() {
        return new ArrayList<>(eventHistory);
    }

    /**
     * Get events as formatted text for LLM context.
     */
    public String getEventsAsContext(int count, long currentTick) {
        List<ColonyEvent> events = getRecentEvents(count);
        if (events.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("RECENT COLONY EVENTS:\n");
        for (ColonyEvent event : events) {
            long ticksAgo = currentTick - event.getTimestamp();
            String timeAgo = formatTicksAgo(ticksAgo);
            sb.append("- ").append(timeAgo).append(": ").append(event.getDescription()).append("\n");
        }
        return sb.toString();
    }

    private String formatTicksAgo(long ticks) {
        long minutes = ticks / 1200; // 20 ticks/sec * 60 sec
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        long days = hours / 24;
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }

    /**
     * Get the weather state tracker.
     */
    public WeatherState getWeatherState() {
        return weatherState;
    }

    /**
     * Serialize manager state to NBT.
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("colonyId", colonyId);
        tag.putLong("lastEventCheckTick", lastEventCheckTick);
        tag.putLong("nextEventCheckTick", nextEventCheckTick);
        tag.put("weatherState", weatherState.toNBT());

        ListTag eventList = new ListTag();
        for (ColonyEvent event : eventHistory) {
            eventList.add(event.toNBT());
        }
        tag.put("eventHistory", eventList);

        return tag;
    }

    /**
     * Load manager state from NBT.
     */
    public void loadFromNBT(CompoundTag tag) {
        lastEventCheckTick = tag.getLong("lastEventCheckTick");
        nextEventCheckTick = tag.getLong("nextEventCheckTick");

        if (tag.contains("weatherState", Tag.TAG_COMPOUND)) {
            WeatherState loaded = WeatherState.fromNBT(tag.getCompound("weatherState"));
            // Copy loaded state - we can't replace the final field
            // Instead, we rely on the weather state updating on next tick
        }

        eventHistory.clear();
        if (tag.contains("eventHistory", Tag.TAG_LIST)) {
            ListTag eventList = tag.getList("eventHistory", Tag.TAG_COMPOUND);
            for (int i = 0; i < eventList.size(); i++) {
                try {
                    ColonyEvent event = ColonyEvent.fromNBT(eventList.getCompound(i));
                    eventHistory.add(event);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load event from NBT: {}", e.getMessage());
                }
            }
        }

        LOGGER.debug("Loaded {} events for colony {}", eventHistory.size(), colonyId);
    }

    /**
     * Static method to load a manager from NBT.
     */
    public static ColonyEventManager fromNBT(CompoundTag tag) {
        int colonyId = tag.getInt("colonyId");
        ColonyEventManager manager = getInstance(colonyId);
        manager.loadFromNBT(tag);
        return manager;
    }
}
