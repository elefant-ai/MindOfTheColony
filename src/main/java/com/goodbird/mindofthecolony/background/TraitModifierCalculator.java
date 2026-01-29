package com.goodbird.mindofthecolony.background;

import java.util.List;
import java.util.Map;

/**
 * Calculates combined modifiers from multiple traits.
 * Multiplicative modifiers are multiplied together.
 * Additive modifiers are summed.
 */
public final class TraitModifierCalculator {

    private TraitModifierCalculator() {}

    public static TraitModifiers calculate(List<String> traitIds) {
        double diseaseRate = 1.0;
        double contactDiseaseRate = 1.0;
        double happinessBase = 0.0;
        double happinessDecayRate = 1.0;
        double workSpeed = 1.0;
        double foodConsumption = 1.0;

        for (String traitId : traitIds) {
            TraitDefinition trait = TraitRegistry.getTrait(traitId);
            if (trait == null) continue;

            Map<String, Double> mods = trait.modifiers();
            if (mods == null) continue;

            diseaseRate *= mods.getOrDefault("diseaseRate", 1.0);
            contactDiseaseRate *= mods.getOrDefault("contactDiseaseRate", 1.0);
            happinessBase += mods.getOrDefault("happinessBase", 0.0);
            happinessDecayRate *= mods.getOrDefault("happinessDecayRate", 1.0);
            workSpeed *= mods.getOrDefault("workSpeed", 1.0);
            foodConsumption *= mods.getOrDefault("foodConsumption", 1.0);
        }

        return new TraitModifiers(
            diseaseRate,
            contactDiseaseRate,
            happinessBase,
            happinessDecayRate,
            workSpeed,
            foodConsumption
        );
    }
}
