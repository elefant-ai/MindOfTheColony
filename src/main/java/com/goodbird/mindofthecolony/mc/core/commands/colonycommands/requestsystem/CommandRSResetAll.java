package com.goodbird.mindofthecolony.mc.core.commands.colonycommands.requestsystem;

import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.colony.IColonyManager;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCOPCommand;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static com.goodbird.mindofthecolony.mc.api.util.constant.translation.CommandTranslationConstants.COMMAND_REQUEST_SYSTEM_RESET_ALL_SUCCESS;

public class CommandRSResetAll implements IMCOPCommand
{
    /**
     * What happens when the command is executed after preConditions are successful.
     *
     * @param context the context of the command execution
     */
    @Override
    public int onExecute(final CommandContext<CommandSourceStack> context)
    {
        for (final IColony colony : IColonyManager.getInstance().getAllColonies())
        {
            colony.getRequestManager().reset();
        }
        context.getSource().sendSuccess(() -> Component.translatableEscape(COMMAND_REQUEST_SYSTEM_RESET_ALL_SUCCESS), true);

        return 1;
    }

    /**
     * Name string of the command.
     */
    @Override
    public String getName()
    {
        return "requestsystem-reset-all";
    }
}
