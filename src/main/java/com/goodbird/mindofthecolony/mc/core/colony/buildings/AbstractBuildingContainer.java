package com.goodbird.mindofthecolony.mc.core.colony.buildings;

import com.ldtteam.structurize.storage.StructurePacks;
import com.goodbird.mindofthecolony.mc.api.blocks.AbstractBlockHut;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuildingContainer;
import com.goodbird.mindofthecolony.mc.api.tileentities.AbstractTileEntityColonyBuilding;
import com.goodbird.mindofthecolony.mc.api.util.NBTUtils;
import com.goodbird.mindofthecolony.mc.core.tileentities.TileEntityColonyBuilding;
import com.goodbird.mindofthecolony.mc.core.tileentities.TileEntityRack;
import com.goodbird.mindofthecolony.mc.core.blocks.BlockMinecoloniesRack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

import static com.goodbird.mindofthecolony.mc.api.colony.requestsystem.requestable.deliveryman.AbstractDeliverymanRequestable.getMaxBuildingPriority;
import static com.goodbird.mindofthecolony.mc.api.util.constant.NbtTagConstants.*;

/**
 * Class containing the container action of the buildings.
 */
public abstract class AbstractBuildingContainer extends AbstractSchematicProvider implements IBuildingContainer
{
    /**
     * A list which contains the position of all containers which belong to the worker building.
     */
    protected final Set<BlockPos> containerList = new HashSet<>();

    /**
     * List of items the worker should keep. With the quantity and if he should keep it in the inventory as well.
     */
    protected final Map<Predicate<ItemStack>, Tuple<Integer, Boolean>> keepX = new HashMap<>();

    /**
     * The tileEntity of the building.
     */
    protected AbstractTileEntityColonyBuilding tileEntity;

    /**
     * Priority of the building in the pickUpList. This is the unscaled value (mainly for a more intuitive GUI).
     */
    private int unscaledPickUpPriority = 5;

    /**
     * The constructor for the building container.
     *
     * @param pos    the position of it.
     * @param colony the colony.
     */
    public AbstractBuildingContainer(final BlockPos pos, final IColony colony)
    {
        super(pos, colony);
    }

    @Override
    public void deserializeNBT(@NotNull final HolderLookup.Provider provider, final CompoundTag compound)
    {
        super.deserializeNBT(provider, compound);

        final ListTag containerTagList = compound.getList(TAG_CONTAINERS, Tag.TAG_INT_ARRAY);
        for (int i = 0; i < containerTagList.size(); ++i)
        {
            containerList.add(NBTUtils.readBlockPos(containerTagList.get(i)));
        }
        if (compound.contains(TAG_PRIO))
        {
            this.unscaledPickUpPriority = compound.getInt(TAG_PRIO);
        }
        if (compound.contains(TAG_PRIO_STATE))
        {
            // This was the old int representation of Pickup:Never
            if (compound.getInt(TAG_PRIO_STATE) == 0)
            {
                this.unscaledPickUpPriority = 0;
            }
        }
    }

    @Override
    public CompoundTag serializeNBT(@NotNull final HolderLookup.Provider provider)
    {
        final CompoundTag compound = super.serializeNBT(provider);

        @NotNull final ListTag containerTagList = new ListTag();
        for (@NotNull final BlockPos pos : containerList)
        {
            containerTagList.add(NBTUtils.writeBlockPos(pos));
        }
        compound.put(TAG_CONTAINERS, containerTagList);
        compound.putInt(TAG_PRIO, this.unscaledPickUpPriority);

        return compound;
    }

    @Override
    public int getPickUpPriority()
    {
        return this.unscaledPickUpPriority;
    }

    @Override
    public void alterPickUpPriority(final int value)
    {
        this.unscaledPickUpPriority = Mth.clamp(this.unscaledPickUpPriority + value, 0, getMaxBuildingPriority(false));
    }

    @Override
    public void addContainerPosition(@NotNull final BlockPos pos)
    {
        containerList.add(pos);
    }

    @Override
    public void removeContainerPosition(final BlockPos pos)
    {
        containerList.remove(pos);
    }

    @Override
    public List<BlockPos> getContainers()
    {
        final List<BlockPos> list = new ArrayList<>(containerList);;
        list.add(this.getPosition());
        return list;
    }

    @Override
    public void registerBlockPosition(@NotNull final BlockState blockState, @NotNull final BlockPos pos, @NotNull final Level world)
    {
        registerBlockPosition(blockState.getBlock(), pos, world);
    }

    @Override
    @SuppressWarnings("squid:S1172")
    public void registerBlockPosition(@NotNull final Block block, @NotNull final BlockPos pos, @NotNull final Level world)
    {
        if (block instanceof AbstractBlockHut)
        {
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof TileEntityColonyBuilding buildingEntity)
            {
                buildingEntity.setStructurePack(StructurePacks.getStructurePack(getStructurePack()));
                final IBuilding building = colony.getServerBuildingManager().getBuilding(pos);
                if (building != null)
                {
                    building.setStructurePack(getStructurePack());
                    building.setParent(getID());
                }
            }
        }
        else if (block instanceof BlockMinecoloniesRack)
        {
            addContainerPosition(pos);
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof TileEntityRack rackEntity)
            {
                rackEntity.setBuildingPos(this.getID());
            }
        }
    }

    /**
     * Gets the list of tags, and finds the first location registered there. 
     * @param tagName the name of the tag to query
     * @return the BlockPos, or null if not found
     */
    @Nullable
    protected BlockPos getFirstLocationFromTag(@NotNull final String tagName)
    {
        final List<BlockPos> locations = getLocationsFromTag(tagName);
        return locations.isEmpty() ? null : locations.get(0);
    }

    @Override
    public List<BlockPos> getLocationsFromTag(@NotNull final String tagName)
    {
        if (getTileEntity() != null)
        {
            return getTileEntity().getCachedWorldTagNamePosMap().getOrDefault(tagName, Collections.emptyList());
        }
        return Collections.emptyList();
    }

    @Override
    public void setTileEntity(final AbstractTileEntityColonyBuilding te)
    {
        tileEntity = te;
        if (te != null && te.isOutdated())
        {
            safeUpdateTEDataFromSchematic();
        }
    }

    //------------------------- !Start! Capabilities handling for minecolonies buildings -------------------------//

    @Override
    public @Nullable IItemHandler getItemHandlerCap(Direction direction)
    {
        return getTileEntity().getItemHandlerCap(direction);
    }

    //------------------------- !End! Capabilities handling for minecolonies buildings -------------------------//
}
