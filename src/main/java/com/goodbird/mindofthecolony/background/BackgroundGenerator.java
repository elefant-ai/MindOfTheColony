package com.goodbird.mindofthecolony.background;

import com.goodbird.mindofthecolony.config.ModSettings;

import java.util.List;
import java.util.Random;

/**
 * Generates a random CitizenBackground.
 * Called exactly once per citizen on first-ever load.
 */
public final class BackgroundGenerator {

    private static final Random RANDOM = new Random();

    private BackgroundGenerator() {}

    public static CitizenBackground generate() {
        CitizenBackground bg = new CitizenBackground();

        List<BackgroundDefinitions.BackgroundEntry> origins = BackgroundDefinitions.getOrigins();
        List<BackgroundDefinitions.BackgroundEntry> traits = BackgroundDefinitions.getPersonalityTraits();
        List<BackgroundDefinitions.PenaltyEntry> penalties = BackgroundDefinitions.getPenalties();

        BackgroundDefinitions.BackgroundEntry origin = origins.get(RANDOM.nextInt(origins.size()));
        bg.setOrigin(origin.id());

        BackgroundDefinitions.BackgroundEntry trait = traits.get(RANDOM.nextInt(traits.size()));
        bg.setPersonalityTrait(trait.id());

        double penaltyChance = ModSettings.PENALTY_CHANCE.get();
        double secondPenaltyChance = ModSettings.SECOND_PENALTY_CHANCE.get();
        int maxPenalties = ModSettings.MAX_PENALTIES.get();

        if (!penalties.isEmpty() && maxPenalties > 0 && RANDOM.nextDouble() < penaltyChance) {
            BackgroundDefinitions.PenaltyEntry penalty =
                penalties.get(RANDOM.nextInt(penalties.size()));
            bg.addPenalty(penalty.id());

            if (maxPenalties > 1 && penalties.size() > 1 && RANDOM.nextDouble() < secondPenaltyChance) {
                BackgroundDefinitions.PenaltyEntry second;
                do {
                    second = penalties.get(RANDOM.nextInt(penalties.size()));
                } while (second.id().equals(penalty.id()));
                bg.addPenalty(second.id());
            }
        }

        return bg;
    }
}
