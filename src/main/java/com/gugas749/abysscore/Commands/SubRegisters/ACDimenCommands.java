package com.gugas749.abysscore.Commands.SubRegisters;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Dimen.ACDimensionData;
import com.gugas749.abysscore.Features.Dimen.ACDimensionManager;
import com.gugas749.abysscore.Network.Dimen.OpenDimenCreateScreenPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

public class ACDimenCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("abysscore")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("dimen")

                    // ── tp ────────────────────────────────────────────────────
                    .then(Commands.literal("tp")
                        .then(Commands.argument("dimension", ResourceLocationArgument.id())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                getAllLoadedDimKeys(ctx.getSource().getServer()), builder
                            ))
                            .executes(ACDimenCommands::executeTp)
                        )
                    )

                    // ── list ──────────────────────────────────────────────────
                    .then(Commands.literal("list")
                        .executes(ACDimenCommands::executeList)
                    )

                    // ── create ────────────────────────────────────────────────
                    .then(Commands.literal("create")
                        .executes(ACDimenCommands::executeCreate)
                    )

                    // ── unload ────────────────────────────────────────────────
                    .then(Commands.literal("unload")
                        .then(Commands.argument("dimension", ResourceLocationArgument.id())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                getUnloadableDimKeys(ctx.getSource().getServer()), builder
                            ))
                            .executes(ACDimenCommands::executeUnload)
                        )
                    )

                    // ── load ──────────────────────────────────────────────────
                    .then(Commands.literal("load")
                        .then(Commands.argument("dimension", ResourceLocationArgument.id())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                getManagedUnloadedDimNames(), builder
                            ))
                            .executes(ACDimenCommands::executeLoad)
                        )
                    )

                    // ── remove ────────────────────────────────────────────────
                    .then(Commands.literal("remove")
                        .then(Commands.argument("dimension", ResourceLocationArgument.id())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                getManagedDimNames(), builder
                            ))
                            .executes(ACDimenCommands::executeRemove)
                        )
                    )
                )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore dimen <tp|list|create|unload|load|remove>");
    }

    // ── /abysscore dimen tp <dimension> ──────────────────────────────────────

    private static int executeTp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String dimArg = ResourceLocationArgument.getId(ctx, "dimension").toString();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.dimen.player_only"));
            return 0;
        }

        MinecraftServer server = source.getServer();

        // Try to find the dimension — accept short names like "the_nether" or full
        // "minecraft:the_nether" or AbyssCore-managed "my_world" → "abysscore:my_world"
        ServerLevel target = findLevel(server, dimArg);

        if (target == null) {
            source.sendFailure(Component.translatable("message.abysscore.dimen.not_found_or_unloaded", dimArg));
            return 0;
        }

        // Teleport to dimension
        net.minecraft.core.BlockPos spawn = target.getSharedSpawnPos();
        player.teleportTo(
            target,
            spawn.getX() + 0.5,
            spawn.getY(),
            spawn.getZ() + 0.5,
            player.getYRot(),
            player.getXRot()
        );

        source.sendSuccess(
            () -> Component.translatable("message.abysscore.dimen.teleported",
                target.dimension().location().toString()),
            false
        );
        return 1;
    }

    // ── /abysscore dimen list ─────────────────────────────────────────────────

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();

        source.sendSuccess(
            () -> Component.translatable("message.abysscore.dimen.list_header"),
            false
        );

        // All loaded levels — vanilla + modded + AbyssCore
        server.getAllLevels().forEach(level -> {
            String key = level.dimension().location().toString();
            String stateTag = "";

            // Check if it's AbyssCore-managed
            String name = level.dimension().location().getPath();
            Optional<ACDimensionData> managed = ACDimensionManager.get(name);
            if (managed.isPresent()) {
                stateTag = " [abysscore:" + managed.get().state.name().toLowerCase() + "]";
            }

            final String line = "  " + key + stateTag;
            source.sendSuccess(() -> Component.literal(line), false);
        });

        // Also show PENDING_CREATE and UNLOADED dims (not currently loaded)
        ACDimensionManager.getAll().forEach(data -> {
            if (data.state == ACDimensionData.State.PENDING_CREATE
                    || data.state == ACDimensionData.State.UNLOADED
                    || data.state == ACDimensionData.State.PENDING_REMOVE) {
                String line = "  abysscore:" + data.name
                    + " [" + data.state.name().toLowerCase() + "]"
                    + (data.state == ACDimensionData.State.PENDING_CREATE
                        ? " (restart required)" : "");
                source.sendSuccess(() -> Component.literal(line), false);
            }
        });

        return 1;
    }

    // ── /abysscore dimen create ───────────────────────────────────────────────

    private static int executeCreate(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.dimen.player_only"));
            return 0;
        }

        // Send packet to open the creation screen
        PacketDistributor.sendToPlayer(player, new OpenDimenCreateScreenPacket());
        return 1;
    }

    // ── /abysscore dimen unload <dimension> ──────────────────────────────────

    private static int executeUnload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String dimArg  = ResourceLocationArgument.getId(ctx, "dimension").toString();
        String dimName = ResourceLocationArgument.getId(ctx, "dimension").getPath();

        if (isVanillaDim(dimArg)) {
            source.sendFailure(Component.translatable("message.abysscore.dimen.cannot_unload_vanilla", dimArg));
            return 0;
        }

        // Check if it's an AbyssCore-managed dim
        if (ACDimensionManager.exists(dimName)) {
            boolean success = ACDimensionManager.unload(dimName, source.getServer());
            if (!success) {
                source.sendFailure(Component.translatable("message.abysscore.dimen.not_found_or_unloaded", dimArg));
                return 0;
            }
            source.sendSuccess(
                    () -> Component.translatable("message.abysscore.dimen.unloaded_pending", dimArg), true);
            return 1;
        }

        source.sendFailure(Component.translatable("message.abysscore.dimen.cannot_unload_external", dimArg));
        return 0;
    }

    // ── /abysscore dimen load <dimension> ────────────────────────────────────

    private static int executeLoad(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String dimArg  = ResourceLocationArgument.getId(ctx, "dimension").toString();
        String dimName = ResourceLocationArgument.getId(ctx, "dimension").getPath();

        boolean success = ACDimensionManager.load(dimName, source.getServer());
        if (!success) {
            source.sendFailure(Component.translatable("message.abysscore.dimen.not_found_or_loaded", dimArg));
            return 0;
        }

        source.sendSuccess(
                () -> Component.translatable("message.abysscore.dimen.load_pending", dimArg),
                true
        );
        return 1;
    }

    // ── /abysscore dimen remove <dimension> ──────────────────────────────────

    private static int executeRemove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String dimArg  = ResourceLocationArgument.getId(ctx, "dimension").toString();
        String dimName = ResourceLocationArgument.getId(ctx, "dimension").getPath();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.dimen.player_only"));
            return 0;
        }

        if (isVanillaDim(dimArg)) {
            source.sendFailure(Component.translatable("message.abysscore.dimen.cannot_remove_vanilla", dimArg));
            return 0;
        }

        if (!ACDimensionManager.exists(dimName)) {
            source.sendFailure(Component.translatable("message.abysscore.dimen.not_found", dimArg));
            return 0;
        }

        if (!ACDimensionManager.hasPendingConfirm(player.getUUID(), dimName)) {
            ACDimensionManager.startRemoveConfirm(player.getUUID(), dimName);
            source.sendSuccess(
                    () -> Component.translatable("message.abysscore.dimen.remove_confirm", dimArg),
                    false
            );
            return 0;
        }

        boolean success = ACDimensionManager.confirmRemove(dimName, source.getServer());
        if (!success) {
            source.sendFailure(Component.translatable("message.abysscore.dimen.remove_failed", dimArg));
            return 0;
        }

        source.sendSuccess(
                () -> Component.translatable("message.abysscore.dimen.removed_pending", dimArg),
                true
        );
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ServerLevel findLevel(MinecraftServer server, String dimArg) {
        for (ServerLevel level : server.getAllLevels()) {
            String key = level.dimension().location().toString();
            String path = level.dimension().location().getPath();

            if (key.equals(dimArg) || path.equals(dimArg)
                    || key.equals("minecraft:" + dimArg)
                    || key.equals("abysscore:" + dimArg)) {
                return level;
            }
        }
        return null;
    }

    private static List<String> getAllLoadedDimKeys(MinecraftServer server) {
        List<String> keys = new java.util.ArrayList<>();
        server.getAllLevels().forEach(l -> keys.add(l.dimension().location().toString()));
        return keys;
    }

    private static List<String> getManagedDimNames() {
        return ACDimensionManager.getAll().stream()
            .map(d -> d.name)
            .toList();
    }

    private static List<String> getManagedUnloadedDimNames() {
        return ACDimensionManager.getAll().stream()
            .filter(d -> d.state == ACDimensionData.State.UNLOADED)
            .map(d -> d.name)
            .toList();
    }

    private static boolean isVanillaDim(String name) {
        return name.equals("overworld") || name.equals("the_nether") || name.equals("the_end")
            || name.equals("minecraft:overworld") || name.equals("minecraft:the_nether")
            || name.equals("minecraft:the_end");
    }

    private static List<String> getUnloadableDimKeys(MinecraftServer server) {
        List<String> keys = new java.util.ArrayList<>();
        server.getAllLevels().forEach(level -> {
            String key = level.dimension().location().toString();
            if (!isVanillaDim(key)) {
                keys.add(key);
            }
        });
        return keys;
    }
}
