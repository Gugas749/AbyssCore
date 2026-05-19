package com.gugas749.abysscore.Commands;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Bulk.BulkCommandData;
import com.gugas749.abysscore.Bulk.BulkCommandManager;
import com.gugas749.abysscore.Network.Bulk.OpenBulkScreenPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * /abysscore bulk add → opens the creation GUI on the OP's client
 * /abysscore bulk remove <name>  → deletes a saved bulk command
 * /abysscore bulk list → lists all saved bulk commands
 * /abysscore run <name> → executes a saved bulk command
 */
public class ACBulkCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
            Commands.literal("abysscore")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("bulk")

                    // /abysscore bulk add — opens GUI on the OP's client
                    .then(Commands.literal("add")
                        .executes(ACBulkCommands::executeBulkAdd)
                    )

                    // /abysscore bulk remove <name>
                    .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests((ctx, builder) ->
                                SharedSuggestionProvider.suggest(BulkCommandManager.getNames(), builder)
                            )
                            .executes(ACBulkCommands::executeBulkRemove)
                        )
                    )

                    // /abysscore bulk list
                    .then(Commands.literal("list")
                        .executes(ACBulkCommands::executeBulkList)
                    )
                )
        );

        // /abysscore run <name> — usable by anyone depending on the command's permLevel
        dispatcher.register(
            Commands.literal("abysscore")
                .then(Commands.literal("run")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) ->
                            SharedSuggestionProvider.suggest(BulkCommandManager.getNames(), builder)
                        )
                        .executes(ACBulkCommands::executeRun)
                    )
                )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore bulk & /abysscore run");
    }

    // ── /abysscore bulk add ──────────────────────────────────────────────────

    private static int executeBulkAdd(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.bulk.player_only"));
            return 0;
        }

        // Send S2C packet → client opens BulkCommandScreen
        PacketDistributor.sendToPlayer(player, new OpenBulkScreenPacket());
        return 1;
    }

    // ── /abysscore bulk remove ───────────────────────────────────────────────

    private static int executeBulkRemove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name").toLowerCase();

        if (BulkCommandManager.remove(name)) {
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.bulk.removed", name),
                true
            );
            return 1;
        } else {
            source.sendFailure(Component.translatable("message.abysscore.bulk.not_found", name));
            return 0;
        }
    }

    // ── /abysscore bulk list ─────────────────────────────────────────────────

    private static int executeBulkList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var all = BulkCommandManager.getAll();

        if (all.isEmpty()) {
            source.sendSuccess(
                () -> Component.translatable("message.abysscore.bulk.list_empty"),
                false
            );
            return 0;
        }

        source.sendSuccess(
            () -> Component.translatable("message.abysscore.bulk.list_header", all.size()),
            false
        );

        for (BulkCommandData cmd : all) {
            String permStr = cmd.permLevel == 0 ? "everyone" : "OP";
            source.sendSuccess(
                () -> Component.literal(
                    "  §e" + cmd.name + "§7 [perm: " + permStr + "] — " + cmd.commands.size() + " command(s)"
                ),
                false
            );
        }

        return all.size();
    }

    // ── /abysscore run <name> ────────────────────────────────────────────────

    private static int executeRun(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name").toLowerCase();

        Optional<BulkCommandData> opt = BulkCommandManager.get(name);
        if (opt.isEmpty()) {
            source.sendFailure(Component.translatable("message.abysscore.bulk.not_found", name));
            return 0;
        }

        BulkCommandData data = opt.get();

        // Check permission level
        if (!source.hasPermission(data.permLevel)) {
            source.sendFailure(Component.translatable("message.abysscore.bulk.no_permission"));
            return 0;
        }

        int executed = 0;
        for (String cmd : data.commands) {
            try {
                source.getServer().getCommands().getDispatcher().execute(cmd, source);
                executed++;
            } catch (Exception e) {
                Abysscore.LOGGER.warn(
                    "[AbyssCore] Bulk command '{}' failed on sub-command '{}': {}",
                    name, cmd, e.getMessage()
                );
                source.sendFailure(
                    Component.translatable("message.abysscore.bulk.cmd_failed", cmd)
                );
            }
        }

        int finalExecuted = executed;
        source.sendSuccess(
            () -> Component.translatable("message.abysscore.bulk.run_success", name, finalExecuted),
            false
        );

        return executed;
    }
}
