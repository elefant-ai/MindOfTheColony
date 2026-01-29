package com.goodbird.mindofthecolony.background;

/**
 * Holds computed modifier values for a citizen.
 * Multiplicative modifiers default to 1.0, additive modifiers default to 0.0.
 */
public record TraitModifiers(
    double diseaseRate,
    double contactDiseaseRate,
    double happinessBase,
    double happinessDecayRate,
    double workSpeed,
    double foodConsumption
) {
    public static final TraitModifiers DEFAULT = new TraitModifiers(
        1.0,  // diseaseRate
        1.0,  // contactDiseaseRate
        0.0,  // happinessBase
        1.0,  // happinessDecayRate
        1.0,  // workSpeed
        1.0   // foodConsumption
    );
}
