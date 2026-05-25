package com.gugas749.abysscore.Network.Nametag;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NametagSyncPacket(
    boolean canSeeNametags,
    boolean hideAllTags
) implements CustomPacketPayload {

    public NametagSyncPacket(boolean canSeeNametags) {
        this(canSeeNametags, false);
    }

    public static final Type<NametagSyncPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "nametag_sync"));

    public static final StreamCodec<ByteBuf, NametagSyncPacket> CODEC =
        StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBoolean(pkt.canSeeNametags());
                buf.writeBoolean(pkt.hideAllTags());
            },
            buf -> new NametagSyncPacket(buf.readBoolean(), buf.readBoolean())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
