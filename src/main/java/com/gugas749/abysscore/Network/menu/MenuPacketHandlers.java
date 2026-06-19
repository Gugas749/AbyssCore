package com.gugas749.abysscore.Network.menu;

import com.gugas749.abysscore.Commands.SubRegisters.ACGodCommands;
import com.gugas749.abysscore.Features.Bind.BindManager;
import com.gugas749.abysscore.Features.Blind.ACBlindManager;
import com.gugas749.abysscore.Features.Bulk.BulkCommandManager;
import com.gugas749.abysscore.Features.Dimen.ACDimensionManager;
import com.gugas749.abysscore.Features.Help.ACHelpManager;
import com.gugas749.abysscore.Features.Staff.StaffProfileManager;
import com.gugas749.abysscore.Features.title.ACTitle;
import com.gugas749.abysscore.Features.title.ACTitleManager;
import com.gugas749.abysscore.Features.Vanish.ACVanishExtras;
import com.gugas749.abysscore.Network.title.SaveTitlePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

public class MenuPacketHandlers {

    // ── Build packet ──────────────────────────────────────────────────────────

    public static OpenMainMenuPacket buildMenuPacket(ServerPlayer viewer) {
        var server = viewer.getServer();

        // Players
        List<OpenMainMenuPacket.PlayerState> players = server.getPlayerList().getPlayers().stream()
                .map(p -> new OpenMainMenuPacket.PlayerState(
                        p.getUUID(), p.getName().getString(),
                        StaffProfileManager.isStaffMode(p),
                        isVanished(p),
                        ACGodCommands.hasGodMode(p.getUUID()),
                        ACBlindManager.isBlinded(p.getUUID())
                )).toList();

        // Titles
        List<OpenMainMenuPacket.TitleEntry> titles = ACTitleManager.getAll().stream()
                .map(t -> new OpenMainMenuPacket.TitleEntry(
                        t.id, t.name, t.titleText, t.subtitleText, t.fadeIn, t.stay, t.fadeOut))
                .toList();

        // Dims — all managed + all loaded non-vanilla
        List<OpenMainMenuPacket.DimEntry> dims = new ArrayList<>();
        ACDimensionManager.getAll().forEach(d ->
                dims.add(new OpenMainMenuPacket.DimEntry(
                        "abysscore:" + d.name, d.state.name())));
        server.getAllLevels().forEach(level -> {
            String key = level.dimension().location().toString();
            if (!key.startsWith("abysscore:") && !isVanillaDim(key)) {
                dims.add(new OpenMainMenuPacket.DimEntry(key, "LOADED"));
            }
        });

        // Help requests
        List<OpenMainMenuPacket.HelpEntry> help = ACHelpManager.getAll().stream()
                .map(r -> new OpenMainMenuPacket.HelpEntry(r.playerUUID, r.playerName, r.reason))
                .toList();

        // Bulk commands
        List<OpenMainMenuPacket.BulkEntry> bulk = BulkCommandManager.getAll().stream()
                .map(b -> new OpenMainMenuPacket.BulkEntry(b.name))
                .toList();

        // Bind slots (1-9)
        Map<Integer, String> binds = new LinkedHashMap<>();
        for (int i = 1; i <= 9; i++) {
            String bound = BindManager.getBinding(viewer.getUUID(), i);
            if (bound != null && !bound.isBlank()) binds.put(i, bound);
        }

        return new OpenMainMenuPacket(players, titles, dims, help, bulk, binds,
                ACVanishExtras.isTeamVisibilityEnabled());
    }

    // ── Handle action ─────────────────────────────────────────────────────────

