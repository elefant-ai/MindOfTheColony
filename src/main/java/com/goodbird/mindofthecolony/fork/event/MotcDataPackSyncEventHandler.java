package com.goodbird.mindofthecolony.fork.event;

import com.minecolonies.core.event.DataPackSyncEventHandler;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Replacement event handler for MineColonies' DataPackSyncEventHandler.
 * Delegates all events to the original static methods, giving us a control point
 * to modify or replace individual handlers in the future.
 */
public class MotcDataPackSyncEventHandler {

    public static class ServerEvents {

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onDataPackSync(final OnDatapackSyncEvent event) {
            DataPackSyncEventHandler.ServerEvents.onDataPackSync(event);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void sendOnLogin(final PlayerEvent.PlayerLoggedInEvent event) {
            DataPackSyncEventHandler.ServerEvents.sendOnLogin(event);
        }
    }

    public static class ClientEvents {

        @SubscribeEvent
        public static void onRecipesLoaded(@NotNull final RecipesUpdatedEvent event) {
            DataPackSyncEventHandler.ClientEvents.onRecipesLoaded(event);
        }
    }
}
