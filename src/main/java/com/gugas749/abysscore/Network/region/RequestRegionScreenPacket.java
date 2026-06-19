package com.gugas749.abysscore.Network.region;

import com.gugas749.abysscore.Abysscore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestRegionScreenPacket() implements CustomPacketPayload {
    public static final Type<RequestRegionScreenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Abysscore.MODID, "request_region_screen"));
    public static final StreamCodec<ByteBuf, RequestRegionScreenPacket> CODEC =
            StreamCodec.unit(new RequestRegionScreenPacket());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
