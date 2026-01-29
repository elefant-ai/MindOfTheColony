package com.goodbird.mindofthecolony.background;

import com.goodbird.mindofthecolony.config.ModSettings;

import java.util.*;

/**
 * Generates a random CitizenBackground.
 * Called exactly once per citizen on first-ever load.
 */
public final class BackgroundGenerator {

    private static final Random RANDOM = new Random();

    private BackgroundGenerator() {}

    public static CitizenBackground generate() {
        CitizenBackground bg = new CitizenBackground();

        // Pick 2-4 unique traits using weighted selection
        int minTraits = ModSettings.MIN_TRAITS.get();
        int maxTraits = ModSettings.MAX_TRAITS.get();
        int traitCount = minTraits + RANDOM.nextInt(maxTraits - minTraits + 1);

        Set<String> usedTraitIds = new HashSet<>();

        for (int i = 0; i < traitCount; i++) {
            TraitDefinition trait = pickWeightedTrait(usedTraitIds);
            if (trait != null) {
                bg.addTrait(trait.id());
                usedTraitIds.add(trait.id());
            }
        }

        return bg;
    }

    /**
     * Pick a random trait using weighted selection, excluding already-picked traits.
     */
    private static TraitDefinition pickWeightedTrait(Set<String> excludeIds) {
        List<TraitDefinition> allTraits = TraitRegistry.getAllTraits();

        // Filter out already-used traits
        List<TraitDefinition> available = new ArrayList<>();
        double totalWeight = 0;
        for (TraitDefinition trait : allTraits) {
            if (!excludeIds.contains(trait.id())) {
                available.add(trait);
                totalWeight += trait.weight();
            }
        }

        if (available.isEmpty() || totalWeight <= 0) {
            return null;
        }

        // Weighted random selection
        double roll = RANDOM.nextDouble() * totalWeight;
        double cumulative = 0;
        for (TraitDefinition trait : available) {
            cumulative += trait.weight();
            if (roll < cumulative) {
                return trait;
            }
        }

        // Fallback (should not happen)
        return available.get(available.size() - 1);
    }
}
