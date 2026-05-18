package com.gugas749.abysscore.Commands;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Regions.ACBlockProtectionListener;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class ACTagFlagCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("protect")
                                .then(Commands.argument("targets", EntityArgument.players())

                                        .then(Commands.literal("add")
                                                .executes(ctx -> executeProtect(ctx, Action.ADD))
                                        )

                                        .then(Commands.literal("remove")
                                                .executes(ctx -> executeProtect(ctx, Action.REMOVE))
                                        )

                                        .then(Commands.literal("status")
                                                .executes(ctx -> executeProtect(ctx, Action.STATUS))
                                        )
                                )
                        )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore protect <player> <add|remove|status>");
    }

    // -------------------------------------------------------------------------
    // Command logic
    // -------------------------------------------------------------------------

    private static int executeProtect(CommandContext<CommandSourceStack> ctx, Action action) {
        CommandSourceStack source = ctx.getSource();
        Collection<ServerPlayer> targets;

        try {
            targets = EntityArgument.getPlayers(ctx, "targets");
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.abysscore.protect.player_not_found"));
            return 0;
        }

        int affected = 0;

        for (ServerPlayer player : targets) {
            String playerName = player.getName().getString();

            switch (action) {
                case ADD -> {
                    if (player.getTags().contains(ACBlockProtectionListener.NO_BUILD_TAG)) {
                        source.sendSuccess(
                                () -> Component.translatable("message.abysscore.protect.already_restricted", playerName),
                                false
                        );
                    } else {
                        player.addTag(ACBlockProtectionListener.NO_BUILD_TAG);
                        source.sendSuccess(
                                () -> Component.translatable("message.abysscore.protect.added_op", playerName),
                                true
                        );
                        player.sendSystemMessage(
                                Component.translatable("message.abysscore.protect.added_player")
                        );
                        affected++;
                    }
                }

                case REMOVE -> {
                    if (!player.getTags().contains(ACBlockProtectionListener.NO_BUILD_TAG)) {
                        source.sendSuccess(
                                () -> Component.translatable("message.abysscore.protect.not_restricted", playerName),
                                false
                        );
                    } else {
                        player.removeTag(ACBlockProtectionListener.NO_BUILD_TAG);
                        source.sendSuccess(
                                () -> Component.translatable("message.abysscore.protect.removed_op", playerName),
                                true
                        );
                        player.sendSystemMessage(
                                Component.translatable("message.abysscore.protect.removed_player")
                        );
                        affected++;
                    }
                }

                case STATUS -> {
                    boolean restricted = player.getTags().contains(ACBlockProtectionListener.NO_BUILD_TAG);
                    source.sendSuccess(
                            () -> Component.translatable(
                                    restricted
                                            ? "message.abysscore.protect.status_restricted"
                                            : "message.abysscore.protect.status_free",
                                    playerName
                            ),
                            false
                    );
                    affected++;
                }
            }
        }

        return affected;
    }

    private enum Action { ADD, REMOVE, STATUS }
}
