package com.gugas749.abysscore.Features.Status;

import com.google.gson.*;
import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Nametag.ACNametagManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ACStatusManager {

    public enum Status {
        ON  ("ON RP",  ChatFormatting.GREEN),
        OFF ("OFF RP", ChatFormatting.RED),
        AFK ("AFK",    ChatFormatting.GRAY);

        public final String display;
        public final ChatFormatting color;

        Status(String display, ChatFormatting color) {
            this.display = display;
            this.color = color;
        }

        public static Status fromString(String s) {
            return switch (s.toLowerCase()) {
                case "on", "on_rp", "onrp"   -> ON;
                case "off", "off_rp", "offrp" -> OFF;
                case "afk"                    -> AFK;
                default -> null;
            };
        }

        public MutableComponent tagComponent() {
            return Component.literal("[" + display + "]").withStyle(color);
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE =
        FMLPaths.CONFIGDIR.get().resolve("abysscore_status.json");

    private static final Map<String, Status> statuses = new LinkedHashMap<>();

    // Players who opted out of seeing status tags
    private static final Set<UUID> tagDisplayOptOut = new HashSet<>();

    // Server-wide override set by OPs — when true, normal players cannot see tags
    private static boolean serverWideHidden = false;

    // ── Load / Save ───────────────────────────────────────────────────────────

    public static void load() {
        statuses.clear();
        if (!Files.exists(CONFIG_FILE)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            if (obj != null) obj.entrySet().forEach(e -> {
                Status s = Status.fromString(e.getValue().getAsString());
                if (s != null) statuses.put(e.getKey(), s);
            });
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to load statuses: {}", e.getMessage());
        }
    }

    private static void save() {
        JsonObject obj = new JsonObject();
        statuses.forEach((k, v) -> obj.addProperty(k, v.name()));
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(obj, writer);
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to save statuses: {}", e.getMessage());
        }
    }

    // ── Status API ────────────────────────────────────────────────────────────

    public static void setStatus(ServerPlayer player, Status status) {
        statuses.put(player.getUUID().toString(), status);
        save();
        syncPlayerToAll(player);
    }

    public static void clearStatus(ServerPlayer player) {
        statuses.remove(player.getUUID().toString());
        save();
        syncPlayerToAll(player);
    }

    public static Status getStatus(UUID uuid) {
        return statuses.get(uuid.toString());
    }

    // ── Personal opt-out (any player) ─────────────────────────────────────────

    public static boolean togglePersonalDisplay(ServerPlayer player) {
        boolean nowHidden;
        if (tagDisplayOptOut.contains(player.getUUID())) {
            tagDisplayOptOut.remove(player.getUUID());
            nowHidden = false;
        } else {
            tagDisplayOptOut.add(player.getUUID());
            nowHidden = true;
        }

        // Re-sync all players' custom names to this player
        MinecraftServer server = player.getServer();
        if (server != null) {
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (!other.equals(player)) {
                    sendCustomNameTo(other, player);
                }
            }
        }
        return nowHidden;
    }

    public static boolean isPersonalDisplayHidden(UUID uuid) {
        return tagDisplayOptOut.contains(uuid);
    }

    // ── Server-wide toggle (OP only) ──────────────────────────────────────────

    public static boolean toggleServerWide(MinecraftServer server) {
        serverWideHidden = !serverWideHidden;

        // Re-sync all players to all viewers
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncPlayerToAll(player);
        }

        return serverWideHidden;
    }

    public static boolean isServerWideHidden() {
        return serverWideHidden;
    }

    // ── Join hooks ────────────────────────────────────────────────────────────

    public static void onPlayerJoin(ServerPlayer joiningPlayer) {
        syncPlayerToAll(joiningPlayer);

        MinecraftServer server = joiningPlayer.getServer();
        if (server == null) return;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (!other.equals(joiningPlayer)) {
                sendCustomNameTo(other, joiningPlayer);
            }
        }
    }

    public static void onNametagToggle(ServerPlayer op) {
        MinecraftServer server = op.getServer();
        if (server == null) return;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (!other.equals(op)) {
                sendCustomNameTo(other, op);
            }
        }
    }

    // ── Core sync logic ───────────────────────────────────────────────────────

    private static void syncPlayerToAll(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Status status = statuses.get(player.getUUID().toString());

        // Set base server-side state (used as fallback)
        if (status != null) {
            player.setCustomName(status.tagComponent());
            player.setCustomNameVisible(true);
        } else {
            player.setCustomName(null);
            player.setCustomNameVisible(false);
        }

        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            sendCustomNameTo(player, viewer);
        }
    }

    private static void sendCustomNameTo(ServerPlayer target, ServerPlayer viewer) {
        if (viewer.equals(target)) return;

        Status status = statuses.get(target.getUUID().toString());
        boolean viewerIsOp = viewer.hasPermissions(2);
        boolean viewerCanSeeName = ACNametagManager.canSeeNametags(viewer);
        boolean viewerOptedOut = tagDisplayOptOut.contains(viewer.getUUID());

        Component nameToShow;

        if (viewerIsOp && viewerCanSeeName) {
            // OP with nametags on: always sees everything
            if (status != null) {
                nameToShow = status.tagComponent()
                    .append(Component.literal(" "))
                    .append(Component.literal(target.getName().getString())
                        .withStyle(ChatFormatting.WHITE));
            } else {
                nameToShow = target.getName();
            }
        } else if (viewerOptedOut) {
            // Personal opt-out: see nothing
            nameToShow = null;
        } else if (serverWideHidden && !viewerIsOp) {
            // Server-wide hidden, non-OP: see nothing
            nameToShow = null;
        } else if (status != null) {
            // Normal player, no opt-out, tags showing: see tag only
            nameToShow = status.tagComponent();
        } else {
            // No status, not OP: see nothing
            nameToShow = null;
        }

        // Send per-viewer packet
        Component previous = target.getCustomName();
        boolean previousVisible = target.isCustomNameVisible();

        target.setCustomName(nameToShow);
        target.setCustomNameVisible(nameToShow != null);

        var nonDefault = target.getEntityData().getNonDefaultValues();
        if (nonDefault != null && !nonDefault.isEmpty()) {
            viewer.connection.send(
                new ClientboundSetEntityDataPacket(target.getId(), nonDefault)
            );
        }

        // Restore server-side state
        target.setCustomName(previous);
        target.setCustomNameVisible(previousVisible);
    }
}
