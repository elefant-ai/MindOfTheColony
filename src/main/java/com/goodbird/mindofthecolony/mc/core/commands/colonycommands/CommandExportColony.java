package com.goodbird.mindofthecolony.mc.core.commands.colonycommands;

import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.core.commands.arguments.ColonyIdArgument;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCCommand;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCOPCommand;
import com.goodbird.mindofthecolony.mc.core.util.BackUpHelper;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static com.goodbird.mindofthecolony.mc.api.util.constant.translation.CommandTranslationConstants.COMMAND_COLONY_EXPORT_SUCCESS;
import static com.goodbird.mindofthecolony.mc.core.commands.CommandArgumentNames.COLONYID_ARG;

/**
 * Command to export a colony from a world save, exports region and backup file.
 */
public class CommandExportColony implements IMCOPCommand
{
    @Override
    public int onExecute(final CommandContext<CommandSourceStack> context)
    {
        final IColony colony = ColonyIdArgument.getColony(context, COLONYID_ARG);
        BackUpHelper.backupColonyData(context.getSource().getLevel().registryAccess());

        final String filename = BackUpHelper.exportColony(colony);
        context.getSource().sendSuccess(() -> Component.translatable(COMMAND_COLONY_EXPORT_SUCCESS, filename), true);
        return 1;
    }

    /**
     * Name string of the command.
     */
    @Override
    public String getName()
    {
        return "export";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return IMCCommand.newLiteral(getName())
                 .then(IMCCommand.newArgument(COLONYID_ARG, ColonyIdArgument.id()).executes(this::checkPreConditionAndExecute));
    }
}
