package com.gugas749.abysscore.Network.Figura;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → Client packet.
 * When received on the client, calls Figura reload logic.
 */
public record FiguraReloadPacket(boolean force) implements CustomPacketPayload {

    public FiguraReloadPacket() {
        this(false);
    }

    public static final Type<FiguraReloadPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "figura_reload"));

    public static final StreamCodec<ByteBuf, FiguraReloadPacket> CODEC =
            ByteBufCodecs.BOOL.map(FiguraReloadPacket::new, FiguraReloadPacket::force);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
