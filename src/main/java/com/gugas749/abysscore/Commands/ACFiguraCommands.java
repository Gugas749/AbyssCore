package com.gugas749.abysscore.Commands;

import com.gugas749.abysscore.Abysscore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class ACFiguraCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("figura")
                                .then(Commands.literal("reloadall")
                                        .executes(ACFiguraCommands::executeReloadAll)
                                )
                        )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore figura reloadall");
    }

    // -------------------------------------------------------------------------
    // /abysscore figura reloadall
    // -------------------------------------------------------------------------

    private static int executeReloadAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Collection<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();

        if (players.isEmpty()) {
            source.sendFailure(Component.translatable("message.abysscore.figura.no_players"));
            return 0;
        }

        int count = 0;
        for (ServerPlayer player : players) {
            try {
                source.getServer()
                        .getCommands()
                        .getDispatcher()
                        .execute("figura reload", player.createCommandSourceStack());
                count++;
            } catch (Exception e) {
                Abysscore.LOGGER.warn(
                        "[AbyssCore] Failed to reload Figura avatar for {}: {}",
                        player.getName().getString(), e.getMessage()
                );
            }
        }

        int finalCount = count;
        int totalPlayers = players.size();
        source.sendSuccess(
                () -> Component.translatable("message.abysscore.figura.reloaded", finalCount, totalPlayers),
                true
        );

        return finalCount;
    }
}
