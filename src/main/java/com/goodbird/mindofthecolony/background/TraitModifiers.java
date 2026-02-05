package com.goodbird.mindofthecolony.background;

import java.util.Map;

/**
 * Holds computed modifier values for a citizen.
 * Multiplicative modifiers default to 1.0, additive modifiers default to 0.0.
 * Skill bonuses are additive integers that modify the effective skill level.
 */
public record TraitModifiers(
    double diseaseRate,
    double contactDiseaseRate,
    double happinessBase,
    double happinessDecayRate,
    double workSpeed,
    double foodConsumption,
    Map<String, Integer> skillBonuses
) {
    public static final TraitModifiers DEFAULT = new TraitModifiers(
        1.0,  // diseaseRate
        1.0,  // contactDiseaseRate
        0.0,  // happinessBase
        1.0,  // happinessDecayRate
        1.0,  // workSpeed
        1.0,  // foodConsumption
        Map.of()  // skillBonuses
    );

    /**
     * Get the skill bonus for a specific skill.
     * @param skillName The skill name (case-insensitive)
     * @return The bonus (positive or negative), or 0 if no bonus
     */
    public int getSkillBonus(String skillName) {
        return skillBonuses.getOrDefault(skillName.toLowerCase(), 0);
    }
}
