package com.gugas749.abysscore.Network.region;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NoEntryPacket(
    double exitX,
    double exitY,
    double exitZ,
    float  exitYaw  // 180° from entry direction — player faces away from the region
) implements CustomPacketPayload {

    public static final Type<NoEntryPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "no_entry"));

    public static final StreamCodec<FriendlyByteBuf, NoEntryPacket> CODEC = StreamCodec.of(
        (buf, pkt) -> {
            buf.writeDouble(pkt.exitX());
            buf.writeDouble(pkt.exitY());
            buf.writeDouble(pkt.exitZ());
            buf.writeFloat(pkt.exitYaw());
        },
        buf -> new NoEntryPacket(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readFloat()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
