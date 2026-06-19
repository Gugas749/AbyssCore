package com.gugas749.abysscore.Network.region;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record OpenRegionScreenPacket(List<RegionEntry> regions) implements CustomPacketPayload {

    public record RegionEntry(
        String name,
        String dimension,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ,
        Set<String> tags,
        String entryFilterTag   // empty = no filter (nobody enters)
    ) {}

    public static final Type<OpenRegionScreenPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "open_region_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenRegionScreenPacket> CODEC = StreamCodec.of(
        (buf, pkt) -> {
            buf.writeInt(pkt.regions().size());
            for (RegionEntry r : pkt.regions()) {
                buf.writeUtf(r.name());
                buf.writeUtf(r.dimension());
                buf.writeInt(r.minX()); buf.writeInt(r.minY()); buf.writeInt(r.minZ());
                buf.writeInt(r.maxX()); buf.writeInt(r.maxY()); buf.writeInt(r.maxZ());
                buf.writeInt(r.tags().size());
                r.tags().forEach(buf::writeUtf);
                buf.writeUtf(r.entryFilterTag() != null ? r.entryFilterTag() : "");
            }
        },
        buf -> {
            int count = buf.readInt();
            List<RegionEntry> regions = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String name = buf.readUtf(), dim = buf.readUtf();
                int minX = buf.readInt(), minY = buf.readInt(), minZ = buf.readInt();
                int maxX = buf.readInt(), maxY = buf.readInt(), maxZ = buf.readInt();
                int tagCount = buf.readInt();
                Set<String> tags = new LinkedHashSet<>();
                for (int t = 0; t < tagCount; t++) tags.add(buf.readUtf());
                String filterTag = buf.readUtf();
                regions.add(new RegionEntry(name, dim, minX, minY, minZ, maxX, maxY, maxZ, tags, filterTag));
            }
            return new OpenRegionScreenPacket(regions);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
