package com.gugas749.abysscore.Features.Vanish;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Handles vanish-related NeoForge events.
 *
 * KEY FIX for the team visibility bug:
 * Vanilla Minecraft and Vanishmod both have logic that shows players to
 * team members. We intercept the player tracking/visibility events at
 * HIGH priority to override that behaviour for vanished players.
 */
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
    //
    // When an entity enters a player's tracking range, the server sends spawn
    // packets. We intercept EntityJoinLevelEvent to prevent vanished players
    // from being spawned for non-vanished viewers.
    //
    // NOTE: EntityJoinLevelEvent fires when an entity is added to the level,
    // not specifically for tracking range. The actual per-player packet sending
    // is handled in ACVanishManager.hideFromPlayer via direct packet dispatch.
    // This handler ensures the state is consistent when players respawn/change dim.

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer vanishedPlayer)) return;
        if (!ACVanishManager.isVanished(vanishedPlayer)) return;
        if (vanishedPlayer.level().isClientSide()) return;

        // Re-hide from all non-vanished players after dimension change / respawn
        // (Small delay via server task to ensure the player entity is fully loaded)
        vanishedPlayer.getServer().execute(() -> {
            for (ServerPlayer other : vanishedPlayer.getServer().getPlayerList().getPlayers()) {
                if (other.getUUID().equals(vanishedPlayer.getUUID())) continue;
                if (!ACVanishManager.isVanished(other)) {
                    ACVanishManager.hideFromPlayer(vanishedPlayer, other);
                }
            }
        });
    }

    // ── Chat — vanished players' chat is visible only to other vanished ───────
    //
    // We do NOT suppress chat by default since staff may need to talk.
    // If you want vanished-only chat, that's a separate feature.
    // The current implementation only hides the player entity and tab list entry.
}
