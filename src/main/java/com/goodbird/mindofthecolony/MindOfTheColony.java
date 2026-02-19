package com.goodbird.mindofthecolony;

import com.goodbird.mindofthecolony.background.BackgroundGenerationService;
import com.goodbird.mindofthecolony.command.MotcCommand;
import com.goodbird.mindofthecolony.config.BackgroundConfigLoader;
import com.goodbird.mindofthecolony.config.DiseaseConfig;
import com.goodbird.mindofthecolony.config.EventConfig;
import com.goodbird.mindofthecolony.config.ModSettings;
import com.goodbird.mindofthecolony.event.NpcEventHandler;
import com.goodbird.mindofthecolony.god.ColonyGod;
import com.goodbird.mindofthecolony.god.NpcColonyRegistry;
import com.goodbird.mindofthecolony.network.ModNetworking;
import com.goodbird.mindofthecolony.worldgen.NpcColonyWorldgen;
import game.player2.npc.Player2NpcLib;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod class for Mind of the Colony.
 * Brings your colonists to life with AI conversations.
 */
@Mod("mindofthecolony")
public class MindOfTheColony {
    private static final Logger LOGGER = LoggerFactory.getLogger(MindOfTheColony.class);

    public MindOfTheColony(IEventBus modEventBus, ModContainer modContainer) {
        // Register network message handlers
        modEventBus.register(ModNetworking.class);

        // Register ourselves for server and other game events
        NeoForge.EVENT_BUS.register(this);

        // Register command handler
        NeoForge.EVENT_BUS.register(MotcCommand.class);

        // Register worldgen structure detection handler
        NeoForge.EVENT_BUS.register(NpcColonyWorldgen.class);

        // Register TOML configs
        modContainer.registerConfig(ModConfig.Type.COMMON, ModSettings.SPEC, "mindofthecolony/settings.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, DiseaseConfig.SPEC, "mindofthecolony/diseases.toml");

        LOGGER.info("Mind of the Colony initialized");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Load JSON configs
        BackgroundConfigLoader.loadOrCreate();
        EventConfig.loadOrCreate();

        // Load NPC colony registry and processed structure positions
        NpcColonyRegistry.load();
        NpcColonyWorldgen.loadProcessedPositions();

        // Initialize java-npc library and register event listeners
        Player2NpcLib.initialize();
        Player2NpcLib.addListener(new NpcEventHandler());

        // Register background generation listener to ensure it's ready before any generation requests
        BackgroundGenerationService.getInstance().registerListener();

        // Initialize the manager when server starts
        CitizenNpcManager.getInstance().initialize();

        // Check for any existing citizens without backgrounds and generate them
        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
        if (overworld != null) {
            CitizenNpcManager.getInstance().checkAndGenerateMissingBackgrounds(overworld);

            // Initialize event managers for colonies
            CitizenNpcManager.getInstance().initializeEventManagers(overworld);
        }

        LOGGER.info("Mind of the Colony ready - citizens can now chat!");
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        CitizenNpcManager.getInstance().onServerTick();
        ColonyGod.tickAll();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Mind of the Colony is shutting down AI bridges.");
        NpcColonyRegistry.save();
        NpcColonyWorldgen.saveProcessedPositions();
        ColonyGod.shutdownAll();
        CitizenNpcManager.getInstance().clearAllAIs();
    }
}
