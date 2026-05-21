package com.gugas749.abysscore.Features.Vanish;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

public class ACVoiceVanishIntegration {

    // ── Plasmo Voice ──────────────────────────────────────────────────────────

    public static void updatePlasmoVoice(ServerPlayer player, boolean vanished) {
        if (!ModList.get().isLoaded("plasmovoice")) return;

        try {
            Class<?> apiClass = Class.forName("su.plo.voice.api.server.PlasmoVoiceServer");
            Class<?> factoryClass = Class.forName("su.plo.voice.server.PlasmoBaseVoiceServer");
            Object voiceServer = null;

            try {
                java.lang.reflect.Field instanceField = factoryClass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                voiceServer = instanceField.get(null);
            } catch (NoSuchFieldException e) {
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

            Method getPlayerManager = voiceServer.getClass().getMethod("getPlayerManager");
            Object playerManager = getPlayerManager.invoke(voiceServer);

            Method getPlayerById = playerManager.getClass().getMethod("getPlayerById", UUID.class);
            Object optionalVoicePlayer = getPlayerById.invoke(playerManager, player.getUUID());

            Method isPresent = optionalVoicePlayer.getClass().getMethod("isPresent");
            if (!(boolean) isPresent.invoke(optionalVoicePlayer)) return;

            Method get = optionalVoicePlayer.getClass().getMethod("get");
            Object voicePlayer = get.invoke(optionalVoicePlayer);

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

    public static void updateSimpleVoiceChat(ServerPlayer player, boolean vanished) {
        if (!ModList.get().isLoaded("voicechat")) return;

        try {
            Abysscore.LOGGER.debug("[AbyssCore] Simple Voice Chat: vanish state updated for {} ({})",
                player.getName().getString(), vanished ? "vanished" : "visible");

        } catch (Exception e) {
            Abysscore.LOGGER.warn("[AbyssCore] Failed to update Simple Voice Chat vanish state: {}", e.getMessage());
        }
    }

    // ── Combined update ───────────────────────────────────────────────────────

    public static void updateAll(ServerPlayer player, boolean vanished) {
        updatePlasmoVoice(player, vanished);
        updateSimpleVoiceChat(player, vanished);
    }
}
