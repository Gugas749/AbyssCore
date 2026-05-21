package com.gugas749.abysscore.Features.Regions;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class ACBlockProtectionListener {

    // ── Tag constants ─────────────────────────────────────────────────────────
    public static final String NO_BUILD_TAG        = "no_build";
    public static final String NO_INTERACT_TAG     = "no_interact";
    public static final String NO_FLY_TAG          = "no_fly";
    public static final String NO_FRIENDLYFIRE_TAG = "no_friendlyfire";
    public static final String NO_HUNGER_TAG       = "no_hunger";

    // ── no_build — block break ────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (isExempt(player)) return;

        BlockPos pos = event.getPos();
        if (isRestrictedAt(player, pos, NO_BUILD_TAG)) {
            event.setCanceled(true);
            notify(player, "message.abysscore.no_build_blocked");
            Abysscore.LOGGER.debug("[AbyssCore] Blocked break by {} at {}", player.getName().getString(), pos);
        }
    }

    // ── no_build — block place ────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isExempt(player)) return;

        BlockPos pos = event.getPos();
        if (isRestrictedAt(player, pos, NO_BUILD_TAG)) {
            event.setCanceled(true);
            notify(player, "message.abysscore.no_build_blocked");
            Abysscore.LOGGER.debug("[AbyssCore] Blocked place by {} at {}", player.getName().getString(), pos);
        }
    }

    // ── no_interact — right-click block ──────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isExempt(player)) return;

        BlockPos pos = event.getPos();
        if (isRestrictedAt(player, pos, NO_INTERACT_TAG)) {
            event.setCanceled(true);
            notify(player, "message.abysscore.no_interact_blocked");
        }
    }

    // ── no_interact — right-click entity ─────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isExempt(player)) return;

        // Use the player's current position as the reference point for entity interactions
        BlockPos pos = player.blockPosition();
        if (isRestrictedAt(player, pos, NO_INTERACT_TAG)) {
            event.setCanceled(true);
            notify(player, "message.abysscore.no_interact_blocked");
        }
    }

    // ── no_fly + no_hunger — checked every server tick ────────────────────────

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (isExempt(player)) return;

        BlockPos pos = player.blockPosition();
        String dimension = getDimension(player);

        // ── no_fly ────────────────────────────────────────────────────────────
        if (isRestrictedAt(dimension, pos, player, NO_FLY_TAG)) {
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                notify(player, "message.abysscore.no_fly_blocked");
            }
            if (player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.onUpdateAbilities();
            }
        }

        // ── no_hunger ─────────────────────────────────────────────────────────
        if (isRestrictedAt(dimension, pos, player, NO_HUNGER_TAG)) {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0f);
        }
    }

    // ── no_friendlyfire ───────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker)) return;
        if (!(event.getTarget() instanceof ServerPlayer)) return;
        if (isExempt(attacker)) return;

        BlockPos pos = attacker.blockPosition();
        if (isRestrictedAt(attacker, pos, NO_FRIENDLYFIRE_TAG)) {
            event.setCanceled(true);
            notify(attacker, "message.abysscore.no_friendlyfire_blocked");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isExempt(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    private boolean isRestrictedAt(ServerPlayer player, BlockPos pos, String tag) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        String dimension = level.dimension().location().toString();
        return ACRegionSavedData.get(level).isRestrictedAt(dimension, pos, tag);
    }

    private boolean isRestrictedAt(String dimension, BlockPos pos, ServerPlayer player, String tag) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        return ACRegionSavedData.get(level).isRestrictedAt(dimension, pos, tag);
    }

    private String getDimension(ServerPlayer player) {
        return player.level().dimension().location().toString();
    }

    private void notify(Player player, String translationKey) {
        player.sendSystemMessage(Component.translatable(translationKey));
    }
}
