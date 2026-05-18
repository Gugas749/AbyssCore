package com.gugas749.abysscore.Commands;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Regions.ACRegion;
import com.gugas749.abysscore.Regions.ACRegionSavedData;
import com.gugas749.abysscore.Regions.ACWorldEditSelection;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class ACRegionCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("region")
                                .then(Commands.literal("list")
                                        .executes(ACRegionCommands::executeList)
                                )
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(ACRegionCommands::executeRemove)
                                        )
                                )
                                .then(Commands.literal("add")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(ACRegionCommands::executeAddFromWorldEdit)
                                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                                                .executes(ACRegionCommands::executeAddFromCorners)
                                                        )
                                                )
                                        )
                                )
                        )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore region <add|list|remove>");
    }

    private static int executeAddFromWorldEdit(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.abysscore.region.player_required"));
            return 0;
        }

        Optional<ACWorldEditSelection.Selection> selection;
        try {
            selection = ACWorldEditSelection.getSelection(player);
        } catch (ACWorldEditSelection.SelectionUnavailableException e) {
            if (e.reason() == ACWorldEditSelection.SelectionUnavailableReason.INCOMPLETE) {
                source.sendFailure(Component.translatable("message.abysscore.region.worldedit_incomplete"));
            } else {
                source.sendFailure(Component.translatable("message.abysscore.region.worldedit_error"));
                Abysscore.LOGGER.warn("[AbyssCore] Failed to read WorldEdit selection", e);
            }
            return 0;
        }

        if (selection.isEmpty()) {
            source.sendFailure(Component.translatable("message.abysscore.region.worldedit_missing"));
            return 0;
        }

        return addRegion(source, name, selection.get().min(), selection.get().max());
    }

    private static int executeAddFromCorners(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        BlockPos from = BlockPosArgument.getBlockPos(ctx, "from");
        BlockPos to = BlockPosArgument.getBlockPos(ctx, "to");
        return addRegion(source, name, from, to);
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ACRegionSavedData data = ACRegionSavedData.get(source.getLevel());

        if (data.regions().isEmpty()) {
            source.sendSuccess(
                    () -> Component.translatable("message.abysscore.region.list_empty"),
                    false
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.translatable("message.abysscore.region.list_header", data.regions().size()),
                false
        );

        for (ACRegion region : data.regions()) {
            source.sendSuccess(
                    () -> Component.translatable(
                            "message.abysscore.region.list_entry",
                            region.name(),
                            region.dimension(),
                            region.minX(), region.minY(), region.minZ(),
                            region.maxX(), region.maxY(), region.maxZ()
                    ),
                    false
            );
        }

        return data.regions().size();
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        boolean removed = ACRegionSavedData.get(source.getLevel()).removeRegion(name);

        if (!removed) {
            source.sendFailure(Component.translatable("message.abysscore.region.not_found", name));
            return 0;
        }

        source.sendSuccess(
                () -> Component.translatable("message.abysscore.region.removed", name),
                true
        );

        return 1;
    }

    private static int addRegion(CommandSourceStack source, String name, BlockPos from, BlockPos to) {
        ServerLevel level = source.getLevel();
        String dimension = level.dimension().location().toString();

        boolean added = ACRegionSavedData.get(level).addRegion(name, dimension, from, to);

        if (!added) {
            source.sendFailure(Component.translatable("message.abysscore.region.already_exists", name));
            return 0;
        }

        source.sendSuccess(
                () -> Component.translatable(
                        "message.abysscore.region.added",
                        name,
                        dimension,
                        from.getX(), from.getY(), from.getZ(),
                        to.getX(), to.getY(), to.getZ()
                ),
                true
        );

        return 1;
    }
}
