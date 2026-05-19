package com.gugas749.abysscore.Network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → Client: tells the client to open the bulk command creation screen.
 * Carries no payload — the screen is purely client-side input.
 */
public record OpenBulkScreenPacket() implements CustomPacketPayload {

    public static final Type<OpenBulkScreenPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "open_bulk_screen"));

    public static final StreamCodec<ByteBuf, OpenBulkScreenPacket> CODEC =
        StreamCodec.unit(new OpenBulkScreenPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
