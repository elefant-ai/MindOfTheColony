package com.goodbird.mindofthecolony.command;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.goodbird.mindofthecolony.background.BackgroundGenerationService;
import com.goodbird.mindofthecolony.bridge.CitizenNpcBridge;
import com.goodbird.mindofthecolony.config.ModSettings;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MotcCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MotcCommand.class);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("motc")
                .requires(source -> source.hasPermission(2))
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
                .then(Commands.literal("regenbackground")
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                        .then(Commands.argument("citizenId", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                int colonyId = IntegerArgumentType.getInteger(ctx, "colonyId");
                                int citizenId = IntegerArgumentType.getInteger(ctx, "citizenId");
                                return regenBackground(ctx.getSource().getPlayer(), colonyId, citizenId);
                            })
                        )
                    )
                    .executes(ctx -> {
                        ctx.getSource().sendFailure(Component.literal(
                            "Usage: /motc regenbackground <colonyId> <citizenId>"));
                        return 0;
                    })
                )
                .then(Commands.literal("regenall")
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int colonyId = IntegerArgumentType.getInteger(ctx, "colonyId");
                            return regenAllBackgrounds(ctx.getSource().getPlayer(), colonyId);
                        })
                    )
                    .executes(ctx -> {
                        ctx.getSource().sendFailure(Component.literal(
                            "Usage: /motc regenall <colonyId>"));
                        return 0;
                    })
                )
                .then(Commands.literal("regenvisitors")
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int colonyId = IntegerArgumentType.getInteger(ctx, "colonyId");
                            return regenAllVisitorBackgrounds(ctx.getSource().getPlayer(), colonyId);
                        })
                    )
                    .executes(ctx -> {
                        ctx.getSource().sendFailure(Component.literal(
                            "Usage: /motc regenvisitors <colonyId>"));
                        return 0;
                    })
                )
        );
    }

    private static int regenBackground(ServerPlayer player, int colonyId, int citizenId) {
        if (player == null) {
            return 0;
        }

        IColony colony = IColonyManager.getInstance().getColonyByDimension(colonyId, player.level().dimension());
        if (colony == null) {
            player.sendSystemMessage(Component.literal("Colony not found: " + colonyId));
            return 0;
        }

        ICitizenData citizenData = colony.getCitizenManager().getCivilian(citizenId);
        if (citizenData == null) {
            player.sendSystemMessage(Component.literal("Citizen not found: " + citizenId));
            return 0;
        }

        if (!(citizenData instanceof IExtendedCitizenData extData)) {
            player.sendSystemMessage(Component.literal("Citizen data not extended (mixin issue)"));
            return 0;
        }

        String gameId = CitizenNpcManager.getInstance().getGameId();
        if (gameId == null) {
            player.sendSystemMessage(Component.literal("NPC system not initialized"));
            return 0;
        }

        player.sendSystemMessage(Component.literal("Regenerating background for " + citizenData.getName() + "..."));

        // Generate new background using AI
        BackgroundGenerationService.getInstance().generateBackground(citizenData, gameId)
            .thenAccept(background -> {
                // Clear old traits and set new background
                extData.setCitizenBackground(background);

                LOGGER.info("Regenerated background for citizen {}: backstory='{}', traits={}",
                    citizenData.getName(),
                    background.getBackstory() != null
                        ? background.getBackstory().substring(0, Math.min(80, background.getBackstory().length()))
                        : "none",
                    background.getTraits());

                // Respawn the NPC with new background
                CitizenNpcBridge bridge = CitizenNpcManager.getInstance().getBridge(citizenId);
                if (bridge != null) {
                    bridge.respawn().thenAccept(npcId -> {
                        player.sendSystemMessage(Component.literal(
                            "Background regenerated for " + citizenData.getName() +
                            "\nBackstory: " + (background.getBackstory() != null ? background.getBackstory() : "none") +
                            "\nTraits: " + background.getTraits()));
                    });
                } else {
                    player.sendSystemMessage(Component.literal(
                        "Background regenerated for " + citizenData.getName() +
                        " (NPC not active)\nBackstory: " + background.getBackstory() +
                        "\nTraits: " + background.getTraits()));
                }
            })
            .exceptionally(ex -> {
                LOGGER.error("Failed to regenerate background for {}", citizenData.getName(), ex);
                player.sendSystemMessage(Component.literal(
                    "Failed to regenerate background: " + ex.getMessage()));
                return null;
            });

        return 1;
    }

    private static int regenAllBackgrounds(ServerPlayer player, int colonyId) {
        if (player == null) {
            return 0;
        }

        IColony colony = IColonyManager.getInstance().getColonyByDimension(colonyId, player.level().dimension());
        if (colony == null) {
            player.sendSystemMessage(Component.literal("Colony not found: " + colonyId));
            return 0;
        }

        String gameId = CitizenNpcManager.getInstance().getGameId();
        if (gameId == null) {
            player.sendSystemMessage(Component.literal("NPC system not initialized"));
            return 0;
        }

        int count = 0;
        for (ICitizenData citizenData : colony.getCitizenManager().getCitizens()) {
            if (citizenData instanceof IExtendedCitizenData) {
                regenBackground(player, colonyId, citizenData.getId());
                count++;
            }
        }

        player.sendSystemMessage(Component.literal(
            "Started background regeneration for " + count + " citizens in colony " + colonyId));

        return count;
    }

    private static int regenAllVisitorBackgrounds(ServerPlayer player, int colonyId) {
        if (player == null) {
            return 0;
        }

        IColony colony = IColonyManager.getInstance().getColonyByDimension(colonyId, player.level().dimension());
        if (colony == null) {
            player.sendSystemMessage(Component.literal("Colony not found: " + colonyId));
            return 0;
        }

        String gameId = CitizenNpcManager.getInstance().getGameId();
        if (gameId == null) {
            player.sendSystemMessage(Component.literal("NPC system not initialized"));
            return 0;
        }

        int count = 0;
        for (ICivilianData visitorData : colony.getVisitorManager().getCivilianDataMap().values()) {
            if (visitorData instanceof IExtendedCitizenData extData) {
                player.sendSystemMessage(Component.literal("Regenerating background for visitor: " + visitorData.getName() + "..."));

                BackgroundGenerationService.getInstance().generateBackground(visitorData, gameId)
                    .thenAccept(background -> {
                        extData.setCitizenBackground(background);

                        LOGGER.info("Regenerated background for visitor {}: backstory='{}', traits={}",
                            visitorData.getName(),
                            background.getBackstory() != null
                                ? background.getBackstory().substring(0, Math.min(80, background.getBackstory().length()))
                                : "none",
                            background.getTraits());

                        player.sendSystemMessage(Component.literal(
                            "Background regenerated for visitor " + visitorData.getName() +
                            "\nTraits: " + background.getTraits()));
                    })
                    .exceptionally(ex -> {
                        LOGGER.error("Failed to regenerate background for visitor {}", visitorData.getName(), ex);
                        player.sendSystemMessage(Component.literal(
                            "Failed to regenerate background for " + visitorData.getName() + ": " + ex.getMessage()));
                        return null;
                    });
                count++;
            }
        }

        player.sendSystemMessage(Component.literal(
            "Started background regeneration for " + count + " visitors in colony " + colonyId));

        return count;
    }
}
