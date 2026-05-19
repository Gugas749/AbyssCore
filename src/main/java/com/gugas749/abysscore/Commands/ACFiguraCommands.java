package com.gugas749.abysscore.Commands;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Network.Figura.FiguraReloadPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;

public class ACFiguraCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("figura")
                                .then(Commands.literal("reloadall")
                                        .executes(ACFiguraCommands::executeReloadAll)
                                        .then(Commands.literal("force")
                                                .executes(ACFiguraCommands::executeReloadAllForce)
                                        )
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

        // Send our custom S2C packet to every online player.
        // Their client will receive it and call "figura reload" on the CLIENT dispatcher,
        // where Figura actually registered it. This is the correct approach since
        // Figura's reload command is purely client-side.
        for (ServerPlayer player : players) {
            PacketDistributor.sendToPlayer(player, new FiguraReloadPacket());
        }

        int total = players.size();
        source.sendSuccess(
                () -> Component.translatable("message.abysscore.figura.reloaded", total, total),
                true
        );

        Abysscore.LOGGER.info("[AbyssCore] Sent Figura reload packet to {} player(s).", total);
        return total;
    }


    // -------------------------------------------------------------------------
    // /abysscore figura reloadall force
    // -------------------------------------------------------------------------

    private static int executeReloadAllForce(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        // Check if Figura is installed by trying to load AvatarManager
        boolean figuraPresent;
        try {
            Class.forName("org.figuramc.figura.avatar.AvatarManager");
            figuraPresent = true;
        } catch (ClassNotFoundException e) {
            figuraPresent = false;
        }

        if (!figuraPresent) {
            source.sendFailure(Component.translatable("message.abysscore.figura.not_installed"));
            return 0;
        }

        Collection<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();

        if (players.isEmpty()) {
            source.sendFailure(Component.translatable("message.abysscore.figura.no_players"));
            return 0;
        }

        // Send force reload packet — client handler will call AvatarManager.clearAllAvatars()
        for (ServerPlayer player : players) {
            PacketDistributor.sendToPlayer(player, new FiguraReloadPacket(true));
        }

        int total = players.size();
        source.sendSuccess(
                () -> Component.translatable("message.abysscore.figura.force_reloaded", total, total),
                true
        );

        Abysscore.LOGGER.info("[AbyssCore] Sent Figura FORCE reload packet to {} player(s).", total);
        return total;
    }
}
