package com.gugas749.abysscore.Staff;

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
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

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
            "current_impulse_context_reset_grace_time",
            "playerGameType",
            "previousPlayerGameType"
    );

    // ── NBT keys used internally to store compat mod data ────────────────────
    private static final String CURIOS_KEY       = "CuriosData";
    private static final String ACCESSORIES_KEY  = "AccessoriesData";
    private static final String COS_ARMOR_KEY    = "CosmeticArmorData";

    // =========================================================================
    // Public API
    // =========================================================================

    /** Returns true if the player currently has staff mode active. */
    public static boolean isStaffMode(ServerPlayer player) {
        return Files.exists(activePath(player.getUUID()));
    }

    /**
     * Enables staff mode for the player:
     * 1. Saves their current survival profile to disk.
     * 2. Loads (or creates) their staff profile from disk.
     * 3. Writes an "active" marker file.
     */
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
            writeActiveMarker(uuid);

            Abysscore.LOGGER.info("[AbyssCore] Staff mode ENABLED for {}", player.getScoreboardName());
            return true;

        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to enable staff mode for {}: {}",
                    player.getScoreboardName(), e.getMessage());
            return false;
        }
    }

    /**
     * Disables staff mode for the player:
     * 1. Saves their current staff profile to disk.
     * 2. Restores their survival profile from disk.
     * 3. Removes the "active" marker file.
     */
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

            saveProfile(player, profilePath(uuid, "staff"));

            applyProfile(player, NbtIo.readCompressed(survivalPath, NbtAccounter.unlimitedHeap()));

            Files.deleteIfExists(activePath(uuid));

            Abysscore.LOGGER.info("[AbyssCore] Staff mode DISABLED for {}", player.getScoreboardName());
            return true;

        } catch (IOException e) {
            Abysscore.LOGGER.error("[AbyssCore] Failed to disable staff mode for {}: {}",
                    player.getScoreboardName(), e.getMessage());
            return false;
        }
    }

    /**
     * Deletes the staff profile .dat for a player.
     * Useful if their staff profile gets corrupted or needs resetting.
     * Does NOT affect the survival profile or active state.
     */
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

    /**
     * Saves the player's current state to a .dat file at the given path.
     * Includes vanilla profile keys + all installed compat mod inventories.
     */
    private static void saveProfile(ServerPlayer player, Path path) throws IOException {
        CompoundTag fullTag = player.saveWithoutId(new CompoundTag());
        CompoundTag profileTag = new CompoundTag();

        // ── Vanilla keys ──────────────────────────────────────────────────────
        for (String key : PROFILE_KEYS) {
            if (fullTag.contains(key)) {
                profileTag.put(key, fullTag.get(key).copy());
            }
        }

        // ── Curios ────────────────────────────────────────────────────────────
        if (ModList.get().isLoaded("curios")) {
            saveCurios(player, profileTag);
        }

        // ── Accessories ───────────────────────────────────────────────────────
        if (ModList.get().isLoaded("accessories")) {
            saveAccessories(fullTag, profileTag);
        }

        // ── Cosmetic Armor Reworked ───────────────────────────────────────────
        if (ModList.get().isLoaded("cosmeticarmorreworked")) {
            saveCosmeticArmor(player, fullTag, profileTag);
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

    private static void saveAccessories(CompoundTag fullTag, CompoundTag profileTag) {
        try {
            if (fullTag.contains("neoforge:attachments")) {
                CompoundTag attachments = fullTag.getCompound("neoforge:attachments");
                if (attachments.contains("accessories:capability")) {
                    CompoundTag accessoriesData = attachments.getCompound("accessories:capability").copy();
                    profileTag.put(ACCESSORIES_KEY, accessoriesData);
                    Abysscore.LOGGER.debug("[AbyssCore] Saved Accessories inventory.");
                }
            }
        } catch (Exception e) {
            Abysscore.LOGGER.warn("[AbyssCore] Failed to save Accessories inventory: {}", e.getMessage());
        }
    }

    // ── Cosmetic Armor save ───────────────────────────────────────────────────

    private static void saveCosmeticArmor(ServerPlayer player, CompoundTag fullTag, CompoundTag profileTag) {
        try {
            CAStacksBase cosStacks = CosArmorAPI.getCAStacks(player.getUUID());

            if (cosStacks == null) {
                Abysscore.LOGGER.debug("[AbyssCore] No Cosmetic Armor stacks found for {}", player.getScoreboardName());
                return;
            }

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
            Abysscore.LOGGER.debug("[AbyssCore] Saved Cosmetic Armor ({} slots) for {}",
                    cosStacks.getSlots(), player.getScoreboardName());

        } catch (Exception e) {
            Abysscore.LOGGER.warn("[AbyssCore] Failed to save Cosmetic Armor inventory: {}", e.getMessage());
        }
    }

    // =========================================================================
    // Apply
    // =========================================================================

    /**
     * Applies a saved profile .dat back onto the player.
     * Restores vanilla state + all compat mod inventories.
     * Also syncs everything to the client so they see the correct state immediately.
     */
    private static void applyProfile(ServerPlayer player, CompoundTag profileTag) {

        // ── Close any open container first ────────────────────────────────────
        // Prevents item duplication bugs when the inventory changes under an open screen
        player.closeContainer();

        // ── Clear effects before loading ──────────────────────────────────────
        player.removeAllEffects();

        // ── Apply vanilla profile keys ────────────────────────────────────────
        player.readAdditionalSaveData(profileTag);
        player.loadGameTypes(profileTag);

        // ── Sync vanilla state to client ──────────────────────────────────────
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        player.onUpdateAbilities();

        // Sync health and food explicitly
        player.connection.send(new ClientboundSetHealthPacket(
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel()
        ));

        // Sync all attribute modifiers
        player.connection.send(new ClientboundUpdateAttributesPacket(
                player.getId(),
                player.getAttributes().getSyncableAttributes()
        ));

        // Sync active potion effects
        player.getActiveEffects().forEach(effect ->
                player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect, false))
        );

        // ── Curios ────────────────────────────────────────────────────────────
        if (ModList.get().isLoaded("curios") && profileTag.contains(CURIOS_KEY)) {
            applyCurios(player, profileTag);
        }

        // ── Accessories ───────────────────────────────────────────────────────
        if (ModList.get().isLoaded("accessories") && profileTag.contains(ACCESSORIES_KEY)) {
            applyAccessories(player, profileTag);
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

                // broadcastChanges after loading so the Curios GUI reflects the restored inventory immediately on the client side
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
            CompoundTag accessoriesData = profileTag.getCompound(ACCESSORIES_KEY);

            CompoundTag persistentData = player.getPersistentData();
            if (!persistentData.contains("neoforge:attachments")) {
                persistentData.put("neoforge:attachments", new CompoundTag());
            }
            persistentData.getCompound("neoforge:attachments")
                    .put("accessories:capability", accessoriesData.copy());

            // Force the player data to be reloaded so Accessories picks up the change
            player.readAdditionalSaveData(persistentData);
            player.inventoryMenu.broadcastChanges();
            Abysscore.LOGGER.debug("[AbyssCore] Applied Accessories inventory.");
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
            ListTag slotList = cosTag.getList("Slots", 10); // 10 = CompoundTag NBT type ID

            for (int slot = 0; slot < cosStacks.getSlots(); slot++) {
                cosStacks.setStackInSlot(slot, ItemStack.EMPTY);
            }

            for (int i = 0; i < slotList.size(); i++) {
                CompoundTag slotTag = slotList.getCompound(i);
                int slot = slotTag.getInt("Slot");

                if (slot < 0 || slot >= cosStacks.getSlots()) continue;

                if (slotTag.contains("Item")) {
                    ItemStack stack = ItemStack.parseOptional(
                            player.registryAccess(),
                            slotTag.getCompound("Item")
                    );
                    cosStacks.setStackInSlot(slot, stack);
                }
            }

            // Broadcast inventory changes so the client cosmetic render updates
            player.inventoryMenu.broadcastChanges();
            Abysscore.LOGGER.debug("[AbyssCore] Applied Cosmetic Armor for {}", player.getScoreboardName());

        } catch (Exception e) {
            Abysscore.LOGGER.warn("[AbyssCore] Failed to apply Cosmetic Armor inventory: {}", e.getMessage());
        }
    }

    // =========================================================================
    // Initial staff profile
    // =========================================================================

    /**
     * Creates a clean initial staff profile
     */
    private static CompoundTag createInitialStaffProfile(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();

        // Health and absorption
        tag.putFloat("Health", player.getMaxHealth());
        tag.putFloat("AbsorptionAmount", 0.0F);

        // Empty main inventory and ender chest
        tag.put("Inventory", new ListTag());
        tag.putInt("SelectedItemSlot", 0);
        tag.put("EnderItems", new ListTag());

        // XP
        tag.putFloat("XpP", 0.0F);
        tag.putInt("XpLevel", 0);
        tag.putInt("XpTotal", 0);
        tag.putInt("XpSeed", player.getRandom().nextInt());
        tag.putInt("Score", player.getScore());

        // Full food, no exhaustion
        tag.putInt("foodLevel", 20);
        tag.putInt("foodTickTimer", 0);
        tag.putFloat("foodSaturationLevel", 5.0F);
        tag.putFloat("foodExhaustionLevel", 0.0F);

        tag.put("abilities", player.saveWithoutId(new CompoundTag()).getCompound("abilities"));

        tag.putInt("playerGameType", player.gameMode.getGameModeForPlayer().getId());

        return tag;
    }

    // =========================================================================
    // File helpers
    // =========================================================================

    private static void writeActiveMarker(UUID uuid) throws IOException {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("active", true);
        NbtIo.writeCompressed(tag, activePath(uuid));
    }

    /** Path to the survival or staff .dat file: <uuid>_survival.dat / <uuid>_staff.dat */
    private static Path profilePath(UUID uuid, String profile) {
        return SAVE_DIR.resolve(uuid + "_" + profile + ".dat");
    }

    /** Path to the active marker file: <uuid>_active.dat */
    private static Path activePath(UUID uuid) {
        return SAVE_DIR.resolve(uuid + "_active.dat");
    }
}