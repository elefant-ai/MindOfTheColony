package com.goodbird.mindofthecolony.preference;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores work preferences for a citizen.
 * Preferences are integers from -10 to 10, where:
 * - Positive values indicate preference (want this work)
 * - Negative values indicate avoidance (don't want this work)
 * - 0 is neutral
 */
public class WorkPreferences {
    // Category preferences: "building", "decoration", "miner", "plantation_field"
    private final Map<String, Integer> categoryPreferences = new HashMap<>();

    // Action preferences: "BUILD", "UPGRADE", "REPAIR", "REMOVE"
    private final Map<String, Integer> actionPreferences = new HashMap<>();

    // Specific building type preferences: "residence", "barracks", "farm", etc.
    private final Map<String, Integer> buildingTypePreferences = new HashMap<>();

    public void setCategoryPreference(String category, int preference) {
        categoryPreferences.put(category.toLowerCase(), clamp(preference));
    }

    public void setActionPreference(String action, int preference) {
        actionPreferences.put(action.toUpperCase(), clamp(preference));
    }

    public void setBuildingTypePreference(String buildingType, int preference) {
        buildingTypePreferences.put(buildingType.toLowerCase(), clamp(preference));
    }

    public int getCategoryPreference(String category) {
        return categoryPreferences.getOrDefault(category.toLowerCase(), 0);
    }

    public int getActionPreference(String action) {
        return actionPreferences.getOrDefault(action.toUpperCase(), 0);
    }

    public int getBuildingTypePreference(String buildingType) {
        return buildingTypePreferences.getOrDefault(buildingType.toLowerCase(), 0);
    }

    /**
     * Calculate overall preference score for a work order.
     * Higher score = more preferred.
     *
     * @param category Work order category (building, decoration, miner, plantation_field)
     * @param action Work order action (BUILD, UPGRADE, REPAIR, REMOVE)
     * @param buildingType Specific building type if applicable (residence, barracks, etc.)
     * @return Combined preference score
     */
    public int calculateScore(String category, String action, String buildingType) {
        int score = 0;

        // Add category preference
        if (category != null) {
            score += categoryPreferences.getOrDefault(category.toLowerCase(), 0);
        }

        // Add action preference
        if (action != null) {
            score += actionPreferences.getOrDefault(action.toUpperCase(), 0);
        }

        // Add building type preference (most specific)
        if (buildingType != null && !buildingType.isEmpty()) {
            score += buildingTypePreferences.getOrDefault(buildingType.toLowerCase(), 0);
        }

        return score;
    }

    /**
     * Check if this citizen has any preferences set.
     */
    public boolean hasPreferences() {
        return !categoryPreferences.isEmpty()
            || !actionPreferences.isEmpty()
            || !buildingTypePreferences.isEmpty();
    }

    private int clamp(int value) {
        return Math.max(-10, Math.min(10, value));
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("categoryPrefs", mapToListTag(categoryPreferences));
        tag.put("actionPrefs", mapToListTag(actionPreferences));
        tag.put("buildingTypePrefs", mapToListTag(buildingTypePreferences));
        return tag;
    }

    public static WorkPreferences fromNBT(CompoundTag tag) {
        WorkPreferences prefs = new WorkPreferences();
        if (tag.contains("categoryPrefs", Tag.TAG_LIST)) {
            listTagToMap(tag.getList("categoryPrefs", Tag.TAG_COMPOUND), prefs.categoryPreferences);
        }
        if (tag.contains("actionPrefs", Tag.TAG_LIST)) {
            listTagToMap(tag.getList("actionPrefs", Tag.TAG_COMPOUND), prefs.actionPreferences);
        }
        if (tag.contains("buildingTypePrefs", Tag.TAG_LIST)) {
            listTagToMap(tag.getList("buildingTypePrefs", Tag.TAG_COMPOUND), prefs.buildingTypePreferences);
        }
        return prefs;
    }

    private ListTag mapToListTag(Map<String, Integer> map) {
        ListTag list = new ListTag();
        for (var entry : map.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("key", entry.getKey());
            entryTag.putInt("value", entry.getValue());
            list.add(entryTag);
        }
        return list;
    }

    private static void listTagToMap(ListTag list, Map<String, Integer> map) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            map.put(entryTag.getString("key"), entryTag.getInt("value"));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WorkPreferences{");
        if (!categoryPreferences.isEmpty()) {
            sb.append("categories=").append(categoryPreferences);
        }
        if (!actionPreferences.isEmpty()) {
            if (sb.length() > 16) sb.append(", ");
            sb.append("actions=").append(actionPreferences);
        }
        if (!buildingTypePreferences.isEmpty()) {
            if (sb.length() > 16) sb.append(", ");
            sb.append("buildingTypes=").append(buildingTypePreferences);
        }
        sb.append("}");
        return sb.toString();
    }
}
