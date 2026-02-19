package com.goodbird.mindofthecolony.mc.core.commands.colonycommands.requestsystem;

import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.util.constant.translation.CommandTranslationConstants;
import com.goodbird.mindofthecolony.mc.core.MineColonies;
import com.goodbird.mindofthecolony.mc.core.commands.arguments.ColonyIdArgument;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static com.goodbird.mindofthecolony.mc.core.commands.CommandArgumentNames.COLONYID_ARG;

public class CommandRSReset implements IMCCommand
{
    /**
     * What happens when the command is executed after preConditions are successful.
     *
     * @param context the context of the command execution
     */
    @Override
    public int onExecute(final CommandContext<CommandSourceStack> context)
    {
        final IColony colony = ColonyIdArgument.getColony(context, COLONYID_ARG);

        if (!context.getSource().hasPermission(OP_PERM_LEVEL) && !MineColonies.getConfig().getServer().canPlayerUseResetCommand.get())
        {
            context.getSource().sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_DISABLED_IN_CONFIG), true);
            return 0;
        }

        colony.getRequestManager().reset();
        context.getSource().sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_REQUEST_SYSTEM_RESET_SUCCESS, colony.getName()), true);

        return 1;
    }

    /**
     * Name string of the command.
     */
    @Override
    public String getName()
    {
        return "requestsystem-reset";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return IMCCommand.newLiteral(getName())
                 .then(IMCCommand.newArgument(COLONYID_ARG, ColonyIdArgument.id()).executes(this::checkPreConditionAndExecute));
    }
}
