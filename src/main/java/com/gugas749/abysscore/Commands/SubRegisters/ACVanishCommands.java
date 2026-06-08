package com.gugas749.abysscore.Commands.SubRegisters;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Vanish.ACVanishExtras;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ACVanishCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("abysscore")
                .then(Commands.literal("vanish")

                    .then(Commands.literal("teamvisibility")
                        .requires(source -> source.hasPermission(2))
                        .executes(ACVanishCommands::executeTeamVisibility)
                    )

                    .then(Commands.literal("show")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(ACVanishCommands::executeShow)
                        )
                    )

                    .then(Commands.literal("hide")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(ACVanishCommands::executeHide)
                        )
                    )

                    .then(Commands.literal("clearexceptions")
                        .executes(ACVanishCommands::executeClear)
                    )

                    .then(Commands.literal("status")
                        .executes(ACVanishCommands::executeStatus)
                    )
                )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore vanish <teamvisibility|show|hide|clearexceptions|status>");
    }

    private static int executeTeamVisibility(CommandContext<CommandSourceStack> ctx) {
        boolean nowEnabled = ACVanishExtras.toggleTeamVisibility();
        ctx.getSource().sendSuccess(
            () -> Component.translatable(
                nowEnabled ? "message.abysscore.vanish.team_visible"
                           : "message.abysscore.vanish.team_hidden"
            ),
            true
        );
        return 1;
    }

    private static int executeShow(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer self)) {
            source.sendFailure(Component.translatable("message.abysscore.vanish.player_only"));
            return 0;
        }
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            if (target.equals(self)) {
                source.sendFailure(Component.translatable("message.abysscore.vanish.cannot_target_self"));
                return 0;
            }
            // Pass server so the change takes effect instantly
            ACVanishExtras.showTo(self.getUUID(), target.getUUID(), source.getServer());
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.vanish.shown_to", target.getName().getString()),
                false
            );
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.abysscore.vanish.player_not_found"));
            return 0;
        }
    }

    private static int executeHide(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer self)) {
            source.sendFailure(Component.translatable("message.abysscore.vanish.player_only"));
            return 0;
        }
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            if (target.equals(self)) {
                source.sendFailure(Component.translatable("message.abysscore.vanish.cannot_target_self"));
                return 0;
            }
            // Pass server so the change takes effect instantly
            ACVanishExtras.hideTo(self.getUUID(), target.getUUID(), source.getServer());
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.vanish.hidden_from", target.getName().getString()),
                false
            );
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.abysscore.vanish.player_not_found"));
            return 0;
        }
    }

    private static int executeClear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer self)) {
            source.sendFailure(Component.translatable("message.abysscore.vanish.player_only"));
            return 0;
        }
        ACVanishExtras.clearExceptions(self.getUUID(), source.getServer());
        source.sendSuccess(
            () -> Component.translatable("message.abysscore.vanish.exceptions_cleared"),
            false
        );
        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer self)) {
            source.sendFailure(Component.translatable("message.abysscore.vanish.player_only"));
            return 0;
        }

        if (self.hasPermissions(2)) {
            boolean teamVis = ACVanishExtras.isTeamVisibilityEnabled();
            source.sendSuccess(
                () -> Component.translatable(
                    teamVis ? "message.abysscore.vanish.team_status_on"
                            : "message.abysscore.vanish.team_status_off"
                ),
                false
            );
        }

        var shownTo    = ACVanishExtras.getShownTo(self.getUUID());
        var hiddenFrom = ACVanishExtras.getHiddenFrom(self.getUUID());

        if (shownTo.isEmpty() && hiddenFrom.isEmpty()) {
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.vanish.no_exceptions"),
                false
            );
            return 1;
        }

        shownTo.forEach(uuid -> {
            ServerPlayer p = source.getServer().getPlayerList().getPlayer(uuid);
            String name = p != null ? p.getName().getString() : uuid.toString();
            source.sendSuccess(() -> Component.literal("  §aVisible to: §f" + name), false);
        });

        hiddenFrom.forEach(uuid -> {
            ServerPlayer p = source.getServer().getPlayerList().getPlayer(uuid);
            String name = p != null ? p.getName().getString() : uuid.toString();
            source.sendSuccess(() -> Component.literal("  §cHidden from: §f" + name), false);
        });

        return 1;
    }
}
