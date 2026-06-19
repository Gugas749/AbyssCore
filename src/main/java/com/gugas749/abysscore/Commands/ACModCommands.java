package com.gugas749.abysscore.Commands;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Commands.SubRegisters.*;
import com.gugas749.abysscore.Network.menu.MenuPacketHandlers;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class ACModCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        Abysscore.LOGGER.info("[AbyssCore] Registering commands...");

        register(event.getDispatcher());

        ACRegionCommands.register(event.getDispatcher());
        ACBulkCommands.register(event.getDispatcher());
        ACBindCommands.register(event.getDispatcher());
        //ACStaffCommands.register(event.getDispatcher());
        ACDimenCommands.register(event.getDispatcher());
        ACDimenSettingsCommands.register(event.getDispatcher());
        ACHelpCommands.register(event.getDispatcher());
        ACVanishCommands.register(event.getDispatcher());
        ACGodCommands.register(event.getDispatcher());
        ACBlindCommands.register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return 0;
                            PacketDistributor.sendToPlayer(player,
                                    MenuPacketHandlers.buildMenuPacket(player));
                            return 1;
                        })
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore");
    }

}
