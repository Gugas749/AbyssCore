package com.gugas749.abysscore.Network.title;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveTitlePacket(
    String id,           // empty = create new
    String name,
    String titleText,
    String subtitleText,
    int fadeIn,
    int stay,
    int fadeOut
) implements CustomPacketPayload {

    public static final Type<SaveTitlePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Abysscore.MODID, "save_title"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveTitlePacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUtf(pkt.id());
                buf.writeUtf(pkt.name());
                buf.writeUtf(pkt.titleText());
                buf.writeUtf(pkt.subtitleText());
                buf.writeInt(pkt.fadeIn());
                buf.writeInt(pkt.stay());
                buf.writeInt(pkt.fadeOut());
            },
            buf -> new SaveTitlePacket(
                    buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                    buf.readInt(), buf.readInt(), buf.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
