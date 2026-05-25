package com.gugas749.abysscore.Network.Nametag;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → Client: tells the client whether they should see nametags.
 * Sent on login and whenever the OP toggles.
 */
public record NametagSyncPacket(boolean canSeeNametags) implements CustomPacketPayload {

    public static final Type<NametagSyncPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "nametag_sync"));

    public static final StreamCodec<ByteBuf, NametagSyncPacket> CODEC =
        ByteBufCodecs.BOOL.map(NametagSyncPacket::new, NametagSyncPacket::canSeeNametags);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
