package com.gugas749.abysscore.Network.Vanish;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server → Client: tells the client whether they are currently vanished. */
public record VanishStateSyncPacket(boolean vanished) implements CustomPacketPayload {

    public static final Type<VanishStateSyncPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "vanish_state"));

    public static final StreamCodec<ByteBuf, VanishStateSyncPacket> CODEC =
        ByteBufCodecs.BOOL.map(VanishStateSyncPacket::new, VanishStateSyncPacket::vanished);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
