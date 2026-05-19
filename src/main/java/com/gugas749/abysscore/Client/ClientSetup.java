package com.gugas749.abysscore.Client;

import com.gugas749.abysscore.Network.FiguraReloadPacket;
import com.gugas749.abysscore.Network.FiguraReloadPacketHandler;
import com.gugas749.abysscore.Network.OpenBulkScreenPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Registers all client-side packet handlers.
 */
public class ClientSetup {

    public static void registerPacketHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        // S2C: server tells client to reload Figura
        registrar.playToClient(
            FiguraReloadPacket.TYPE,
            FiguraReloadPacket.CODEC,
            FiguraReloadPacketHandler::handle
        );

        // S2C: server tells client to open the bulk command creation screen
        registrar.playToClient(
            OpenBulkScreenPacket.TYPE,
            OpenBulkScreenPacket.CODEC,
            ClientSetup::handleOpenBulkScreen
        );
    }

    private static void handleOpenBulkScreen(OpenBulkScreenPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
            Minecraft.getInstance().setScreen(new BulkCommandScreen())
        );
    }
}
