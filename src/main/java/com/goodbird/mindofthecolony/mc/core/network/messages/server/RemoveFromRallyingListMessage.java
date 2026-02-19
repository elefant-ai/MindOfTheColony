package com.goodbird.mindofthecolony.mc.core.network.messages.server;

import com.ldtteam.common.network.AbstractServerPlayMessage;
import com.ldtteam.common.network.PlayMessageType;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.StandardFactoryController;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.location.ILocation;
import com.goodbird.mindofthecolony.mc.api.util.InventoryUtils;
import com.goodbird.mindofthecolony.mc.api.util.ItemStackUtils;
import com.goodbird.mindofthecolony.mc.api.util.MessageUtils;
import com.goodbird.mindofthecolony.mc.api.util.Utils;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.goodbird.mindofthecolony.mc.api.util.constant.TranslationConstants.COM_MINECOLONIES_BANNER_RALLY_GUARDS_GUI_ERROR;
import static com.goodbird.mindofthecolony.mc.core.items.ItemBannerRallyGuards.removeGuardTowerAtLocation;

/**
 * Removes a guard tower from the rallying list
 */
public class RemoveFromRallyingListMessage extends AbstractServerPlayMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "remove_from_rallying_list", RemoveFromRallyingListMessage::new);

    /**
     * The banner to be modified.
     */
    private final ItemStack banner;

    /**
     * The position of the guard tower that should be removed.
     */
    private final BlockPos location;

    /**
     * Remove the guard tower from the rallying list
     *
     * @param banner   The banner to be modified.
     * @param pos The position of the guard tower
     */
    public RemoveFromRallyingListMessage(final ItemStack banner, final BlockPos pos)
    {
        super(TYPE);
        this.banner = banner;
        this.location = pos;
    }

    protected RemoveFromRallyingListMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        banner = Utils.deserializeCodecMess(buf);
        location = buf.readBlockPos();
    }

    @Override
    protected void toBytes(@NotNull final RegistryFriendlyByteBuf buf)
    {
        Utils.serializeCodecMess(buf, banner);
        buf.writeBlockPos(location);
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player)
    {
        final int slot = InventoryUtils.findFirstSlotInItemHandlerWith(new InvWrapper(player.getInventory()),
          (itemStack -> ItemStackUtils.compareItemStacksIgnoreStackSize(itemStack, banner)));

        if (slot == -1)
        {
            MessageUtils.format(COM_MINECOLONIES_BANNER_RALLY_GUARDS_GUI_ERROR).sendTo(player);
            return;
        }

        removeGuardTowerAtLocation(player.getInventory().getItem(slot), location);
    }
}
