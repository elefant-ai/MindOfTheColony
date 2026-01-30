package com.goodbird.mindofthecolony.event.evaluator;

import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.config.EventConfig;
import com.goodbird.mindofthecolony.effect.TemporaryModifier;
import com.goodbird.mindofthecolony.effect.TemporaryTrait;
import com.goodbird.mindofthecolony.event.*;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.ICitizenData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates weather-related events (thunderstorms, prolonged rain).
 * Applies temporary traits and modifiers to affected citizens.
 */
public class WeatherEventEvaluator implements EventEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherEventEvaluator.class);

    // Track if we've already processed the current thunderstorm
    private boolean thunderstormActive = false;
    private boolean prolongedRainTriggered = false;
    private int lastRainTicksChecked = 0;

    @Override
    public String getEventType() {
        return "weather";
    }

    @Override
    public boolean isEnabled() {
        return EventConfig.getThunderstormConfig().enabled ||
               EventConfig.getProlongedRainConfig().enabled;
    }

    @Override
    public List<ColonyEvent> evaluate(EventContext context) {
        List<ColonyEvent> events = new ArrayList<>();

        // Check for thunderstorm events
        if (EventConfig.getThunderstormConfig().enabled) {
            evaluateThunderstorm(context, events);
        }

        // Check for prolonged rain events
        if (EventConfig.getProlongedRainConfig().enabled) {
            evaluateProlongedRain(context, events);
        }

        return events;
    }

    private void evaluateThunderstorm(EventContext context, List<ColonyEvent> events) {
        EventConfig.ThunderstormConfig config = EventConfig.getThunderstormConfig();
        WeatherState weather = context.weather();

        // Detect thunderstorm start
        if (weather.isCurrentlyThundering() && !thunderstormActive) {
            thunderstormActive = true;
            LOGGER.debug("Thunderstorm started in colony {}", context.colony().getID());

            List<String> affectedNames = new ArrayList<>();

            // Apply effects to susceptible citizens
            for (ICitizenData citizen : context.citizens()) {
                if (isSusceptibleToThunder(citizen, config)) {
                    applyThunderstormEffects(citizen, context, config);
                    affectedNames.add(citizen.getName());
                }
            }

            // Create event
            WeatherEvent event = new WeatherEvent(
                context.currentTick(),
                context.colony().getID(),
                WeatherEvent.WeatherType.THUNDERSTORM_START,
                affectedNames
            );
            events.add(event);

            LOGGER.info("Thunderstorm event: {} citizens affected", affectedNames.size());

        } else if (!weather.isCurrentlyThundering() && thunderstormActive) {
            // Thunderstorm ended
            thunderstormActive = false;
            LOGGER.debug("Thunderstorm ended in colony {}", context.colony().getID());

            WeatherEvent event = new WeatherEvent(
                context.currentTick(),
                context.colony().getID(),
                WeatherEvent.WeatherType.THUNDERSTORM_END,
                List.of()
            );
            events.add(event);
        }
    }

    private boolean isSusceptibleToThunder(ICitizenData citizen, EventConfig.ThunderstormConfig config) {
        if (!(citizen instanceof IExtendedCitizenData extData)) {
            return false;
        }

        CitizenBackground bg = extData.getCitizenBackground();
        if (bg == null) {
            return false;
        }

        // Check if citizen has any of the susceptible traits
        List<String> affectedTraits = config.affectedTraits;
        for (String trait : bg.getTraits()) {
            if (affectedTraits.contains(trait)) {
                return true;
            }
        }

        return false;
    }

    private void applyThunderstormEffects(ICitizenData citizen, EventContext context,
                                          EventConfig.ThunderstormConfig config) {
        if (!(citizen instanceof IExtendedCitizenData extData)) {
            return;
        }

        // Add temporary "frightened" trait
        if (config.temporaryTrait != null && !config.temporaryTrait.isEmpty()) {
            TemporaryTrait trait = new TemporaryTrait(
                config.temporaryTrait,
                "thunderstorm",
                context.currentTick(),
                config.traitDurationTicks
            );
            extData.addTemporaryTrait(trait);
            LOGGER.debug("Applied {} trait to {}", config.temporaryTrait, citizen.getName());
        }

        // Add decaying happiness penalty modifier
        if (config.happinessPenalty != 0) {
            TemporaryModifier modifier = new TemporaryModifier(
                "thunderstorm",
                "happinessBase",
                config.happinessPenalty,
                context.currentTick(),
                config.traitDurationTicks,
                true  // decays over time
            );
            extData.addTemporaryModifier(modifier);
            LOGGER.debug("Applied happiness penalty {} to {}", config.happinessPenalty, citizen.getName());
        }
    }

    private void evaluateProlongedRain(EventContext context, List<ColonyEvent> events) {
        EventConfig.ProlongedRainConfig config = EventConfig.getProlongedRainConfig();
        WeatherState weather = context.weather();

        int rainTicks = weather.getTicksRaining();

        // Check if rain has reached threshold
        if (rainTicks >= config.rainTicksThreshold && !prolongedRainTriggered) {
            prolongedRainTriggered = true;
            LOGGER.debug("Prolonged rain triggered in colony {} after {} ticks",
                context.colony().getID(), rainTicks);

            List<String> affectedNames = new ArrayList<>();

            // Apply effects to all citizens
            for (ICitizenData citizen : context.citizens()) {
                applyProlongedRainEffects(citizen, context, config);
                affectedNames.add(citizen.getName());
            }

            // Create event
            WeatherEvent event = new WeatherEvent(
                context.currentTick(),
                context.colony().getID(),
                WeatherEvent.WeatherType.PROLONGED_RAIN,
                affectedNames
            );
            events.add(event);

            LOGGER.info("Prolonged rain event: {} citizens affected", affectedNames.size());

        } else if (rainTicks == 0 && prolongedRainTriggered) {
            // Rain stopped
            prolongedRainTriggered = false;
            LOGGER.debug("Rain stopped in colony {}", context.colony().getID());

            WeatherEvent event = new WeatherEvent(
                context.currentTick(),
                context.colony().getID(),
                WeatherEvent.WeatherType.RAIN_END,
                List.of()
            );
            events.add(event);
        }

        lastRainTicksChecked = rainTicks;
    }

    private void applyProlongedRainEffects(ICitizenData citizen, EventContext context,
                                           EventConfig.ProlongedRainConfig config) {
        if (!(citizen instanceof IExtendedCitizenData extData)) {
            return;
        }

        // Add temporary "damp" trait
        if (config.temporaryTrait != null && !config.temporaryTrait.isEmpty()) {
            TemporaryTrait trait = new TemporaryTrait(
                config.temporaryTrait,
                "prolonged_rain",
                context.currentTick(),
                config.traitDurationTicks
            );
            extData.addTemporaryTrait(trait);
            LOGGER.debug("Applied {} trait to {}", config.temporaryTrait, citizen.getName());
        }

        // Add disease rate modifier
        if (config.diseaseRateMultiplier != 1.0) {
            TemporaryModifier modifier = new TemporaryModifier(
                "prolonged_rain",
                "diseaseRate",
                config.diseaseRateMultiplier,
                context.currentTick(),
                config.traitDurationTicks,
                false  // doesn't decay, full effect until expiry
            );
            extData.addTemporaryModifier(modifier);
            LOGGER.debug("Applied disease rate modifier {} to {}",
                config.diseaseRateMultiplier, citizen.getName());
        }
    }

    /**
     * Reset state when manager is reloaded.
     */
    public void reset() {
        thunderstormActive = false;
        prolongedRainTriggered = false;
        lastRainTicksChecked = 0;
    }
}
