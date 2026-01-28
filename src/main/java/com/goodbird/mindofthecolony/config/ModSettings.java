package com.goodbird.mindofthecolony.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModSettings {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue PENALTY_CHANCE = BUILDER
        .comment("Chance that a citizen gets assigned at least one penalty on spawn (0.0 = never, 1.0 = always)")
        .defineInRange("penaltyChance", 0.6, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue SECOND_PENALTY_CHANCE = BUILDER
        .comment("If a citizen already has one penalty, chance they get a second (0.0 = never, 1.0 = always)")
        .defineInRange("secondPenaltyChance", 0.3, 0.0, 1.0);

    public static final ModConfigSpec.IntValue MAX_PENALTIES = BUILDER
        .comment("Maximum number of penalties a citizen can be assigned")
        .defineInRange("maxPenalties", 2, 0, 10);

    // --- Event System Settings ---

    public static final ModConfigSpec.BooleanValue EVENTS_ENABLED = BUILDER
        .comment("Enable or disable the periodic events system entirely")
        .define("events.enabled", true);

    public static final ModConfigSpec.IntValue EVENT_CHECK_INTERVAL_TICKS = BUILDER
        .comment("How often (in ticks) to check for colony state events and flavor generation. 600 = every 30 seconds.")
        .defineInRange("events.checkIntervalTicks", 600, 20, 12000);

    public static final ModConfigSpec.IntValue MAX_ACTIVE_EVENTS = BUILDER
        .comment("Maximum number of active events per colony at any time")
        .defineInRange("events.maxActiveEvents", 10, 1, 50);

    public static final ModConfigSpec.IntValue FLAVOR_EVENT_INTERVAL_DAYS = BUILDER
        .comment("Minimum colony days between random flavor events. 0 = disabled.")
        .defineInRange("events.flavorEventIntervalDays", 2, 0, 30);

    public static final ModConfigSpec.DoubleValue FLAVOR_EVENT_CHANCE = BUILDER
        .comment("Chance per eligible check that a flavor event is generated (0.0 - 1.0)")
        .defineInRange("events.flavorEventChance", 0.3, 0.0, 1.0);

    public static final ModConfigSpec.IntValue EVENT_HISTORY_SIZE = BUILDER
        .comment("Maximum number of expired events kept in history per colony")
        .defineInRange("events.historySize", 50, 0, 200);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
