package com.goodbird.mindofthecolony;

import com.goodbird.mindofthecolony.config.BackgroundConfigLoader;
import com.goodbird.mindofthecolony.config.ModSettings;
import com.goodbird.mindofthecolony.event.NpcEventHandler;
import com.goodbird.mindofthecolony.network.ModNetworking;
import game.player2.npc.Player2NpcLib;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
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

        // Register TOML config
        modContainer.registerConfig(ModConfig.Type.COMMON, ModSettings.SPEC, "mindofthecolony/settings.toml");

        LOGGER.info("Mind of the Colony initialized");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Load background definitions from JSON config
        BackgroundConfigLoader.loadOrCreate();

        // Initialize java-npc library and register event listener
        Player2NpcLib.initialize();
        Player2NpcLib.addListener(new NpcEventHandler());

        // Initialize the manager when server starts
        CitizenNpcManager.getInstance().initialize();
        LOGGER.info("Mind of the Colony ready - citizens can now chat!");
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        CitizenNpcManager.getInstance().onServerTick();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Mind of the Colony is shutting down AI bridges.");
        CitizenNpcManager.getInstance().clearAllAIs();
    }
}
