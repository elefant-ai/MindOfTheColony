package com.goodbird.mindofthecolony.effect;

import net.minecraft.nbt.CompoundTag;

/**
 * Represents a temporary trait applied to a citizen by an event.
 * The trait references a TraitDefinition by ID and has a duration.
 */
public class TemporaryTrait {
    public static final int PERMANENT_DURATION = -1;

    private final String traitId;
    private final String source;
    private final long appliedTick;
    private final int durationTicks;

    public TemporaryTrait(String traitId, String source, long appliedTick, int durationTicks) {
        this.traitId = traitId;
        this.source = source;
        this.appliedTick = appliedTick;
        this.durationTicks = durationTicks;
    }

    public String getTraitId() {
        return traitId;
    }

    public String getSource() {
        return source;
    }

    public long getAppliedTick() {
        return appliedTick;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    /**
     * Check if this trait is permanent (must be explicitly removed).
     */
    public boolean isPermanent() {
        return durationTicks == PERMANENT_DURATION;
    }

    /**
     * Check if this trait has expired.
     */
    public boolean isExpired(long currentTick) {
        if (isPermanent()) {
            return false;
        }
        return currentTick - appliedTick >= durationTicks;
    }

    /**
     * Get remaining duration in ticks, or -1 if permanent.
     */
    public int getRemainingTicks(long currentTick) {
        if (isPermanent()) {
            return PERMANENT_DURATION;
        }
        long elapsed = currentTick - appliedTick;
        return Math.max(0, durationTicks - (int) elapsed);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("traitId", traitId);
        tag.putString("source", source);
        tag.putLong("appliedTick", appliedTick);
        tag.putInt("durationTicks", durationTicks);
        return tag;
    }

    public static TemporaryTrait fromNBT(CompoundTag tag) {
        return new TemporaryTrait(
            tag.getString("traitId"),
            tag.getString("source"),
            tag.getLong("appliedTick"),
            tag.getInt("durationTicks")
        );
    }

    @Override
    public String toString() {
        return "TemporaryTrait{" +
            "traitId='" + traitId + '\'' +
            ", source='" + source + '\'' +
            ", duration=" + (isPermanent() ? "permanent" : durationTicks) +
            '}';
    }
}
