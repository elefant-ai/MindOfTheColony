package com.goodbird.mindofthecolony.config;

/**
 * Gson-serializable POJOs for the event JSON config files.
 * Each inner class maps to one JSON file's array element.
 */
public class EventConfigData {

    public static class DetectedEventData {
        public String id;
        public String category;
        public String descriptionTemplate;
        public int durationDays;
        public double happinessModifier;
    }

    public static class FlavorEventData {
        public String id;
        public String category;
        public String descriptionTemplate;
        public int durationDays;
        public double happinessModifier;
        public double weight;
    }
}
