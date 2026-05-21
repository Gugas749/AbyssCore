package com.gugas749.abysscore.Network.Bulk;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Bulk.BulkCommandData;
import com.gugas749.abysscore.Features.Bulk.BulkCommandManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Handles SubmitBulkCommandPacket on the SERVER side.
 * Validates the sender has OP (level 2), then saves the new bulk command.
 */
public class SubmitBulkCommandHandler {

    public static void handle(SubmitBulkCommandPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            // Security: only OPs can create bulk commands
            if (!player.hasPermissions(2)) {
                Abysscore.LOGGER.warn(
                    "[AbyssCore] Player {} tried to create a bulk command without OP permissions!",
                    player.getName().getString()
                );
                return;
            }

            String name = packet.name().trim().toLowerCase();

            // Validate name
            if (name.isEmpty() || !name.matches("[a-z0-9_]+")) {
                player.sendSystemMessage(
                    Component.translatable("message.abysscore.bulk.invalid_name")
                );
                return;
            }

            // Validate commands list
            if (packet.commands().isEmpty()) {
                player.sendSystemMessage(
                    Component.translatable("message.abysscore.bulk.no_commands")
                );
                return;
            }

            // Save
            BulkCommandData data = new BulkCommandData(name, packet.permLevel(), packet.commands());
            boolean added = BulkCommandManager.add(data);

            if (!added) {
                player.sendSystemMessage(
                    Component.translatable("message.abysscore.bulk.already_exists", name)
                );
                return;
            }

            player.sendSystemMessage(
                Component.translatable("message.abysscore.bulk.created", name, packet.commands().size())
            );

            Abysscore.LOGGER.info(
                "[AbyssCore] {} created bulk command '{}' with {} sub-command(s).",
                player.getName().getString(), name, packet.commands().size()
            );
        });
    }
}
