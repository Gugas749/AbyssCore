package com.gugas749.abysscore.Network.Dimen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SubmitDimenCreatePacket(
        String name,
        String displayName,
        String style,     // "NORMAL", "SUPERFLAT", or "VOID"
        long   seed       // only used when style is NORMAL
) implements CustomPacketPayload {

    public static final Type<SubmitDimenCreatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "submit_dimen_create"));

    public static final StreamCodec<FriendlyByteBuf, SubmitDimenCreatePacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SubmitDimenCreatePacket::name,
                    ByteBufCodecs.STRING_UTF8, SubmitDimenCreatePacket::displayName,
                    ByteBufCodecs.STRING_UTF8, SubmitDimenCreatePacket::style,
                    ByteBufCodecs.VAR_LONG,    SubmitDimenCreatePacket::seed,
                    SubmitDimenCreatePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
