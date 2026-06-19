package com.gugas749.abysscore.Features.Blind;

import com.gugas749.abysscore.Network.Blind.BlindSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ACBlindManager {

    private static final Set<UUID> blindedPlayers = new HashSet<>();

    public static void blind(ServerPlayer player) {
        blindedPlayers.add(player.getUUID());
        PacketDistributor.sendToPlayer(player, new BlindSyncPacket(true));
    }

    public static void unblind(ServerPlayer player) {
        blindedPlayers.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new BlindSyncPacket(false));
    }

    public static boolean isBlinded(UUID uuid) {
        return blindedPlayers.contains(uuid);
    }

    /** Called on player join — re-syncs their blind state. */
    public static void onPlayerJoin(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
            new BlindSyncPacket(blindedPlayers.contains(player.getUUID())));
    }

    /** Called on player leave — keep state so it persists on reconnect. */
    // We intentionally do NOT remove on leave so the blind persists.

    public static Set<UUID> getBlindedPlayers() {
        return Collections.unmodifiableSet(blindedPlayers);
    }
}
