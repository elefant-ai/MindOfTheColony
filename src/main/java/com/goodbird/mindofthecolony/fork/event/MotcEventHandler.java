package com.goodbird.mindofthecolony.fork.event;

import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.event.EventHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

import static net.neoforged.bus.api.EventPriority.HIGHEST;
import static net.neoforged.bus.api.EventPriority.LOWEST;

/**
 * Replacement event handler for MineColonies' EventHandler.
 * Delegates all events to the original static methods, giving us a control point
 * to modify or replace individual handlers in the future.
 */
public class MotcEventHandler {

    @SubscribeEvent
    public static void onCommandsRegister(final RegisterCommandsEvent event) {
        EventHandler.onCommandsRegister(event);
    }

    @SubscribeEvent(priority = HIGHEST)
    public static void onEntityAdded(@NotNull final EntityJoinLevelEvent event) {
        EventHandler.onEntityAdded(event);
    }

    @SubscribeEvent
    public static void onLootTableLoad(@NotNull final LootTableLoadEvent event) {
        EventHandler.onLootTableLoad(event);
    }

    @SubscribeEvent
    public static void onChunkLoad(@NotNull final ChunkEvent.Load event) {
        EventHandler.onChunkLoad(event);
    }

    @SubscribeEvent
    public static void onChunkUnLoad(final ChunkEvent.Unload event) {
        EventHandler.onChunkUnLoad(event);
    }

    @SubscribeEvent(priority = LOWEST)
    public static void onEntityTravelToDimensionEvent(final EntityTravelToDimensionEvent event) {
        EventHandler.onEntityTravelToDimensionEvent(event);
    }

    @SubscribeEvent
    public static void playerChangeDim(final PlayerEvent.PlayerChangedDimensionEvent event) {
        EventHandler.playerChangeDim(event);
    }

    @SubscribeEvent
    public static void onEnteringChunk(final PlayerTickEvent.Pre event) {
        EventHandler.onEnteringChunk(event);
    }

    @SubscribeEvent
    public static void on(final MobSpawnEvent.PositionCheck event) {
        EventHandler.on(event);
    }

    @SubscribeEvent
    public static void onPlayerEnterWorld(final PlayerEvent.PlayerLoggedInEvent event) {
        EventHandler.onPlayerEnterWorld(event);
    }

    @SubscribeEvent
    public static void onPlayerLeaveWorld(final PlayerEvent.PlayerLoggedOutEvent event) {
        EventHandler.onPlayerLeaveWorld(event);
    }

    @SubscribeEvent
    public static void onBlockBreak(@NotNull final BlockEvent.BreakEvent event) {
        EventHandler.onBlockBreak(event);
    }

    @SubscribeEvent
    public static void onPlayerInteract(@NotNull final PlayerInteractEvent.RightClickBlock event) {
        EventHandler.onPlayerInteract(event);
    }

    @SubscribeEvent(priority = HIGHEST)
    public static void onWorldLoad(@NotNull final LevelEvent.Load event) {
        EventHandler.onWorldLoad(event);
    }

    @SubscribeEvent
    public static void onWorldUnload(@NotNull final LevelEvent.Unload event) {
        EventHandler.onWorldUnload(event);
    }

    @SubscribeEvent
    public static void onPlayerLogout(@NotNull final ClientPlayerNetworkEvent.LoggingOut event) {
        IColonyManager.getInstance().resetColonyViews();
        Log.getLogger().info("Removed all colony views");
    }

    @SubscribeEvent
    public static void onCropTrample(BlockEvent.FarmlandTrampleEvent event) {
        EventHandler.onCropTrample(event);
    }

    @SubscribeEvent
    public static void onEntityConverted(@NotNull final LivingConversionEvent.Pre event) {
        EventHandler.onEntityConverted(event);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        EventHandler.onServerTick(event);
    }
}
