package com.goodbird.mindofthecolony.background;

import java.util.*;

/**
 * Runtime storage for trait definitions loaded from config.
 */
public final class TraitRegistry {

    private TraitRegistry() {}

    private static Map<String, TraitDefinition> traits = new HashMap<>();
    private static List<TraitDefinition> traitList = new ArrayList<>();

    public static void load(List<TraitDefinition> newTraits) {
        traits.clear();
        traitList.clear();
        for (TraitDefinition t : newTraits) {
            traits.put(t.id(), t);
            traitList.add(t);
        }
    }

    public static TraitDefinition getTrait(String id) {
        return traits.get(id);
    }

    public static List<TraitDefinition> getAllTraits() {
        return Collections.unmodifiableList(traitList);
    }

    public static double getTotalWeight() {
        return traitList.stream().mapToDouble(TraitDefinition::weight).sum();
    }
}
