package com.gugas749.abysscore.Regions;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class ACBlockProtectionListener {

    public static final String NO_BUILD_TAG = "no_build";

    // -------------------------------------------------------------------------
    // Block BREAK
    // -------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        if (hasNoBuildTag(player) && isProtectedPosition(player, event.getPos())) {
            event.setCanceled(true);
            sendBlockedMessage(player);
            Abysscore.LOGGER.debug(
                    "[AbyssCore] Blocked block break by {} (has '{}' tag)",
                    player.getName().getString(), NO_BUILD_TAG
            );
        }
    }

    // -------------------------------------------------------------------------
    // Block PLACE
    // -------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (hasNoBuildTag(player) && isProtectedPosition(player, event.getPos())) {
            event.setCanceled(true);
            sendBlockedMessage(player);
            Abysscore.LOGGER.debug(
                    "[AbyssCore] Blocked block place by {} (has '{}' tag)",
                    player.getName().getString(), NO_BUILD_TAG
            );
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean hasNoBuildTag(ServerPlayer player) {
        return player.getTags().contains(NO_BUILD_TAG);
    }

    private boolean isProtectedPosition(ServerPlayer player, BlockPos pos) {
        String dimension = player.serverLevel().dimension().location().toString();
        return ACRegionSavedData.get(player.serverLevel()).isProtected(dimension, pos);
    }

    private void sendBlockedMessage(ServerPlayer player) {
        player.sendSystemMessage(
                Component.translatable("message.abysscore.no_build_blocked")
        );
    }
}
