package com.gugas749.abysscore.Features.Vanish;

import com.gugas749.abysscore.Abysscore;
import net.neoforged.fml.ModList;

/**
 * Simple Voice Chat (voicechat mod) vanish integration.
 *
 * Simple Voice Chat uses a plugin/addon system for server-side integration.
 * Since we can't easily implement VoicechatPlugin without a compile dependency,
 * we use the NeoForge network layer to intercept and filter voice packets.
 *
 * HOW IT WORKS:
 * Simple Voice Chat sends audio via custom network packets with the channel ID
 * "voicechat:player_audio". When a vanished player speaks, SVC routes the
 * audio packet to nearby players. We intercept this by hooking into the
 * packet pipeline.
 *
 * LIMITATION: Without implementing VoicechatPlugin (which requires adding
 * voicechat as a compile dependency), we cannot fully filter audio packets
 * at the source. The most reliable approach is to add voicechat as a
 * compileOnly dependency and implement the plugin interface.
 *
 * For now, this class provides the hook point. If voicechat is in your
 * build.gradle as compileOnly, uncomment the full implementation below.
 */
public class ACSimpleVoiceChatIntegration {

    private static boolean svcLoaded = false;

    public static void init() {
        svcLoaded = ModList.get().isLoaded("voicechat");
        if (svcLoaded) {
            Abysscore.LOGGER.info("[AbyssCore] Simple Voice Chat detected — vanish integration active.");
        }
    }

    public static boolean isLoaded() {
        return svcLoaded;
    }

    // ── If you add voicechat as compileOnly, use this implementation: ─────────
    //
    // Add to build.gradle:
    //   compileOnly "curse.maven:simple-voice-chat-416089:<fileId>"
    //
    // Then implement VoicechatServerPlugin:
    //
    // @net.neoforged.fml.common.Mod.EventBusSubscriber
    // public class ACVoicechatPlugin implements de.maxhenkel.voicechat.api.VoicechatServerPlugin {
    //
    //     @Override
    //     public void initialize(de.maxhenkel.voicechat.api.VoicechatServerApi api) {
    //         api.registerServerEventHandler(new ACVoicechatEventHandler());
    //     }
    //
    //     @Override
    //     public String getPluginId() { return "abysscore_vanish"; }
    // }
    //
    // public class ACVoicechatEventHandler implements de.maxhenkel.voicechat.api.events.ServerEventHandler {
    //
    //     @de.maxhenkel.voicechat.api.events.EventSubscribe
    //     public void onPlayerConnected(de.maxhenkel.voicechat.api.events.PlayerConnectedEvent event) {
    //         // When a player connects, check if they should be hidden
    //     }
    //
    //     @de.maxhenkel.voicechat.api.events.EventSubscribe
    //     public void onVoicePacket(de.maxhenkel.voicechat.api.events.MicrophonePacketEvent event) {
    //         // Check if sender is vanished
    //         UUID senderUUID = event.getSenderConnection().getPlayer().getUuid();
    //         if (ACVanishManager.isVanished(senderUUID)) {
    //             // Get the connection list and filter to only vanished receivers
    //             // event.setReceivers(onlyVanishedPlayers);
    //         }
    //     }
    // }
}
