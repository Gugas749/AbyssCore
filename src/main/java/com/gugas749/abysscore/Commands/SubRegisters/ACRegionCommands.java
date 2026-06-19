package com.gugas749.abysscore.Commands.SubRegisters;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Regions.ACRegion;
import com.gugas749.abysscore.Features.Regions.ACRegionSavedData;
import com.gugas749.abysscore.Features.Regions.ACWorldEditSelection;
import com.gugas749.abysscore.Network.region.OpenRegionScreenPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ACRegionCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("abysscore")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("region")

                    // ── add via WorldEdit selection ───────────────────────────
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

                    // ── tp to region ──────────────────────────────────────────
                    .then(Commands.literal("tp")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                getRegionNames(ctx.getSource().getLevel()), builder
                            ))
                            .executes(ACRegionCommands::executeTp)
                        )
                    )

                    // ── open region manager screen ────────────────────────────
                    .then(Commands.literal("screen")
                        .executes(ACRegionCommands::executeScreen)
                    )
                )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore region <add|tp|screen>");
    }

    // ── screen ────────────────────────────────────────────────────────────────

    private static int executeScreen(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.region.player_required"));
            return 0;
        }

        ACRegionSavedData data = ACRegionSavedData.get(source.getLevel());
        List<OpenRegionScreenPacket.RegionEntry> entries = new ArrayList<>();

        for (ACRegion region : data.regions()) {
            entries.add(new OpenRegionScreenPacket.RegionEntry(
                    region.name(), region.dimension(),
                    region.minX(), region.minY(), region.minZ(),
                    region.maxX(), region.maxY(), region.maxZ(),
                    region.tags(),
                    region.entryFilterTag()
            ));
        }

        PacketDistributor.sendToPlayer(player, new OpenRegionScreenPacket(entries));
        return 1;
    }

    // ── tp ────────────────────────────────────────────────────────────────────

    private static int executeTp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("message.abysscore.region.player_required"));
            return 0;
        }

        Optional<ACRegion> regionOpt = ACRegionSavedData.get(source.getLevel()).getRegion(name);
        if (regionOpt.isEmpty()) {
            source.sendFailure(Component.translatable("message.abysscore.region.not_found", name));
            return 0;
        }

        ACRegion region = regionOpt.get();
        ServerLevel targetLevel = null;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            if (level.dimension().location().toString().equals(region.dimension())) {
                targetLevel = level;
                break;
            }
        }

        if (targetLevel == null) {
            source.sendFailure(Component.translatable("message.abysscore.region.dimension_not_loaded",
                region.dimension()));
            return 0;
        }

        double cx = (region.minX() + region.maxX()) / 2.0 + 0.5;
        double cz = (region.minZ() + region.maxZ()) / 2.0 + 0.5;
        double cy = region.minY() + 1;
        player.teleportTo(targetLevel, cx, cy, cz, player.getYRot(), player.getXRot());

        source.sendSuccess(
            () -> Component.translatable("message.abysscore.region.tp", name), false);
        return 1;
    }

    // ── add ───────────────────────────────────────────────────────────────────

    private static int executeAddFromWorldEdit(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");

        ServerPlayer player;
        try { player = source.getPlayerOrException(); }
        catch (Exception e) {
            source.sendFailure(Component.translatable("message.abysscore.region.player_required"));
            return 0;
        }

        Optional<ACWorldEditSelection.Selection> selection;
        try {
            selection = ACWorldEditSelection.getSelection(player);
        } catch (ACWorldEditSelection.SelectionUnavailableException e) {
            source.sendFailure(Component.translatable(
                e.reason() == ACWorldEditSelection.SelectionUnavailableReason.INCOMPLETE
                    ? "message.abysscore.region.worldedit_incomplete"
                    : "message.abysscore.region.worldedit_error"
            ));
            return 0;
        }

        if (selection.isEmpty()) {
            source.sendFailure(Component.translatable("message.abysscore.region.worldedit_missing"));
            return 0;
        }

        return addRegion(source, name, selection.get().min(), selection.get().max());
    }

    private static int executeAddFromCorners(CommandContext<CommandSourceStack> ctx) {
        return addRegion(
            ctx.getSource(),
            StringArgumentType.getString(ctx, "name"),
            BlockPosArgument.getBlockPos(ctx, "from"),
            BlockPosArgument.getBlockPos(ctx, "to")
        );
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
            () -> Component.translatable("message.abysscore.region.added",
                name, dimension,
                from.getX(), from.getY(), from.getZ(),
                to.getX(), to.getY(), to.getZ()),
            true
        );
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static List<String> getRegionNames(ServerLevel level) {
        return ACRegionSavedData.get(level).regions().stream()
            .map(ACRegion::name).toList();
    }
}
