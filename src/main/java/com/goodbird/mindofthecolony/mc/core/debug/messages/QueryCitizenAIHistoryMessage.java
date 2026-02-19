package com.goodbird.mindofthecolony.mc.core.debug.messages;

import com.ldtteam.common.network.PlayMessageType;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenDataView;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.util.constant.Constants;
import com.goodbird.mindofthecolony.mc.core.debug.DebugPlayerManager;
import com.goodbird.mindofthecolony.mc.core.entity.citizen.EntityCitizen;
import com.goodbird.mindofthecolony.mc.core.network.messages.server.AbstractColonyServerMessage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Message to query ai history from the server
 */
public class QueryCitizenAIHistoryMessage extends AbstractColonyServerMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "debug_aihistory", QueryCitizenAIHistoryMessage::new);

    /**
     * Citizen id
     */
    private int id;

    public QueryCitizenAIHistoryMessage(final ICitizenDataView citizen)
    {
        super(TYPE, citizen.getColony());
        this.id = citizen.getId();
    }

    protected QueryCitizenAIHistoryMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        this.id = buf.readInt();
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {
        super.toBytes(buf);
        buf.writeInt(id);
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, final IColony colony)
    {
        if (player == null || !DebugPlayerManager.hasDebugEnabled(player))
        {
            return;
        }

        final ICitizenData citizen = colony.getCitizenManager().getCivilian(id);
        if (citizen == null || !citizen.getEntity().isPresent())
        {
            return;
        }

        if (citizen.getEntity().get() instanceof EntityCitizen entityCitizen)
        {
            MutableComponent message = Component.literal("Citizen AI: ").append(entityCitizen.getCitizenAI().getHistory());

            if (entityCitizen.getCitizenJobHandler().getColonyJob() != null)
            {
                message.append(Component.literal("Job AI: ").append(entityCitizen.getCitizenJobHandler().getWorkAI().getStateAI().getHistory()));
            }

            new DebugOutputMessage(message, true).sendToPlayer(player);
        }
    }
}
