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

    public static final ModConfigSpec SPEC = BUILDER.build();
}
