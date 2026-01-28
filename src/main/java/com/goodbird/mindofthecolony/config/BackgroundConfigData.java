package com.goodbird.mindofthecolony.config;

/**
 * Gson-serializable POJOs for the JSON config files.
 * Each inner class maps to one JSON file's array element.
 */
public class BackgroundConfigData {

    public static class OriginData {
        public String id;
        public String displayText;
    }

    public static class TraitData {
        public String id;
        public String displayText;
    }

    public static class PenaltyData {
        public String id;
        public String category;
        public String displayText;
    }
}
