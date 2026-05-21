package com.gugas749.abysscore.Features.Vanish;

import com.gugas749.abysscore.Abysscore;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class ACVanishManager {

    private static final Set<UUID> vanishedPlayers = new HashSet<>();

    // ── State ─────────────────────────────────────────────────────────────────

    public static boolean isVanished(UUID uuid) {
        return vanishedPlayers.contains(uuid);
    }

    public static boolean isVanished(ServerPlayer player) {
        return vanishedPlayers.contains(player.getUUID());
    }

    public static Set<UUID> getVanishedPlayers() {
        return Collections.unmodifiableSet(vanishedPlayers);
    }

    // ── Vanish ────────────────────────────────────────────────────────────────

    public static void vanish(ServerPlayer player) {
        if (vanishedPlayers.contains(player.getUUID())) return;
        vanishedPlayers.add(player.getUUID());

        // Hide from all non-vanished players
        for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
            if (other.getUUID().equals(player.getUUID())) continue;
            if (isVanished(other)) continue; // other vanished players keep seeing them
            hideFromPlayer(player, other);
        }

        // Send fake quit message to non-vanished players
        // FIX: canSendChatMessage() doesn't exist — just send directly
        Component leaveMsg = Component.translatable("multiplayer.player.left", player.getDisplayName());
        for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
            if (!isVanished(other) && !other.getUUID().equals(player.getUUID())) {
                other.sendSystemMessage(leaveMsg);
            }
        }

        Abysscore.LOGGER.info("[AbyssCore] {} is now vanished.", player.getName().getString());
    }

    // ── Unvanish ──────────────────────────────────────────────────────────────

    public static void unvanish(ServerPlayer player) {
        if (!vanishedPlayers.contains(player.getUUID())) return;
        vanishedPlayers.remove(player.getUUID());

        // Show to all non-vanished players
        for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
            if (other.getUUID().equals(player.getUUID())) continue;
            if (isVanished(other)) continue;
            showToPlayer(player, other);
        }

        // Send fake join message to non-vanished players
        Component joinMsg = Component.translatable("multiplayer.player.joined", player.getDisplayName());
        for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
            if (!isVanished(other) && !other.getUUID().equals(player.getUUID())) {
                other.sendSystemMessage(joinMsg);
            }
        }

        Abysscore.LOGGER.info("[AbyssCore] {} is no longer vanished.", player.getName().getString());
    }

    // ── Join / Leave hooks ────────────────────────────────────────────────────

    public static void onPlayerJoin(ServerPlayer joiningPlayer) {
        // Hide all vanished players from the joining player
        for (UUID vanishedUUID : vanishedPlayers) {
            ServerPlayer vanished = joiningPlayer.getServer().getPlayerList().getPlayer(vanishedUUID);
            if (vanished != null) {
                hideFromPlayer(vanished, joiningPlayer);
            }
        }
    }

    public static void onPlayerLeave(ServerPlayer player) {
        vanishedPlayers.remove(player.getUUID());
    }

    // ── Packet helpers ────────────────────────────────────────────────────────

    public static void hideFromPlayer(ServerPlayer target, ServerPlayer viewer) {
        viewer.connection.send(
                new ClientboundRemoveEntitiesPacket(target.getId())
        );
        viewer.connection.send(
                new ClientboundPlayerInfoRemovePacket(List.of(target.getUUID()))
        );
    }

    public static void showToPlayer(ServerPlayer target, ServerPlayer viewer) {
        // Re-add to tab list
        viewer.connection.send(
                ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(target))
        );
        // Re-spawn the entity
        viewer.connection.send(
                new ClientboundAddEntityPacket(target, 0, target.blockPosition())
        );

        var nonDefault = target.getEntityData().getNonDefaultValues();
        if (nonDefault != null && !nonDefault.isEmpty()) {
            viewer.connection.send(
                    new ClientboundSetEntityDataPacket(target.getId(), nonDefault)
            );
        }

        viewer.connection.send(
                new ClientboundSetEquipmentPacket(
                        target.getId(),
                        target.getAllSlots()
                                .spliterator()
                                .trySplit() != null
                                ? buildEquipmentList(target)
                                : List.of()
                )
        );
    }

    // ── Toggle ────────────────────────────────────────────────────────────────

    public static boolean toggle(ServerPlayer player) {
        if (isVanished(player)) {
            unvanish(player);
            return false;
        } else {
            vanish(player);
            return true;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static List<Pair<EquipmentSlot, ItemStack>>
    buildEquipmentList(ServerPlayer player) {
        List<Pair<EquipmentSlot, ItemStack>> list = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                list.add(Pair.of(slot, stack));
            }
        }
        return list;
    }
}

