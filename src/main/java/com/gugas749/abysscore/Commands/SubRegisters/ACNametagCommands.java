package com.gugas749.abysscore.Commands.SubRegisters;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Nametag.ACNametagManager;
import com.gugas749.abysscore.Network.Nametag.NametagSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * /abysscore nametag toggle — OP toggles their own nametag visibility
 * /abysscore nametag status — OP checks their current state
 */
public class ACNametagCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("abysscore")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("nametag")

                    .then(Commands.literal("toggle")
                        .executes(ACNametagCommands::executeToggle)
                    )

                    .then(Commands.literal("status")
                        .executes(ACNametagCommands::executeStatus)
                    )
                )
        );
        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore nametag <toggle|status>");
    }

    private static int executeToggle(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer op)) {
            ctx.getSource().sendFailure(Component.translatable("message.abysscore.nametag.player_only"));
            return 0;
        }

        boolean nowHidden = ACNametagManager.toggle(op);

        // Sync new state to the OP's client
        PacketDistributor.sendToPlayer(op,
            new NametagSyncPacket(ACNametagManager.canSeeNametags(op))
        );

        ctx.getSource().sendSuccess(
            () -> Component.translatable(
                nowHidden
                    ? "message.abysscore.nametag.now_hidden"
                    : "message.abysscore.nametag.now_visible"
            ),
            false
        );
        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer op)) {
            ctx.getSource().sendFailure(Component.translatable("message.abysscore.nametag.player_only"));
            return 0;
        }

        boolean canSee = ACNametagManager.canSeeNametags(op);
        ctx.getSource().sendSuccess(
            () -> Component.translatable(
                canSee
                    ? "message.abysscore.nametag.status_visible"
                    : "message.abysscore.nametag.status_hidden"
            ),
            false
        );
        return 1;
    }

    public static void syncOnJoin(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
            new NametagSyncPacket(ACNametagManager.canSeeNametags(player))
        );
    }
}
