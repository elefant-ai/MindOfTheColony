package com.goodbird.mindofthecolony.mc.core.commands.colonycommands;

import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.colony.permissions.Rank;
import com.goodbird.mindofthecolony.mc.api.util.constant.translation.CommandTranslationConstants;
import com.goodbird.mindofthecolony.mc.core.commands.arguments.ColonyIdArgument;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCCommand;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCOPCommand;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;

import java.util.Locale;

import static com.goodbird.mindofthecolony.mc.core.commands.CommandArgumentNames.COLONYID_ARG;
import static com.goodbird.mindofthecolony.mc.core.commands.CommandArgumentNames.PLAYERNAME_ARG;

public class CommandSetRank implements IMCOPCommand
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
            context.getSource().sendSuccess(() -> Component.translatable(CommandTranslationConstants.COMMAND_PLAYER_NOT_FOUND, profile.getName()), true);
            return 0;
        }

        String rankName = StringArgumentType.getString(context, "rank");
        if (rankName != null && rankName.contains(" "))
        {
            rankName = rankName.split(" ")[1];
        }

        Rank rankFound = null;
        for (final Rank rank : colony.getPermissions().getRanks().values())
        {
            if (rankName != null && !rankName.isEmpty() && rankName.toLowerCase(Locale.US).equals(rank.getName().toLowerCase(Locale.US)))
            {
                rankFound = rank;
                break;
            }
        }

        if (rankFound == null)
        {
            context.getSource().sendSuccess(() -> Component.literal("Rank does not exist"), true);
            return 0;
        }

        colony.getPermissions().setPlayerRank(profile.getId(), rankFound, colony.getWorld());
        final String finalRankName = rankName;
        context.getSource().sendSuccess(() -> Component.literal("Set player: " + profile.getName() + " to rank:" + finalRankName), true);
        return 1;
    }

    /**
     * Name string of the command.
     */
    @Override
    public String getName()
    {
        return "setRank";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return IMCCommand.newLiteral(getName())
          .then(IMCCommand.newArgument(COLONYID_ARG, ColonyIdArgument.id())
            .then(IMCCommand.newArgument(PLAYERNAME_ARG, GameProfileArgument.gameProfile())
              .then(IMCCommand.newArgument("rank", StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    final IColony colony = ColonyIdArgument.getColony(context, COLONYID_ARG);

                    for (final Rank rank : colony.getPermissions().getRanks().values())
                    {
                        builder.suggest(rank.getId() + " " + rank.getName());
                    }

                    return builder.buildFuture();
                })
                .executes(this::checkPreConditionAndExecute))));
    }
}
