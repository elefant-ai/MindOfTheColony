package com.goodbird.mindofthecolony.mc.core.commands.citizencommands;

import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.entity.citizen.AbstractEntityCitizen;
import com.goodbird.mindofthecolony.mc.api.util.DamageSourceKeys;
import com.goodbird.mindofthecolony.mc.api.util.constant.translation.CommandTranslationConstants;
import com.goodbird.mindofthecolony.mc.core.MineColonies;
import com.goodbird.mindofthecolony.mc.core.commands.arguments.ColonyIdArgument;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCColonyOfficerCommand;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Optional;

import static com.goodbird.mindofthecolony.mc.core.commands.CommandArgumentNames.CITIZENID_ARG;
import static com.goodbird.mindofthecolony.mc.core.commands.CommandArgumentNames.COLONYID_ARG;

/**
 * Kills the given citizen.
 */
public class CommandCitizenKill implements IMCColonyOfficerCommand
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

        if (!context.getSource().hasPermission(OP_PERM_LEVEL) && !MineColonies.getConfig().getServer().canPlayerUseKillCitizensCommand.get())
        {
            context.getSource().sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_DISABLED_IN_CONFIG), true);
            return 0;
        }

        final ICitizenData citizenData = colony.getCitizenManager().getCivilian(IntegerArgumentType.getInteger(context, CITIZENID_ARG));

        if (citizenData == null)
        {
            context.getSource().sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_CITIZEN_NOT_FOUND), true);
            return 0;
        }

        final Optional<AbstractEntityCitizen> optionalEntityCitizen = citizenData.getEntity();

        if (!optionalEntityCitizen.isPresent())
        {
            context.getSource().sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_CITIZEN_NOT_LOADED), true);
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_CITIZEN_INFO, citizenData.getId(), citizenData.getName()), true);
        final BlockPos position = optionalEntityCitizen.get().blockPosition();
        context.getSource().sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_CITIZEN_INFO_POSITION, position.getX(), position.getY(), position.getZ()), true);
        context.getSource()
          .sendSuccess(() -> Component.translatableEscape(CommandTranslationConstants.COMMAND_CITIZEN_KILL_SUCCESS, position.getX(), position.getY(), position.getZ()), true);

        optionalEntityCitizen.get().die(context.getSource().getLevel().damageSources().source(DamageSourceKeys.CONSOLE));

        return 1;
    }

    /**
     * Name string of the command.
     */
    @Override
    public String getName()
    {
        return "kill";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return IMCCommand.newLiteral(getName())
                 .then(IMCCommand.newArgument(COLONYID_ARG, ColonyIdArgument.id())
                         .then(IMCCommand.newArgument(CITIZENID_ARG, IntegerArgumentType.integer(1)).executes(this::checkPreConditionAndExecute)));
    }
}
