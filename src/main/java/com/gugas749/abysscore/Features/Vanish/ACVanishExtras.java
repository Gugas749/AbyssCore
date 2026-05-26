package com.gugas749.abysscore.Features.Vanish;

import com.gugas749.abysscore.Abysscore;

import java.util.*;

public class ACVanishExtras {

    private static boolean teamVisibilityEnabled = true;
    private static final Map<UUID, Set<UUID>> explicitlyShownTo = new HashMap<>();
    private static final Map<UUID, Set<UUID>> explicitlyHiddenFrom = new HashMap<>();

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

    public static void showTo(UUID vanishedPlayer, UUID viewer) {
        explicitlyShownTo.computeIfAbsent(vanishedPlayer, k -> new HashSet<>()).add(viewer);
        // Remove from hidden list if present — show wins
        Set<UUID> hidden = explicitlyHiddenFrom.get(vanishedPlayer);
        if (hidden != null) hidden.remove(viewer);
    }

    public static void hideTo(UUID vanishedPlayer, UUID viewer) {
        explicitlyHiddenFrom.computeIfAbsent(vanishedPlayer, k -> new HashSet<>()).add(viewer);
        // Remove from shown list if present — hide wins
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
        if (hiddenFrom != null && hiddenFrom.contains(viewer)) {
            return Boolean.FALSE;
        }

        Set<UUID> shownTo = explicitlyShownTo.get(vanishedPlayer);
        if (shownTo != null && shownTo.contains(viewer)) {
            return Boolean.TRUE;
        }

        if (!teamVisibilityEnabled) {
            return Boolean.FALSE;
        }

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
}
