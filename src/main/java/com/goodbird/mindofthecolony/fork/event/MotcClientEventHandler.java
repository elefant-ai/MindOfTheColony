package com.goodbird.mindofthecolony.fork.event;

import com.minecolonies.core.event.ClientEventHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Replacement event handler for MineColonies' ClientEventHandler.
 * Delegates all events to the original static methods, giving us a control point
 * to modify or replace individual handlers in the future.
 */
@OnlyIn(Dist.CLIENT)
public class MotcClientEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderWorldLastEvent(@NotNull final RenderLevelStageEvent event) {
        ClientEventHandler.renderWorldLastEvent(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onwWorldTick(@NotNull final LevelTickEvent.Pre event) {
        ClientEventHandler.onwWorldTick(event);
    }

    @SubscribeEvent
    public static void onPlayerLogout(@NotNull final ClientPlayerNetworkEvent.LoggingOut event) {
        ClientEventHandler.onPlayerLogout(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlaySoundEvent(final PlaySoundEvent event) {
        ClientEventHandler.onPlaySoundEvent(event);
    }

    @SubscribeEvent
    public static void onItemTooltipEvent(final ItemTooltipEvent event) {
        ClientEventHandler.onItemTooltipEvent(event);
    }

    @SubscribeEvent
    public static void onDebugOverlay(final CustomizeGuiOverlayEvent.DebugText event) {
        ClientEventHandler.onDebugOverlay(event);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onUseItem(@NotNull final PlayerInteractEvent.RightClickItem event) {
        ClientEventHandler.onUseItem(event);
    }
}
