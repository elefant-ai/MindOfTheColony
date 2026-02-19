package com.goodbird.mindofthecolony.mc.core.network.messages.client.colony;

import com.ldtteam.common.network.AbstractClientPlayMessage;
import com.ldtteam.common.network.PlayMessageType;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.colony.IColonyManager;
import com.goodbird.mindofthecolony.mc.api.colony.IColonyView;
import com.goodbird.mindofthecolony.mc.api.research.IResearchManager;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Message to synch research manager to colony.
 */
public class ColonyViewResearchManagerViewMessage extends AbstractClientPlayMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forClient(Constants.MOD_ID, "colony_view_research_manager_view", ColonyViewResearchManagerViewMessage::new);

    private final int             colonyId;
    private final CompoundTag researchManagerData;

    /**
     * Dimension of the colony.
     */
    private final ResourceKey<Level> dimension;

    /**
     * Creates a message to send the research manager to the client.
     * @param colony the colony.
     * @param researchManager the research manager.
     */
    public ColonyViewResearchManagerViewMessage(final IColony colony, @NotNull final IResearchManager researchManager)
    {
        super(TYPE);
        this.colonyId = colony.getID();
        this.dimension = colony.getDimension();

        final CompoundTag researchCompound = new CompoundTag();
        researchManager.writeToNBT(colony.getWorld().registryAccess(), researchCompound);
        this.researchManagerData = researchCompound;
    }

    public ColonyViewResearchManagerViewMessage(@NotNull final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        colonyId = buf.readInt();
        dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(buf.readUtf(32767)));
        researchManagerData = buf.readNbt();
    }

    @Override
    protected void toBytes(@NotNull final RegistryFriendlyByteBuf buf)
    {
        buf.writeInt(colonyId);
        buf.writeUtf(dimension.location().toString());
        buf.writeNbt(researchManagerData);
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final Player player)
    {
        final IColonyView colonyView = IColonyManager.getInstance().getColonyView(colonyId, dimension);
        if (colonyView != null)
        {
            colonyView.handleColonyViewResearchManagerUpdate(player.level().registryAccess(), researchManagerData);
        }
    }
}
