package com.goodbird.mindofthecolony.mc.core.commands.citizencommands;

import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.util.constant.translation.CommandTranslationConstants;
import com.goodbird.mindofthecolony.mc.core.commands.arguments.ColonyIdArgument;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCColonyOfficerCommand;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static com.goodbird.mindofthecolony.mc.core.commands.CommandArgumentNames.CITIZENID_ARG;
import static com.goodbird.mindofthecolony.mc.core.commands.CommandArgumentNames.COLONYID_ARG;

/**
 * Reloads a citizen entity from its Citizendata.
 */
public class CommandCitizenReload implements IMCColonyOfficerCommand
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
        final ICitizenData citizenData = colony.getCitizenManager().getCivilian(IntegerArgumentType.getInteger(context, CITIZENID_ARG));

        if (citizenData == null)
        {
            context.getSource().sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_CITIZEN_NOT_FOUND), true);
            return 0;
        }

        citizenData.updateEntityIfNecessary();
        context.getSource().sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_CITIZEN_RELOAD_SUCCESS, citizenData.getId()), true);
        return 1;
    }

    /**
     * Name string of the command.
     */
    @Override
    public String getName()
    {
        return "reload";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return IMCCommand.newLiteral(getName())
                 .then(IMCCommand.newArgument(COLONYID_ARG, ColonyIdArgument.id())
                         .then(IMCCommand.newArgument(CITIZENID_ARG, IntegerArgumentType.integer(1)).executes(this::checkPreConditionAndExecute)));
    }
}
