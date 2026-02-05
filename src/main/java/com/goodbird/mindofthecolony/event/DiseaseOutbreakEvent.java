package com.goodbird.mindofthecolony.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Event representing a disease outbreak affecting one or more citizens.
 */
public class DiseaseOutbreakEvent implements ColonyEvent {
    private final String id;
    private final long timestamp;
    private final int colonyId;
    private final String diseaseId;
    private final String diseaseName;
    private final List<String> affectedCitizenNames;

    public DiseaseOutbreakEvent(long timestamp, int colonyId, String diseaseId, String diseaseName, List<String> affectedCitizenNames) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = timestamp;
        this.colonyId = colonyId;
        this.diseaseId = diseaseId;
        this.diseaseName = diseaseName;
        this.affectedCitizenNames = new ArrayList<>(affectedCitizenNames);
    }

    private DiseaseOutbreakEvent(String id, long timestamp, int colonyId, String diseaseId, String diseaseName, List<String> affectedCitizenNames) {
        this.id = id;
        this.timestamp = timestamp;
        this.colonyId = colonyId;
        this.diseaseId = diseaseId;
        this.diseaseName = diseaseName;
        this.affectedCitizenNames = new ArrayList<>(affectedCitizenNames);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return "disease_outbreak";
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int getColonyId() {
        return colonyId;
    }

    public String getDiseaseId() {
        return diseaseId;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public List<String> getAffectedCitizenNames() {
        return new ArrayList<>(affectedCitizenNames);
    }

    @Override
    public String getDescription() {
        int count = affectedCitizenNames.size();
        if (count == 0) {
            return "A " + diseaseName + " outbreak threatened the colony but no one was affected.";
        } else if (count == 1) {
            return affectedCitizenNames.get(0) + " has fallen ill with " + diseaseName + ".";
        } else if (count == 2) {
            return affectedCitizenNames.get(0) + " and " + affectedCitizenNames.get(1) +
                   " have fallen ill with " + diseaseName + ".";
        } else {
            return "A " + diseaseName + " outbreak has affected " + count + " citizens including " +
                   affectedCitizenNames.get(0) + ".";
        }
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("type", "disease_outbreak");
        tag.putLong("timestamp", timestamp);
        tag.putInt("colonyId", colonyId);
        tag.putString("diseaseId", diseaseId);
        tag.putString("diseaseName", diseaseName);

        ListTag nameList = new ListTag();
        for (String name : affectedCitizenNames) {
            nameList.add(StringTag.valueOf(name));
        }
        tag.put("affectedCitizens", nameList);

        return tag;
    }

    public static DiseaseOutbreakEvent fromNBT(CompoundTag tag) {
        List<String> names = new ArrayList<>();
        if (tag.contains("affectedCitizens", Tag.TAG_LIST)) {
            ListTag nameList = tag.getList("affectedCitizens", Tag.TAG_STRING);
            for (int i = 0; i < nameList.size(); i++) {
                names.add(nameList.getString(i));
            }
        }

        return new DiseaseOutbreakEvent(
            tag.getString("id"),
            tag.getLong("timestamp"),
            tag.getInt("colonyId"),
            tag.getString("diseaseId"),
            tag.getString("diseaseName"),
            names
        );
    }

    @Override
    public String toString() {
        return "DiseaseOutbreakEvent{" +
            "disease='" + diseaseName + '\'' +
            ", affected=" + affectedCitizenNames.size() +
            '}';
    }
}
