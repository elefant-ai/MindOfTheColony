package com.goodbird.mindofthecolony.event.evaluator;

import com.goodbird.mindofthecolony.config.EventConfig;
import com.goodbird.mindofthecolony.event.*;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.datalistener.DiseasesListener;
import com.minecolonies.core.datalistener.model.Disease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates conditions for disease outbreaks.
 * Replaces the vanilla MineColonies random disease checks with a more
 * sophisticated system based on weather, food, and happiness.
 */
public class DiseaseOutbreakEvaluator implements EventEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseOutbreakEvaluator.class);

    // Max rain ticks for full intensity
    private static final int MAX_RAIN_TICKS = 24000;

    @Override
    public String getEventType() {
        return "disease_outbreak";
    }

    @Override
    public boolean isEnabled() {
        return EventConfig.getDiseaseOutbreakConfig().enabled;
    }

    @Override
    public List<ColonyEvent> evaluate(EventContext context) {
        List<ColonyEvent> events = new ArrayList<>();
        EventConfig.DiseaseOutbreakConfig config = EventConfig.getDiseaseOutbreakConfig();

        // Track affected citizens by disease
        Map<String, List<String>> affectedByDisease = new HashMap<>();

        for (ICitizenData citizen : context.citizens()) {
            // Skip citizens who are already sick or immune
            if (citizen.getCitizenDiseaseHandler().isSick()) {
                continue;
            }

            // Calculate disease chance for this citizen
            double diseaseChance = calculateDiseaseChance(citizen, context, config);

            // Roll for disease
            if (context.random().nextFloat() < diseaseChance) {
                // Get a random disease
                Disease disease = DiseasesListener.getRandomDisease(context.random());
                if (disease == null) {
                    LOGGER.debug("No diseases available to apply");
                    continue;
                }

                // Try to apply the disease
                boolean applied = citizen.getCitizenDiseaseHandler().setDisease(disease);
                if (applied) {
                    String citizenName = citizen.getName();
                    String diseaseId = disease.id().toString();
                    String diseaseName = disease.name().getString();

                    LOGGER.info("Disease {} applied to citizen {} (chance was {})",
                        diseaseName, citizenName, diseaseChance);

                    affectedByDisease
                        .computeIfAbsent(diseaseId + "|" + diseaseName, k -> new ArrayList<>())
                        .add(citizenName);
                }
            }
        }

        // Create events for each disease that affected citizens
        for (Map.Entry<String, List<String>> entry : affectedByDisease.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            String diseaseId = parts[0];
            String diseaseName = parts.length > 1 ? parts[1] : diseaseId;

            DiseaseOutbreakEvent event = new DiseaseOutbreakEvent(
                context.currentTick(),
                context.colony().getID(),
                diseaseId,
                diseaseName,
                entry.getValue()
            );
            events.add(event);
        }

        return events;
    }

    /**
     * Calculate the disease chance for a specific citizen.
     */
    private double calculateDiseaseChance(ICitizenData citizen, EventContext context,
                                          EventConfig.DiseaseOutbreakConfig config) {
        double chance = config.baseChance;

        // Rain intensity modifier (0 to rainMultiplier)
        float rainIntensity = context.weather().getRecentRainIntensity(MAX_RAIN_TICKS);
        chance *= 1.0 + (rainIntensity * (config.rainMultiplier - 1.0));

        // Low food modifier
        double saturation = citizen.getSaturation();
        if (saturation < config.lowFoodThreshold) {
            chance *= config.lowFoodMultiplier;
        }

        // Low happiness modifier
        if (citizen.getCitizenHappinessHandler() != null) {
            double happiness = citizen.getCitizenHappinessHandler().getHappiness(citizen.getColony(), citizen);
            if (happiness < config.lowHappinessThreshold) {
                chance *= config.lowHappinessMultiplier;
            }
        }

        // Citizen's trait-based disease modifier
        double traitModifier = citizen.getDiseaseModifier();
        chance *= traitModifier;

        // Apply temporary trait modifiers if available
        if (citizen instanceof IExtendedCitizenData extData) {
            var tempTraits = extData.getTemporaryTraits();
            var tempMods = extData.getTemporaryModifiers();
            // Calculate additional disease rate from temporary effects
            for (var mod : tempMods) {
                if (!mod.isExpired(context.currentTick()) && "diseaseRate".equals(mod.getModifierType())) {
                    chance *= mod.getCurrentValue(context.currentTick());
                }
            }
        }

        LOGGER.debug("Disease chance for {}: base={}, rain={}, final={}",
            citizen.getName(), config.baseChance, rainIntensity, chance);

        return chance;
    }
}
