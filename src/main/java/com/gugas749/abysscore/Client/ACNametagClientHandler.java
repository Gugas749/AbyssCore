package com.gugas749.abysscore.Client;

import com.gugas749.abysscore.Network.Nametag.NametagSyncPacket;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public class ACNametagClientHandler {

    private static boolean canSeeNametags = false;

    // ── Packet handler ────────────────────────────────────────────────────────

    public static void handleSync(NametagSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> canSeeNametags = packet.canSeeNametags());
    }

    // ── RenderNameTagEvent ────────────────────────────────────────────────────

    @SubscribeEvent
    public void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getEntity() == net.minecraft.client.Minecraft.getInstance().player) return;

        if (!canSeeNametags) {
            event.setCanRender(TriState.FALSE);
        }
    }

    // ── Getter (for debug/status command) ────────────────────────────────────

    public static boolean canSeeNametags() {
        return canSeeNametags;
    }
}
