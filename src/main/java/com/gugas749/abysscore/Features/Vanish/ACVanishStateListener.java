package com.gugas749.abysscore.Features.Vanish;

import com.gugas749.abysscore.Network.Vanish.VanishStateSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ACVanishStateListener {

    private static final Map<UUID, Boolean> lastKnownState = new HashMap<>();

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Send initial state
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

        // Only check every 20 ticks (1 second) to avoid overhead
        if (player.tickCount % 20 != 0) return;

        boolean currentlyVanished = isVanished(player);
        Boolean lastState = lastKnownState.get(player.getUUID());

        if (lastState == null || lastState != currentlyVanished) {
            lastKnownState.put(player.getUUID(), currentlyVanished);
            PacketDistributor.sendToPlayer(player, new VanishStateSyncPacket(currentlyVanished));
        }
    }

    private static boolean isVanished(ServerPlayer player) {
        try {
            Class<?> utils = Class.forName("RedstoneDubstep.Vanishmod.VanishUtils");
            java.lang.reflect.Method m = utils.getMethod("isVanished", ServerPlayer.class);
            return (boolean) m.invoke(null, player);
        } catch (Exception e) {
            return false;
        }
    }
}
