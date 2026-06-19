package com.gugas749.abysscore.Features.Regions;

import com.gugas749.abysscore.Network.region.NoEntryPacket;
import com.gugas749.abysscore.Network.region.ReadyToTeleportPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ACNoEntryListener {

    // Pending teleports: UUID → exit position + yaw, waiting for client ready signal
    private record PendingTeleport(double x, double y, double z, float yaw, ServerLevel level) {}
    private static final Map<UUID, PendingTeleport> pendingTeleports = new HashMap<>();

    // Cooldown: recently ejected players won't be re-checked for 3 seconds
    private static final Map<UUID, Long> ejectCooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 3000;

    // ── Server tick — detect entry ────────────────────────────────────────────

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (player.hasPermissions(2)) return;

        if (player.tickCount % 20 != 0) return;

        // Skip if already waiting for this player's ready signal
        if (pendingTeleports.containsKey(player.getUUID())) return;

        Long lastEject = ejectCooldowns.get(player.getUUID());
        if (lastEject != null && System.currentTimeMillis() - lastEject < COOLDOWN_MS) return;

        if (!(player.level() instanceof ServerLevel level)) return;

        BlockPos pos = player.blockPosition();
        String dimension = level.dimension().location().toString();
        ACRegionSavedData data = ACRegionSavedData.get(level);

        for (ACRegion region : data.regions()) {
            if (!region.contains(dimension, pos)) continue;
            if (!region.hasTag(ACBlockProtectionListener.NO_ENTRY_TAG)) continue;

            // Check filter tag — if set, players WITH this tag are allowed in
            String filterTag = region.entryFilterTag();
            if (filterTag != null && !filterTag.isBlank()) {
                if (player.getTags().contains(filterTag)) {
                    // Player has the whitelist tag — allowed to enter
                    continue;
                }
            }

            // Player should not be here — calculate exit and send blackscreen
            double[] exit = calculateExitPosition(player, region);
            float exitYaw = player.getYRot() + 180.0f;

            // Store the teleport for when client signals ready
            pendingTeleports.put(player.getUUID(),
                new PendingTeleport(exit[0], exit[1], exit[2], exitYaw, level));

            // Send blackscreen packet to client
            PacketDistributor.sendToPlayer(player,
                new NoEntryPacket(exit[0], exit[1], exit[2], exitYaw));

            ejectCooldowns.put(player.getUUID(), System.currentTimeMillis());
            break;
        }
    }

    // ── C2S: client at peak black — execute teleport now ─────────────────────

    public static void handleReadyToTeleport(ReadyToTeleportPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            PendingTeleport pending = pendingTeleports.remove(player.getUUID());
            if (pending == null) return;

            // Teleport now — client is fully black, they won't see the world change
            player.teleportTo(
                pending.level(),
                pending.x(), pending.y(), pending.z(),
                pending.yaw(), player.getXRot()
            );
        });
    }

    // ── Exit position calculation ─────────────────────────────────────────────

    private static double[] calculateExitPosition(ServerPlayer player, ACRegion region) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        double dMinX = Math.abs(px - region.minX());
        double dMaxX = Math.abs(px - region.maxX());
        double dMinZ = Math.abs(pz - region.minZ());
        double dMaxZ = Math.abs(pz - region.maxZ());

        double minDist = Math.min(Math.min(dMinX, dMaxX), Math.min(dMinZ, dMaxZ));

        double exitX = px, exitZ = pz;

        if (minDist == dMinX)      exitX = region.minX() - 2;
        else if (minDist == dMaxX) exitX = region.maxX() + 2;
        else if (minDist == dMinZ) exitZ = region.minZ() - 2;
        else                       exitZ = region.maxZ() + 2;

        double exitY = Math.max(py, region.minY() + 1);
        return new double[]{exitX, exitY, exitZ};
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    public static void onPlayerLeave(UUID uuid) {
        pendingTeleports.remove(uuid);
        ejectCooldowns.remove(uuid);
    }
}
