package com.gugas749.abysscore.Commands.SubRegisters;

import com.gugas749.abysscore.Abysscore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ACGodCommands {

    private static final Set<UUID> godModePlayers = new HashSet<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("abysscore")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("god")

                    .executes(ACGodCommands::executeToggleSelf)

                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ACGodCommands::executeToggleTarget)
                    )
                )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore god [player]");
    }

    private static int executeToggleSelf(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.god.player_only"));
            return 0;
        }
        return toggle(source, player);
    }

    private static int executeToggleTarget(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        try {
            return toggle(source, EntityArgument.getPlayer(ctx, "target"));
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.abysscore.god.player_not_found"));
            return 0;
        }
    }

    private static int toggle(CommandSourceStack source, ServerPlayer player) {
        boolean nowGod;

        if (godModePlayers.contains(player.getUUID())) {
            godModePlayers.remove(player.getUUID());
            nowGod = false;
        } else {
            godModePlayers.add(player.getUUID());
            nowGod = true;
        }

        source.sendSuccess(
            () -> Component.translatable(
                nowGod ? "message.abysscore.god.enabled" : "message.abysscore.god.disabled",
                player.getName().getString()
            ),
            true
        );

        if (source.getEntity() != player) {
            player.sendSystemMessage(Component.translatable(
                nowGod ? "message.abysscore.god.enabled_self" : "message.abysscore.god.disabled_self"
            ));
        }

        Abysscore.LOGGER.info("[AbyssCore] God mode {} for {}",
            nowGod ? "ENABLED" : "DISABLED", player.getName().getString());
        return 1;
    }

    // ── Damage cancel ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (godModePlayers.contains(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static boolean hasGodMode(UUID uuid) {
        return godModePlayers.contains(uuid);
    }

    public static void onPlayerLeave(UUID uuid) {
        godModePlayers.remove(uuid);
    }
}
