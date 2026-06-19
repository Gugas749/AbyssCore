package com.gugas749.abysscore.Features.Vanish;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Network.Vanish.VanishStateSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Field;
import java.util.*;

public class ACVanishStateListener {

    private static final Map<UUID, Boolean> lastKnownState = new HashMap<>();

    // Cached reflection references — resolved once on first use
    private static Field vanishedPlayersField = null;
    private static boolean reflectionFailed = false;

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean vanished = isVanished(player);
        lastKnownState.put(player.getUUID(), vanished);
        PacketDistributor.sendToPlayer(player, new VanishStateSyncPacket(vanished));
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        lastKnownState.remove(player.getUUID());
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (player.tickCount % 20 != 0) return;

        boolean currentlyVanished = isVanished(player);
        Boolean lastState = lastKnownState.get(player.getUUID());

        if (lastState == null || lastState != currentlyVanished) {
            lastKnownState.put(player.getUUID(), currentlyVanished);
            PacketDistributor.sendToPlayer(player, new VanishStateSyncPacket(currentlyVanished));
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean isVanished(ServerPlayer player) {
        if (reflectionFailed) return false;

        try {
            if (vanishedPlayersField == null) {
                Class<?> vanishUtil = Class.forName("redstonedubstep.mods.vanishmod.VanishUtil");
                vanishedPlayersField = vanishUtil.getDeclaredField("VANISHED_PLAYERS");
                vanishedPlayersField.setAccessible(true);
                Abysscore.LOGGER.info("[AbyssCore] VanishUtil.VANISHED_PLAYERS field resolved.");
            }
            Set<UUID> vanished = (Set<UUID>) vanishedPlayersField.get(null);
            return vanished != null && vanished.contains(player.getUUID());
        } catch (Exception e) {
            reflectionFailed = true;
            Abysscore.LOGGER.warn("[AbyssCore] Could not access VanishUtil.VANISHED_PLAYERS: {}. HUD warning disabled.", e.getMessage());
            return false;
        }
    }
}
