package com.gugas749.abysscore.Client.screens;

import com.gugas749.abysscore.Network.Blind.BlindSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public class BlindScreen {

    private static boolean blinded = false;

    public static void handleSync(BlindSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> blinded = packet.blinded());
    }

    public static boolean isBlinded() { return blinded; }

    @SubscribeEvent
    public void onRenderGuiPre(RenderGuiEvent.Pre event) {
        renderBlack(event.getGuiGraphics());
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        renderBlack(event.getGuiGraphics());
    }

    private void renderBlack(GuiGraphics g) {
        if (!blinded) return;
        if (Minecraft.getInstance().player == null) return;
        Minecraft mc = Minecraft.getInstance();
        g.fill(0, 0,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(),
                0xFF000000
        );
    }
}
