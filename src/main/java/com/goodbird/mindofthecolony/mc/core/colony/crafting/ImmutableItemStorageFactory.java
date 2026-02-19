package com.goodbird.mindofthecolony.mc.core.colony.crafting;

import com.google.common.reflect.TypeToken;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.StandardFactoryController;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.factory.FactoryVoidInput;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.factory.IFactoryController;
import com.goodbird.mindofthecolony.mc.api.crafting.IImmutableItemStorageFactory;
import com.goodbird.mindofthecolony.mc.api.crafting.ImmutableItemStorage;
import com.goodbird.mindofthecolony.mc.api.crafting.ItemStorage;
import com.goodbird.mindofthecolony.mc.api.util.constant.SerializationIdentifierConstants;
import com.goodbird.mindofthecolony.mc.api.util.constant.TypeConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * Factory implementation taking care of creating new instances, serializing and deserializing ImmutableItemStorage.
 */
public class ImmutableItemStorageFactory implements IImmutableItemStorageFactory
{

    @NotNull
    @Override
    public TypeToken<? extends ImmutableItemStorage> getFactoryOutputType()
    {
        return TypeConstants.IMMUTABLEITEMSTORAGE;
    }

    @NotNull
    @Override
    public TypeToken<? extends FactoryVoidInput> getFactoryInputType()
    {
        return TypeConstants.FACTORYVOIDINPUT;
    }

    @Override
    public short getSerializationId()
    {
        return SerializationIdentifierConstants.IMMUTABLE_ITEM_STORAGE_ID;
    }

    @Override
    public CompoundTag serialize(@NotNull final HolderLookup.Provider provider, IFactoryController controller, ImmutableItemStorage output)
    {
        @NotNull final CompoundTag compound = StandardFactoryController.getInstance().serializeTag(provider, output.copy());

        return compound;
    }

    @Override
    public ImmutableItemStorage deserialize(@NotNull final HolderLookup.Provider provider, IFactoryController controller, CompoundTag nbt) throws Throwable
    {
        final ItemStorage readStorage = StandardFactoryController.getInstance().deserializeTag(provider, nbt);
        return readStorage.toImmutable();
    }

    @Override
    public void serialize(IFactoryController controller, ImmutableItemStorage output, RegistryFriendlyByteBuf packetBuffer)
    {
        StandardFactoryController.getInstance().serialize(packetBuffer, output.copy());
    }

    @Override
    public ImmutableItemStorage deserialize(IFactoryController controller, RegistryFriendlyByteBuf buffer) throws Throwable
    {
        @NotNull final ItemStorage newItem = StandardFactoryController.getInstance().deserialize(buffer);
        return newItem.toImmutable();
    }

    @Override
    public ImmutableItemStorage getNewInstance(ItemStack stack, int size)
    {
        @NotNull final ItemStorage newItem = new ItemStorage(stack);
        newItem.setAmount(size);
        return newItem.toImmutable();
    }
    
}
