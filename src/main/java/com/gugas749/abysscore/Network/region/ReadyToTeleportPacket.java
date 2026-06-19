package com.gugas749.abysscore.Network.region;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → Server: blackscreen is at peak opacity, safe to teleport now.
 * Server teleports the player on receiving this — the world change is
 * hidden by the black overlay.
 */
public record ReadyToTeleportPacket() implements CustomPacketPayload {

    public static final Type<ReadyToTeleportPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "ready_to_tp"));

    public static final StreamCodec<ByteBuf, ReadyToTeleportPacket> CODEC =
        StreamCodec.unit(new ReadyToTeleportPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
