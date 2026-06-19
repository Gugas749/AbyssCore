package com.gugas749.abysscore.Network.menu;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record OpenMainMenuPacket(
        List<PlayerState>   players,
        List<TitleEntry>    titles,
        List<DimEntry>      dims,
        List<HelpEntry>     helpRequests,
        List<BulkEntry>     bulkCommands,
        Map<Integer, String> bindSlots,    // slot (1-9) → bulk command name
        boolean             teamVisibility // vanish team visibility state
) implements CustomPacketPayload {

    public record PlayerState(UUID uuid, String name,
                              boolean staffMode, boolean vanished, boolean godMode, boolean blinded) {}

    public record TitleEntry(String id, String name, String titleText,
                             String subtitleText, int fadeIn, int stay, int fadeOut) {}

    public record DimEntry(String name, String state) {}  // state = LOADED/PENDING/UNLOADED

    public record HelpEntry(UUID playerUUID, String playerName, String reason) {}

    public record BulkEntry(String name) {}

    public static final Type<OpenMainMenuPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Abysscore.MODID, "open_main_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMainMenuPacket> CODEC = StreamCodec.of(
            (buf, pkt) -> {
                // Players
                buf.writeInt(pkt.players().size());
                for (var p : pkt.players()) {
                    buf.writeUUID(p.uuid()); buf.writeUtf(p.name());
                    buf.writeBoolean(p.staffMode()); buf.writeBoolean(p.vanished());
                    buf.writeBoolean(p.godMode()); buf.writeBoolean(p.blinded());
                }
                // Titles
                buf.writeInt(pkt.titles().size());
                for (var t : pkt.titles()) {
                    buf.writeUtf(t.id()); buf.writeUtf(t.name());
                    buf.writeUtf(t.titleText()); buf.writeUtf(t.subtitleText());
                    buf.writeInt(t.fadeIn()); buf.writeInt(t.stay()); buf.writeInt(t.fadeOut());
                }
                // Dims
                buf.writeInt(pkt.dims().size());
                for (var d : pkt.dims()) { buf.writeUtf(d.name()); buf.writeUtf(d.state()); }
                // Help requests
                buf.writeInt(pkt.helpRequests().size());
                for (var h : pkt.helpRequests()) {
                    buf.writeUUID(h.playerUUID()); buf.writeUtf(h.playerName()); buf.writeUtf(h.reason());
                }
                // Bulk commands
                buf.writeInt(pkt.bulkCommands().size());
                for (var b : pkt.bulkCommands()) {
                    buf.writeUtf(b.name());
                }
                // Bind slots
                buf.writeInt(pkt.bindSlots().size());
                pkt.bindSlots().forEach((slot, name) -> { buf.writeInt(slot); buf.writeUtf(name); });
                // Team visibility
                buf.writeBoolean(pkt.teamVisibility());
            },
            buf -> {
                int pc = buf.readInt();
                List<PlayerState> players = new ArrayList<>();
                for (int i = 0; i < pc; i++) players.add(new PlayerState(
                        buf.readUUID(), buf.readUtf(),
                        buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));

                int tc = buf.readInt();
                List<TitleEntry> titles = new ArrayList<>();
                for (int i = 0; i < tc; i++) titles.add(new TitleEntry(
                        buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                        buf.readInt(), buf.readInt(), buf.readInt()));

                int dc = buf.readInt();
                List<DimEntry> dims = new ArrayList<>();
                for (int i = 0; i < dc; i++) dims.add(new DimEntry(buf.readUtf(), buf.readUtf()));

                int hc = buf.readInt();
                List<HelpEntry> help = new ArrayList<>();
                for (int i = 0; i < hc; i++) help.add(new HelpEntry(
                        buf.readUUID(), buf.readUtf(), buf.readUtf()));

                int bc = buf.readInt();
                List<BulkEntry> bulk = new ArrayList<>();
                for (int i = 0; i < bc; i++) bulk.add(new BulkEntry(buf.readUtf()));

                int bindCount = buf.readInt();
                Map<Integer, String> binds = new LinkedHashMap<>();
                for (int i = 0; i < bindCount; i++) binds.put(buf.readInt(), buf.readUtf());

                boolean teamVis = buf.readBoolean();

                return new OpenMainMenuPacket(players, titles, dims, help, bulk, binds, teamVis);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}