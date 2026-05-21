package com.gugas749.abysscore.Network.Dimen;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenDimenCreateScreenPacket() implements CustomPacketPayload {

    public static final Type<OpenDimenCreateScreenPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "open_dimen_create"));

    public static final StreamCodec<ByteBuf, OpenDimenCreateScreenPacket> CODEC =
        StreamCodec.unit(new OpenDimenCreateScreenPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
