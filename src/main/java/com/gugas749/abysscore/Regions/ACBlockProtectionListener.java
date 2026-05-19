package com.gugas749.abysscore.Regions;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Enforces all tag-based restrictions.
 *
 * Tags (applied via /abysscore protect <player> add <tag>  OR  /tag <player> add <tag>):
 *
 *   no_build — cannot break or place blocks
 *   no_interact — cannot interact with blocks (buttons, chests, doors…) or entities
 *   no_fly — cannot use creative/elytra flight (ability stripped every tick)
 *   no_friendlyfire — cannot deal damage to other players
 *   no_hunger — hunger bar is frozen (food level changes are cancelled)
 */
public class ACBlockProtectionListener {

    // ── Tag constants ────────────────────────────────────────────────────────
    public static final String NO_BUILD_TAG        = "no_build";
    public static final String NO_INTERACT_TAG     = "no_interact";
    public static final String NO_FLY_TAG          = "no_fly";
    public static final String NO_FRIENDLYFIRE_TAG = "no_friendlyfire";
    public static final String NO_HUNGER_TAG       = "no_hunger";

    // ────────────────────────────────────────────────────────────────────────
    // ── no_build — block break ───────────────────────────────────────────────
    // ────────────────────────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.getTags().contains(NO_BUILD_TAG)) {
            event.setCanceled(true);
            notify(player, "message.abysscore.no_build_blocked");
            Abysscore.LOGGER.debug("[AbyssCore] Blocked break by {}", player.getName().getString());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // ── no_build — block place ──────────────────────────────────────────────
    // ────────────────────────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getTags().contains(NO_BUILD_TAG)) {
            event.setCanceled(true);
            notify(player, "message.abysscore.no_build_blocked");
            Abysscore.LOGGER.debug("[AbyssCore] Blocked place by {}", player.getName().getString());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // ── no_interact — right-click on block (buttons, chests, doors…) ────────
    // ────────────────────────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getTags().contains(NO_INTERACT_TAG)) {
            event.setCanceled(true);
            notify(player, "message.abysscore.no_interact_blocked");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // ── no_interact — right-click on entity (villagers, item frames…) ───────
    // ────────────────────────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getTags().contains(NO_INTERACT_TAG)) {
            event.setCanceled(true);
            notify(player, "message.abysscore.no_interact_blocked");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // ── no_fly — strip flight every server tick ──────────────────────────────
    // Use POST tick so the check runs after the player's own ability updates.
    // ────────────────────────────────────────────────────────────────────────
    // ── no_hunger — freeze hunger by resetting food level every tick ────────
    // Reset the food data on each tick to keep it frozen. ────────────────────
    // ────────────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        // ── no_fly ───────────────────────────────────────────────────────────
        if (player.getTags().contains(NO_FLY_TAG)) {
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

        // ── no_hunger ────────────────────────────────────────────────────────
        if (player.getTags().contains(NO_HUNGER_TAG)) {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0f);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // ── no_friendlyfire — block PvP damage ──────────────────────────────────
    // ────────────────────────────────────────────────────────────────────────

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer attacker)) return;
        // Only block player vs player
        if (!(event.getTarget() instanceof ServerPlayer)) return;

        if (attacker.getTags().contains(NO_FRIENDLYFIRE_TAG)) {
            event.setCanceled(true);
            notify(attacker, "message.abysscore.no_friendlyfire_blocked");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void notify(Player player, String translationKey) {
        player.sendSystemMessage(Component.translatable(translationKey));
    }
}
