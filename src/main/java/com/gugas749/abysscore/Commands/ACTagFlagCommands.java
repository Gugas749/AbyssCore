package com.gugas749.abysscore.Commands;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Regions.ACBlockProtectionListener;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * /abysscore protect <player(s)> add    <tag>
 * /abysscore protect <player(s)> remove <tag>
 * /abysscore protect <player(s)> status
 *
 * Available tags (tab-completed):
 *   no_build, no_interact, no_fly, no_friendlyfire, no_hunger
 */
public class ACTagFlagCommands {

    // All available restriction tags
    private static final List<String> ALL_TAGS = List.of(
            ACBlockProtectionListener.NO_BUILD_TAG,
            ACBlockProtectionListener.NO_INTERACT_TAG,
            ACBlockProtectionListener.NO_FLY_TAG,
            ACBlockProtectionListener.NO_FRIENDLYFIRE_TAG,
            ACBlockProtectionListener.NO_HUNGER_TAG
    );

    // Tab-completion suggestion provider for tags
    private static final SuggestionProvider<CommandSourceStack> TAG_SUGGESTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(ALL_TAGS, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("protect")
                                .then(Commands.argument("targets", EntityArgument.players())

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

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore protect <player> <add|remove|status> [tag]");
    }

    // -------------------------------------------------------------------------
    // Command logic
    // -------------------------------------------------------------------------

    private static int executeProtect(CommandContext<CommandSourceStack> ctx, Action action) {
        CommandSourceStack source = ctx.getSource();
        Collection<ServerPlayer> targets;

        try {
            targets = EntityArgument.getPlayers(ctx, "targets");
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.abysscore.protect.player_not_found"));
            return 0;
        }

        // Get tag argument for add/remove actions
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
        int affected = 0;

        for (ServerPlayer player : targets) {
            String playerName = player.getName().getString();

            switch (action) {
                case ADD -> {
                    if (player.getTags().contains(finalTag)) {
                        source.sendSuccess(
                                () -> Component.translatable("message.abysscore.protect.already_restricted", playerName, finalTag),
                                false
                        );
                    } else {
                        player.addTag(finalTag);
                        source.sendSuccess(
                                () -> Component.translatable("message.abysscore.protect.added_op", playerName, finalTag),
                                true
                        );
                        affected++;
                    }
                }

                case REMOVE -> {
                    if (!player.getTags().contains(finalTag)) {
                        source.sendSuccess(
                                () -> Component.translatable("message.abysscore.protect.not_restricted", playerName, finalTag),
                                false
                        );
                    } else {
                        player.removeTag(finalTag);
                        source.sendSuccess(
                                () -> Component.translatable("message.abysscore.protect.removed_op", playerName, finalTag),
                                true
                        );
                        affected++;
                    }
                }

                case STATUS -> {
                    // Show all active tags for this player
                    List<String> active = ALL_TAGS.stream()
                            .filter(t -> player.getTags().contains(t))
                            .toList();

                    if (active.isEmpty()) {
                        source.sendSuccess(
                                () -> Component.translatable("message.abysscore.protect.status_none", playerName),
                                false
                        );
                    } else {
                        String tagList = String.join(", ", active);
                        source.sendSuccess(
                                () -> Component.translatable("message.abysscore.protect.status_active", playerName, tagList),
                                false
                        );
                    }
                    affected++;
                }
            }
        }

        return affected;
    }

    private enum Action { ADD, REMOVE, STATUS }
}
