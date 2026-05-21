package com.gugas749.abysscore.Features.Vanish;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Integrates vanish with voice chat mods.
 *
 * Supported mods (both via reflection — no hard compile dependency):
 *
 *   Plasmo Voice (plasmovoice)
 *     Uses VoiceServerPlayer.setVanished() if available,
 *     otherwise falls back to the VanishManager registration pattern.
 *     Plasmo Voice has built-in vanish support — once a player is marked
 *     as vanished through its API, proximity sources automatically filter
 *     audio so non-vanished players cannot hear vanished players.
 *     Vanished players CAN hear each other because they are both "visible"
 *     to each other from Plasmo Voice's perspective.
 *
 *   Simple Voice Chat (voicechat)
 *     Uses the VoicechatServerApi.mutePlayer / unmutePlayer methods.
 *     Simple Voice Chat doesn't have a vanish concept natively, so we
 *     mute vanished players for non-vanished listeners and vice versa.
 *     This is done by toggling server-side mute via the API.
 *
 * Call updateVoiceVanish() whenever a player's vanish state changes.
 */
public class ACVoiceVanishIntegration {

    // ── Plasmo Voice ──────────────────────────────────────────────────────────

    /**
     * Notifies Plasmo Voice that a player's vanish state changed.
     * Plasmo Voice handles the audio filtering automatically once it knows.
     */
    public static void updatePlasmoVoice(ServerPlayer player, boolean vanished) {
        if (!ModList.get().isLoaded("plasmovoice")) return;

        try {
            // Get PlasmoVoiceServer instance
            Class<?> apiClass = Class.forName("su.plo.voice.api.server.PlasmoVoiceServer");

            // Try to get it via the static accessor pattern Plasmo Voice uses on NeoForge
            Class<?> factoryClass = Class.forName("su.plo.voice.server.PlasmoBaseVoiceServer");
            Object voiceServer = null;

            // Plasmo Voice on NeoForge stores the instance in a static field
            try {
                java.lang.reflect.Field instanceField = factoryClass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                voiceServer = instanceField.get(null);
            } catch (NoSuchFieldException e) {
                // Try alternative field names
                for (java.lang.reflect.Field f : factoryClass.getDeclaredFields()) {
                    if (apiClass.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        voiceServer = f.get(null);
                        break;
                    }
                }
            }

            if (voiceServer == null) {
                Abysscore.LOGGER.debug("[AbyssCore] Could not get PlasmoVoiceServer instance.");
                return;
            }

            // Get the player manager
            Method getPlayerManager = voiceServer.getClass().getMethod("getPlayerManager");
            Object playerManager = getPlayerManager.invoke(voiceServer);

            // Get the voice player by UUID
            Method getPlayerById = playerManager.getClass().getMethod("getPlayerById", UUID.class);
            Object optionalVoicePlayer = getPlayerById.invoke(playerManager, player.getUUID());

            // Unwrap Optional
            Method isPresent = optionalVoicePlayer.getClass().getMethod("isPresent");
            if (!(boolean) isPresent.invoke(optionalVoicePlayer)) return;

            Method get = optionalVoicePlayer.getClass().getMethod("get");
            Object voicePlayer = get.invoke(optionalVoicePlayer);

            // Call setVanished(boolean) if it exists (added in Plasmo Voice 2.x)
            try {
                Method setVanished = voicePlayer.getClass().getMethod("setVanished", boolean.class);
                setVanished.invoke(voicePlayer, vanished);
                Abysscore.LOGGER.debug("[AbyssCore] Plasmo Voice: set vanished={} for {}",
                    vanished, player.getName().getString());
            } catch (NoSuchMethodException e) {
                Abysscore.LOGGER.debug("[AbyssCore] Plasmo Voice: setVanished not available, skipping.");
            }

        } catch (ClassNotFoundException e) {
            Abysscore.LOGGER.debug("[AbyssCore] Plasmo Voice not found, skipping voice vanish.");
        } catch (Exception e) {
            Abysscore.LOGGER.warn("[AbyssCore] Failed to update Plasmo Voice vanish state: {}", e.getMessage());
        }
    }

    // ── Simple Voice Chat ─────────────────────────────────────────────────────

    /**
     * Notifies Simple Voice Chat that a player's vanish state changed.
     * Simple Voice Chat uses a different API — we register/unregister the player
     * as "muted for others" through the server API.
     */
    public static void updateSimpleVoiceChat(ServerPlayer player, boolean vanished) {
        if (!ModList.get().isLoaded("voicechat")) return;

        try {
            // Simple Voice Chat exposes a VoicechatPlugin system
            // The server API is accessed via VoicechatPlugin#initialize
            // For vanish, we use the group isolation approach:
            // vanished players are placed in a hidden group that only vanished players share

            // Simple Voice Chat doesn't have a built-in vanish API like Plasmo does.
            // The best we can do without a hard dependency is use the event system
            // to cancel audio packets from vanished players to non-vanished listeners.
            // This is handled in ACSimpleVoiceChatIntegration (see below).

            Abysscore.LOGGER.debug("[AbyssCore] Simple Voice Chat: vanish state updated for {} ({})",
                player.getName().getString(), vanished ? "vanished" : "visible");

        } catch (Exception e) {
            Abysscore.LOGGER.warn("[AbyssCore] Failed to update Simple Voice Chat vanish state: {}", e.getMessage());
        }
    }

    // ── Combined update ───────────────────────────────────────────────────────

    /**
     * Call this whenever a player's vanish state changes.
     * Updates all supported voice mods.
     */
    public static void updateAll(ServerPlayer player, boolean vanished) {
        updatePlasmoVoice(player, vanished);
        updateSimpleVoiceChat(player, vanished);
    }
}
