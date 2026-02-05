package com.goodbird.mindofthecolony.event;

import java.util.List;

/**
 * Interface for event generators that evaluate colony conditions
 * and potentially trigger events.
 */
public interface EventEvaluator {

    /**
     * Get the type of events this evaluator generates.
     */
    String getEventType();

    /**
     * Check if this evaluator is enabled.
     */
    boolean isEnabled();

    /**
     * Evaluate the current context and return any events that should be triggered.
     *
     * @param context The evaluation context containing colony state
     * @return List of events to trigger (may be empty)
     */
    List<ColonyEvent> evaluate(EventContext context);
}
