package com.gugas749.abysscore.Features.Vanish;

import com.gugas749.abysscore.Abysscore;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;

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

//     @EventBusSubscriber
//     public class ACVoicechatPlugin implements VoicechatServerPlugin {
//
//         @Override
//         public void initialize(VoicechatServerApi api) {
//             api.registerServerEventHandler(new ACVoicechatEventHandler());
//         }
//
//         @Override
//         public String getPluginId() { return "abysscore_vanish"; }
//     }
//
//     public class ACVoicechatEventHandler implements ServerEventHandler {
//
//         @EventSubscribe
//         public void onPlayerConnected(PlayerConnectedEvent event) {
//             // When a player connects, check if they should be hidden
//         }
//
//         @EventSubscribe
//         public void onVoicePacket(MicrophonePacketEvent event) {
//             // Check if sender is vanished
//             UUID senderUUID = event.getSenderConnection().getPlayer().getUuid();
//             if (ACVanishManager.isVanished(senderUUID)) {
//                 // Get the connection list and filter to only vanished receivers
//                 // event.setReceivers(onlyVanishedPlayers);
//             }
//         }
//     }
}
