package com.gugas749.abysscore.Commands.SubRegisters;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Blind.ACBlindManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ACBlindCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("abysscore")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("blind")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ACBlindCommands::executeBlind)
                    )
                )

                .then(Commands.literal("unblind")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ACBlindCommands::executeUnblind)
                    )
                )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore blind|unblind <player>");
    }

    private static int executeBlind(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            ACBlindManager.blind(target);
            ctx.getSource().sendSuccess(
                () -> Component.translatable("message.abysscore.blind.enabled",
                    target.getName().getString()),
                true
            );
            target.sendSystemMessage(
                Component.translatable("message.abysscore.blind.enabled_self"));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(
                Component.translatable("message.abysscore.blind.player_not_found"));
            return 0;
        }
    }

    private static int executeUnblind(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            ACBlindManager.unblind(target);
            ctx.getSource().sendSuccess(
                () -> Component.translatable("message.abysscore.blind.disabled",
                    target.getName().getString()),
                true
            );
            target.sendSystemMessage(
                Component.translatable("message.abysscore.blind.disabled_self"));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(
                Component.translatable("message.abysscore.blind.player_not_found"));
            return 0;
        }
    }
}
