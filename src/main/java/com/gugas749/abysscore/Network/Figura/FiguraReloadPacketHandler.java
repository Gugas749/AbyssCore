package com.gugas749.abysscore.Network.Figura;

import com.gugas749.abysscore.Abysscore;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Method;

/**
 * Handles FiguraReloadPacket on the CLIENT side.
 *
 * execute "/figura reload" through the CLIENT-side command dispatcher,
 */
public class FiguraReloadPacketHandler {

    public static void handle(FiguraReloadPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (packet.force()) {
                handleForce(mc);
            } else {
                handleNormal(mc);
            }
        });
    }

    // ── normal reload ─────────────────────────────────────────────

    private static void handleNormal(Minecraft mc) {
        try {
            mc.player.connection.sendCommand("figura reload");
            Abysscore.LOGGER.debug("[AbyssCore] Figura reload triggered for local client.");
        } catch (Exception e) {
            Abysscore.LOGGER.warn(
                    "[AbyssCore] Could not trigger Figura reload on client: {}", e.getMessage()
            );
        }
    }

    // ── force reload ──────────

    private static void handleForce(Minecraft mc) {
        try {
            Class<?> avatarManagerClass =
                    Class.forName("org.figuramc.figura.avatar.AvatarManager");

            // clearAllAvatars() wipes all loaded avatars from the client cache.
            Method clearAll = avatarManagerClass.getMethod("clearAllAvatars");
            clearAll.invoke(null);

            Abysscore.LOGGER.debug("[AbyssCore] Figura force reload: AvatarManager.clearAllAvatars() called.");
        } catch (ClassNotFoundException e) {
            // Figura not installed on this client — shouldn't happen since the
            // server already checked, but handle gracefully just in case.
            Abysscore.LOGGER.warn("[AbyssCore] Figura not found on client during force reload.");
        } catch (Exception e) {
            Abysscore.LOGGER.warn(
                    "[AbyssCore] Figura force reload failed: {}", e.getMessage()
            );
        }
    }
}
