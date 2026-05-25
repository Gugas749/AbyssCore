package com.gugas749.abysscore.Commands;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Status.ACStatusManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ACStatusCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("abysscore")

                .then(Commands.literal("status")

                    // /abysscore status — show current status
                    .executes(ACStatusCommands::executeShow)

                    // /abysscore status hide — personal opt-out (any player)
                    .then(Commands.literal("hide")
                        .executes(ACStatusCommands::executeHide)
                    )

                    // /abysscore status hideall — server-wide toggle (OP only)
                    .then(Commands.literal("hideall")
                        .requires(source -> source.hasPermission(2))
                        .executes(ACStatusCommands::executeHideAll)
                    )

                    // /abysscore status <tag|clear>
                    .then(Commands.argument("tag", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                            java.util.List.of("on", "off", "afk", "clear"),
                            builder
                        ))
                        .executes(ACStatusCommands::executeSet)
                    )
                )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore status");
    }

    private static int executeShow(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.status.player_only"));
            return 0;
        }

        ACStatusManager.Status status = ACStatusManager.getStatus(player.getUUID());
        boolean hiding = ACStatusManager.isPersonalDisplayHidden(player.getUUID());

        if (status == null) {
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.status.current_none"),
                false
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.status.current", status.display)
                    .withStyle(status.color),
                false
            );
        }

        if (hiding) {
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.status.hiding_note"),
                false
            );
        }

        return 1;
    }

    private static int executeSet(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.status.player_only"));
            return 0;
        }

        String tag = StringArgumentType.getString(ctx, "tag");

        if (tag.equalsIgnoreCase("clear")) {
            ACStatusManager.clearStatus(player);
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.status.cleared"),
                false
            );
            return 1;
        }

        ACStatusManager.Status status = ACStatusManager.Status.fromString(tag);
        if (status == null) {
            source.sendFailure(Component.translatable("message.abysscore.status.invalid"));
            return 0;
        }

        ACStatusManager.setStatus(player, status);
        source.sendSuccess(
            () -> Component.translatable("message.abysscore.status.set", status.display)
                .withStyle(status.color),
            false
        );
        return 1;
    }

    // /abysscore status hide — any player toggles their personal tag visibility
    private static int executeHide(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.status.player_only"));
            return 0;
        }

        boolean nowHidden = ACStatusManager.togglePersonalDisplay(player);
        source.sendSuccess(
            () -> Component.translatable(
                nowHidden
                    ? "message.abysscore.status.hide_enabled"
                    : "message.abysscore.status.hide_disabled"
            ),
            false
        );
        return 1;
    }

    // /abysscore status hideall — OP toggles server-wide visibility for non-OPs
    private static int executeHideAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        boolean nowHidden = ACStatusManager.toggleServerWide(source.getServer());
        source.sendSuccess(
            () -> Component.translatable(
                nowHidden
                    ? "message.abysscore.status.hideall_enabled"
                    : "message.abysscore.status.hideall_disabled"
            ),
            true
        );
        return 1;
    }
}
