package com.goodbird.mindofthecolony.mc.core.colony.events.raid.pirateEvent;

import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.colony.colonyEvents.EventStatus;
import com.goodbird.mindofthecolony.mc.api.entity.mobs.AbstractEntityMinecoloniesRaider;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import com.goodbird.mindofthecolony.mc.core.colony.events.raid.HordeRaidEvent;
import com.goodbird.mindofthecolony.mc.core.entity.mobs.raider.pirates.EntityArcherPirateRaider;
import com.goodbird.mindofthecolony.mc.core.entity.mobs.raider.pirates.EntityCaptainPirateRaider;
import com.goodbird.mindofthecolony.mc.core.entity.mobs.raider.pirates.EntityPirateRaider;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import static com.goodbird.mindofthecolony.mc.api.entity.ModEntities.*;
import static com.goodbird.mindofthecolony.mc.api.util.constant.TranslationConstants.RAID_PIRATE;

/**
 * The Pirate raid event, spawns the worst pirates you've ever heard of.
 */
public class PirateGroundRaidEvent extends HordeRaidEvent
{
    /**
     * This raids event id, registry entries use res locations as ids.
     */
    public static final ResourceLocation PIRATE_GROUND_RAID_EVENT_TYPE_ID = new ResourceLocation(Constants.MOD_ID, "pirate_ground_raid");

    public PirateGroundRaidEvent(IColony colony)
    {
        super(colony);
    }

    @Override
    public ResourceLocation getEventTypeID()
    {
        return PIRATE_GROUND_RAID_EVENT_TYPE_ID;
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    protected void updateRaidBar()
    {
        super.updateRaidBar();
        raidBar.setDarkenScreen(true);
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
    }

    @Override
    public void registerEntity(final Entity entity)
    {
        if (!(entity instanceof AbstractEntityMinecoloniesRaider) || !entity.isAlive())
        {
            entity.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        if (entity instanceof EntityCaptainPirateRaider && boss.keySet().size() < horde.numberOfBosses)
        {
            boss.put(entity, entity.getUUID());
            return;
        }

        if (entity instanceof EntityArcherPirateRaider && archers.keySet().size() < horde.numberOfArchers)
        {
            archers.put(entity, entity.getUUID());
            return;
        }

        if (entity instanceof EntityPirateRaider && normal.keySet().size() < horde.numberOfRaiders)
        {
            normal.put(entity, entity.getUUID());
            return;
        }

        entity.remove(Entity.RemovalReason.DISCARDED);
    }

    @Override
    public void onEntityDeath(final LivingEntity entity)
    {
        super.onEntityDeath(entity);
        if (!(entity instanceof AbstractEntityMinecoloniesRaider))
        {
            return;
        }

        if (entity instanceof EntityCaptainPirateRaider)
        {
            boss.remove(entity);
            horde.numberOfBosses--;
        }

        if (entity instanceof EntityArcherPirateRaider)
        {
            archers.remove(entity);
            horde.numberOfArchers--;
        }

        if (entity instanceof EntityPirateRaider)
        {
            normal.remove(entity);
            horde.numberOfRaiders--;
        }

        horde.hordeSize--;

        if (horde.hordeSize == 0)
        {
            status = EventStatus.DONE;
        }

        sendHordeMessage();
    }

    /**
     * Loads the event from the nbt compound.
     *
     * @param colony   colony to load into
     * @param compound NBTcompound with saved values
     * @return the raid event.
     */
    public static PirateGroundRaidEvent loadFromNBT(final IColony colony, final CompoundTag compound, @NotNull final HolderLookup.Provider provider)
    {
        PirateGroundRaidEvent event = new PirateGroundRaidEvent(colony);
        event.deserializeNBT(provider, compound);
        return event;
    }

    @Override
    public EntityType<?> getNormalRaiderType()
    {
        return PIRATE;
    }

    @Override
    public EntityType<?> getArcherRaiderType()
    {
        return ARCHERPIRATE;
    }

    @Override
    public EntityType<?> getBossRaiderType()
    {
        return CHIEFPIRATE;
    }

    @Override
    protected MutableComponent getDisplayName()
    {
        return Component.translatableEscape(RAID_PIRATE);
    }
}
