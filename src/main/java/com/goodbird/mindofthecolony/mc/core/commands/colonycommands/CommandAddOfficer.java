package com.goodbird.mindofthecolony.mc.core.commands.colonycommands;

import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.util.constant.translation.CommandTranslationConstants;
import com.goodbird.mindofthecolony.mc.core.MineColonies;
import com.goodbird.mindofthecolony.mc.core.commands.arguments.ColonyIdArgument;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCColonyOfficerCommand;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCCommand;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;

import static com.goodbird.mindofthecolony.mc.core.commands.CommandArgumentNames.COLONYID_ARG;
import static com.goodbird.mindofthecolony.mc.core.commands.CommandArgumentNames.PLAYERNAME_ARG;

public class CommandAddOfficer implements IMCColonyOfficerCommand
{
    /**
     * What happens when the command is executed after preConditions are successful.
     *
     * @param context the context of the command execution
     */
    @Override
    public int onExecute(final CommandContext<CommandSourceStack> context)
    {
        if (!context.getSource().hasPermission(OP_PERM_LEVEL) && !MineColonies.getConfig().getServer().canPlayerUseAddOfficerCommand.get())
        {
            context.getSource().sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_DISABLED_IN_CONFIG), true);
            return 0;
        }

        final IColony colony = ColonyIdArgument.getColony(context, COLONYID_ARG);

        GameProfile profile;
        try
        {
            profile = GameProfileArgument.getGameProfiles(context, PLAYERNAME_ARG).stream().findFirst().orElse(null);
        }
        catch (CommandSyntaxException e)
        {
            return 0;
        }

        if (context.getSource().getServer().getPlayerList().getPlayer(profile.getId()) == null)
        {
            // could not find player with given name.
            context.getSource().sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_PLAYER_NOT_FOUND, profile.getName()), true);
            return 0;
        }
        colony.getPermissions().addPlayer(profile, colony.getPermissions().getRank(colony.getPermissions().OFFICER_RANK_ID));
        colony.getPackageManager().addImportantColonyPlayer(context.getSource().getServer().getPlayerList().getPlayer(profile.getId()));

        context.getSource().sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_OFFICER_ADD_SUCCESS, profile.getName(), colony.getName()), true);
        return 1;
    }

    /**
     * Name string of the command.
     */
    @Override
    public String getName()
    {
        return "addOfficer";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return IMCCommand.newLiteral(getName())
                 .then(IMCCommand.newArgument(COLONYID_ARG, ColonyIdArgument.id())
                         .then(IMCCommand.newArgument(PLAYERNAME_ARG, GameProfileArgument.gameProfile()).executes(this::checkPreConditionAndExecute)));
    }
}
