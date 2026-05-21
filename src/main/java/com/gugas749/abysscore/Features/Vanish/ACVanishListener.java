package com.gugas749.abysscore.Features.Vanish;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public class ACVanishListener {

    // ── Player joins the server ───────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joiningPlayer)) return;

        // Hide all currently vanished players from the newly joining player
        ACVanishManager.onPlayerJoin(joiningPlayer);

        // If the joining player is themselves vanished (e.g. persisted state),
        // hide them from all non-vanished players
        if (ACVanishManager.isVanished(joiningPlayer)) {
            for (ServerPlayer other : joiningPlayer.getServer().getPlayerList().getPlayers()) {
                if (other.getUUID().equals(joiningPlayer.getUUID())) continue;
                if (!ACVanishManager.isVanished(other)) {
                    ACVanishManager.hideFromPlayer(joiningPlayer, other);
                }
            }
        }
    }

    // ── Player leaves the server ──────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ACVanishManager.onPlayerLeave(player);
    }

    // ── Entity tracker — fixes the team visibility bypass ────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer vanishedPlayer)) return;
        if (!ACVanishManager.isVanished(vanishedPlayer)) return;
        if (vanishedPlayer.level().isClientSide()) return;

        vanishedPlayer.getServer().execute(() -> {
            for (ServerPlayer other : vanishedPlayer.getServer().getPlayerList().getPlayers()) {
                if (other.getUUID().equals(vanishedPlayer.getUUID())) continue;
                if (!ACVanishManager.isVanished(other)) {
                    ACVanishManager.hideFromPlayer(vanishedPlayer, other);
                }
            }
        });
    }
}
