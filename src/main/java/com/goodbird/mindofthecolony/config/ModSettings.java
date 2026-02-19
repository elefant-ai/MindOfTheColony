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

    public static final ModConfigSpec.BooleanValue TTS_ENABLED = BUILDER
        .comment("Enable text-to-speech for NPC responses (requires Player2 app)")
        .define("ttsEnabled", false);

    public static final ModConfigSpec.DoubleValue TTS_SPEED = BUILDER
        .comment("Text-to-speech speed (0.25-4.0, default 1.0)")
        .defineInRange("ttsSpeed", 1.0, 0.25, 4.0);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
