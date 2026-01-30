package com.goodbird.mindofthecolony.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Disease modifiers config. Higher values = more likely to get sick.
 */
public class DiseaseConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Job modifiers for spontaneous illness (1.0 = normal)")
               .push("jobModifiers");
    }

    public static final ModConfigSpec.DoubleValue HEALER_MODIFIER = BUILDER
            .comment("Was immune in vanilla")
            .defineInRange("healer", 0.05, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue MINER_MODIFIER = BUILDER
            .defineInRange("miner", 2.0, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue QUARRIER_MODIFIER = BUILDER
            .defineInRange("quarrier", 2.0, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue SWINEHERDER_MODIFIER = BUILDER
            .defineInRange("swineherder", 2.0, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue COMPOSTER_MODIFIER = BUILDER
            .comment("Base modifier, scaled by skill: base * (100 - skill) / 25")
            .defineInRange("composter", 1.0, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue CRUSHER_MODIFIER = BUILDER
            .comment("Base modifier, scaled by skill: base * (100 - skill) / 25")
            .defineInRange("crusher", 1.0, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue NETHERWORKER_MODIFIER = BUILDER
            .comment("Still immune while invisible, this applies when visible")
            .defineInRange("netherworker", 1.0, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue DEFAULT_JOB_MODIFIER = BUILDER
            .comment("Used for jobs not listed above")
            .defineInRange("default", 1.0, 0.0, 10.0);

    static { BUILDER.pop(); }

    static {
        BUILDER.comment("Contact spread modifiers (multiplier on 1% base chance)")
               .push("contactModifiers");
    }

    public static final ModConfigSpec.DoubleValue HEALER_CONTACT_MODIFIER = BUILDER
            .defineInRange("healer", 0.1, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue DEFAULT_CONTACT_MODIFIER = BUILDER
            .defineInRange("default", 1.0, 0.0, 10.0);

    static { BUILDER.pop(); }

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static double getJobModifier(String jobId) {
        return switch (jobId.toLowerCase()) {
            case "healer" -> HEALER_MODIFIER.get();
            case "miner" -> MINER_MODIFIER.get();
            case "quarrier" -> QUARRIER_MODIFIER.get();
            case "swineherder" -> SWINEHERDER_MODIFIER.get();
            case "composter" -> COMPOSTER_MODIFIER.get();
            case "crusher" -> CRUSHER_MODIFIER.get();
            case "netherworker" -> NETHERWORKER_MODIFIER.get();
            default -> DEFAULT_JOB_MODIFIER.get();
        };
    }

    public static double getContactModifier(String jobId) {
        return switch (jobId.toLowerCase()) {
            case "healer" -> HEALER_CONTACT_MODIFIER.get();
            default -> DEFAULT_CONTACT_MODIFIER.get();
        };
    }
}
