package com.gugas749.abysscore.Network.Bulk;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Client → Server: submits a new bulk command definition from the GUI screen.
 */
public record SubmitBulkCommandPacket(
    String name,
    int permLevel,
    List<String> commands
) implements CustomPacketPayload {

    public static final Type<SubmitBulkCommandPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "submit_bulk_command"));

    public static final StreamCodec<FriendlyByteBuf, SubmitBulkCommandPacket> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SubmitBulkCommandPacket::name,
            ByteBufCodecs.INT,
            SubmitBulkCommandPacket::permLevel,
            ByteBufCodecs.collection(java.util.ArrayList::new, ByteBufCodecs.STRING_UTF8),
            SubmitBulkCommandPacket::commands,
            SubmitBulkCommandPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
