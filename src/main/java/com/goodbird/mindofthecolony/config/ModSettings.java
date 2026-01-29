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

    public static final ModConfigSpec.ConfigValue<String> NPC_LANGUAGE = BUILDER
        .comment("Language for NPC conversations (e.g., english, spanish, french, german, japanese)")
        .define("npcLanguage", "english");

    public static final ModConfigSpec SPEC = BUILDER.build();
}
