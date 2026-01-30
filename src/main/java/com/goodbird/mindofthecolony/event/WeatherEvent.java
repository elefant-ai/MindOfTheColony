package com.goodbird.mindofthecolony.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Event representing significant weather affecting the colony.
 */
public class WeatherEvent implements ColonyEvent {

    public enum WeatherType {
        THUNDERSTORM_START("thunderstorm_start", "A fierce thunderstorm has begun"),
        THUNDERSTORM_ONGOING("thunderstorm_ongoing", "The thunderstorm continues to rage"),
        THUNDERSTORM_END("thunderstorm_end", "The thunderstorm has passed"),
        PROLONGED_RAIN("prolonged_rain", "It has been raining for a long time"),
        RAIN_END("rain_end", "The rain has finally stopped");

        private final String id;
        private final String defaultDescription;

        WeatherType(String id, String defaultDescription) {
            this.id = id;
            this.defaultDescription = defaultDescription;
        }

        public String getId() {
            return id;
        }

        public String getDefaultDescription() {
            return defaultDescription;
        }

        public static WeatherType fromId(String id) {
            for (WeatherType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return PROLONGED_RAIN; // Default
        }
    }

    private final String id;
    private final long timestamp;
    private final int colonyId;
    private final WeatherType weatherType;
    private final List<String> affectedCitizenNames;

    public WeatherEvent(long timestamp, int colonyId, WeatherType weatherType, List<String> affectedCitizenNames) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = timestamp;
        this.colonyId = colonyId;
        this.weatherType = weatherType;
        this.affectedCitizenNames = new ArrayList<>(affectedCitizenNames);
    }

    private WeatherEvent(String id, long timestamp, int colonyId, WeatherType weatherType, List<String> affectedCitizenNames) {
        this.id = id;
        this.timestamp = timestamp;
        this.colonyId = colonyId;
        this.weatherType = weatherType;
        this.affectedCitizenNames = new ArrayList<>(affectedCitizenNames);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return "weather";
    }

    public WeatherType getWeatherType() {
        return weatherType;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int getColonyId() {
        return colonyId;
    }

    public List<String> getAffectedCitizenNames() {
        return new ArrayList<>(affectedCitizenNames);
    }

    @Override
    public String getDescription() {
        String base = weatherType.getDefaultDescription();

        if ((weatherType == WeatherType.THUNDERSTORM_START || weatherType == WeatherType.THUNDERSTORM_ONGOING)
                && !affectedCitizenNames.isEmpty()) {
            if (affectedCitizenNames.size() == 1) {
                return base + ". " + affectedCitizenNames.get(0) + " is frightened by the thunder.";
            } else {
                return base + ". Several colonists including " + affectedCitizenNames.get(0) + " are frightened.";
            }
        }

        if (weatherType == WeatherType.PROLONGED_RAIN && !affectedCitizenNames.isEmpty()) {
            return base + ". The constant dampness is affecting the colony's morale.";
        }

        return base + ".";
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("type", "weather");
        tag.putLong("timestamp", timestamp);
        tag.putInt("colonyId", colonyId);
        tag.putString("weatherType", weatherType.getId());

        ListTag nameList = new ListTag();
        for (String name : affectedCitizenNames) {
            nameList.add(StringTag.valueOf(name));
        }
        tag.put("affectedCitizens", nameList);

        return tag;
    }

    public static WeatherEvent fromNBT(CompoundTag tag) {
        List<String> names = new ArrayList<>();
        if (tag.contains("affectedCitizens", Tag.TAG_LIST)) {
            ListTag nameList = tag.getList("affectedCitizens", Tag.TAG_STRING);
            for (int i = 0; i < nameList.size(); i++) {
                names.add(nameList.getString(i));
            }
        }

        return new WeatherEvent(
            tag.getString("id"),
            tag.getLong("timestamp"),
            tag.getInt("colonyId"),
            WeatherType.fromId(tag.getString("weatherType")),
            names
        );
    }

    @Override
    public String toString() {
        return "WeatherEvent{" +
            "weatherType=" + weatherType +
            ", affected=" + affectedCitizenNames.size() +
            '}';
    }
}
