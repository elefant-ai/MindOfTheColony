package com.goodbird.mindofthecolony.mc.core.colony.managers;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.goodbird.mindofthecolony.background.BackgroundGenerationService;
import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.ICivilianData;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.colony.IVisitorData;
import com.goodbird.mindofthecolony.mc.api.colony.managers.interfaces.IVisitorManager;
import com.goodbird.mindofthecolony.mc.api.entity.ModEntities;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.AbstractCivilianEntity;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.AbstractEntityCitizen;
import com.goodbird.mindofthecolony.mc.api.util.Log;
import com.goodbird.mindofthecolony.mc.api.util.WorldUtil;
import com.goodbird.mindofthecolony.mc.core.colony.VisitorData;
import com.goodbird.mindofthecolony.mc.core.entity.visitor.VisitorCitizen;
import com.goodbird.mindofthecolony.mc.core.network.messages.client.colony.ColonyVisitorViewDataMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.goodbird.mindofthecolony.mc.api.util.constant.Constants.SLIGHTLY_UP;
import static com.goodbird.mindofthecolony.mc.api.util.constant.PathingConstants.HALF_A_BLOCK;

/**
 * Manages all visiting entities to the colony
 */
public class VisitorManager implements IVisitorManager
{
    private static final Logger MOTC_LOGGER = LoggerFactory.getLogger(VisitorManager.class);

    /**
     * NBT Tags
     */
    public static String TAG_VISIT_MANAGER = "visitManager";
    public static String TAG_VISITORS      = "visitors";
    public static String TAG_NEXTID        = "nextID";

    /**
     * Map with visitor ID and data
     */
    private Map<Integer, IVisitorData> visitorMap = new HashMap<>();

    /**
     * Whether this manager is dirty and needs re-serialize
     */
    private boolean isDirty = false;

    /**
     * The colony of the manager.
     */
    private final IColony colony;

    /**
     * The next free ID
     */
    private int nextVisitorID = -1;

    public VisitorManager(final IColony colony)
    {
        this.colony = colony;
    }

