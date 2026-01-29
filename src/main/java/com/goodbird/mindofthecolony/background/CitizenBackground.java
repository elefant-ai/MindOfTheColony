package com.goodbird.mindofthecolony.background;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a citizen's permanent background identity:
 * AI-generated backstory and traits (for modifiers).
 * Assigned once on first spawn, persisted via NBT forever.
 */
public class CitizenBackground {
    private String backstory;  // AI-generated freeform backstory
    private final List<String> traits;  // Trait IDs for modifiers
    private transient TraitModifiers cachedModifiers;

    public CitizenBackground() {
        this.backstory = null;
        this.traits = new ArrayList<>();
        this.cachedModifiers = null;
    }

    public String getBackstory() {
        return backstory;
    }

    public void setBackstory(String backstory) {
        this.backstory = backstory;
    }

    public List<String> getTraits() {
        return traits;
    }

    public void addTrait(String trait) {
        this.traits.add(trait);
        invalidateCache();
    }

    public boolean isInitialized() {
        return backstory != null && !backstory.isEmpty();
    }

    private void invalidateCache() {
        this.cachedModifiers = null;
    }

    /**
     * Get computed modifiers from all traits.
     */
    public TraitModifiers getModifiers() {
        if (cachedModifiers == null) {
            cachedModifiers = TraitModifierCalculator.calculate(traits);
        }
        return cachedModifiers;
    }

    /**
     * Convenience method for disease rate modifier.
     */
    public double getDiseaseRateModifier() {
        return getModifiers().diseaseRate();
    }

    /**
     * Convenience method for contact disease rate modifier.
     */
    public double getContactDiseaseRateModifier() {
        return getModifiers().contactDiseaseRate();
    }

    // --- NBT serialization ---

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        if (backstory != null) {
            tag.putString("backstory", backstory);
        }
        ListTag traitList = new ListTag();
        for (String t : traits) {
            traitList.add(StringTag.valueOf(t));
        }
        tag.put("traits", traitList);
        return tag;
    }

    public static CitizenBackground fromNBT(CompoundTag tag) {
        CitizenBackground bg = new CitizenBackground();

        if (tag.contains("backstory", Tag.TAG_STRING)) {
            bg.backstory = tag.getString("backstory");
        }

        if (tag.contains("traits", Tag.TAG_LIST)) {
            ListTag list = tag.getList("traits", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                bg.traits.add(list.getString(i));
            }
        }

        return bg;
    }

    // --- AI prompt generation ---

    public String toSystemPromptSection() {
        StringBuilder sb = new StringBuilder("BACKGROUND:\n");

        // AI-generated backstory
        if (backstory != null && !backstory.isEmpty()) {
            sb.append(backstory).append("\n");
        }

        // Traits (for display, show their descriptions)
        for (String traitId : traits) {
            TraitDefinition traitDef = TraitRegistry.getTrait(traitId);
            if (traitDef != null) {
                sb.append("- ").append(traitDef.displayText()).append("\n");
            }
        }

        return sb.toString();
    }

    public String toStatusString() {
        StringBuilder sb = new StringBuilder();
        sb.append("backstory=").append(backstory != null ? "\"" + backstory.substring(0, Math.min(50, backstory.length())) + "...\"" : "none");
        sb.append(", traits=[");
        sb.append(String.join(", ", traits));
        sb.append("]");
        return sb.toString();
    }
}
