package com.gugas749.abysscore.Features.Nametag;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ACNametagManager {

    private static final Set<UUID> opsWithHiddenTags = new HashSet<>();

    // ── API ───────────────────────────────────────────────────────────────────

    public static boolean toggle(ServerPlayer op) {
        if (opsWithHiddenTags.contains(op.getUUID())) {
            opsWithHiddenTags.remove(op.getUUID());
            Abysscore.LOGGER.debug("[AbyssCore] {} nametags: visible", op.getName().getString());
            return false; // now visible
        } else {
            opsWithHiddenTags.add(op.getUUID());
            Abysscore.LOGGER.debug("[AbyssCore] {} nametags: hidden", op.getName().getString());
            return true; // now hidden
        }
    }

    public static boolean canSeeNametags(ServerPlayer player) {
        if (!player.hasPermissions(2)) return false; // non-OP never sees
        return !opsWithHiddenTags.contains(player.getUUID()); // OP sees unless toggled off
    }

    public static boolean isHidingTags(UUID uuid) {
        return opsWithHiddenTags.contains(uuid);
    }

    public static void onPlayerLeave(ServerPlayer player) {
        // Clean up on disconnect — optional
        // opsWithHiddenTags.remove(player.getUUID());
    }
}
