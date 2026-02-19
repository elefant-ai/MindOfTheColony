package com.goodbird.mindofthecolony.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModSettings {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MIN_TRAITS = BUILDER
        .comment("Minimum number of traits a citizen gets assigned on spawn")
        .defineInRange("minTraits", 2, 1, 10);

    public static final ModConfigSpec.IntValue MAX_TRAITS = BUILDER
        .comment("Maximum number of traits a citizen gets assigned on spawn")
        .defineInRange("maxTraits", 4, 1, 10);

    public static final ModConfigSpec.ConfigValue<String> NPC_LANGUAGE = BUILDER
        .comment("Language for NPC conversations (e.g., english, spanish, french, german, japanese)")
        .define("npcLanguage", "english");

    // Colony God settings
    public static final ModConfigSpec.BooleanValue GOD_ENABLED = BUILDER
        .comment("Enable the AI Colony God that manages worker assignments and build priorities")
        .define("godEnabled", true);

    public static final ModConfigSpec.IntValue GOD_TICK_INTERVAL = BUILDER
        .comment("Ticks between Colony God periodic checks (20 ticks = 1 second). Events are processed immediately.")
        .defineInRange("godTickInterval", 100, 20, 6000);

    public static final ModConfigSpec.IntValue GOD_MAX_HISTORY = BUILDER
        .comment("Maximum number of messages to keep in the Colony God's conversation history")
        .defineInRange("godMaxHistory", 20, 5, 100);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
