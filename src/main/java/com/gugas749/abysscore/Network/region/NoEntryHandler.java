package com.gugas749.abysscore.Network.region;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public class NoEntryHandler {

    private static long effectStartMs = -1;
    private static boolean teleportSent = false;

    private static final long FADE_IN_MS  = 400;
    private static final long HOLD_MS     = 200;
    private static final long FADE_OUT_MS = 400;
    private static final long TOTAL_MS    = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

    public static void handlePacket(NoEntryPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            effectStartMs = System.currentTimeMillis();
            teleportSent = false;
        });
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (effectStartMs < 0) return;

        long elapsed = System.currentTimeMillis() - effectStartMs;
        if (elapsed >= TOTAL_MS) {
            effectStartMs = -1;
            teleportSent = false;
            return;
        }

        int alpha;
        if (elapsed < FADE_IN_MS) {
            alpha = (int) (255 * elapsed / FADE_IN_MS);
        } else if (elapsed < FADE_IN_MS + HOLD_MS) {
            alpha = 255;
        } else {
            long fadeElapsed = elapsed - FADE_IN_MS - HOLD_MS;
            alpha = 255 - (int) (255 * fadeElapsed / FADE_OUT_MS);
        }

        alpha = Math.max(0, Math.min(255, alpha));

        // Send the teleport signal exactly once when we first hit full black
        if (alpha == 255 && !teleportSent) {
            teleportSent = true;
            PacketDistributor.sendToServer(new ReadyToTeleportPacket());
        }

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        g.fill(0, 0,
            mc.getWindow().getGuiScaledWidth(),
            mc.getWindow().getGuiScaledHeight(),
            (alpha << 24) | 0x000000
        );
    }
}
