package com.gugas749.abysscore.Commands;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Bind.BindManager;
import com.gugas749.abysscore.Bulk.BulkCommandManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /abysscore bind set <slot 1-9> <bulkName or raw command>
 * /abysscore bind clear <slot 1-9>
 * /abysscore bind list
 */
public class ACBindCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("abysscore")
                        // No .requires() here — all players can manage their own binds

                        .then(Commands.literal("bind")

                                // /abysscore bind set <slot> <value>
                                .then(Commands.literal("set")
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 9))
                                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                                        .suggests((ctx, builder) -> {
                                                            // Suggest existing bulk command names
                                                            List<String> suggestions = new ArrayList<>(BulkCommandManager.getNames());
                                                            return SharedSuggestionProvider.suggest(suggestions, builder);
                                                        })
                                                        .executes(ACBindCommands::executeSet)
                                                )
                                        )
                                )

                                // /abysscore bind clear <slot>
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 9))
                                                .executes(ACBindCommands::executeClear)
                                        )
                                )

                                // /abysscore bind list
                                .then(Commands.literal("list")
                                        .executes(ACBindCommands::executeList)
                                )
                        )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore bind <set|clear|list>");
    }

    // ── /abysscore bind set <slot> <value> ───────────────────────────────────

    private static int executeSet(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.bulk.player_only"));
            return 0;
        }

        int slot    = IntegerArgumentType.getInteger(ctx, "slot");
        String value = StringArgumentType.getString(ctx, "value").trim();

        if (value.isEmpty()) {
            source.sendFailure(Component.translatable("message.abysscore.bind.empty_value"));
            return 0;
        }

        BindManager.setBinding(player.getUUID(), slot, value);

        // Tell the player what got bound, and whether it matched a bulk command
        boolean isBulk = BulkCommandManager.get(value).isPresent();
        source.sendSuccess(
                () -> Component.translatable(
                        isBulk
                                ? "message.abysscore.bind.set_bulk"
                                : "message.abysscore.bind.set_raw",
                        slot, value
                ),
                false
        );

        return 1;
    }

    // ── /abysscore bind clear <slot> ─────────────────────────────────────────

    private static int executeClear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.bulk.player_only"));
            return 0;
        }

        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        String existing = BindManager.getBinding(player.getUUID(), slot);

        if (existing == null) {
            source.sendSuccess(
                    () -> Component.translatable("message.abysscore.bind.already_empty", slot),
                    false
            );
            return 0;
        }

        BindManager.clearBinding(player.getUUID(), slot);
        source.sendSuccess(
                () -> Component.translatable("message.abysscore.bind.cleared", slot),
                false
        );
        return 1;
    }

    // ── /abysscore bind list ──────────────────────────────────────────────────

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.bulk.player_only"));
            return 0;
        }

        Map<String, String> binds = BindManager.getAllBindings(player.getUUID());

        source.sendSuccess(
                () -> Component.translatable("message.abysscore.bind.list_header"),
                false
        );

        boolean anyBound = false;
        for (int i = 1; i <= 9; i++) {
            String bound = binds.get(String.valueOf(i));
            final int slot = i;
            if (bound != null) {
                boolean isBulk = BulkCommandManager.get(bound).isPresent();
                final String display = bound;
                source.sendSuccess(
                        () -> Component.literal(
                                "  Slot " + slot + ": " + display
                                        + (isBulk ? " [bulk]" : " [cmd]")
                        ),
                        false
                );
                anyBound = true;
            } else {
                source.sendSuccess(
                        () -> Component.literal("  Slot " + slot + ": unbound"),
                        false
                );
            }
        }

        return anyBound ? 1 : 0;
    }
}
