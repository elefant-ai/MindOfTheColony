package com.goodbird.mindofthecolony.command;

import com.goodbird.mindofthecolony.config.ModSettings;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class MotcCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("motc")
                .then(Commands.literal("language")
                    .then(Commands.argument("lang", StringArgumentType.word())
                        .executes(ctx -> {
                            String lang = StringArgumentType.getString(ctx, "lang");
                            ModSettings.NPC_LANGUAGE.set(lang);
                            ModSettings.SPEC.save();
                            ctx.getSource().sendSuccess(
                                () -> Component.literal("NPC language set to: " + lang),
                                true
                            );
                            return 1;
                        })
                    )
                    .executes(ctx -> {
                        String current = ModSettings.NPC_LANGUAGE.get();
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("Current NPC language: " + current),
                            false
                        );
                        return 1;
                    })
                )
        );
    }
}
