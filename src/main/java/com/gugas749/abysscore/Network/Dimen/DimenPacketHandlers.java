package com.gugas749.abysscore.Network.Dimen;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Dimen.ACDimensionData;
import com.gugas749.abysscore.Features.Dimen.ACDimensionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Random;

public class DimenPacketHandlers {

    public static void handleCreate(SubmitDimenCreatePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!player.hasPermissions(2)) return;

            String name = packet.name().trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
            if (name.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.abysscore.dimen.invalid_name"));
                return;
            }

            if (ACDimensionManager.exists(name)) {
                player.sendSystemMessage(
                        Component.translatable("message.abysscore.dimen.already_exists", name));
                return;
            }

            // Use a random seed if the user left it at 0
            long seed = packet.seed() == 0 ? new Random().nextLong() : packet.seed();

            String displayName = packet.displayName().isBlank() ? name : packet.displayName().trim();

            ACDimensionData data = new ACDimensionData(
                    name,
                    displayName,
                    ACDimensionData.Style.fromString(packet.style()),
                    seed
            );

            boolean success = ACDimensionManager.create(data, player.getServer());

            if (success) {
                player.sendSystemMessage(
                        Component.translatable("message.abysscore.dimen.created_pending", name));
                Abysscore.LOGGER.info("[AbyssCore] {} created dimension '{}'.",
                        player.getName().getString(), name);
            } else {
                player.sendSystemMessage(
                        Component.translatable("message.abysscore.dimen.create_failed", name));
            }
        });
    }
}
