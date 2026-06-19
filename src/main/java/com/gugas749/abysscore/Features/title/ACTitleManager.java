package com.gugas749.abysscore.Features.title;

import com.google.gson.*;
import com.gugas749.abysscore.Abysscore;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ACTitleManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE =
        FMLPaths.CONFIGDIR.get().resolve("abysscore_titles.json");

    // id → title
    private static final Map<String, ACTitle> titles = new LinkedHashMap<>();

    // ── Load / Save ───────────────────────────────────────────────────────────

    public static void load() {
        titles.clear();
        if (!Files.exists(CONFIG_FILE)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            JsonArray arr = GSON.fromJson(reader, JsonArray.class);
            if (arr != null) arr.forEach(el -> {
                ACTitle t = ACTitle.fromJson(el.getAsJsonObject());
                titles.put(t.id, t);
            });
            Abysscore.LOGGER.info("[AbyssCore] Loaded {} title(s).", titles.size());
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to load titles: {}", e.getMessage());
        }
    }

    public static void save() {
        JsonArray arr = new JsonArray();
        titles.values().forEach(t -> arr.add(t.toJson()));
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(arr, writer);
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to save titles: {}", e.getMessage());
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public static Collection<ACTitle> getAll() {
        return Collections.unmodifiableCollection(titles.values());
    }

    public static Optional<ACTitle> get(String id) {
        return Optional.ofNullable(titles.get(id));
    }

    public static void save(ACTitle title) {
        if (title.id == null || title.id.isBlank()) {
            title.id = UUID.randomUUID().toString();
        }
        titles.put(title.id, title);
        save();
    }

    public static boolean delete(String id) {
        if (titles.remove(id) == null) return false;
        save();
        return true;
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    /**
     * Sends a title to a list of players.
     */
    public static void sendTo(ACTitle title, List<ServerPlayer> targets) {
        Component titleComponent    = Component.literal(applyColors(title.titleText));
        Component subtitleComponent = title.subtitleText != null && !title.subtitleText.isBlank()
            ? Component.literal(applyColors(title.subtitleText))
            : Component.empty();

        for (ServerPlayer player : targets) {
            player.connection.send(
                new ClientboundSetTitlesAnimationPacket(title.fadeIn, title.stay, title.fadeOut));
            player.connection.send(new ClientboundSetTitleTextPacket(titleComponent));
            if (!title.subtitleText.isBlank()) {
                player.connection.send(new ClientboundSetSubtitleTextPacket(subtitleComponent));
            }
        }
    }

    /**
     * Sends to all online players.
     */
    public static void sendToAll(ACTitle title, MinecraftServer server) {
        sendTo(title, server.getPlayerList().getPlayers());
    }

    /**
     * Sends to all players with a specific scoreboard tag.
     */
    public static void sendToTag(ACTitle title, String tag, MinecraftServer server) {
        List<ServerPlayer> targets = server.getPlayerList().getPlayers().stream()
            .filter(p -> p.getTags().contains(tag))
            .toList();
        sendTo(title, targets);
    }

    /**
     * Sends to all players in a specific scoreboard team.
     */
    public static void sendToTeam(ACTitle title, String teamName, MinecraftServer server) {
        List<ServerPlayer> targets = server.getPlayerList().getPlayers().stream()
            .filter(p -> p.getTeam() != null && p.getTeam().getName().equals(teamName))
            .toList();
        sendTo(title, targets);
    }

    // ── Color helper ──────────────────────────────────────────────────────────

    /** Converts & color codes to § so they render in titles. */
    private static String applyColors(String text) {
        return text.replace('&', '\u00a7');
    }
}
