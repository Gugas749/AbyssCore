package com.gugas749.abysscore.Features.Help;

import com.gugas749.abysscore.Features.Staff.StaffProfileManager;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * Manages active /abysshelp requests.
 *
 * One active request per player at a time.
 * Request stays active until a staff member accepts it or the player cancels.
 */
public class ACHelpManager {

    // playerUUID → active request
    private static final Map<UUID, ACHelpRequest> activeRequests = new LinkedHashMap<>();

    // ── Submit ────────────────────────────────────────────────────────────────

    /**
     * Submits a new help request.
     * Returns false if the player already has an active request.
     */
    public static boolean submit(ServerPlayer player, String reason, MinecraftServer server) {
        if (activeRequests.containsKey(player.getUUID())) return false;

        ACHelpRequest request = new ACHelpRequest(
            player.getUUID(),
            player.getName().getString(),
            reason
        );
        activeRequests.put(player.getUUID(), request);

        notifyStaff(request, server);
        return true;
    }

    /**
     * Cancels the active request for a player.
     * Returns false if there was no active request.
     */
    public static boolean cancel(ServerPlayer player) {
        return activeRequests.remove(player.getUUID()) != null;
    }

    /**
     * Returns the active request for a player, or empty.
     */
    public static Optional<ACHelpRequest> getRequest(UUID playerUUID) {
        return Optional.ofNullable(activeRequests.get(playerUUID));
    }

    /**
     * Returns all active requests.
     */
    public static Collection<ACHelpRequest> getAll() {
        return Collections.unmodifiableCollection(activeRequests.values());
    }

    public static boolean hasActiveRequest(UUID playerUUID) {
        return activeRequests.containsKey(playerUUID);
    }

    // ── Accept ────────────────────────────────────────────────────────────────

    /**
     * Staff accepts a help request:
     * - TPs the staff member to the requesting player
     * - Notifies the requesting player
     * - Removes the request
     *
     * @param staff       the staff member accepting the request
     * @param targetUUID  UUID of the player whose request is being accepted
     */
    public static boolean accept(ServerPlayer staff, UUID targetUUID) {
        ACHelpRequest request = activeRequests.get(targetUUID);
        if (request == null) return false;

        ServerPlayer target = staff.getServer().getPlayerList().getPlayer(targetUUID);
        if (target == null) {
            // Player went offline — clean up request
            activeRequests.remove(targetUUID);
            return false;
        }

        // TP staff to player
        target.teleportTo(
            target.serverLevel(),
            target.getX(), target.getY(), target.getZ(),
            staff.getYRot(), staff.getXRot()
        );

        // Notify the requesting player
        target.sendSystemMessage(
            Component.translatable("message.abysscore.help.accepted", staff.getName().getString())
        );

        // Notify the staff member
        staff.sendSystemMessage(
            Component.translatable("message.abysscore.help.staff_accepted", request.playerName)
        );

        activeRequests.remove(targetUUID);
        return true;
    }

    // ── Staff notification ────────────────────────────────────────────────────

    /**
     * Sends the help request notification to all online staff members
     * who currently have staff mode active.
     */
    private static void notifyStaff(ACHelpRequest request, MinecraftServer server) {
        List<ServerPlayer> staffOnline = server.getPlayerList().getPlayers().stream()
            .filter(p -> StaffProfileManager.isStaffMode(p) || p.hasPermissions(2))
            .toList();

        if (staffOnline.isEmpty()) return;

        // Build the notification message with clickable [TP] button
        MutableComponent message = Component.translatable(
            "message.abysscore.help.notification",
            request.playerName,
            request.reason
        );

        // [TP] button — clicking runs /abysscore helpaccept <playerName>
        MutableComponent tpButton = Component.translatable("message.abysscore.help.tp_button")
            .withStyle(Style.EMPTY
                .withColor(net.minecraft.ChatFormatting.GREEN)
                .withBold(true)
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/abysscore helpaccept " + request.playerName
                ))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                    net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                    Component.translatable("message.abysscore.help.tp_hover", request.playerName)
                ))
            );

        MutableComponent full = message.append(" ").append(tpButton);

        for (ServerPlayer staff : staffOnline) {
            staff.sendSystemMessage(full);
        }
    }
}
