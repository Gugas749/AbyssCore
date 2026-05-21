package com.gugas749.abysscore.Commands.SubRegisters;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Features.Regions.ACBlockProtectionListener;
import com.gugas749.abysscore.Features.Regions.ACRegion;
import com.gugas749.abysscore.Features.Regions.ACRegionSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Optional;

public class ACTagFlagCommands {

    private static final List<String> ALL_TAGS = List.of(
            ACBlockProtectionListener.NO_BUILD_TAG,
            ACBlockProtectionListener.NO_INTERACT_TAG,
            ACBlockProtectionListener.NO_FLY_TAG,
            ACBlockProtectionListener.NO_FRIENDLYFIRE_TAG,
            ACBlockProtectionListener.NO_HUNGER_TAG
    );

    // Tab-complete region names from the current level's saved data
    private static final SuggestionProvider<CommandSourceStack> REGION_SUGGESTIONS =
            (ctx, builder) -> {
                ACRegionSavedData data = ACRegionSavedData.get(ctx.getSource().getLevel());
                return SharedSuggestionProvider.suggest(
                        data.regions().stream().map(ACRegion::name).toList(),
                        builder
                );
            };

    private static final SuggestionProvider<CommandSourceStack> TAG_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(ALL_TAGS, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("protect")
                                .then(Commands.argument("region", StringArgumentType.word())
                                        .suggests(REGION_SUGGESTIONS)

                                        .then(Commands.literal("add")
                                                .then(Commands.argument("tag", StringArgumentType.word())
                                                        .suggests(TAG_SUGGESTIONS)
                                                        .executes(ctx -> executeProtect(ctx, Action.ADD))
                                                )
                                        )

                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("tag", StringArgumentType.word())
                                                        .suggests(TAG_SUGGESTIONS)
                                                        .executes(ctx -> executeProtect(ctx, Action.REMOVE))
                                                )
                                        )

                                        .then(Commands.literal("status")
                                                .executes(ctx -> executeProtect(ctx, Action.STATUS))
                                        )
                                )
                        )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore protect <region> <add|remove|status> [tag]");
    }

    private static int executeProtect(CommandContext<CommandSourceStack> ctx, Action action) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        ACRegionSavedData data = ACRegionSavedData.get(level);

        String regionName = StringArgumentType.getString(ctx, "region");

        // Validate region exists
        Optional<ACRegion> regionOpt = data.getRegion(regionName);
        if (regionOpt.isEmpty()) {
            source.sendFailure(Component.translatable("message.abysscore.protect.region_not_found", regionName));
            return 0;
        }

        ACRegion region = regionOpt.get();

        // Get tag for add/remove
        String tag = null;
        if (action != Action.STATUS) {
            try {
                tag = StringArgumentType.getString(ctx, "tag");
                if (!ALL_TAGS.contains(tag)) {
                    source.sendFailure(Component.translatable("message.abysscore.protect.invalid_tag", tag));
                    return 0;
                }
            } catch (Exception e) {
                source.sendFailure(Component.translatable("message.abysscore.protect.invalid_tag", "?"));
                return 0;
            }
        }

        final String finalTag = tag;

        switch (action) {
            case ADD -> {
                boolean added = data.addTagToRegion(regionName, finalTag);
                if (!added) {
                    source.sendSuccess(
                            () -> Component.translatable("message.abysscore.protect.region_already_has_tag", regionName, finalTag),
                            false
                    );
                } else {
                    source.sendSuccess(
                            () -> Component.translatable("message.abysscore.protect.region_tag_added", regionName, finalTag),
                            true
                    );
                }
            }

            case REMOVE -> {
                boolean removed = data.removeTagFromRegion(regionName, finalTag);
                if (!removed) {
                    source.sendSuccess(
                            () -> Component.translatable("message.abysscore.protect.region_no_tag", regionName, finalTag),
                            false
                    );
                } else {
                    source.sendSuccess(
                            () -> Component.translatable("message.abysscore.protect.region_tag_removed", regionName, finalTag),
                            true
                    );
                }
            }

            case STATUS -> {
                if (region.tags().isEmpty()) {
                    source.sendSuccess(
                            () -> Component.translatable("message.abysscore.protect.region_status_none", regionName),
                            false
                    );
                } else {
                    String tagList = String.join(", ", region.tags());
                    source.sendSuccess(
                            () -> Component.translatable("message.abysscore.protect.region_status_active", regionName, tagList),
                            false
                    );
                }
            }
        }

        return 1;
    }

    private enum Action { ADD, REMOVE, STATUS }
}
