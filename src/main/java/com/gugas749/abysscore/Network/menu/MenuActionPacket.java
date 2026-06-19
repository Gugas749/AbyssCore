package com.gugas749.abysscore.Network.menu;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record MenuActionPacket(
        Action action,
        UUID   targetUUID,
        String stringArg1,
        String stringArg2,
        int    intArg
) implements CustomPacketPayload {

    public enum Action {
        // Player actions
        TOGGLE_STAFF, TOGGLE_VANISH, TOGGLE_GOD, TOGGLE_BLIND,
        // Title actions
        DELETE_TITLE, SEND_TITLE_ALL, SEND_TITLE_TAG, SEND_TITLE_TEAM, SEND_TITLE_PLAYER,
        // Vanish actions
        TOGGLE_TEAM_VISIBILITY,
        VANISH_SHOW_TO,   // stringArg1 = vanished player UUID str, targetUUID = viewer
        VANISH_HIDE_FROM, // stringArg1 = vanished player UUID str, targetUUID = viewer
        VANISH_CLEAR,     // stringArg1 = vanished player UUID str
        // Dimen actions
        DIMEN_TP,         // stringArg1 = dim key
        DIMEN_CREATE,     // opens create screen — handled client-side
        // Help actions
        HELP_ACCEPT,      // targetUUID = requester UUID
        // Bulk actions
        BULK_RUN,         // stringArg1 = bulk name
        BULK_BIND,        // stringArg1 = bulk name, intArg = slot (1-9)
        BULK_UNBIND,      // intArg = slot
    }

    public static MenuActionPacket of(Action action) {
        return new MenuActionPacket(action, null, "", "", 0);
    }
    public static MenuActionPacket playerAction(Action action, UUID uuid) {
        return new MenuActionPacket(action, uuid, "", "", 0);
    }
    public static MenuActionPacket stringAction(Action action, String a1, String a2) {
        return new MenuActionPacket(action, null, a1, a2, 0);
    }
    public static MenuActionPacket intAction(Action action, String a1, int intArg) {
        return new MenuActionPacket(action, null, a1, "", intArg);
    }

    public static final Type<MenuActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Abysscore.MODID, "menu_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeInt(pkt.action().ordinal());
                buf.writeBoolean(pkt.targetUUID() != null);
                if (pkt.targetUUID() != null) buf.writeUUID(pkt.targetUUID());
                buf.writeUtf(pkt.stringArg1() != null ? pkt.stringArg1() : "");
                buf.writeUtf(pkt.stringArg2() != null ? pkt.stringArg2() : "");
                buf.writeInt(pkt.intArg());
            },
            buf -> {
                Action action = Action.values()[buf.readInt()];
                UUID uuid = buf.readBoolean() ? buf.readUUID() : null;
                return new MenuActionPacket(action, uuid, buf.readUtf(), buf.readUtf(), buf.readInt());
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}