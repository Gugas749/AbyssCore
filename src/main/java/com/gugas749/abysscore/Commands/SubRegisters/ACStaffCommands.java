package com.gugas749.abysscore.Commands.SubRegisters;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Staff.StaffProfileManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ACStaffCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("staff")
                                .executes(ACStaffCommands::executeToggleSelf)

                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ACStaffCommands::executeToggleTarget)
                                )

                                .then(Commands.literal("status")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ACStaffCommands::executeStatus)
                                        )
                                )

                                .then(Commands.literal("reset")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ACStaffCommands::executeReset)
                                        )
                                )
                        )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore staff");
    }

    private static int executeToggleSelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.translatable("message.abysscore.staff.player_only"));
            return 0;
        }

        return toggle(ctx.getSource(), player);
    }

    private static int executeToggleTarget(CommandContext<CommandSourceStack> ctx) {
        try {
            return toggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"));
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("message.abysscore.staff.player_not_found"));
            return 0;
        }
    }

    private static int executeStatus(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            boolean enabled = StaffProfileManager.isStaffMode(target);
            ctx.getSource().sendSuccess(
                    () -> Component.translatable(
                            enabled ? "message.abysscore.staff.status_enabled" : "message.abysscore.staff.status_disabled",
                            target.getName().getString()
                    ),
                    false
            );
            return enabled ? 1 : 0;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("message.abysscore.staff.player_not_found"));
            return 0;
        }
    }

    private static int executeReset(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            if (StaffProfileManager.isStaffMode(target)) {
                ctx.getSource().sendFailure(Component.translatable("message.abysscore.staff.reset_active", target.getName().getString()));
                return 0;
            }

            if (StaffProfileManager.resetStaffProfile(target)) {
                ctx.getSource().sendSuccess(
                        () -> Component.translatable("message.abysscore.staff.reset", target.getName().getString()),
                        true
                );
                return 1;
            }

            ctx.getSource().sendFailure(Component.translatable("message.abysscore.staff.reset_failed", target.getName().getString()));
            return 0;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("message.abysscore.staff.player_not_found"));
            return 0;
        }
    }

    private static int toggle(CommandSourceStack source, ServerPlayer player) {
        boolean currentlyEnabled = StaffProfileManager.isStaffMode(player);
        boolean success = currentlyEnabled ? StaffProfileManager.disable(player) : StaffProfileManager.enable(player);

        if (!success) {
            source.sendFailure(Component.translatable("message.abysscore.staff.toggle_failed", player.getName().getString()));
            return 0;
        }

        source.sendSuccess(
                () -> Component.translatable(
                        currentlyEnabled ? "message.abysscore.staff.disabled" : "message.abysscore.staff.enabled",
                        player.getName().getString()
                ),
                true
        );

        if (source.getEntity() != player) {
            player.sendSystemMessage(Component.translatable(
                    currentlyEnabled ? "message.abysscore.staff.disabled_self" : "message.abysscore.staff.enabled_self"
            ));
        }

        return 1;
    }
}
