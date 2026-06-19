package com.gugas749.abysscore.Network.Binds;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → Server: a keybind slot was pressed.
 * slot: 1-9
 */
public record KeyPressPacket(int slot) implements CustomPacketPayload {

    public static final Type<KeyPressPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("abysscore", "key_press"));

    public static final StreamCodec<ByteBuf, KeyPressPacket> CODEC =
            ByteBufCodecs.INT.map(KeyPressPacket::new, KeyPressPacket::slot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