    @Override
    public void registerCivilian(final AbstractCivilianEntity visitor)
    {
        if (visitor.getCivilianID() == 0 || visitorMap.get(visitor.getCivilianID()) == null)
        {
            if (!visitor.isAddedToLevel())
            {
                Log.getLogger().warn("Discarding entity not added to world, should be only called after:", new Exception());
            }
            visitor.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        final ICitizenData data = visitorMap.get(visitor.getCivilianID());

        if (data == null || !visitor.getUUID().equals(data.getUUID()))
        {
            if (!visitor.isAddedToLevel())
            {
                Log.getLogger().warn("Discarding entity not added to world, should be only called after:", new Exception());
            }
            visitor.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        final Optional<AbstractEntityCitizen> existingCitizen = data.getEntity();

        if (!existingCitizen.isPresent())
        {
            data.setEntity(visitor);
            visitor.setCivilianData(data);
            return;
        }

        if (existingCitizen.get() == visitor)
        {
            return;
        }

        if (visitor.isAlive())
        {
            existingCitizen.get().remove(Entity.RemovalReason.DISCARDED);
            data.setEntity(visitor);
            visitor.setCivilianData(data);
            return;
        }

        if (!visitor.isAddedToLevel())
        {
            Log.getLogger().warn("Discarding entity not added to world, should be only called after:", new Exception());
        }
        visitor.remove(Entity.RemovalReason.DISCARDED);
    }

    @Override
    public void unregisterCivilian(final AbstractCivilianEntity entity)
    {
        final ICitizenData data = visitorMap.get(entity.getCivilianID());
        if (data != null && data.getEntity().isPresent() && data.getEntity().get() == entity)
        {
            visitorMap.get(entity.getCivilianID()).setEntity(null);
        }
    }

    @Override
    public void read(@NotNull final HolderLookup.Provider provider, @NotNull final CompoundTag compound)
    {
        if (compound.contains(TAG_VISIT_MANAGER))
        {
            final CompoundTag visitorManagerNBT = compound.getCompound(TAG_VISIT_MANAGER);
            final ListTag citizenList = visitorManagerNBT.getList(TAG_VISITORS, Tag.TAG_COMPOUND);
            for (final Tag citizen : citizenList)
            {
                final IVisitorData data = VisitorData.loadVisitorFromNBT(colony, (CompoundTag) citizen, provider);
                visitorMap.put(data.getId(), data);
            }

            nextVisitorID = visitorManagerNBT.getInt(TAG_NEXTID);
        }
        markDirty();
    }

    @Override
    public void write(@NotNull final HolderLookup.Provider provider, @NotNull final CompoundTag compoundNBT)
    {
        final CompoundTag visitorManagerNBT = new CompoundTag();

        final ListTag citizenList = new ListTag();
        for (Map.Entry<Integer, IVisitorData> entry : visitorMap.entrySet())
        {
            citizenList.add(entry.getValue().serializeNBT(provider));
        }

        visitorManagerNBT.put(TAG_VISITORS, citizenList);
        visitorManagerNBT.putInt(TAG_NEXTID, nextVisitorID);
        compoundNBT.put(TAG_VISIT_MANAGER, visitorManagerNBT);
    }

    @Override
    public void sendPackets(@NotNull final Set<ServerPlayer> closeSubscribers, @NotNull final Set<ServerPlayer> newSubscribers)
    {
        Set<IVisitorData> toSend = null;
        boolean refresh = !newSubscribers.isEmpty() || this.isDirty;

        if (refresh)
        {
            toSend = new HashSet<>(visitorMap.values());
            for (final IVisitorData data : visitorMap.values())
            {
                data.clearDirty();
            }
            this.clearDirty();
        }
        else
        {
            for (final IVisitorData data : visitorMap.values())
            {
                if (data.isDirty())
                {
                    if (toSend == null)
                    {
                        toSend = new HashSet<>();
                    }

                    toSend.add(data);
                }
                data.clearDirty();
            }
        }

        if (toSend == null || toSend.isEmpty())
        {
            return;
        }

        Set<ServerPlayer> players = new HashSet<>(newSubscribers);
        players.addAll(closeSubscribers);
        new ColonyVisitorViewDataMessage(colony, toSend, refresh).sendToPlayer(players);
    }

    @NotNull
    @Override
    public Map<Integer, ICivilianData> getCivilianDataMap()
    {
        return Collections.unmodifiableMap(visitorMap);
    }

    @Override
    public IVisitorData getCivilian(final int citizenId)
    {
        return visitorMap.get(citizenId);
    }

    @Override
    public <T extends IVisitorData> T getVisitor(int citizenId)
    {
        return (T) visitorMap.get(citizenId);
    }

    @Override
    public IVisitorData spawnOrCreateCivilian(ICivilianData data, final Level world, final BlockPos spawnPos, final boolean force)
    {
        if (!WorldUtil.isEntityBlockLoaded(world, spawnPos))
        {
            return (IVisitorData) data;
        }

        if (data == null)
        {
            data = createAndRegisterCivilianData();
        }

        VisitorCitizen citizenEntity = (VisitorCitizen) ModEntities.VISITOR.create(colony.getWorld());

        if (citizenEntity == null)
        {
            return (IVisitorData) data;
        }

        citizenEntity.setUUID(data.getUUID());
        citizenEntity.setPos(spawnPos.getX() + HALF_A_BLOCK, spawnPos.getY() + SLIGHTLY_UP, spawnPos.getZ() + HALF_A_BLOCK);
        world.addFreshEntity(citizenEntity);

        citizenEntity.setCitizenId(data.getId());
        citizenEntity.getCitizenColonyHandler().setColonyId(colony.getID());
        if (citizenEntity.isAddedToLevel())
        {
            citizenEntity.getCitizenColonyHandler().registerWithColony(data.getColony().getID(), data.getId());
        }

        return (IVisitorData) data;
    }

    @Override
    public IVisitorData createAndRegisterCivilianData()
    {
        markDirty();
        final IVisitorData data = new VisitorData(nextVisitorID--, colony);
        data.initForNewCivilian();
        visitorMap.put(data.getId(), data);

        // MOTC: Generate backgrounds for visitors when they are created
        if (data instanceof IExtendedCitizenData extData) {
            CitizenBackground existing = extData.getCitizenBackground();

            // Only generate if no background exists
            if (existing == null || !existing.isInitialized()) {
                String gameId = CitizenNpcManager.getInstance().getGameId();

                if (gameId != null) {
                    MOTC_LOGGER.info("Generating AI background for new visitor: {}", data.getName());

                    BackgroundGenerationService.getInstance().generateBackground(data, gameId)
                        .thenAccept(background -> {
                            extData.setCitizenBackground(background);
                            MOTC_LOGGER.info("Generated background for visitor {}: backstory='{}...', traits={}",
                                data.getName(),
                                background.getBackstory() != null
                                    ? background.getBackstory().substring(0, Math.min(50, background.getBackstory().length()))
                                    : "none",
                                background.getTraits());
                        })
                        .exceptionally(ex -> {
                            MOTC_LOGGER.error("Failed to generate background for visitor {}: {}",
                                data.getName(), ex.getMessage());
                            return null;
                        });
                } else {
                    MOTC_LOGGER.warn("Cannot generate background for visitor {} - NPC system not initialized",
                        data.getName());
                }
            }
        }

        return data;
    }

    @Override
    public void removeCivilian(@NotNull final ICivilianData citizen)
    {
        final IVisitorData data = visitorMap.remove(citizen.getId());
        if (data != null && data.getEntity().isPresent())
        {
            data.getEntity().get().remove(Entity.RemovalReason.DISCARDED);
        }
    }

    @Override
    public void markDirty()
    {
        this.isDirty = true;
    }

    @Override
    public void clearDirty()
    {
        this.isDirty = false;
    }

    @Override
    public void onColonyTick(final IColony colony)
    {
        if (colony.getServerBuildingManager().hasTownHall())
        {
            for (final IVisitorData data : visitorMap.values())
            {
                data.updateEntityIfNecessary();
            }
        }
    }
}
