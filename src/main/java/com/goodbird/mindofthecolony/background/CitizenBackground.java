package com.goodbird.mindofthecolony.background;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a citizen's permanent background identity:
 * origin backstory and personality/physical/social traits.
 * Assigned once on first spawn, persisted via NBT forever.
 */
public class CitizenBackground {
    private String origin;
    private final List<String> traits;
    private transient TraitModifiers cachedModifiers;

    public CitizenBackground() {
        this.origin = null;
        this.traits = new ArrayList<>();
        this.cachedModifiers = null;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
        invalidateCache();
    }

    public List<String> getTraits() {
        return traits;
    }

    public void addTrait(String trait) {
        this.traits.add(trait);
        invalidateCache();
    }

    public boolean isInitialized() {
        return origin != null && !traits.isEmpty();
    }

    private void invalidateCache() {
        this.cachedModifiers = null;
    }

    /**
     * Get computed modifiers from all traits and origin.
     */
    public TraitModifiers getModifiers() {
        if (cachedModifiers == null) {
            cachedModifiers = TraitModifierCalculator.calculate(traits, origin);
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
        if (origin != null) {
            tag.putString("origin", origin);
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

        // Origin
        if (tag.contains("origin", Tag.TAG_STRING)) {
            bg.origin = tag.getString("origin");
        }

        // NEW FORMAT: traits list
        if (tag.contains("traits", Tag.TAG_LIST)) {
            ListTag list = tag.getList("traits", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                bg.traits.add(list.getString(i));
            }
        }
        // OLD FORMAT: personality + penalties -> migrate to traits
        else {
            if (tag.contains("personality", Tag.TAG_STRING)) {
                bg.traits.add(tag.getString("personality"));
            }
            if (tag.contains("penalties", Tag.TAG_LIST)) {
                ListTag list = tag.getList("penalties", Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) {
                    bg.traits.add(list.getString(i));
                }
            }
        }

        return bg;
    }

    // --- AI prompt generation ---

    public String toSystemPromptSection() {
        StringBuilder sb = new StringBuilder("BACKGROUND:\n");

        // Origin
        OriginDefinition originDef = TraitRegistry.getOrigin(origin);
        if (originDef != null) {
            sb.append("- Origin: ").append(originDef.displayText()).append("\n");
        }

        // Traits
        for (String traitId : traits) {
            TraitDefinition traitDef = TraitRegistry.getTrait(traitId);
            if (traitDef != null) {
                sb.append("- Trait: ").append(traitDef.displayText()).append("\n");
            }
        }

        return sb.toString();
    }

    public String toStatusString() {
        StringBuilder sb = new StringBuilder();
        sb.append("origin=").append(origin != null ? origin : "none");
        sb.append(", traits=[");
        sb.append(String.join(", ", traits));
        sb.append("]");
        return sb.toString();
    }
}
