package com.gugas749.abysscore.Client;

import com.gugas749.abysscore.Network.Binds.KeyPressPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class ClientTickHandler {
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // Don't fire if not in-game
        if (mc.player == null || mc.level == null) return;
        // Don't fire while a screen is open
        if (mc.screen != null) return;

        for (int i = 0; i < KeyBindings.SLOT_COUNT; i++) {
            // consumeClick returns true once per physical key press
            if (KeyBindings.SLOTS[i].consumeClick()) {
                PacketDistributor.sendToServer(new KeyPressPacket(i + 1)); // slots are 1-indexed
            }
        }
    }
}
