package com.goodbird.mindofthecolony.background;

import java.util.Random;

/**
 * Generates a random CitizenBackground.
 * Called exactly once per citizen on first-ever load.
 */
public final class BackgroundGenerator {

    private static final Random RANDOM = new Random();

    private static final double PENALTY_CHANCE = 0.6;
    private static final double SECOND_PENALTY_CHANCE = 0.3;

    private BackgroundGenerator() {}

    public static CitizenBackground generate() {
        CitizenBackground bg = new CitizenBackground();

        BackgroundDefinitions.BackgroundEntry origin =
            BackgroundDefinitions.ORIGINS.get(RANDOM.nextInt(BackgroundDefinitions.ORIGINS.size()));
        bg.setOrigin(origin.id());

        BackgroundDefinitions.BackgroundEntry trait =
            BackgroundDefinitions.PERSONALITY_TRAITS.get(RANDOM.nextInt(BackgroundDefinitions.PERSONALITY_TRAITS.size()));
        bg.setPersonalityTrait(trait.id());

        if (RANDOM.nextDouble() < PENALTY_CHANCE) {
            BackgroundDefinitions.PenaltyEntry penalty =
                BackgroundDefinitions.PENALTIES.get(RANDOM.nextInt(BackgroundDefinitions.PENALTIES.size()));
            bg.addPenalty(penalty.id());

            if (RANDOM.nextDouble() < SECOND_PENALTY_CHANCE) {
                BackgroundDefinitions.PenaltyEntry second;
                do {
                    second = BackgroundDefinitions.PENALTIES.get(
                        RANDOM.nextInt(BackgroundDefinitions.PENALTIES.size()));
                } while (second.id().equals(penalty.id()));
                bg.addPenalty(second.id());
            }
        }

        return bg;
    }
}
