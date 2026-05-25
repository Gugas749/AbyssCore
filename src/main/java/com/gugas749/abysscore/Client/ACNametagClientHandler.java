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
    private static boolean hideAllTags    = false;

    public static void handleSync(NametagSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            canSeeNametags = packet.canSeeNametags();
            hideAllTags    = packet.hideAllTags();
        });
    }

    @SubscribeEvent
    public void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getEntity() == net.minecraft.client.Minecraft.getInstance().player) return;

        // Personal hide: block ALL tags including status tags
        if (hideAllTags) {
            event.setCanRender(TriState.FALSE);
            return;
        }

        // Not personal hide — apply normal nametag rules
        if (!canSeeNametags) {
            // Allow status tags ([TAG] format), block plain names
            var customName = event.getEntity().getCustomName();
            if (customName == null || !customName.getString().startsWith("[")) {
                event.setCanRender(TriState.FALSE);
            }
        }
        // canSeeNametags = true: render everything normally
    }

    public static boolean canSeeNametags() { return canSeeNametags; }
    public static boolean hideAllTags()    { return hideAllTags; }
}
