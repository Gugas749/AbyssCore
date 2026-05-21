package com.gugas749.abysscore.Commands.SubRegisters;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Vanish.ACVanishManager;
import com.gugas749.abysscore.Features.Vanish.ACVoiceVanishIntegration;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /abysscore vanish          — toggle vanish for yourself (OP only)
 * /abysscore vanish <player> — toggle vanish for another player (OP only)
 * /abysscore vanish list     — list all vanished players (OP only)
 */
public class ACVanishCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
            Commands.literal("abysscore")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("vanish")

                    // /abysscore vanish — toggle self
                    .executes(ACVanishCommands::executeToggleSelf)

                    // /abysscore vanish <player>
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ACVanishCommands::executeToggleTarget)
                    )

                    // /abysscore vanish list
                    .then(Commands.literal("list")
                        .executes(ACVanishCommands::executeList)
                    )
                )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore vanish");
    }

    private static int executeToggleSelf(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.vanish.player_only"));
            return 0;
        }
        return toggleVanish(source, player);
    }

    private static int executeToggleTarget(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            return toggleVanish(source, target);
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.abysscore.vanish.player_not_found"));
            return 0;
        }
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var vanished = ACVanishManager.getVanishedPlayers();

        if (vanished.isEmpty()) {
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.vanish.list_empty"), false);
            return 0;
        }

        source.sendSuccess(
            () -> Component.translatable("message.abysscore.vanish.list_header", vanished.size()), false);

        vanished.forEach(uuid -> {
            ServerPlayer p = source.getServer().getPlayerList().getPlayer(uuid);
            String name = p != null ? p.getName().getString() : uuid.toString();
            source.sendSuccess(() -> Component.literal("  §8" + name), false);
        });

        return vanished.size();
    }

    private static int toggleVanish(CommandSourceStack source, ServerPlayer player) {
        boolean nowVanished = ACVanishManager.toggle(player);

        // Update voice chat mods
        ACVoiceVanishIntegration.updateAll(player, nowVanished);

        // Feedback to OP
        source.sendSuccess(
            () -> Component.translatable(
                nowVanished
                    ? "message.abysscore.vanish.enabled"
                    : "message.abysscore.vanish.disabled",
                player.getName().getString()
            ),
            true
        );

        // Feedback to the vanished player themselves (if OP toggled someone else)
        if (source.getEntity() != player) {
            player.sendSystemMessage(Component.translatable(
                nowVanished
                    ? "message.abysscore.vanish.enabled_self"
                    : "message.abysscore.vanish.disabled_self"
            ));
        }

        return 1;
    }
}
