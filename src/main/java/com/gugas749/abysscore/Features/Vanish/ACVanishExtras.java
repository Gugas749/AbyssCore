package com.gugas749.abysscore.Features.Vanish;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.*;

public class ACVanishExtras {

    private static boolean teamVisibilityEnabled = true;
    private static final Map<UUID, Set<UUID>> explicitlyShownTo = new HashMap<>();
    private static final Map<UUID, Set<UUID>> explicitlyHiddenFrom = new HashMap<>();

    // Cached Vanishmod reflection
    private static Method updateVanishedStatusMethod = null;
    private static boolean vanishReflectionFailed = false;

    // ── Team visibility toggle ─────────────────────────────────────────────────

    public static boolean isTeamVisibilityEnabled() {
        return teamVisibilityEnabled;
    }

    public static boolean toggleTeamVisibility() {
        teamVisibilityEnabled = !teamVisibilityEnabled;
        Abysscore.LOGGER.info("[AbyssCore] Vanish team visibility: {}", teamVisibilityEnabled ? "ON" : "OFF");
        return teamVisibilityEnabled;
    }

    // ── Explicit show/hide ─────────────────────────────────────────────────────

    /**
     * Makes vanishedPlayer visible to viewer instantly.
     * Triggers Vanishmod's updateVanishedStatus so the change takes effect
     * without requiring the vanished player to toggle vanish.
     */
    public static void showTo(UUID vanishedPlayerUUID, UUID viewerUUID, MinecraftServer server) {
        explicitlyShownTo.computeIfAbsent(vanishedPlayerUUID, k -> new HashSet<>()).add(viewerUUID);
        Set<UUID> hidden = explicitlyHiddenFrom.get(vanishedPlayerUUID);
        if (hidden != null) hidden.remove(viewerUUID);

        // Force Vanishmod to re-evaluate visibility immediately
        refreshVanishVisibility(vanishedPlayerUUID, server);
    }

    /**
     * Hides vanishedPlayer from viewer instantly, even if they are teammates.
     */
    public static void hideTo(UUID vanishedPlayerUUID, UUID viewerUUID, MinecraftServer server) {
        explicitlyHiddenFrom.computeIfAbsent(vanishedPlayerUUID, k -> new HashSet<>()).add(viewerUUID);
        Set<UUID> shown = explicitlyShownTo.get(vanishedPlayerUUID);
        if (shown != null) shown.remove(viewerUUID);

        // Force Vanishmod to re-evaluate visibility immediately
        refreshVanishVisibility(vanishedPlayerUUID, server);
    }

    public static void clearExceptions(UUID vanishedPlayer, MinecraftServer server) {
        explicitlyShownTo.remove(vanishedPlayer);
        explicitlyHiddenFrom.remove(vanishedPlayer);
        refreshVanishVisibility(vanishedPlayer, server);
    }

    // Keep old signatures for compatibility (called from leave handler where no server needed)
    public static void showTo(UUID vanishedPlayer, UUID viewer) {
        explicitlyShownTo.computeIfAbsent(vanishedPlayer, k -> new HashSet<>()).add(viewer);
        Set<UUID> hidden = explicitlyHiddenFrom.get(vanishedPlayer);
        if (hidden != null) hidden.remove(viewer);
    }

    public static void hideTo(UUID vanishedPlayer, UUID viewer) {
        explicitlyHiddenFrom.computeIfAbsent(vanishedPlayer, k -> new HashSet<>()).add(viewer);
        Set<UUID> shown = explicitlyShownTo.get(vanishedPlayer);
        if (shown != null) shown.remove(viewer);
    }

    public static void clearExceptions(UUID vanishedPlayer) {
        explicitlyShownTo.remove(vanishedPlayer);
        explicitlyHiddenFrom.remove(vanishedPlayer);
    }

    public static void onPlayerLeave(UUID uuid) {
        explicitlyShownTo.remove(uuid);
        explicitlyHiddenFrom.remove(uuid);
        explicitlyShownTo.values().forEach(set -> set.remove(uuid));
        explicitlyHiddenFrom.values().forEach(set -> set.remove(uuid));
    }

    // ── Visibility check ──────────────────────────────────────────────────────

    public static Boolean checkVisibility(UUID vanishedPlayer, UUID viewer) {
        Set<UUID> hiddenFrom = explicitlyHiddenFrom.get(vanishedPlayer);
        if (hiddenFrom != null && hiddenFrom.contains(viewer)) return Boolean.FALSE;

        Set<UUID> shownTo = explicitlyShownTo.get(vanishedPlayer);
        if (shownTo != null && shownTo.contains(viewer)) return Boolean.TRUE;

        if (!teamVisibilityEnabled) return Boolean.FALSE;

        return null;
    }

    // ── Status queries ────────────────────────────────────────────────────────

    public static Set<UUID> getShownTo(UUID vanishedPlayer) {
        return Collections.unmodifiableSet(
            explicitlyShownTo.getOrDefault(vanishedPlayer, Collections.emptySet()));
    }

    public static Set<UUID> getHiddenFrom(UUID vanishedPlayer) {
        return Collections.unmodifiableSet(
            explicitlyHiddenFrom.getOrDefault(vanishedPlayer, Collections.emptySet()));
    }

    // ── Vanishmod refresh ─────────────────────────────────────────────────────

    /**
     * Forces Vanishmod to re-run its visibility logic for the given player.
     * Calls VanishingHandler.updateVanishedStatus(player, currentVanishState)
     * which re-sends all hide/show packets to online players.
     *
     * This makes show/hide exceptions take effect instantly without requiring
     * the vanished player to toggle vanish off and on again.
     */
    private static void refreshVanishVisibility(UUID vanishedPlayerUUID, MinecraftServer server) {
        if (server == null) return;

        ServerPlayer vanishedPlayer = server.getPlayerList().getPlayer(vanishedPlayerUUID);
        if (vanishedPlayer == null) return;

        // Check if they're actually vanished — no point refreshing if not vanished
        if (!ACVanishStateListener.isVanished(vanishedPlayer)) return;

        if (vanishReflectionFailed) return;

        try {
            if (updateVanishedStatusMethod == null) {
                Class<?> handlerClass = Class.forName(
                    "redstonedubstep.mods.vanishmod.VanishingHandler");
                updateVanishedStatusMethod = handlerClass.getDeclaredMethod(
                    "updateVanishedStatus",
                    ServerPlayer.class,
                    boolean.class
                );
                updateVanishedStatusMethod.setAccessible(true);
                Abysscore.LOGGER.info("[AbyssCore] VanishingHandler.updateVanishedStatus resolved.");
            }

            // Call updateVanishedStatus(player, true) — true = player is vanished
            // This makes Vanishmod re-send hide/show packets to all online players
            // using the updated VanishUtil.playerAllowedToSeeOther() logic
            // (which now includes our ACVanishExtras exceptions via the Mixin)
            updateVanishedStatusMethod.invoke(null, vanishedPlayer, true);

            Abysscore.LOGGER.debug("[AbyssCore] Refreshed vanish visibility for {}",
                vanishedPlayer.getName().getString());

        } catch (Exception e) {
            vanishReflectionFailed = true;
            Abysscore.LOGGER.warn("[AbyssCore] Could not call VanishingHandler.updateVanishedStatus: {}. " +
                "Instant update disabled — changes take effect on next vanish toggle.", e.getMessage());
        }
    }
}
