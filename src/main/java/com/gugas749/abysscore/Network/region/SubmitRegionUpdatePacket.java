package com.gugas749.abysscore.Network.region;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record SubmitRegionUpdatePacket(
    String regionName,
    boolean delete,
    Set<String> tags,
    String entryFilterTag   // empty = no filter
) implements CustomPacketPayload {

    public static final Type<SubmitRegionUpdatePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "submit_region_update"));

    public static final StreamCodec<FriendlyByteBuf, SubmitRegionUpdatePacket> CODEC = StreamCodec.of(
        (buf, pkt) -> {
            buf.writeUtf(pkt.regionName());
            buf.writeBoolean(pkt.delete());
            buf.writeInt(pkt.tags().size());
            pkt.tags().forEach(buf::writeUtf);
            buf.writeUtf(pkt.entryFilterTag() != null ? pkt.entryFilterTag() : "");
        },
        buf -> {
            String name = buf.readUtf();
            boolean delete = buf.readBoolean();
            int tagCount = buf.readInt();
            Set<String> tags = new LinkedHashSet<>();
            for (int i = 0; i < tagCount; i++) tags.add(buf.readUtf());
            String filterTag = buf.readUtf();
            return new SubmitRegionUpdatePacket(name, delete, tags, filterTag);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
