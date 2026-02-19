package com.goodbird.mindofthecolony.fork.event;

import com.minecolonies.core.event.FMLEventHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Replacement event handler for MineColonies' FMLEventHandler.
 * Delegates all events to the original static methods, giving us a control point
 * to modify or replace individual handlers in the future.
 */
public class MotcFMLEventHandler {

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Pre event) {
        FMLEventHandler.onServerTick(event);
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Pre event) {
        FMLEventHandler.onClientTick(event);
    }

    @SubscribeEvent
    public static void onPlayerLogin(@NotNull final PlayerEvent.PlayerLoggedInEvent event) {
        FMLEventHandler.onPlayerLogin(event);
    }

    @SubscribeEvent
    public static void onAddReloadListenerEvent(@NotNull final AddReloadListenerEvent event) {
        FMLEventHandler.onAddReloadListenerEvent(event);
    }

    @SubscribeEvent
    public static void onServerStarted(@NotNull final ServerStartedEvent event) {
        FMLEventHandler.onServerStarted(event);
    }

    @SubscribeEvent
    public static void onWorldTick(final LevelTickEvent.Pre event) {
        FMLEventHandler.onWorldTick(event);
    }

    @SubscribeEvent
    public static void onServerAboutToStart(@NotNull final ServerAboutToStartEvent event) {
        FMLEventHandler.onServerAboutToStart(event);
    }

    @SubscribeEvent
    public static void onServerStopped(@NotNull final ServerStoppingEvent event) {
        FMLEventHandler.onServerStopped(event);
    }
}
