package com.goodbird.mindofthecolony.mc.core.commands.citizencommands;

import com.goodbird.mindofthecolony.mc.api.IMinecoloniesAPI;
import com.goodbird.mindofthecolony.mc.api.colony.ICitizenData;
import com.goodbird.mindofthecolony.mc.api.colony.IColony;
import com.goodbird.mindofthecolony.mc.api.eventbus.events.colony.citizens.CitizenAddedModEvent;
import com.goodbird.mindofthecolony.mc.core.commands.arguments.ColonyIdArgument;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCCommand;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCOPCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static com.goodbird.mindofthecolony.mc.api.util.constant.translation.CommandTranslationConstants.COMMAND_CITIZEN_SPAWN_SUCCESS;
import static com.goodbird.mindofthecolony.mc.core.commands.CommandArgumentNames.COLONYID_ARG;

/**
 * Force-spawns a new citizen in the given colony.
 */
public class CommandCitizenSpawnNew implements IMCOPCommand
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
        final ICitizenData newCitizen = colony.getCitizenManager().spawnOrCreateCivilian(null, colony.getWorld(), null, true);
        context.getSource().sendSuccess(() -> Component.translatableEscape(COMMAND_CITIZEN_SPAWN_SUCCESS, newCitizen.getName()), true);

        IMinecoloniesAPI.getInstance().getEventBus().post(new CitizenAddedModEvent(newCitizen, CitizenAddedModEvent.CitizenAddedSource.COMMANDS));
        return 1;
    }

    /**
     * Name string of the command.
     */
    @Override
    public String getName()
    {
        return "spawnNew";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return IMCCommand.newLiteral(getName())
                 .then(IMCCommand.newArgument(COLONYID_ARG, ColonyIdArgument.id()).executes(this::checkPreConditionAndExecute));
    }
}
