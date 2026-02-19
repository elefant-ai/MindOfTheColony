package com.goodbird.mindofthecolony.mc.core.commands.killcommands;

import com.goodbird.mindofthecolony.mc.api.colony.colonyEvents.EventStatus;
import com.goodbird.mindofthecolony.mc.api.colony.colonyEvents.IColonyEvent;
import com.goodbird.mindofthecolony.mc.api.entity.mobs.AbstractEntityMinecoloniesRaider;
import com.goodbird.mindofthecolony.mc.api.util.DamageSourceKeys;
import com.goodbird.mindofthecolony.mc.core.commands.commandTypes.IMCOPCommand;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;

public class CommandKillRaider implements IMCOPCommand
{
    private int entitiesKilled = 0;

    /**
     * What happens when the command is executed
     *
     * @param context the context of the command execution
     */
    @Override
    public int onExecute(final CommandContext<CommandSourceStack> context)
    {
        entitiesKilled = 0;

        context.getSource().getLevel().getEntities(EntityTypeTest.forClass(AbstractEntityMinecoloniesRaider.class), (e) -> true).forEach(entity ->
        {
            if (entity != null)
            {
                final AbstractEntityMinecoloniesRaider mob = (AbstractEntityMinecoloniesRaider) entity;
                mob.die(context.getSource().getLevel().damageSources().source(DamageSourceKeys.CONSOLE));
                mob.remove(Entity.RemovalReason.DISCARDED);

                final IColonyEvent event = mob.getColony().getEventManager().getEventByID(mob.getEventID());

                if (event != null)
                {
                    event.setStatus(EventStatus.DONE);
                }

                entitiesKilled++;
            }
        });
        context.getSource().sendSuccess(() -> Component.literal(entitiesKilled + " entities killed"), true);
        return 1;
    }

    /**
     * Name string of the command.
     */
    @Override
    public String getName()
    {
        return "raider";
    }
}
