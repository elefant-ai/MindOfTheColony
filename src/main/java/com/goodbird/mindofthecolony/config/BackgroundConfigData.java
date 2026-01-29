package com.goodbird.mindofthecolony.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Gson-serializable POJOs for the JSON config files.
 * Each inner class maps to one JSON file's array element.
 */
public class BackgroundConfigData {

    public static class OriginData {
        public String id;
        public String displayText;
        public Map<String, Double> modifiers;
    }

    public static class TraitData {
        public String id;
        public String displayText;
        public double weight = 1.0;
        public Map<String, Double> modifiers = new HashMap<>();
    }
}
