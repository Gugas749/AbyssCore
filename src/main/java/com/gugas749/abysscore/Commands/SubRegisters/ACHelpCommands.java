package com.gugas749.abysscore.Commands.SubRegisters;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Help.ACHelpManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ACHelpCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // ── /abysshelp ────────────────────────────────────────────────────────
        dispatcher.register(
            Commands.literal("abysshelp")

                // /abysshelp <reason> — available to everyone
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(ACHelpCommands::executeHelp)
                )

                // /abysshelp cancel
                .then(Commands.literal("cancel")
                    .executes(ACHelpCommands::executeCancel)
                )

                // /abysshelp list — OP only
                .then(Commands.literal("list")
                    .requires(source -> source.hasPermission(2))
                    .executes(ACHelpCommands::executeList)
                )
        );

        // ── /abysscore helpaccept <player> ────────────────────────────────────
        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("helpaccept")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                ACHelpManager.getAll().stream()
                                                        .map(r -> r.playerName)
                                                        .toList(),
                                                builder
                                        ))
                                        .executes(ACHelpCommands::executeAccept)
                                )
                        )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysshelp and /abysscore helpaccept");
    }

    // ── /abysshelp <reason> ───────────────────────────────────────────────────

    private static int executeHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.help.player_only"));
            return 0;
        }

        if (ACHelpManager.hasActiveRequest(player.getUUID())) {
            source.sendFailure(Component.translatable("message.abysscore.help.already_pending"));
            return 0;
        }

        String reason = StringArgumentType.getString(ctx, "reason");
        boolean submitted = ACHelpManager.submit(player, reason, source.getServer());

        if (submitted) {
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.help.submitted"),
                false
            );
        } else {
            source.sendFailure(Component.translatable("message.abysscore.help.already_pending"));
        }

        return submitted ? 1 : 0;
    }

    // ── /abysshelp cancel ────────────────────────────────────────────────────

    private static int executeCancel(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.help.player_only"));
            return 0;
        }

        boolean cancelled = ACHelpManager.cancel(player);
        if (cancelled) {
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.help.cancelled"),
                false
            );
        } else {
            source.sendFailure(Component.translatable("message.abysscore.help.no_request"));
        }

        return cancelled ? 1 : 0;
    }

    // ── /abysshelp list ───────────────────────────────────────────────────────

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var all = ACHelpManager.getAll();

        if (all.isEmpty()) {
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.help.list_empty"),
                false
            );
            return 0;
        }

        source.sendSuccess(
            () -> Component.translatable("message.abysscore.help.list_header", all.size()),
            false
        );

        all.forEach(req -> source.sendSuccess(
            () -> Component.literal("  §e" + req.playerName + "§7: §f" + req.reason),
            false
        ));

        return all.size();
    }

    // ── /abysscore helpaccept <player> ────────────────────────────────────────

    private static int executeAccept(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer staff)) {
            source.sendFailure(Component.translatable("message.abysscore.help.player_only"));
            return 0;
        }

        String targetName = StringArgumentType.getString(ctx, "target");

        UUID targetUUID = ACHelpManager.getAll().stream()
                .filter(r -> r.playerName.equalsIgnoreCase(targetName))
                .map(r -> r.playerUUID)
                .findFirst()
                .orElse(null);

        if (targetUUID == null) {
            source.sendFailure(
                    Component.translatable("message.abysscore.help.no_request_for", targetName)
            );
            return 0;
        }

        boolean accepted = ACHelpManager.accept(staff, targetUUID);
        if (!accepted) {
            source.sendFailure(
                    Component.translatable("message.abysscore.help.accept_failed", targetName)
            );
            return 0;
        }

        return 1;
    }
}
