package com.gugas749.abysscore.Features.Staff;

import com.gugas749.abysscore.Abysscore;
import lain.mods.cos.api.CosArmorAPI;
import lain.mods.cos.api.inventory.CAStacksBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import top.theillusivec4.curios.api.CuriosApi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

public class StaffProfileManager {

    private static final Path SAVE_DIR = FMLPaths.CONFIGDIR.get().resolve("abysscore_staff_profiles");

    private static final Set<String> PROFILE_KEYS = Set.of(
            "Health",
            "HurtTime",
            "HurtByTimestamp",
            "DeathTime",
            "AbsorptionAmount",
            "attributes",
            "active_effects",
            "FallFlying",
            "Inventory",
            "SelectedItemSlot",
            "XpP",
            "XpLevel",
            "XpTotal",
            "XpSeed",
            "Score",
            "foodLevel",
            "foodTickTimer",
            "foodSaturationLevel",
            "foodExhaustionLevel",
            "abilities",
            "EnderItems",
            "ShoulderEntityLeft",
            "ShoulderEntityRight",
            "current_explosion_impact_pos",
            "ignore_fall_damage_from_current_explosion",
            "current_impulse_context_reset_grace_time"
    );

    private static final String CURIOS_KEY      = "CuriosData";
    private static final String ACCESSORIES_KEY = "AccessoriesData";
    private static final String COS_ARMOR_KEY   = "CosmeticArmorData";

    // ── Gamemode constants ────────────────────────────────────────────────────
    private static final GameType DEFAULT_STAFF_GAMETYPE = GameType.CREATIVE;
    private static final String ACTIVE_GAMETYPE_KEY = "staffGameType";

    // =========================================================================
    // Public API
    // =========================================================================

    public static boolean isStaffMode(ServerPlayer player) {
        return Files.exists(activePath(player.getUUID()));
    }

