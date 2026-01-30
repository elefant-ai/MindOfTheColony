package com.goodbird.mindofthecolony.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Tracks weather state over time for event evaluation.
 * Maintains history of rain/thunder for calculating prolonged weather effects.
 */
public class WeatherState {

    private int ticksRaining = 0;
    private int ticksThundering = 0;
    private boolean wasRaining = false;
    private boolean wasThundering = false;
    private long lastUpdateTick = 0;

    /**
     * Update weather state based on current level conditions.
     * Should be called periodically (e.g., every tick or every few ticks).
     *
     * @param level       The server level to check weather
     * @param currentTick Current game tick
     * @param ticksElapsed Number of ticks since last update
     */
    public void update(ServerLevel level, long currentTick, int ticksElapsed) {
        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();

        if (isRaining) {
            ticksRaining += ticksElapsed;
        } else {
            ticksRaining = 0;
        }

        if (isThundering) {
            ticksThundering += ticksElapsed;
        } else {
            ticksThundering = 0;
        }

        wasRaining = isRaining;
        wasThundering = isThundering;
        lastUpdateTick = currentTick;
    }

    /**
     * Check if it's currently raining.
     */
    public boolean isCurrentlyRaining() {
        return wasRaining;
    }

    /**
     * Check if it's currently thundering.
     */
    public boolean isCurrentlyThundering() {
        return wasThundering;
    }

    /**
     * Get how long it has been raining in ticks.
     */
    public int getTicksRaining() {
        return ticksRaining;
    }

    /**
     * Get how long it has been thundering in ticks.
     */
    public int getTicksThundering() {
        return ticksThundering;
    }

    /**
     * Get rain intensity as a value from 0 to 1.
     * Based on how long it has been raining, capped at a threshold.
     *
     * @param maxRainTicks The number of rain ticks for maximum intensity (1.0)
     */
    public float getRecentRainIntensity(int maxRainTicks) {
        if (maxRainTicks <= 0) return 0f;
        return Math.min(1.0f, (float) ticksRaining / maxRainTicks);
    }

    /**
     * Check if a thunderstorm just started (was not thundering, now is).
     */
    public boolean didThunderstormStart(ServerLevel level) {
        return !wasThundering && level.isThundering();
    }

    /**
     * Check if rain just stopped (was raining, now is not).
     */
    public boolean didRainStop(ServerLevel level) {
        return wasRaining && !level.isRaining();
    }

    /**
     * Check if a thunderstorm just ended.
     */
    public boolean didThunderstormEnd(ServerLevel level) {
        return wasThundering && !level.isThundering();
    }

    /**
     * Get last update tick.
     */
    public long getLastUpdateTick() {
        return lastUpdateTick;
    }

    /**
     * Serialize to NBT for persistence.
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("ticksRaining", ticksRaining);
        tag.putInt("ticksThundering", ticksThundering);
        tag.putBoolean("wasRaining", wasRaining);
        tag.putBoolean("wasThundering", wasThundering);
        tag.putLong("lastUpdateTick", lastUpdateTick);
        return tag;
    }

    /**
     * Deserialize from NBT.
     */
    public static WeatherState fromNBT(CompoundTag tag) {
        WeatherState state = new WeatherState();
        state.ticksRaining = tag.getInt("ticksRaining");
        state.ticksThundering = tag.getInt("ticksThundering");
        state.wasRaining = tag.getBoolean("wasRaining");
        state.wasThundering = tag.getBoolean("wasThundering");
        state.lastUpdateTick = tag.getLong("lastUpdateTick");
        return state;
    }

    @Override
    public String toString() {
        return "WeatherState{" +
            "raining=" + wasRaining +
            ", ticksRaining=" + ticksRaining +
            ", thundering=" + wasThundering +
            ", ticksThundering=" + ticksThundering +
            '}';
    }
}
