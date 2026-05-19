package com.gugas749.abysscore.Bulk;

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
 * Stores bulk command definitions in a JSON file:
 *   <game_dir>/config/abysscore_bulk_commands.json
 */
public class BulkCommandManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_PATH =
        FMLPaths.CONFIGDIR.get().resolve("abysscore_bulk_commands.json");

    // name → BulkCommandData
    private static final Map<String, BulkCommandData> commands = new LinkedHashMap<>();

    // ── Load / Save ──────────────────────────────────────────────────────────

    public static void load() {
        commands.clear();
        if (!Files.exists(SAVE_PATH)) {
            Abysscore.LOGGER.info("[AbyssCore] No bulk commands file found, starting fresh.");
            return;
        }
        try (Reader reader = Files.newBufferedReader(SAVE_PATH)) {
            Type listType = new TypeToken<List<BulkCommandData>>() {}.getType();
            List<BulkCommandData> list = GSON.fromJson(reader, listType);
            if (list != null) {
                for (BulkCommandData cmd : list) {
                    commands.put(cmd.name.toLowerCase(), cmd);
                }
            }
            Abysscore.LOGGER.info("[AbyssCore] Loaded {} bulk command(s).", commands.size());
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to load bulk commands: {}", e.getMessage());
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(SAVE_PATH)) {
            GSON.toJson(new ArrayList<>(commands.values()), writer);
            Abysscore.LOGGER.debug("[AbyssCore] Bulk commands saved.");
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to save bulk commands: {}", e.getMessage());
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public static boolean add(BulkCommandData data) {
        if (commands.containsKey(data.name.toLowerCase())) return false;
        commands.put(data.name.toLowerCase(), data);
        save();
        return true;
    }

    public static boolean remove(String name) {
        boolean removed = commands.remove(name.toLowerCase()) != null;
        if (removed) save();
        return removed;
    }

    public static Optional<BulkCommandData> get(String name) {
        return Optional.ofNullable(commands.get(name.toLowerCase()));
    }

    public static Collection<BulkCommandData> getAll() {
        return Collections.unmodifiableCollection(commands.values());
    }

    public static Set<String> getNames() {
        return Collections.unmodifiableSet(commands.keySet());
    }
}
