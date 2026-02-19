package com.goodbird.mindofthecolony.command;

import com.goodbird.mindofthecolony.CitizenNpcManager;
import com.goodbird.mindofthecolony.background.BackgroundGenerationService;
import com.goodbird.mindofthecolony.background.CitizenBackground;
import com.goodbird.mindofthecolony.background.TraitDefinition;
import com.goodbird.mindofthecolony.background.TraitRegistry;
import com.goodbird.mindofthecolony.bridge.CitizenNpcBridge;
import com.goodbird.mindofthecolony.config.ModSettings;
import com.goodbird.mindofthecolony.config.NpcInteractionConfig;
import com.goodbird.mindofthecolony.effect.TemporaryTrait;
import com.goodbird.mindofthecolony.interaction.CitizenRelationship;
import com.goodbird.mindofthecolony.interaction.NpcConversation;
import com.goodbird.mindofthecolony.interaction.NpcConversationManager;
import game.player2.npc.Player2NpcLib;
import com.goodbird.mindofthecolony.mixin.IExtendedCitizenData;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MotcCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MotcCommand.class);

    // Suggestion provider for colony IDs
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_COLONIES = (context, builder) -> {
        if (context.getSource().getPlayer() != null) {
            ServerPlayer player = context.getSource().getPlayer();
            IColonyManager.getInstance().getColonies(player.level()).forEach(colony ->
                builder.suggest(colony.getID(), Component.literal(colony.getName())));
        }
        return builder.buildFuture();
    };

    // Suggestion provider for citizen IDs (depends on colonyId argument)
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CITIZENS = (context, builder) -> {
        if (context.getSource().getPlayer() != null) {
            try {
                int colonyId = IntegerArgumentType.getInteger(context, "colonyId");
                ServerPlayer player = context.getSource().getPlayer();
                IColony colony = IColonyManager.getInstance().getColonyByDimension(colonyId, player.level().dimension());
                if (colony != null) {
                    // Add citizens
                    colony.getCitizenManager().getCitizens().forEach(citizen ->
                        builder.suggest(citizen.getId(), Component.literal(citizen.getName())));
                    // Add visitors
                    colony.getVisitorManager().getCivilianDataMap().values().forEach(visitor ->
                        builder.suggest(visitor.getId(), Component.literal(visitor.getName())));
                }
            } catch (IllegalArgumentException ignored) {
                // colonyId not yet specified
            }
        }
        return builder.buildFuture();
    };

    // Suggestion provider for trait IDs
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TRAITS = (context, builder) -> {
        TraitRegistry.getAllTraits().forEach(trait ->
            builder.suggest(trait.id(), Component.literal(trait.displayText().substring(0, Math.min(50, trait.displayText().length())))));
        return builder.buildFuture();
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("motc")
                .then(Commands.literal("language")
                    .requires(source -> source.hasPermission(2))
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
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                        .suggests(SUGGEST_COLONIES)
                        .then(Commands.argument("citizenId", IntegerArgumentType.integer(1))
                            .suggests(SUGGEST_CITIZENS)
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
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                        .suggests(SUGGEST_COLONIES)
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
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                        .suggests(SUGGEST_COLONIES)
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
                .then(Commands.literal("addtrait")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                        .suggests(SUGGEST_COLONIES)
                        .then(Commands.argument("citizenId", IntegerArgumentType.integer())
                            .suggests(SUGGEST_CITIZENS)
                            .then(Commands.argument("traitId", StringArgumentType.word())
                                .suggests(SUGGEST_TRAITS)
                                // With duration (temporary trait)
                                .then(Commands.argument("durationSeconds", IntegerArgumentType.integer(1))
                                    .executes(ctx -> {
                                        int colonyId = IntegerArgumentType.getInteger(ctx, "colonyId");
                                        int citizenId = IntegerArgumentType.getInteger(ctx, "citizenId");
                                        String traitId = StringArgumentType.getString(ctx, "traitId");
                                        int durationSeconds = IntegerArgumentType.getInteger(ctx, "durationSeconds");
                                        return addTrait(ctx.getSource().getPlayer(), colonyId, citizenId, traitId, durationSeconds);
                                    })
                                )
                                // Without duration (permanent trait)
                                .executes(ctx -> {
                                    int colonyId = IntegerArgumentType.getInteger(ctx, "colonyId");
                                    int citizenId = IntegerArgumentType.getInteger(ctx, "citizenId");
                                    String traitId = StringArgumentType.getString(ctx, "traitId");
                                    return addTrait(ctx.getSource().getPlayer(), colonyId, citizenId, traitId, -1);
                                })
                            )
                        )
                    )
                    .executes(ctx -> {
                        ctx.getSource().sendFailure(Component.literal(
                            "Usage: /motc addtrait <colonyId> <citizenId> <traitId> [durationSeconds]"));
                        return 0;
                    })
                )
                .then(Commands.literal("removetrait")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                        .suggests(SUGGEST_COLONIES)
                        .then(Commands.argument("citizenId", IntegerArgumentType.integer())
                            .suggests(SUGGEST_CITIZENS)
                            .then(Commands.argument("traitId", StringArgumentType.word())
                                .suggests(SUGGEST_TRAITS)
                                .executes(ctx -> {
                                    int colonyId = IntegerArgumentType.getInteger(ctx, "colonyId");
                                    int citizenId = IntegerArgumentType.getInteger(ctx, "citizenId");
                                    String traitId = StringArgumentType.getString(ctx, "traitId");
                                    return removeTrait(ctx.getSource().getPlayer(), colonyId, citizenId, traitId);
                                })
                            )
                        )
                    )
                    .executes(ctx -> {
                        ctx.getSource().sendFailure(Component.literal(
                            "Usage: /motc removetrait <colonyId> <citizenId> <traitId>"));
                        return 0;
                    })
                )
                .then(Commands.literal("listtraits")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        return listTraits(ctx.getSource().getPlayer());
                    })
                )
                .then(Commands.literal("genmissing")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                        .suggests(SUGGEST_COLONIES)
                        .executes(ctx -> {
                            int colonyId = IntegerArgumentType.getInteger(ctx, "colonyId");
                            return genMissingBackgrounds(ctx.getSource().getPlayer(), colonyId);
                        })
                    )
                    .executes(ctx -> {
                        // Run for all colonies
                        return genMissingBackgroundsAll(ctx.getSource().getPlayer());
                    })
                )
                .then(Commands.literal("status")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                        .suggests(SUGGEST_COLONIES)
                        .executes(ctx -> {
                            int colonyId = IntegerArgumentType.getInteger(ctx, "colonyId");
                            return showStatus(ctx.getSource().getPlayer(), colonyId);
                        })
                    )
                )
                .then(Commands.literal("tts")
                    .then(Commands.literal("on")
                        .executes(ctx -> {
                            ModSettings.TTS_ENABLED.set(true);
                            ModSettings.SPEC.save();
                            ctx.getSource().sendSuccess(
                                () -> Component.literal("TTS enabled"),
                                true
                            );
                            return 1;
                        })
                    )
                    .then(Commands.literal("off")
                        .executes(ctx -> {
                            ModSettings.TTS_ENABLED.set(false);
                            ModSettings.SPEC.save();
                            ctx.getSource().sendSuccess(
                                () -> Component.literal("TTS disabled"),
                                true
                            );
                            return 1;
                        })
                    )
                    .then(Commands.literal("speed")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.25, 4.0))
                            .executes(ctx -> {
                                double speed = DoubleArgumentType.getDouble(ctx, "value");
                                ModSettings.TTS_SPEED.set(speed);
                                ModSettings.SPEC.save();
                                ctx.getSource().sendSuccess(
                                    () -> Component.literal("TTS speed set to: " + speed),
                                    true
                                );
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("stop")
                        .executes(ctx -> {
                            Player2NpcLib.ttsStop();
                            ctx.getSource().sendSuccess(
                                () -> Component.literal("TTS playback stopped"),
                                true
                            );
                            return 1;
                        })
                    )
                    .executes(ctx -> {
                        boolean enabled = ModSettings.TTS_ENABLED.get();
                        double speed = ModSettings.TTS_SPEED.get();
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("TTS: " + (enabled ? "enabled" : "disabled") + ", speed: " + speed),
                            false
                        );
                        return 1;
                    })
                )
                .then(Commands.literal("npc-chat")
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                        .suggests(SUGGEST_COLONIES)
                        .then(Commands.argument("citizenId1", IntegerArgumentType.integer())
                            .suggests(SUGGEST_CITIZENS)
                            .then(Commands.argument("citizenId2", IntegerArgumentType.integer())
                                .suggests(SUGGEST_CITIZENS)
                                .executes(ctx -> {
                                    int colonyId = IntegerArgumentType.getInteger(ctx, "colonyId");
                                    int citizenId1 = IntegerArgumentType.getInteger(ctx, "citizenId1");
                                    int citizenId2 = IntegerArgumentType.getInteger(ctx, "citizenId2");
                                    return forceNpcChat(ctx.getSource().getPlayer(), colonyId, citizenId1, citizenId2);
                                })
                            )
                        )
                    )
                    .executes(ctx -> {
                        ctx.getSource().sendFailure(Component.literal(
                            "Usage: /motc npc-chat <colonyId> <citizenId1> <citizenId2>"));
                        return 0;
                    })
                )
                .then(Commands.literal("npc-status")
                    .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                        .suggests(SUGGEST_COLONIES)
                        .executes(ctx -> {
                            int colonyId = IntegerArgumentType.getInteger(ctx, "colonyId");
                            return showNpcInteractionStatus(ctx.getSource().getPlayer(), colonyId);
                        })
                    )
                    .executes(ctx -> {
                        ctx.getSource().sendFailure(Component.literal(
                            "Usage: /motc npc-status <colonyId>"));
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
            if (visitorData instanceof IExtendedCitizenData extData && visitorData instanceof ICitizenData citizenData) {
                player.sendSystemMessage(Component.literal("Regenerating background for visitor: " + visitorData.getName() + "..."));

                BackgroundGenerationService.getInstance().generateBackground(citizenData, gameId)
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

    private static int addTrait(ServerPlayer player, int colonyId, int citizenId, String traitId, int durationSeconds) {
        if (player == null) {
            return 0;
        }

        IColony colony = IColonyManager.getInstance().getColonyByDimension(colonyId, player.level().dimension());
        if (colony == null) {
            player.sendSystemMessage(Component.literal("Colony not found: " + colonyId));
            return 0;
        }

        // Try citizen manager first, then visitor manager
        ICitizenData citizenData = colony.getCitizenManager().getCivilian(citizenId);
        if (citizenData == null) {
            citizenData = (ICitizenData) colony.getVisitorManager().getCivilian(citizenId);
        }
        if (citizenData == null) {
            player.sendSystemMessage(Component.literal("Citizen not found: " + citizenId));
            return 0;
        }

        if (!(citizenData instanceof IExtendedCitizenData extData)) {
            player.sendSystemMessage(Component.literal("Citizen data not extended (mixin issue)"));
            return 0;
        }

        // Validate trait exists
        TraitDefinition traitDef = TraitRegistry.getTrait(traitId);
        if (traitDef == null) {
            player.sendSystemMessage(Component.literal("Unknown trait: " + traitId + ". Use /motc listtraits to see available traits."));
            return 0;
        }

        String citizenName = citizenData.getName();

        if (durationSeconds > 0) {
            // Add as temporary trait
            int durationTicks = durationSeconds * 20;
            long currentTick = player.level().getGameTime();
            TemporaryTrait tempTrait = new TemporaryTrait(traitId, "command", currentTick, durationTicks);
            extData.addTemporaryTrait(tempTrait);
            player.sendSystemMessage(Component.literal(
                "Added temporary trait '" + traitId + "' to " + citizenName + " for " + durationSeconds + " seconds"));
        } else {
            // Add as permanent trait
            CitizenBackground bg = extData.getCitizenBackground();
            if (bg == null || !bg.isInitialized()) {
                player.sendSystemMessage(Component.literal("Citizen has no background initialized. Use /motc regenbackground first."));
                return 0;
            }
            if (bg.getTraits().contains(traitId)) {
                player.sendSystemMessage(Component.literal(citizenName + " already has permanent trait: " + traitId));
                return 0;
            }
            bg.addTrait(traitId);
            player.sendSystemMessage(Component.literal(
                "Added permanent trait '" + traitId + "' to " + citizenName));
        }

        return 1;
    }

    private static int removeTrait(ServerPlayer player, int colonyId, int citizenId, String traitId) {
        if (player == null) {
            return 0;
        }

        IColony colony = IColonyManager.getInstance().getColonyByDimension(colonyId, player.level().dimension());
        if (colony == null) {
            player.sendSystemMessage(Component.literal("Colony not found: " + colonyId));
            return 0;
        }

        // Try citizen manager first, then visitor manager
        ICitizenData citizenData = colony.getCitizenManager().getCivilian(citizenId);
        if (citizenData == null) {
            citizenData = (ICitizenData) colony.getVisitorManager().getCivilian(citizenId);
        }
        if (citizenData == null) {
            player.sendSystemMessage(Component.literal("Citizen not found: " + citizenId));
            return 0;
        }

        if (!(citizenData instanceof IExtendedCitizenData extData)) {
            player.sendSystemMessage(Component.literal("Citizen data not extended (mixin issue)"));
            return 0;
        }

        String citizenName = citizenData.getName();
        boolean removed = false;

        // Try removing temporary trait first
        if (extData.hasTemporaryTrait(traitId)) {
            extData.removeTemporaryTrait(traitId);
            player.sendSystemMessage(Component.literal(
                "Removed temporary trait '" + traitId + "' from " + citizenName));
            removed = true;
        }

        // Also try removing permanent trait
        CitizenBackground bg = extData.getCitizenBackground();
        if (bg != null && bg.isInitialized() && bg.getTraits().contains(traitId)) {
            bg.getTraits().remove(traitId);
            player.sendSystemMessage(Component.literal(
                "Removed permanent trait '" + traitId + "' from " + citizenName));
            removed = true;
        }

        if (!removed) {
            player.sendSystemMessage(Component.literal(
                citizenName + " does not have trait: " + traitId));
            return 0;
        }

        return 1;
    }

    private static int listTraits(ServerPlayer player) {
        if (player == null) {
            return 0;
        }

        StringBuilder sb = new StringBuilder("Available traits:\n");
        for (TraitDefinition trait : TraitRegistry.getAllTraits()) {
            sb.append("- ").append(trait.id());
            if (trait.temporaryOnly()) {
                sb.append(" (temp only)");
            }
            sb.append("\n");
        }
        player.sendSystemMessage(Component.literal(sb.toString()));
        return 1;
    }

    private static int genMissingBackgrounds(ServerPlayer player, int colonyId) {
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
            CitizenNpcManager.getInstance().initialize();
            gameId = CitizenNpcManager.getInstance().getGameId();
        }

        int missingCount = 0;
        int totalCount = 0;

        // Check citizens
        for (ICitizenData citizenData : colony.getCitizenManager().getCitizens()) {
            totalCount++;
            if (citizenData instanceof IExtendedCitizenData extData) {
                CitizenBackground bg = extData.getCitizenBackground();
                if (bg == null || !bg.isInitialized()) {
                    missingCount++;
                    player.sendSystemMessage(Component.literal(
                        "Generating background for citizen: " + citizenData.getName() +
                        " (ID: " + citizenData.getId() + ", child: " + citizenData.isChild() + ")"));

                    final String gid = gameId;
                    BackgroundGenerationService.getInstance().generateBackground(citizenData, gid)
                        .thenAccept(background -> {
                            extData.setCitizenBackground(background);
                            player.sendSystemMessage(Component.literal(
                                "✓ Generated background for " + citizenData.getName() + ": " + background.getTraits()));
                        })
                        .exceptionally(ex -> {
                            player.sendSystemMessage(Component.literal(
                                "✗ Failed for " + citizenData.getName() + ": " + ex.getMessage()));
                            return null;
                        });
                }
            } else {
                player.sendSystemMessage(Component.literal(
                    "WARNING: Citizen " + citizenData.getName() + " is not IExtendedCitizenData (mixin not applied?)"));
            }
        }

        // Check visitors
        for (ICivilianData civilianData : colony.getVisitorManager().getCivilianDataMap().values()) {
            totalCount++;
            if (civilianData instanceof ICitizenData citizenData && civilianData instanceof IExtendedCitizenData extData) {
                CitizenBackground bg = extData.getCitizenBackground();
                if (bg == null || !bg.isInitialized()) {
                    missingCount++;
                    player.sendSystemMessage(Component.literal(
                        "Generating background for visitor: " + citizenData.getName() + " (ID: " + citizenData.getId() + ")"));

                    final String gid = gameId;
                    BackgroundGenerationService.getInstance().generateBackground(citizenData, gid)
                        .thenAccept(background -> {
                            extData.setCitizenBackground(background);
                            player.sendSystemMessage(Component.literal(
                                "✓ Generated background for visitor " + citizenData.getName() + ": " + background.getTraits()));
                        })
                        .exceptionally(ex -> {
                            player.sendSystemMessage(Component.literal(
                                "✗ Failed for visitor " + citizenData.getName() + ": " + ex.getMessage()));
                            return null;
                        });
                }
            }
        }

        player.sendSystemMessage(Component.literal(
            "Colony " + colonyId + ": " + missingCount + " missing out of " + totalCount + " total"));
        return missingCount;
    }

    private static int genMissingBackgroundsAll(ServerPlayer player) {
        if (player == null) {
            return 0;
        }

        int total = 0;
        for (IColony colony : IColonyManager.getInstance().getColonies(player.level())) {
            total += genMissingBackgrounds(player, colony.getID());
        }
        return total;
    }

    private static int showStatus(ServerPlayer player, int colonyId) {
        if (player == null) {
            return 0;
        }

        IColony colony = IColonyManager.getInstance().getColonyByDimension(colonyId, player.level().dimension());
        if (colony == null) {
            player.sendSystemMessage(Component.literal("Colony not found: " + colonyId));
            return 0;
        }

        player.sendSystemMessage(Component.literal("=== Colony " + colonyId + " Status ==="));

        int withBg = 0;
        int withoutBg = 0;

        // Check citizens
        for (ICitizenData citizenData : colony.getCitizenManager().getCitizens()) {
            boolean hasBg = false;
            String bgStatus = "NOT IExtendedCitizenData";

            if (citizenData instanceof IExtendedCitizenData extData) {
                CitizenBackground bg = extData.getCitizenBackground();
                if (bg != null && bg.isInitialized()) {
                    hasBg = true;
                    bgStatus = "OK (" + bg.getTraits().size() + " traits)";
                    withBg++;
                } else if (bg != null) {
                    bgStatus = "NOT INITIALIZED (backstory=" + (bg.getBackstory() == null ? "null" : "empty") + ")";
                    withoutBg++;
                } else {
                    bgStatus = "NULL";
                    withoutBg++;
                }
            } else {
                withoutBg++;
            }

            String childStr = citizenData.isChild() ? " [CHILD]" : "";
            player.sendSystemMessage(Component.literal(
                (hasBg ? "✓ " : "✗ ") + citizenData.getName() + " (ID:" + citizenData.getId() + ")" + childStr + " - " + bgStatus));
        }

        // Check visitors
        player.sendSystemMessage(Component.literal("--- Visitors ---"));
        for (ICivilianData civilianData : colony.getVisitorManager().getCivilianDataMap().values()) {
            boolean hasBg = false;
            String bgStatus = "NOT IExtendedCitizenData";

            if (civilianData instanceof IExtendedCitizenData extData) {
                CitizenBackground bg = extData.getCitizenBackground();
                if (bg != null && bg.isInitialized()) {
                    hasBg = true;
                    bgStatus = "OK (" + bg.getTraits().size() + " traits)";
                    withBg++;
                } else if (bg != null) {
                    bgStatus = "NOT INITIALIZED";
                    withoutBg++;
                } else {
                    bgStatus = "NULL";
                    withoutBg++;
                }
            } else {
                withoutBg++;
            }

            player.sendSystemMessage(Component.literal(
                (hasBg ? "✓ " : "✗ ") + civilianData.getName() + " (ID:" + civilianData.getId() + ") - " + bgStatus));
        }

        player.sendSystemMessage(Component.literal(
            "Summary: " + withBg + " with backgrounds, " + withoutBg + " missing"));

        return 1;
    }

    private static int forceNpcChat(ServerPlayer player, int colonyId, int citizenId1, int citizenId2) {
        if (player == null) {
            return 0;
        }

        if (citizenId1 == citizenId2) {
            player.sendSystemMessage(Component.literal("Cannot start conversation with self"));
            return 0;
        }

        IColony colony = IColonyManager.getInstance().getColonyByDimension(colonyId, player.level().dimension());
        if (colony == null) {
            player.sendSystemMessage(Component.literal("Colony not found: " + colonyId));
            return 0;
        }

        ICitizenData citizen1 = colony.getCitizenManager().getCivilian(citizenId1);
        ICitizenData citizen2 = colony.getCitizenManager().getCivilian(citizenId2);

        if (citizen1 == null) {
            player.sendSystemMessage(Component.literal("Citizen not found: " + citizenId1));
            return 0;
        }
        if (citizen2 == null) {
            player.sendSystemMessage(Component.literal("Citizen not found: " + citizenId2));
            return 0;
        }

        // Check if bridges are ready
        CitizenNpcBridge bridge1 = CitizenNpcManager.getInstance().getBridge(citizenId1);
        CitizenNpcBridge bridge2 = CitizenNpcManager.getInstance().getBridge(citizenId2);

        if (bridge1 == null || !bridge1.isReady()) {
            player.sendSystemMessage(Component.literal("NPC bridge not ready for: " + citizen1.getName()));
            return 0;
        }
        if (bridge2 == null || !bridge2.isReady()) {
            player.sendSystemMessage(Component.literal("NPC bridge not ready for: " + citizen2.getName()));
            return 0;
        }

        // Check if NPC interaction is enabled
        if (!NpcInteractionConfig.isEnabled()) {
            player.sendSystemMessage(Component.literal("NPC interaction is disabled in config"));
            return 0;
        }

        // Force start conversation
        NpcConversationManager convManager = NpcConversationManager.getInstance(colonyId);
        long currentTick = player.level().getGameTime();

        // Check if either is already in conversation
        if (convManager.isInConversation(citizenId1)) {
            player.sendSystemMessage(Component.literal(citizen1.getName() + " is already in a conversation"));
            return 0;
        }
        if (convManager.isInConversation(citizenId2)) {
            player.sendSystemMessage(Component.literal(citizen2.getName() + " is already in a conversation"));
            return 0;
        }

        NpcConversation conversation = convManager.startConversation(citizenId1, citizenId2, currentTick);
        player.sendSystemMessage(Component.literal(
            "Started NPC conversation between " + citizen1.getName() + " and " + citizen2.getName() +
            "\nMax turns: " + conversation.getMaxTurns() +
            "\nWatch your chat for their dialogue!"));

        return 1;
    }

    private static int showNpcInteractionStatus(ServerPlayer player, int colonyId) {
        if (player == null) {
            return 0;
        }

        IColony colony = IColonyManager.getInstance().getColonyByDimension(colonyId, player.level().dimension());
        if (colony == null) {
            player.sendSystemMessage(Component.literal("Colony not found: " + colonyId));
            return 0;
        }

        player.sendSystemMessage(Component.literal("=== NPC Interaction Status for Colony " + colonyId + " ==="));

        // Config status
        player.sendSystemMessage(Component.literal(
            "Enabled: " + NpcInteractionConfig.isEnabled() +
            " | Interaction radius: " + NpcInteractionConfig.getProximityConfig().interactionRadius +
            " | Start chance: " + (NpcInteractionConfig.getConversationConfig().startChance * 100) + "%"));

        // Conversation manager status
        NpcConversationManager convManager = NpcConversationManager.getInstance(colonyId);

        // Count citizens with ready bridges
        int readyBridges = 0;
        int totalCitizens = 0;
        for (ICitizenData citizen : colony.getCitizenManager().getCitizens()) {
            totalCitizens++;
            CitizenNpcBridge bridge = CitizenNpcManager.getInstance().getBridge(citizen.getId());
            if (bridge != null && bridge.isReady()) {
                readyBridges++;
            }
        }
        player.sendSystemMessage(Component.literal(
            "Ready NPC bridges: " + readyBridges + "/" + totalCitizens));

        // Check for nearby citizen pairs
        double radius = NpcInteractionConfig.getProximityConfig().interactionRadius;
        int nearbyPairs = 0;
        var citizens = colony.getCitizenManager().getCitizens().stream().toList();
        for (int i = 0; i < citizens.size(); i++) {
            for (int j = i + 1; j < citizens.size(); j++) {
                ICitizenData c1 = citizens.get(i);
                ICitizenData c2 = citizens.get(j);
                if (c1.getEntity().isPresent() && c2.getEntity().isPresent()) {
                    double dist = c1.getEntity().get().distanceToSqr(c2.getEntity().get());
                    if (dist <= radius * radius) {
                        nearbyPairs++;
                        // Check if they can converse
                        boolean can1 = convManager.canStartConversation(c1.getId(), player.level().getGameTime());
                        boolean can2 = convManager.canStartConversation(c2.getId(), player.level().getGameTime());
                        CitizenRelationship rel = convManager.getRelationship(c1.getId(), c2.getId());
                        String relInfo = rel != null ?
                            " (talked " + rel.getConversationCount() + "x, " + rel.getRelationshipLevel() + ")" :
                            " (strangers)";
                        player.sendSystemMessage(Component.literal(
                            "  Near: " + c1.getName() + " <-> " + c2.getName() +
                            " (dist: " + String.format("%.1f", Math.sqrt(dist)) + ")" +
                            (can1 && can2 ? " [CAN CHAT]" : " [on cooldown]") +
                            relInfo));
                    }
                }
            }
        }
        player.sendSystemMessage(Component.literal("Nearby citizen pairs: " + nearbyPairs));

        // Active conversations info would need to be exposed from the manager
        player.sendSystemMessage(Component.literal(
            "\nUse /motc npc-chat <colonyId> <citizen1> <citizen2> to force a conversation"));

        return 1;
    }
}
