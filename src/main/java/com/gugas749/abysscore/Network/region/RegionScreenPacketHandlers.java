package com.gugas749.abysscore.Network.region;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Regions.ACRegionSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class RegionScreenPacketHandlers {

    public static void handleRegionUpdate(SubmitRegionUpdatePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!player.hasPermissions(2)) return;

            ACRegionSavedData data = ACRegionSavedData.get(player.serverLevel());
            String name = packet.regionName();

            if (packet.delete()) {
                if (data.removeRegion(name)) {
                    player.sendSystemMessage(
                            Component.translatable("message.abysscore.region.removed", name));
                    Abysscore.LOGGER.info("[AbyssCore] {} deleted region '{}'.",
                            player.getName().getString(), name);
                }
                return;
            }

            data.getRegion(name).ifPresent(region -> {
                // Clear and re-apply tags
                new java.util.HashSet<>(region.tags()).forEach(tag ->
                        data.removeTagFromRegion(name, tag)
                );
                packet.tags().forEach(tag -> data.addTagToRegion(name, tag));

                // Apply entry filter tag
                data.setEntryFilterTag(name, packet.entryFilterTag());

                player.sendSystemMessage(
                        Component.translatable("message.abysscore.region.screen_saved", name));
                Abysscore.LOGGER.info("[AbyssCore] {} updated region '{}' — tags: {}, filter: '{}'",
                        player.getName().getString(), name, packet.tags(), packet.entryFilterTag());
            });
        });
    }
}
