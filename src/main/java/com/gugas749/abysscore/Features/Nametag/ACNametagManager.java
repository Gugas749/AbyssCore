package com.gugas749.abysscore.Features.Nametag;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Status.ACStatusManager;
import com.gugas749.abysscore.Network.Nametag.NametagSyncPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ACNametagManager {

    // OPs who have toggled nametags OFF
    private static final Set<UUID> opsWithHiddenTags = new HashSet<>();

    // ── API ───────────────────────────────────────────────────────────────────

    public static boolean canSeeNametags(ServerPlayer player) {
        if (!player.hasPermissions(2)) return false;
        return !opsWithHiddenTags.contains(player.getUUID());
    }

    public static boolean toggle(ServerPlayer op) {
        boolean nowHidden;
        if (opsWithHiddenTags.contains(op.getUUID())) {
            opsWithHiddenTags.remove(op.getUUID());
            nowHidden = false;
        } else {
            opsWithHiddenTags.add(op.getUUID());
            nowHidden = true;
        }

        ACStatusManager.onNametagToggle(op);

        // re-sync the nametag sync packet
        syncNametag(op);

        Abysscore.LOGGER.debug("[AbyssCore] {} nametags: {}", op.getName().getString(),
            nowHidden ? "hidden" : "visible");
        return nowHidden;
    }

    public static boolean isHidingTags(UUID uuid) {
        return opsWithHiddenTags.contains(uuid);
    }

    // ── Nametag sync packet ───────────────────────────────────────────────────

    public static void syncNametag(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new NametagSyncPacket(
                        canSeeNametags(player),
                        ACStatusManager.isPersonalDisplayHidden(player.getUUID())
                )
        );
    }
}
