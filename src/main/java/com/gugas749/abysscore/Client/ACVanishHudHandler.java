package com.gugas749.abysscore.Client;

import com.gugas749.abysscore.Network.Vanish.VanishStateSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public class ACVanishHudHandler {

    private static boolean isVanished = false;
    private static long vanishedSince = 0;

    public static void handleSync(VanishStateSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            boolean wasVanished = isVanished;
            isVanished = packet.vanished();
            if (!wasVanished && isVanished) {
                vanishedSince = System.currentTimeMillis();
            }
        });
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (!isVanished) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        // Don't show in F3 debug or inventory screens
        if (mc.screen != null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        Component text = Component.translatable("message.abysscore.vanish.hud_warning");
        int textWidth = mc.font.width(text);

        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 52;

        long elapsed = System.currentTimeMillis() - vanishedSince;
        float pulse = (float) Math.sin(elapsed / 600.0) * 0.5f + 0.5f; // 0.0 to 1.0
        int alpha = (int) (180 + pulse * 75); // 180 to 255

        int padding = 4;
        graphics.fill(
            x - padding,
            y - padding,
            x + textWidth + padding,
            y + mc.font.lineHeight + padding,
            (alpha / 2) << 24  // half opacity background
        );

        // Red text with the calculated alpha
        int color = (alpha << 24) | 0xFF4444; // red with pulsing alpha
        graphics.drawString(mc.font, text, x, y, color, false);
    }

    public static boolean isVanished() { return isVanished; }
}