    public static void handleAction(MenuActionPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer op)) return;
            if (!op.hasPermissions(2)) return;
            var server = op.getServer();

            switch (packet.action()) {
                // ── Player toggles ────────────────────────────────────────────
                case TOGGLE_STAFF -> {
                    ServerPlayer t = getPlayer(server, packet.targetUUID()); if (t == null) return;
                    if (StaffProfileManager.isStaffMode(t)) StaffProfileManager.disable(t);
                    else StaffProfileManager.enable(t);
                }
                case TOGGLE_VANISH -> toggleVanish(server, packet.targetUUID());
                case TOGGLE_GOD -> {
                    UUID uid = packet.targetUUID();
                    if (ACGodCommands.hasGodMode(uid)) ACGodCommands.godModePlayers().remove(uid);
                    else ACGodCommands.godModePlayers().add(uid);
                }
                case TOGGLE_BLIND -> {
                    ServerPlayer t = getPlayer(server, packet.targetUUID()); if (t == null) return;
                    if (ACBlindManager.isBlinded(t.getUUID())) ACBlindManager.unblind(t);
                    else ACBlindManager.blind(t);
                }
                // ── Title actions ─────────────────────────────────────────────
                case DELETE_TITLE -> ACTitleManager.delete(packet.stringArg1());
                case SEND_TITLE_ALL -> ACTitleManager.get(packet.stringArg1())
                        .ifPresent(t -> ACTitleManager.sendToAll(t, server));
                case SEND_TITLE_TAG -> ACTitleManager.get(packet.stringArg1())
                        .ifPresent(t -> ACTitleManager.sendToTag(t, packet.stringArg2(), server));
                case SEND_TITLE_TEAM -> ACTitleManager.get(packet.stringArg1())
                        .ifPresent(t -> ACTitleManager.sendToTeam(t, packet.stringArg2(), server));
                case SEND_TITLE_PLAYER -> {
                    ServerPlayer t = getPlayer(server, packet.targetUUID()); if (t == null) return;
                    ACTitleManager.get(packet.stringArg1())
                            .ifPresent(title -> ACTitleManager.sendTo(title, List.of(t)));
                }
                // ── Vanish ────────────────────────────────────────────────────
                case TOGGLE_TEAM_VISIBILITY -> ACVanishExtras.toggleTeamVisibility();
                case VANISH_SHOW_TO -> {
                    UUID vanished = UUID.fromString(packet.stringArg1());
                    ACVanishExtras.showTo(vanished, packet.targetUUID(), server);
                }
                case VANISH_HIDE_FROM -> {
                    UUID vanished = UUID.fromString(packet.stringArg1());
                    ACVanishExtras.hideTo(vanished, packet.targetUUID(), server);
                }
                case VANISH_CLEAR -> {
                    UUID vanished = UUID.fromString(packet.stringArg1());
                    ACVanishExtras.clearExceptions(vanished, server);
                }
                // ── Dimen ─────────────────────────────────────────────────────
                case DIMEN_TP -> {
                    String dimKey = packet.stringArg1();
                    for (ServerLevel level : server.getAllLevels()) {
                        if (level.dimension().location().toString().equals(dimKey)) {
                            var spawn = level.getSharedSpawnPos();
                            op.teleportTo(level,
                                    spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                                    op.getYRot(), op.getXRot());
                            break;
                        }
                    }
                }
                // ── Help ──────────────────────────────────────────────────────
                case HELP_ACCEPT -> ACHelpManager.accept(op, packet.targetUUID());
                // ── Bulk ──────────────────────────────────────────────────────
                case BULK_RUN -> BulkCommandManager.get(packet.stringArg1()).ifPresent(bulk -> {
                    for (String cmd : bulk.commands) {
                        try {
                            op.getServer().getCommands().performPrefixedCommand(
                                    op.createCommandSourceStack(), cmd);
                        } catch (Exception ignored) {}
                    }
                });
                case BULK_BIND -> BindManager.setBinding(op.getUUID(),
                        packet.intArg(), packet.stringArg1());
                case BULK_UNBIND -> BindManager.setBinding(op.getUUID(), packet.intArg(), "");
            }
        });
    }

    // ── Handle save title ─────────────────────────────────────────────────────

    public static void handleSaveTitle(SaveTitlePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer op)) return;
            if (!op.hasPermissions(2)) return;
            ACTitle title = new ACTitle(
                    packet.id().isBlank() ? null : packet.id(),
                    packet.name(), packet.titleText(), packet.subtitleText(),
                    packet.fadeIn(), packet.stay(), packet.fadeOut());
            ACTitleManager.save(title);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ServerPlayer getPlayer(MinecraftServer server, UUID uuid) {
        return uuid != null ? server.getPlayerList().getPlayer(uuid) : null;
    }

    @SuppressWarnings("unchecked")
    static boolean isVanished(ServerPlayer player) {
        try {
            Class<?> utils = Class.forName("redstonedubstep.mods.vanishmod.VanishUtil");
            java.lang.reflect.Field f = utils.getDeclaredField("VANISHED_PLAYERS");
            f.setAccessible(true);
            return ((Set<UUID>) f.get(null)).contains(player.getUUID());
        } catch (Exception e) { return false; }
    }

    private static void toggleVanish(MinecraftServer server, UUID uuid) {
        ServerPlayer target = getPlayer(server, uuid); if (target == null) return;
        try {
            boolean current = isVanished(target);
            Class<?> handler = Class.forName("redstonedubstep.mods.vanishmod.VanishingHandler");
            java.lang.reflect.Method m = handler.getDeclaredMethod(
                    "updateVanishedStatus", ServerPlayer.class, boolean.class);
            m.setAccessible(true);
            m.invoke(null, target, !current);
        } catch (Exception ignored) {}
    }

    private static boolean isVanillaDim(String key) {
        return key.equals("minecraft:overworld")
                || key.equals("minecraft:the_nether")
                || key.equals("minecraft:the_end");
    }
}