package com.gugas749.abysscore.Network.Bulk;

import com.gugas749.abysscore.Client.BulkCommandScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class OpenBulkScreenHandler {

    public static void handle(OpenBulkScreenPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                Minecraft.getInstance().setScreen(new BulkCommandScreen())
        );
    }
}
