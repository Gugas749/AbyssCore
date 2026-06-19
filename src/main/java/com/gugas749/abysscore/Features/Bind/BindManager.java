package com.gugas749.abysscore.Features.Bind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.gugas749.abysscore.Abysscore;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Stored in: <game_dir>/config/abysscore_binds/<uuid>.json
 * Format: { "1": "morning_routine", "3": "time set day", ... }
 */
public class BindManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path BINDS_DIR =
            FMLPaths.CONFIGDIR.get().resolve("abysscore_binds");

    // Cache: uuid → (slot → command/bulk name)
    private static final Map<UUID, Map<String, String>> cache = new HashMap<>();

    // ── Public API ───────────────────────────────────────────────────────────

    /** Returns the binding for a slot, or null if not set. */
    public static String getBinding(UUID uuid, int slot) {
        Map<String, String> binds = getOrLoad(uuid);
        return binds.get(String.valueOf(slot));
    }

    /** Sets a binding for a slot and saves to disk. */
    public static void setBinding(UUID uuid, int slot, String value) {
        Map<String, String> binds = getOrLoad(uuid);
        binds.put(String.valueOf(slot), value);
        save(uuid, binds);
    }

    /** Clears a binding for a slot and saves to disk. */
    public static void clearBinding(UUID uuid, int slot) {
        Map<String, String> binds = getOrLoad(uuid);
        binds.remove(String.valueOf(slot));
        save(uuid, binds);
    }

    /** Returns all bindings for a player as an unmodifiable map. */
    public static Map<String, String> getAllBindings(UUID uuid) {
        return Collections.unmodifiableMap(getOrLoad(uuid));
    }

    /** Evicts a player's bindings from the cache (call on player disconnect). */
    public static void evict(UUID uuid) {
        cache.remove(uuid);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private static Map<String, String> getOrLoad(UUID uuid) {
        return cache.computeIfAbsent(uuid, BindManager::load);
    }

    private static Map<String, String> load(UUID uuid) {
        Path file = playerFile(uuid);
        if (!Files.exists(file)) return new LinkedHashMap<>();

        try (Reader reader = Files.newBufferedReader(file)) {
            Type type = new TypeToken<LinkedHashMap<String, String>>() {}.getType();
            Map<String, String> result = GSON.fromJson(reader, type);
            return result != null ? result : new LinkedHashMap<>();
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to load binds for {}: {}", uuid, e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private static void save(UUID uuid, Map<String, String> binds) {
        try {
            Files.createDirectories(BINDS_DIR);
            try (Writer writer = Files.newBufferedWriter(playerFile(uuid))) {
                GSON.toJson(binds, writer);
            }
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to save binds for {}: {}", uuid, e.getMessage());
        }
    }

    private static Path playerFile(UUID uuid) {
        return BINDS_DIR.resolve(uuid + ".json");
    }
}
