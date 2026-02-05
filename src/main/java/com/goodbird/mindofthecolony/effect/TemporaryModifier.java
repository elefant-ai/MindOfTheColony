package com.goodbird.mindofthecolony.effect;

import net.minecraft.nbt.CompoundTag;

/**
 * Represents a temporary stat modifier that can be applied to a citizen.
 * Modifiers can optionally decay over time back to the default value.
 */
public class TemporaryModifier {
    private final String source;
    private final String modifierType;
    private final double amount;
    private final long appliedTick;
    private final int durationTicks;
    private final boolean decays;

    public TemporaryModifier(String source, String modifierType, double amount, long appliedTick, int durationTicks, boolean decays) {
        this.source = source;
        this.modifierType = modifierType;
        this.amount = amount;
        this.appliedTick = appliedTick;
        this.durationTicks = durationTicks;
        this.decays = decays;
    }

    public String getSource() {
        return source;
    }

    public String getModifierType() {
        return modifierType;
    }

    public double getAmount() {
        return amount;
    }

    public long getAppliedTick() {
        return appliedTick;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public boolean isDecaying() {
        return decays;
    }

    /**
     * Check if this modifier has expired.
     */
    public boolean isExpired(long currentTick) {
        return currentTick - appliedTick >= durationTicks;
    }

    /**
     * Get the current modifier value, accounting for decay if applicable.
     * For multiplicative modifiers, default is 1.0.
     * For additive modifiers, default is 0.0.
     */
    public double getCurrentValue(long currentTick) {
        long elapsed = currentTick - appliedTick;
        if (elapsed >= durationTicks) {
            return getDefaultValue();
        }
        if (!decays) {
            return amount;
        }
        // Linear decay back to default
        double progress = (double) elapsed / durationTicks;
        double defaultVal = getDefaultValue();
        return amount + (defaultVal - amount) * progress;
    }

    /**
     * Get the default value for this modifier type.
     * Multiplicative modifiers default to 1.0, additive to 0.0.
     */
    private double getDefaultValue() {
        return switch (modifierType) {
            case "happinessBase" -> 0.0;  // Additive
            default -> 1.0;  // Multiplicative (diseaseRate, workSpeed, etc.)
        };
    }

    /**
     * Get remaining duration in ticks.
     */
    public int getRemainingTicks(long currentTick) {
        long elapsed = currentTick - appliedTick;
        return Math.max(0, durationTicks - (int) elapsed);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("source", source);
        tag.putString("modifierType", modifierType);
        tag.putDouble("amount", amount);
        tag.putLong("appliedTick", appliedTick);
        tag.putInt("durationTicks", durationTicks);
        tag.putBoolean("decays", decays);
        return tag;
    }

    public static TemporaryModifier fromNBT(CompoundTag tag) {
        return new TemporaryModifier(
            tag.getString("source"),
            tag.getString("modifierType"),
            tag.getDouble("amount"),
            tag.getLong("appliedTick"),
            tag.getInt("durationTicks"),
            tag.getBoolean("decays")
        );
    }

    @Override
    public String toString() {
        return "TemporaryModifier{" +
            "source='" + source + '\'' +
            ", type='" + modifierType + '\'' +
            ", amount=" + amount +
            ", duration=" + durationTicks +
            ", decays=" + decays +
            '}';
    }
}
