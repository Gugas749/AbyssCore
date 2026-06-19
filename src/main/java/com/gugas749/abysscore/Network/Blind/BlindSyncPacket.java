package com.gugas749.abysscore.Network.Blind;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server → Client: enables or disables the blind screen overlay. */
public record BlindSyncPacket(boolean blinded) implements CustomPacketPayload {

    public static final Type<BlindSyncPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "blind_sync"));

    public static final StreamCodec<ByteBuf, BlindSyncPacket> CODEC =
        ByteBufCodecs.BOOL.map(BlindSyncPacket::new, BlindSyncPacket::blinded);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
