package com.goodbird.mindofthecolony.events;

import com.goodbird.mindofthecolony.config.ModSettings;
import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.entity.citizen.happiness.ExpirationBasedHappinessModifier;
import com.minecolonies.api.entity.citizen.happiness.StaticHappinessSupplier;
import com.minecolonies.api.eventbus.events.colony.ColonyDeletedModEvent;
import com.minecolonies.api.eventbus.events.colony.buildings.BuildingConstructionModEvent;
import com.minecolonies.api.eventbus.events.colony.citizens.CitizenAddedModEvent;
import com.minecolonies.api.eventbus.events.colony.citizens.CitizenDiedModEvent;
import com.minecolonies.api.eventbus.events.colony.citizens.CitizenJobChangedModEvent;
import com.minecolonies.api.util.MessageUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton orchestrator for the colony events system.
 * Manages all ColonyEventLog instances, subscribes to game events,
 * and drives periodic event generation.
 */
public class ColonyEventManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColonyEventManager.class);
    private static final ColonyEventManager INSTANCE = new ColonyEventManager();

    private final Map<Integer, ColonyEventLog> eventLogs = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> wasUnderAttack = new HashMap<>();
    private ColonyEventsSavedData savedData;
    private int tickCounter = 0;

    private ColonyEventManager() {}

    public static ColonyEventManager getInstance() {
        return INSTANCE;
    }

    // ========== Lifecycle ==========

    /**
     * Initialize the event system. Called on server start.
     */
    public void initialize(MinecraftServer server) {
        // Load persisted event data from world save
        ServerLevel overworld = server.overworld();
        savedData = ColonyEventsSavedData.getOrCreate(overworld);

        // Subscribe to MineColonies events
        subscribeToMineColoniesEvents();

        LOGGER.info("Colony event manager initialized with {} colony logs", eventLogs.size());
    }

    /**
     * Called every server tick. Runs event processing at configured intervals.
     */
    public void onServerTick(MinecraftServer server) {
        if (!ModSettings.EVENTS_ENABLED.get()) return;

        tickCounter++;
        if (tickCounter < ModSettings.EVENT_CHECK_INTERVAL_TICKS.get()) return;
        tickCounter = 0;

        // Process all known colonies
        for (IColony colony : IColonyManager.getInstance().getAllColonies()) {
            ColonyEventLog log = getOrCreateLog(colony.getID());
            int currentDay = colony.getDay();

            // Expire old events
            log.tick(currentDay);

            // Poll for state-based events (raid detection)
            pollColonyState(colony, log, currentDay);

            // Try to generate a flavor event
            tryGenerateFlavorEvent(colony, log, currentDay);

            // Persist if dirty
            if (log.isDirty()) {
                markDirty();
                log.clearDirty();
            }
        }
    }

    /**
     * Clean up on server shutdown.
     */
    public void shutdown() {
        if (savedData != null) {
            savedData.setDirty();
        }
        eventLogs.clear();
        wasUnderAttack.clear();
        savedData = null;
        tickCounter = 0;
        LOGGER.info("Colony event manager shut down");
    }

    // ========== MineColonies EventBus Subscriptions ==========

    private void subscribeToMineColoniesEvents() {
        var bus = IMinecoloniesAPI.getInstance().getEventBus();

        bus.subscribe(CitizenDiedModEvent.class, this::onCitizenDied);
        bus.subscribe(CitizenAddedModEvent.class, this::onCitizenAdded);
        bus.subscribe(BuildingConstructionModEvent.class, this::onBuildingConstruction);
        bus.subscribe(CitizenJobChangedModEvent.class, this::onCitizenJobChanged);
        bus.subscribe(ColonyDeletedModEvent.class, this::onColonyDeleted);

        LOGGER.info("Subscribed to MineColonies event bus");
    }

    // ========== Event Handlers ==========

    private void onCitizenDied(CitizenDiedModEvent event) {
        if (!ModSettings.EVENTS_ENABLED.get()) return;

        IColony colony = event.getColony();
        String citizenName = event.getCitizen().getName();

        EventDefinitions.EventTypeEntry template = EventDefinitions.getDetectedTemplate("citizen_died");
        if (template == null) return;

        String description = template.descriptionTemplate()
            .replace("{{citizenName}}", citizenName);

        addDetectedEvent(colony, template, description);
        LOGGER.debug("Event: citizen_died - {} in colony {}", citizenName, colony.getID());
    }

    private void onCitizenAdded(CitizenAddedModEvent event) {
        if (!ModSettings.EVENTS_ENABLED.get()) return;

        IColony colony = event.getColony();
        String citizenName = event.getCitizen().getName();
        CitizenAddedModEvent.CitizenAddedSource source = event.getSource();

        String templateId;
        if (source == CitizenAddedModEvent.CitizenAddedSource.BORN) {
            templateId = "citizen_born";
        } else if (source == CitizenAddedModEvent.CitizenAddedSource.HIRED) {
            templateId = "citizen_hired";
        } else {
            return; // Skip INITIAL, RESURRECTED, COMMANDS
        }

        EventDefinitions.EventTypeEntry template = EventDefinitions.getDetectedTemplate(templateId);
        if (template == null) return;

        String description = template.descriptionTemplate()
            .replace("{{citizenName}}", citizenName);

        addDetectedEvent(colony, template, description);
        LOGGER.debug("Event: {} - {} in colony {}", templateId, citizenName, colony.getID());
    }

    private void onBuildingConstruction(BuildingConstructionModEvent event) {
        if (!ModSettings.EVENTS_ENABLED.get()) return;

        IColony colony = event.getColony();
        String buildingName = event.getBuilding().getBuildingDisplayName();
        int level = event.getBuilding().getBuildingLevel();

        String templateId = level > 1 ? "building_upgraded" : "building_built";
        EventDefinitions.EventTypeEntry template = EventDefinitions.getDetectedTemplate(templateId);
        if (template == null) return;

        String description = template.descriptionTemplate()
            .replace("{{buildingName}}", buildingName)
            .replace("{{level}}", String.valueOf(level));

        addDetectedEvent(colony, template, description);
        LOGGER.debug("Event: {} - {} level {} in colony {}", templateId, buildingName, level, colony.getID());
    }

    private void onCitizenJobChanged(CitizenJobChangedModEvent event) {
        if (!ModSettings.EVENTS_ENABLED.get()) return;

        IColony colony = event.getColony();
        String citizenName = event.getCitizen().getName();

        // Get the new job name
        var citizen = event.getCitizen();
        String jobName = "unemployed";
        if (citizen instanceof ICitizenData citizenData && citizenData.getJob() != null) {
            jobName = citizenData.getJob().getJobRegistryEntry().getKey().getPath();
        }

        EventDefinitions.EventTypeEntry template = EventDefinitions.getDetectedTemplate("citizen_job_changed");
        if (template == null) return;

        String description = template.descriptionTemplate()
            .replace("{{citizenName}}", citizenName)
            .replace("{{jobName}}", jobName);

        addDetectedEvent(colony, template, description);
    }

    private void onColonyDeleted(ColonyDeletedModEvent event) {
        int colonyId = event.getColony().getID();
        removeColonyLog(colonyId);
        wasUnderAttack.remove(colonyId);
        LOGGER.info("Cleaned up event log for deleted colony {}", colonyId);
    }

    // ========== State Polling (Raids) ==========

    private void pollColonyState(IColony colony, ColonyEventLog log, int currentDay) {
        int colonyId = colony.getID();
        boolean underAttack = colony.isColonyUnderAttack();
        boolean wasAttacked = wasUnderAttack.getOrDefault(colonyId, false);

        if (underAttack && !wasAttacked) {
            // Raid started
            EventDefinitions.EventTypeEntry template = EventDefinitions.getDetectedTemplate("raid_started");
            if (template != null) {
                addDetectedEvent(colony, template, template.descriptionTemplate());
                LOGGER.debug("Event: raid_started in colony {}", colonyId);
            }
        } else if (!underAttack && wasAttacked) {
            // Raid ended
            EventDefinitions.EventTypeEntry template = EventDefinitions.getDetectedTemplate("raid_ended");
            if (template != null) {
                addDetectedEvent(colony, template, template.descriptionTemplate());
                LOGGER.debug("Event: raid_ended in colony {}", colonyId);
            }
        }

        wasUnderAttack.put(colonyId, underAttack);
    }

    // ========== Flavor Event Generation ==========

    private void tryGenerateFlavorEvent(IColony colony, ColonyEventLog log, int currentDay) {
        int intervalDays = ModSettings.FLAVOR_EVENT_INTERVAL_DAYS.get();
        if (intervalDays <= 0) return;

        // Check if enough days have passed since last flavor event
        if (currentDay - log.getLastFlavorGenerationDay() < intervalDays) return;

        // Don't generate if max active events reached
        if (log.getActiveEvents().size() >= ModSettings.MAX_ACTIVE_EVENTS.get()) return;

        // Random chance check
        if (Math.random() >= ModSettings.FLAVOR_EVENT_CHANCE.get()) return;

        // Generate the flavor event
        ColonyEvent event = FlavorEventGenerator.generate(colony, currentDay);
        if (event == null) return;

        log.addEvent(event);
        log.setLastFlavorGenerationDay(currentDay);
        broadcastEvent(colony, event);
        applyHappinessModifier(colony, event);
        LOGGER.debug("Generated flavor event: {} in colony {}", event.getEventTypeId(), colony.getID());
    }

    // ========== Event Effects ==========

    /**
     * Add a detected event (from MineColonies EventBus) to the colony log.
     */
    private void addDetectedEvent(IColony colony, EventDefinitions.EventTypeEntry template, String description) {
        ColonyEventLog log = getOrCreateLog(colony.getID());

        ColonyEvent event = new ColonyEvent(
            template.id(),
            template.category(),
            description,
            System.currentTimeMillis(),
            colony.getDay(),
            template.durationDays(),
            template.happinessModifier()
        );

        log.addEvent(event);
        broadcastEvent(colony, event);
        applyHappinessModifier(colony, event);
        markDirty();
    }

    /**
     * Broadcast an event as a colored chat message to all colony players.
     */
    private void broadcastEvent(IColony colony, ColonyEvent event) {
        MessageUtils.MessagePriority priority = switch (event.getCategory()) {
            case CRISIS -> MessageUtils.MessagePriority.DANGER;
            case CELEBRATION -> MessageUtils.MessagePriority.IMPORTANT;
            default -> MessageUtils.MessagePriority.NORMAL;
        };

        MessageUtils.format(Component.literal("[Colony Event] " + event.getDescription()))
            .withPriority(priority)
            .sendTo(colony)
            .forAllPlayers();
    }

    /**
     * Apply a happiness modifier to all citizens in the colony for the event's duration.
     */
    private void applyHappinessModifier(IColony colony, ColonyEvent event) {
        if (event.getHappinessModifier() == 0.0) return;

        // Convert happinessModifier (-2.0 to +2.0) to a factor (0.1 to 2.0)
        // Factor < 1.0 = negative, > 1.0 = positive, 1.0 = neutral
        double factor = 1.0 + (event.getHappinessModifier() / 2.0);
        factor = Math.max(0.1, Math.min(2.0, factor));

        String modifierId = "mindofthecolony:event_" + event.getEventTypeId();

        for (ICitizenData citizen : colony.getCitizenManager().getCitizens()) {
            ExpirationBasedHappinessModifier modifier = new ExpirationBasedHappinessModifier(
                modifierId,
                2.0,
                new StaticHappinessSupplier(factor),
                event.getDurationDays()
            );
            citizen.getCitizenHappinessHandler().addModifier(modifier);
        }
    }

    // ========== Colony Log Access ==========

    public ColonyEventLog getLog(int colonyId) {
        return eventLogs.get(colonyId);
    }

    public ColonyEventLog getOrCreateLog(int colonyId) {
        return eventLogs.computeIfAbsent(colonyId, ColonyEventLog::new);
    }

    public Map<Integer, ColonyEventLog> getAllLogs() {
        return Collections.unmodifiableMap(eventLogs);
    }

    /**
     * Load a colony log from saved data. Called during deserialization.
     */
    public void loadColonyLog(int colonyId, ColonyEventLog log) {
        eventLogs.put(colonyId, log);
    }

    /**
     * Remove a colony's event log (e.g., when colony is deleted).
     */
    public void removeColonyLog(int colonyId) {
        eventLogs.remove(colonyId);
        markDirty();
    }

    /**
     * Mark the saved data as dirty so it will be written on next world save.
     */
    public void markDirty() {
        if (savedData != null) {
            savedData.setDirty();
        }
    }
}
