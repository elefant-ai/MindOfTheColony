package com.goodbird.mindofthecolony.background;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a citizen's permanent background identity:
 * origin backstory, personality traits, and penalties.
 * Assigned once on first spawn, persisted via NBT forever.
 */
public class CitizenBackground {
    private String origin;
    private String personalityTrait;
    private final List<String> penalties;

    public CitizenBackground() {
        this.origin = null;
        this.personalityTrait = null;
        this.penalties = new ArrayList<>();
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getPersonalityTrait() {
        return personalityTrait;
    }

    public void setPersonalityTrait(String trait) {
        this.personalityTrait = trait;
    }

    public List<String> getPenalties() {
        return penalties;
    }

    public void addPenalty(String penalty) {
        this.penalties.add(penalty);
    }

    public boolean isInitialized() {
        return origin != null && personalityTrait != null;
    }

    // --- NBT serialization ---

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        if (origin != null) {
            tag.putString("origin", origin);
        }
        if (personalityTrait != null) {
            tag.putString("personality", personalityTrait);
        }
        ListTag penaltyList = new ListTag();
        for (String p : penalties) {
            penaltyList.add(StringTag.valueOf(p));
        }
        tag.put("penalties", penaltyList);
        return tag;
    }

    public static CitizenBackground fromNBT(CompoundTag tag) {
        CitizenBackground bg = new CitizenBackground();
        if (tag.contains("origin", Tag.TAG_STRING)) {
            bg.origin = tag.getString("origin");
        }
        if (tag.contains("personality", Tag.TAG_STRING)) {
            bg.personalityTrait = tag.getString("personality");
        }
        if (tag.contains("penalties", Tag.TAG_LIST)) {
            ListTag list = tag.getList("penalties", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                bg.penalties.add(list.getString(i));
            }
        }
        return bg;
    }

    // --- AI prompt generation ---

    public String toSystemPromptSection() {
        StringBuilder sb = new StringBuilder("BACKGROUND:\n");

        BackgroundDefinitions.getOrigins().stream()
            .filter(e -> e.id().equals(origin))
            .findFirst()
            .ifPresent(e -> sb.append("- Origin: ").append(e.displayText()).append("\n"));

        BackgroundDefinitions.getPersonalityTraits().stream()
            .filter(e -> e.id().equals(personalityTrait))
            .findFirst()
            .ifPresent(e -> sb.append("- Personality: ").append(e.displayText()).append("\n"));

        for (String penaltyId : penalties) {
            BackgroundDefinitions.getPenalties().stream()
                .filter(e -> e.id().equals(penaltyId))
                .findFirst()
                .ifPresent(e -> {
                    String label = switch (e.category()) {
                        case CRIMINAL -> "Criminal record";
                        case CONVERSATION -> "Hidden secret";
                        case SOCIAL -> "Social burden";
                        case FLAW -> "Personal flaw";
                    };
                    sb.append("- ").append(label).append(": ").append(e.displayText()).append("\n");
                });
        }

        return sb.toString();
    }

    public String toStatusString() {
        StringBuilder sb = new StringBuilder();
        sb.append("origin=").append(origin != null ? origin : "none");
        sb.append(", personality=").append(personalityTrait != null ? personalityTrait : "none");
        sb.append(", penalties=[");
        sb.append(String.join(", ", penalties));
        sb.append("]");
        return sb.toString();
    }
}
