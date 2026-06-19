package com.gugas749.abysscore.Network.Bulk;

import com.gugas749.abysscore.Client.screens.BulkCommandScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class OpenBulkScreenHandler {

    public static void handle(OpenBulkScreenPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                Minecraft.getInstance().setScreen(new BulkCommandScreen())
        );
    }
}
