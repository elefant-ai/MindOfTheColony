package com.goodbird.mindofthecolony.background;

import java.util.*;

/**
 * Runtime storage for trait and origin definitions loaded from config.
 */
public final class TraitRegistry {

    private TraitRegistry() {}

    private static Map<String, TraitDefinition> traits = new HashMap<>();
    private static Map<String, OriginDefinition> origins = new HashMap<>();
    private static List<TraitDefinition> traitList = new ArrayList<>();
    private static List<OriginDefinition> originList = new ArrayList<>();

    public static void load(List<TraitDefinition> newTraits, List<OriginDefinition> newOrigins) {
        traits.clear();
        traitList.clear();
        for (TraitDefinition t : newTraits) {
            traits.put(t.id(), t);
            traitList.add(t);
        }

        origins.clear();
        originList.clear();
        for (OriginDefinition o : newOrigins) {
            origins.put(o.id(), o);
            originList.add(o);
        }
    }

    public static TraitDefinition getTrait(String id) {
        return traits.get(id);
    }

    public static OriginDefinition getOrigin(String id) {
        return origins.get(id);
    }

    public static List<TraitDefinition> getAllTraits() {
        return Collections.unmodifiableList(traitList);
    }

    public static List<OriginDefinition> getAllOrigins() {
        return Collections.unmodifiableList(originList);
    }

    public static double getTotalWeight() {
        return traitList.stream().mapToDouble(TraitDefinition::weight).sum();
    }
}
