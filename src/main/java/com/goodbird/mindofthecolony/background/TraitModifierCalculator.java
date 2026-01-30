package com.goodbird.mindofthecolony.background;

import com.goodbird.mindofthecolony.effect.TemporaryModifier;
import com.goodbird.mindofthecolony.effect.TemporaryTrait;

import java.util.List;
import java.util.Map;

/**
 * Calculates combined modifiers from multiple traits.
 * Multiplicative modifiers are multiplied together.
 * Additive modifiers are summed.
 */
public final class TraitModifierCalculator {

    private TraitModifierCalculator() {}

    /**
     * Calculate modifiers from permanent traits only.
     */
    public static TraitModifiers calculate(List<String> traitIds) {
        return calculate(traitIds, List.of(), List.of(), 0);
    }

    /**
     * Calculate modifiers from permanent traits, temporary traits, and temporary modifiers.
     *
     * @param permanentTraitIds Permanent trait IDs from CitizenBackground
     * @param temporaryTraits   Temporary traits applied by events
     * @param temporaryModifiers Direct temporary modifiers from events
     * @param currentTick       Current game tick for calculating time-based effects
     */
    public static TraitModifiers calculate(
            List<String> permanentTraitIds,
            List<TemporaryTrait> temporaryTraits,
            List<TemporaryModifier> temporaryModifiers,
            long currentTick) {

        double diseaseRate = 1.0;
        double contactDiseaseRate = 1.0;
        double happinessBase = 0.0;
        double happinessDecayRate = 1.0;
        double workSpeed = 1.0;
        double foodConsumption = 1.0;

        // Apply permanent traits
        for (String traitId : permanentTraitIds) {
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

        // Apply temporary traits (look up trait definitions)
        for (TemporaryTrait tempTrait : temporaryTraits) {
            if (tempTrait.isExpired(currentTick)) continue;

            TraitDefinition trait = TraitRegistry.getTrait(tempTrait.getTraitId());
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

        // Apply direct temporary modifiers (with decay support)
        for (TemporaryModifier mod : temporaryModifiers) {
            if (mod.isExpired(currentTick)) continue;

            double value = mod.getCurrentValue(currentTick);
            String type = mod.getModifierType();

            switch (type) {
                case "diseaseRate" -> diseaseRate *= value;
                case "contactDiseaseRate" -> contactDiseaseRate *= value;
                case "happinessBase" -> happinessBase += value;
                case "happinessDecayRate" -> happinessDecayRate *= value;
                case "workSpeed" -> workSpeed *= value;
                case "foodConsumption" -> foodConsumption *= value;
            }
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
