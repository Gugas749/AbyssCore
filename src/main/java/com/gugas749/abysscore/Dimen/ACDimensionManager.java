package com.gugas749.abysscore.Dimen;

import com.google.gson.*;
import com.gugas749.abysscore.Abysscore;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * STORAGE:
 *   config/abysscore_dimensions.json - registry of all managed dims and their state
 * DATAPACK:
 *   <world>/datapacks/abysscore_dims/data/abysscore/dimension/<name>.json
 *   <world>/datapacks/abysscore_dims/data/abysscore/worldgen/noise_settings/<name>.json
 *   <world>/datapacks/abysscore_dims/pack.mcmeta
 */
public class ACDimensionManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_FILE =
        FMLPaths.CONFIGDIR.get().resolve("abysscore_dimensions.json");

    // name → data
    private static final Map<String, ACDimensionData> dimensions = new LinkedHashMap<>();

    // Pending remove-confirmations: player UUID → dimension name, expires after 30s
    private static final Map<UUID, PendingConfirm> pendingConfirms = new HashMap<>();

    private record PendingConfirm(String dimName, long expiresAt) {}

    // ── Load / Save registry ──────────────────────────────────────────────────

    public static void load() {
        dimensions.clear();
        if (!Files.exists(CONFIG_FILE)) {
            Abysscore.LOGGER.info("[AbyssCore] No dimension registry found, starting fresh.");
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            JsonArray array = GSON.fromJson(reader, JsonArray.class);
            if (array != null) {
                for (JsonElement el : array) {
                    ACDimensionData d = ACDimensionData.fromJson(el.getAsJsonObject());
                    dimensions.put(d.name, d);
                }
            }
            Abysscore.LOGGER.info("[AbyssCore] Loaded {} managed dimension(s).", dimensions.size());
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to load dimension registry: {}", e.getMessage());
        }
    }

    public static void save() {
        JsonArray array = new JsonArray();
        dimensions.values().forEach(d -> array.add(d.toJson()));
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(array, writer);
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to save dimension registry: {}", e.getMessage());
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public static Collection<ACDimensionData> getAll() {
        return Collections.unmodifiableCollection(dimensions.values());
    }

    public static Optional<ACDimensionData> get(String name) {
        return Optional.ofNullable(dimensions.get(name));
    }

    public static boolean exists(String name) {
        return dimensions.containsKey(name);
    }

    public static boolean create(ACDimensionData data, MinecraftServer server) {
        if (dimensions.containsKey(data.name)) return false;

        try {
            writeDatapackFiles(data, server);
            dimensions.put(data.name, data);
            save();
            Abysscore.LOGGER.info("[AbyssCore] Dimension '{}' created (pending restart).", data.name);
            return true;
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to write datapack for '{}': {}", data.name, e.getMessage());
            return false;
        }
    }

    public static boolean update(String name, ACDimensionData updated) {
        if (!dimensions.containsKey(name)) return false;
        dimensions.put(name, updated);
        save();
        return true;
    }

    public static boolean unload(String name, MinecraftServer server) {
        ACDimensionData data = dimensions.get(name);
        if (data == null) return false;
        if (data.state == ACDimensionData.State.UNLOADED) return false;

        // Eject all players from this dimension
        ejectPlayers(name, server);

        data.state = ACDimensionData.State.UNLOADED;

        // Write a "disabled" marker file next to the dimension JSON
        try {
            Path disabledMarker = getDatapackDimPath(server)
                .resolve(data.name + ".disabled");
            Files.createDirectories(disabledMarker.getParent());
            Files.writeString(disabledMarker, "disabled");
        } catch (IOException e) {
            Abysscore.LOGGER.warn("[AbyssCore] Could not write disabled marker for '{}': {}", name, e.getMessage());
        }

        save();
        Abysscore.LOGGER.info("[AbyssCore] Dimension '{}' unloaded.", name);
        return true;
    }

    public static boolean load(String name, MinecraftServer server) {
        ACDimensionData data = dimensions.get(name);
        if (data == null) return false;
        if (data.state != ACDimensionData.State.UNLOADED) return false;

        try {
            Path disabledMarker = getDatapackDimPath(server)
                .resolve(data.name + ".disabled");
            Files.deleteIfExists(disabledMarker);
        } catch (IOException e) {
            Abysscore.LOGGER.warn("[AbyssCore] Could not remove disabled marker for '{}': {}", name, e.getMessage());
        }

        data.state = ACDimensionData.State.PENDING_CREATE; // treated as "pending load on restart"
        save();
        Abysscore.LOGGER.info("[AbyssCore] Dimension '{}' marked for load on next restart.", name);
        return true;
    }

    public static void startRemoveConfirm(UUID playerUUID, String dimName) {
        pendingConfirms.put(playerUUID, new PendingConfirm(
            dimName, System.currentTimeMillis() + 30_000
        ));
    }

    public static boolean hasPendingConfirm(UUID playerUUID, String dimName) {
        PendingConfirm confirm = pendingConfirms.get(playerUUID);
        if (confirm == null) return false;
        if (!confirm.dimName().equals(dimName)) return false;
        if (System.currentTimeMillis() > confirm.expiresAt()) {
            pendingConfirms.remove(playerUUID);
            return false;
        }
        return true;
    }

    public static boolean confirmRemove(String name, MinecraftServer server) {
        ACDimensionData data = dimensions.get(name);
        if (data == null) return false;

        // Eject players first
        ejectPlayers(name, server);

        // Delete the datapack JSON so it won't register on next restart
        try {
            Path dimJson = getDatapackDimPath(server).resolve(data.name + ".json");
            Files.deleteIfExists(dimJson);
            Path disabledMarker = getDatapackDimPath(server).resolve(data.name + ".disabled");
            Files.deleteIfExists(disabledMarker);
        } catch (IOException e) {
            Abysscore.LOGGER.warn("[AbyssCore] Could not delete datapack file for '{}': {}", name, e.getMessage());
        }

        data.state = ACDimensionData.State.PENDING_REMOVE;
        save();
        Abysscore.LOGGER.info("[AbyssCore] Dimension '{}' marked for removal on next restart.", name);
        return true;
    }

    // ── On-startup cleanup ────────────────────────────────────────────────────

    public static void onServerStarted(MinecraftServer server) {
        boolean changed = false;

        List<String> toRemove = new ArrayList<>();
        for (ACDimensionData data : dimensions.values()) {
            if (data.state == ACDimensionData.State.PENDING_REMOVE) {
                // Check if the dim is actually gone from the server
                boolean stillLoaded = isLoadedOnServer(data.name, server);
                if (!stillLoaded) {
                    toRemove.add(data.name);
                    changed = true;
                }
            } else if (data.state == ACDimensionData.State.PENDING_CREATE) {
                // Check if it actually loaded
                if (isLoadedOnServer(data.name, server)) {
                    data.state = ACDimensionData.State.LOADED;
                    changed = true;
                }
            }
        }

        toRemove.forEach(dimensions::remove);
        if (changed) save();
    }

    // ── Datapack writing ──────────────────────────────────────────────────────

    private static void writeDatapackFiles(ACDimensionData data, MinecraftServer server) throws IOException {
        Path datapackRoot = getDatapackRoot(server);

        // pack.mcmeta
        Path packMeta = datapackRoot.resolve("pack.mcmeta");
        if (!Files.exists(packMeta)) {
            Files.createDirectories(packMeta.getParent());
            Files.writeString(packMeta, """
                {
                  "pack": {
                    "pack_format": 48,
                    "description": "AbyssCore managed dimensions"
                  }
                }
                """);
        }

        // Dimension JSON
        Path dimPath = getDatapackDimPath(server);
        Files.createDirectories(dimPath);
        Path dimFile = dimPath.resolve(data.name + ".json");
        Files.writeString(dimFile, buildDimensionJson(data));

        Abysscore.LOGGER.info("[AbyssCore] Wrote datapack files for dimension '{}'.", data.name);
    }

    private static String buildDimensionJson(ACDimensionData data) {
        return switch (data.style) {
            case VOID -> """
                {
                  "type": "minecraft:overworld",
                  "generator": {
                    "type": "minecraft:flat",
                    "settings": {
                      "biome": "minecraft:the_void",
                      "layers": [],
                      "structure_overrides": [],
                      "features": false,
                      "lakes": false
                    }
                  }
                }
                """;

            case SUPERFLAT -> """
                {
                  "type": "minecraft:overworld",
                  "generator": {
                    "type": "minecraft:flat",
                    "settings": {
                      "biome": "minecraft:plains",
                      "layers": [
                        { "block": "minecraft:bedrock",   "height": 1 },
                        { "block": "minecraft:dirt",      "height": 3 },
                        { "block": "minecraft:grass_block","height": 1 }
                      ],
                      "structure_overrides": ["minecraft:villages"],
                      "features": %s,
                      "lakes": false
                    }
                  }
                }
                """.formatted(data.generateStructures);

            case NORMAL -> """
                {
                  "type": "minecraft:overworld",
                  "generator": {
                    "type": "minecraft:noise",
                    "seed": %d,
                    "settings": "minecraft:overworld",
                    "biome_source": {
                      "type": "minecraft:multi_noise",
                      "preset": "minecraft:overworld"
                    }
                  }
                }
                """.formatted(data.seed);
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void ejectPlayers(String dimName, MinecraftServer server) {
        String fullKey = "abysscore:" + dimName;
        server.getAllLevels().forEach(level -> {
            if (level.dimension().location().toString().equals(fullKey)) {
                level.players().forEach(player -> {
                    // Teleport to overworld spawn
                    net.minecraft.server.level.ServerLevel overworld = server.overworld();
                    player.teleportTo(
                        overworld,
                        overworld.getSharedSpawnPos().getX(),
                        overworld.getSharedSpawnPos().getY(),
                        overworld.getSharedSpawnPos().getZ(),
                        player.getYRot(),
                        player.getXRot()
                    );
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable(
                            "message.abysscore.dimen.ejected", dimName
                        )
                    );
                });
            }
        });
    }

    private static boolean isLoadedOnServer(String dimName, MinecraftServer server) {
        String fullKey = "abysscore:" + dimName;
        for (var level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(fullKey)) return true;
        }
        return false;
    }

    private static Path getDatapackRoot(MinecraftServer server) {
        return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR)
            .resolve("abysscore_dims");
    }

    private static Path getDatapackDimPath(MinecraftServer server) {
        return getDatapackRoot(server)
            .resolve("data")
            .resolve("abysscore")
            .resolve("dimension");
    }
}
