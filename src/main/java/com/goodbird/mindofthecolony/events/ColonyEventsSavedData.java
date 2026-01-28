package com.goodbird.mindofthecolony.events;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * NeoForge SavedData for persisting colony event logs to the world save.
 */
public class ColonyEventsSavedData extends SavedData {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColonyEventsSavedData.class);
    private static final String DATA_NAME = "mindofthecolony_events";

    public ColonyEventsSavedData() {
    }

    /**
     * Called when loading existing data from disk.
     */
    public static ColonyEventsSavedData load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        ColonyEventsSavedData data = new ColonyEventsSavedData();

        if (tag.contains("colonies", Tag.TAG_LIST)) {
            ListTag coloniesTag = tag.getList("colonies", Tag.TAG_COMPOUND);
            for (int i = 0; i < coloniesTag.size(); i++) {
                CompoundTag colonyTag = coloniesTag.getCompound(i);
                int colonyId = colonyTag.getInt("colonyId");
                ColonyEventLog log = ColonyEventLog.fromNBT(colonyTag);
                ColonyEventManager.getInstance().loadColonyLog(colonyId, log);
            }
            LOGGER.info("Loaded event logs for {} colonies from saved data", coloniesTag.size());
        }

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        ListTag coloniesTag = new ListTag();
        for (Map.Entry<Integer, ColonyEventLog> entry : ColonyEventManager.getInstance().getAllLogs().entrySet()) {
            CompoundTag colonyTag = entry.getValue().toNBT();
            coloniesTag.add(colonyTag);
        }
        tag.put("colonies", coloniesTag);
        return tag;
    }

    /**
     * Get or create the SavedData from the overworld level.
     */
    public static ColonyEventsSavedData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<>(
                ColonyEventsSavedData::new,
                ColonyEventsSavedData::load
            ),
            DATA_NAME
        );
    }
}