    public static boolean enable(ServerPlayer player) {
        UUID uuid = player.getUUID();

        try {
            Files.createDirectories(SAVE_DIR);

            saveProfile(player, profilePath(uuid, "survival"));

            Path staffPath = profilePath(uuid, "staff");
            if (!Files.exists(staffPath)) {
                NbtIo.writeCompressed(createInitialStaffProfile(player), staffPath);
                Abysscore.LOGGER.info("[AbyssCore] Created new staff profile for {}", player.getScoreboardName());
            }

            applyProfile(player, NbtIo.readCompressed(staffPath, NbtAccounter.unlimitedHeap()));

            GameType staffGameType = readStoredStaffGameType(uuid);
            player.setGameMode(staffGameType);
            Abysscore.LOGGER.debug("[AbyssCore] Set staff gamemode to {} for {}",
                    staffGameType.getName(), player.getScoreboardName());

            writeActiveMarker(uuid, staffGameType);

            Abysscore.LOGGER.info("[AbyssCore] Staff mode ENABLED for {}", player.getScoreboardName());
            return true;

        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to enable staff mode for {}: {}",
                    player.getScoreboardName(), e.getMessage());
            return false;
        }
    }

    public static boolean disable(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Path survivalPath = profilePath(uuid, "survival");

        if (!Files.exists(survivalPath)) {
            Abysscore.LOGGER.warn("[AbyssCore] No survival profile found for {}, cannot disable staff mode.",
                    player.getScoreboardName());
            return false;
        }

        try {
            Files.createDirectories(SAVE_DIR);

            GameType currentGameType = player.gameMode.getGameModeForPlayer();
            updateStoredStaffGameType(uuid, currentGameType);

            saveProfile(player, profilePath(uuid, "staff"));

            applyProfile(player, NbtIo.readCompressed(survivalPath, NbtAccounter.unlimitedHeap()));

            CompoundTag survivalTag = NbtIo.readCompressed(survivalPath, NbtAccounter.unlimitedHeap());
            if (survivalTag.contains("playerGameType")) {
                GameType survivalGameType = GameType.byId(survivalTag.getInt("playerGameType"));
                player.setGameMode(survivalGameType);
            }

            Files.deleteIfExists(activePath(uuid));

            Abysscore.LOGGER.info("[AbyssCore] Staff mode DISABLED for {}", player.getScoreboardName());
            return true;

        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to disable staff mode for {}: {}",
                    player.getScoreboardName(), e.getMessage());
            return false;
        }
    }

    public static boolean resetStaffProfile(ServerPlayer player) {
        try {
            Files.createDirectories(SAVE_DIR);
            Files.deleteIfExists(profilePath(player.getUUID(), "staff"));
            Abysscore.LOGGER.info("[AbyssCore] Staff profile reset for {}", player.getScoreboardName());
            return true;
        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to reset staff profile for {}: {}",
                    player.getScoreboardName(), e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // Save
    // =========================================================================

    private static void saveProfile(ServerPlayer player, Path path) throws IOException {
        CompoundTag fullTag = player.saveWithoutId(new CompoundTag());
        CompoundTag profileTag = new CompoundTag();

        // ── Vanilla keys ──────────────────────────────────────────────────────
        for (String key : PROFILE_KEYS) {
            if (fullTag.contains(key)) {
                profileTag.put(key, fullTag.get(key).copy());
            }
        }
        // Always persist gamemode in both profiles for restoration purposes
        if (fullTag.contains("playerGameType")) {
            profileTag.put("playerGameType", fullTag.get("playerGameType").copy());
        }
        if (fullTag.contains("previousPlayerGameType")) {
            profileTag.put("previousPlayerGameType", fullTag.get("previousPlayerGameType").copy());
        }

        // ── Curios ────────────────────────────────────────────────────────────
        if (ModList.get().isLoaded("curios")) {
            saveCurios(player, profileTag);
        }

        // ── Accessories ───────────────────────────────────────────────────────
        if (ModList.get().isLoaded("accessories")) {
            //saveAccessories(player, profileTag);
        }

        // ── Cosmetic Armor ────────────────────────────────────────────────────
        if (ModList.get().isLoaded("cosmeticarmorreworked")) {
            saveCosmeticArmor(player, profileTag);
        }

        NbtIo.writeCompressed(profileTag, path);
        Abysscore.LOGGER.debug("[AbyssCore] Saved profile to {}", path.getFileName());
    }

    // ── Curios save ───────────────────────────────────────────────────────────

    private static void saveCurios(ServerPlayer player, CompoundTag profileTag) {
        try {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                ListTag curiosTag = handler.saveInventory(true);
                profileTag.put(CURIOS_KEY, curiosTag);
                Abysscore.LOGGER.debug("[AbyssCore] Saved Curios inventory.");
            });
        } catch (Exception e) {
            Abysscore.LOGGER.warn("[AbyssCore] Failed to save Curios inventory: {}", e.getMessage());
        }
    }

    // ── Accessories save ──────────────────────────────────────────────────────

    private static void saveAccessories(ServerPlayer player, CompoundTag profileTag) {
        try {
            // Accessories stores its inventory data in the NeoForge attachment system.
            // The correct way to access it is through the player's serialized NBT,
            // specifically the "neoforge:attachments" subtag.
            // We call addAdditionalSaveData which flushes attachment state into the tag.
            CompoundTag entityTag = new CompoundTag();
            player.addAdditionalSaveData(entityTag);

            // Accessories attachment key in the serialized NBT
            if (entityTag.contains("neoforge:attachments")) {
                CompoundTag attachments = entityTag.getCompound("neoforge:attachments");

                // Try both known Accessories attachment keys across versions
                String foundKey = null;
                for (String candidate : new String[]{
                        "accessories:accessories_holder",
                        "accessories:capability",
                        "accessories:data"
                }) {
                    if (attachments.contains(candidate)) {
                        foundKey = candidate;
                        break;
                    }
                }

                if (foundKey != null) {
                    // Save the entire accessories attachment subtag
                    CompoundTag accessoriesTag = new CompoundTag();
                    accessoriesTag.put("key", net.minecraft.nbt.StringTag.valueOf(foundKey));
                    accessoriesTag.put("data", attachments.getCompound(foundKey).copy());
                    profileTag.put(ACCESSORIES_KEY, accessoriesTag);
                    Abysscore.LOGGER.debug("[AbyssCore] Saved Accessories via attachment key '{}'.", foundKey);
                } else {
                    // Log all available attachment keys to help diagnose if key changed
                    Abysscore.LOGGER.warn(
                            "[AbyssCore] Accessories attachment key not found. Available keys: {}",
                            attachments.getAllKeys()
                    );
                }
            } else {
                Abysscore.LOGGER.debug("[AbyssCore] No neoforge:attachments found on player, skipping Accessories save.");
            }

        } catch (Exception e) {
            Abysscore.LOGGER.warn("[AbyssCore] Failed to save Accessories inventory: {}", e.getMessage());
        }
    }

    // ── Cosmetic Armor save ───────────────────────────────────────────────────

    private static void saveCosmeticArmor(ServerPlayer player, CompoundTag profileTag) {
        try {
            CAStacksBase cosStacks = CosArmorAPI.getCAStacks(player.getUUID());
            if (cosStacks == null) return;

            ListTag slotList = new ListTag();
            for (int slot = 0; slot < cosStacks.getSlots(); slot++) {
                ItemStack stack = cosStacks.getStackInSlot(slot);
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", slot);
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    stack.save(player.registryAccess(), itemTag);
                    slotTag.put("Item", itemTag);
                }
                slotList.add(slotTag);
            }

            CompoundTag cosTag = new CompoundTag();
            cosTag.put("Slots", slotList);
            profileTag.put(COS_ARMOR_KEY, cosTag);
            Abysscore.LOGGER.debug("[AbyssCore] Saved Cosmetic Armor ({} slots).", cosStacks.getSlots());

        } catch (Exception e) {
            Abysscore.LOGGER.warn("[AbyssCore] Failed to save Cosmetic Armor: {}", e.getMessage());
        }
    }

    // =========================================================================
    // Apply
    // =========================================================================

    private static void applyProfile(ServerPlayer player, CompoundTag profileTag) {
        player.closeContainer();
        player.removeAllEffects();

        player.readAdditionalSaveData(profileTag);

        // Sync vanilla state to client
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        player.onUpdateAbilities();

        player.connection.send(new ClientboundSetHealthPacket(
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel()
        ));
        player.connection.send(new ClientboundUpdateAttributesPacket(
                player.getId(),
                player.getAttributes().getSyncableAttributes()
        ));
        player.getActiveEffects().forEach(effect ->
                player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect, false))
        );

        // ── Curios ────────────────────────────────────────────────────────────
        if (ModList.get().isLoaded("curios") && profileTag.contains(CURIOS_KEY)) {
            applyCurios(player, profileTag);
        }

        // ── Accessories ───────────────────────────────────────────────────────
        if (ModList.get().isLoaded("accessories") && profileTag.contains(ACCESSORIES_KEY)) {
            //applyAccessories(player, profileTag);
        }

        // ── Cosmetic Armor ────────────────────────────────────────────────────
        if (ModList.get().isLoaded("cosmeticarmorreworked") && profileTag.contains(COS_ARMOR_KEY)) {
            applyCosmeticArmor(player, profileTag);
        }

        Abysscore.LOGGER.debug("[AbyssCore] Applied profile to {}", player.getScoreboardName());
    }

    // ── Curios apply ──────────────────────────────────────────────────────────

    private static void applyCurios(ServerPlayer player, CompoundTag profileTag) {
        try {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                handler.loadInventory(profileTag.getList(CURIOS_KEY, 10));
                player.inventoryMenu.broadcastChanges();
                Abysscore.LOGGER.debug("[AbyssCore] Applied Curios inventory.");
            });
        } catch (Exception e) {
            Abysscore.LOGGER.warn("[AbyssCore] Failed to apply Curios inventory: {}", e.getMessage());
        }
    }

    // ── Accessories apply ─────────────────────────────────────────────────────

    private static void applyAccessories(ServerPlayer player, CompoundTag profileTag) {
        try {
            CompoundTag accessoriesTag = profileTag.getCompound(ACCESSORIES_KEY);
            if (accessoriesTag.isEmpty()) return;

            String key = accessoriesTag.getString("key");
            CompoundTag data = accessoriesTag.getCompound("data");

            if (key.isEmpty() || data.isEmpty()) return;

            // Apply by injecting back into the player's NBT via readAdditionalSaveData.
            // We build a minimal tag with just the attachment we want to restore.
            CompoundTag attachments = new CompoundTag();
            attachments.put(key, data.copy());

            CompoundTag wrapper = new CompoundTag();
            wrapper.put("neoforge:attachments", attachments);

            // readAdditionalSaveData will pick up the attachment and restore it
            player.readAdditionalSaveData(wrapper);

            // Force inventory sync — Accessories hooks into this to push updates to client
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();

            Abysscore.LOGGER.debug("[AbyssCore] Applied Accessories via attachment key '{}'.", key);

        } catch (Exception e) {
            Abysscore.LOGGER.warn("[AbyssCore] Failed to apply Accessories inventory: {}", e.getMessage());
        }
    }

    // ── Cosmetic Armor apply ──────────────────────────────────────────────────

    private static void applyCosmeticArmor(ServerPlayer player, CompoundTag profileTag) {
        try {
            CAStacksBase cosStacks = CosArmorAPI.getCAStacks(player.getUUID());
            if (cosStacks == null) return;

            CompoundTag cosTag = profileTag.getCompound(COS_ARMOR_KEY);
            ListTag slotList = cosTag.getList("Slots", 10);

            // Clear all slots first to avoid leftover items
            for (int slot = 0; slot < cosStacks.getSlots(); slot++) {
                cosStacks.setStackInSlot(slot, ItemStack.EMPTY);
            }

            for (int i = 0; i < slotList.size(); i++) {
                CompoundTag slotTag = slotList.getCompound(i);
                int slot = slotTag.getInt("Slot");
                if (slot < 0 || slot >= cosStacks.getSlots()) continue;
                if (slotTag.contains("Item")) {
                    cosStacks.setStackInSlot(slot, ItemStack.parseOptional(
                            player.registryAccess(), slotTag.getCompound("Item")
                    ));
                }
            }

            player.inventoryMenu.broadcastChanges();
            Abysscore.LOGGER.debug("[AbyssCore] Applied Cosmetic Armor for {}", player.getScoreboardName());

        } catch (Exception e) {
            Abysscore.LOGGER.warn("[AbyssCore] Failed to apply Cosmetic Armor: {}", e.getMessage());
        }
    }

    // =========================================================================
    // Initial staff profile
    // =========================================================================

    private static CompoundTag createInitialStaffProfile(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Health", player.getMaxHealth());
        tag.putFloat("AbsorptionAmount", 0.0F);
        tag.put("Inventory", new ListTag());
        tag.putInt("SelectedItemSlot", 0);
        tag.put("EnderItems", new ListTag());
        tag.putFloat("XpP", 0.0F);
        tag.putInt("XpLevel", 0);
        tag.putInt("XpTotal", 0);
        tag.putInt("XpSeed", player.getRandom().nextInt());
        tag.putInt("Score", player.getScore());
        tag.putInt("foodLevel", 20);
        tag.putInt("foodTickTimer", 0);
        tag.putFloat("foodSaturationLevel", 5.0F);
        tag.putFloat("foodExhaustionLevel", 0.0F);
        tag.put("abilities", player.saveWithoutId(new CompoundTag()).getCompound("abilities"));
        return tag;
    }

    // =========================================================================
    // Gamemode helpers
    // =========================================================================

    private static GameType readStoredStaffGameType(UUID uuid) {
        Path markerPath = activePath(uuid);
        if (!Files.exists(markerPath)) return DEFAULT_STAFF_GAMETYPE;
        try {
            CompoundTag marker = NbtIo.readCompressed(markerPath, NbtAccounter.unlimitedHeap());
            if (marker.contains(ACTIVE_GAMETYPE_KEY)) {
                return GameType.byId(marker.getInt(ACTIVE_GAMETYPE_KEY));
            }
        } catch (IOException e) {
            Abysscore.LOGGER.warn("[AbyssCore] Could not read staff gametype from marker: {}", e.getMessage());
        }
        return DEFAULT_STAFF_GAMETYPE;
    }

    private static void updateStoredStaffGameType(UUID uuid, GameType gameType) {
        Path markerPath = activePath(uuid);
        try {
            CompoundTag marker = Files.exists(markerPath)
                    ? NbtIo.readCompressed(markerPath, NbtAccounter.unlimitedHeap())
                    : new CompoundTag();
            marker.putBoolean("active", true);
            marker.putInt(ACTIVE_GAMETYPE_KEY, gameType.getId());
            NbtIo.writeCompressed(marker, markerPath);
        } catch (IOException e) {
            Abysscore.LOGGER.warn("[AbyssCore] Could not update staff gametype in marker: {}", e.getMessage());
        }
    }

    // =========================================================================
    // File helpers
    // =========================================================================

    private static void writeActiveMarker(UUID uuid, GameType staffGameType) throws IOException {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("active", true);
        // Store the initial staff gamemode so it persists across server restarts
        tag.putInt(ACTIVE_GAMETYPE_KEY, staffGameType.getId());
        NbtIo.writeCompressed(tag, activePath(uuid));
    }

    private static Path profilePath(UUID uuid, String profile) {
        return SAVE_DIR.resolve(uuid + "_" + profile + ".dat");
    }

    private static Path activePath(UUID uuid) {
        return SAVE_DIR.resolve(uuid + "_active.dat");
    }
}
