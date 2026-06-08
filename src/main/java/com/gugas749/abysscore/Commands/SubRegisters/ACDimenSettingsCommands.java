package com.gugas749.abysscore.Commands.SubRegisters;

import com.gugas749.abysscore.Abysscore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

import java.util.ArrayList;
import java.util.List;


public class ACDimenSettingsCommands {
    // All boolean gamerules for tab completion
    private static final List<String> BOOL_GAMERULES = List.of(
            "doFireTick", "mobGriefing", "keepInventory", "doMobSpawning",
            "doMobLoot", "doTileDrops", "commandBlockOutput", "naturalRegeneration",
            "doDaylightCycle", "logAdminCommands", "showDeathMessages",
            "doWeatherCycle", "doLimitedCrafting", "doInsomnia",
            "doImmediateRespawn", "drowningDamage", "fallDamage", "fireDamage",
            "freezeDamage", "doPatrolSpawning", "doTraderSpawning",
            "doWardenSpawning", "forgiveDeadPlayers", "universalAnger",
            "announceAdvancements", "disableRaids", "doEntityDrops",
            "pvp", "sendCommandFeedback", "reducedDebugInfo"
    );

    // All integer gamerules for tab completion
    private static final List<String> INT_GAMERULES = List.of(
            "randomTickSpeed", "spawnRadius", "maxEntityCramming",
            "maxCommandChainLength", "playersSleepingPercentage"
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("dimen")
                                .then(Commands.literal("settings")

                                        .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                        getAllLoadedDimKeys(ctx.getSource().getServer()), builder
                                                ))

                                                // list gamerules for this dimension
                                                .then(Commands.literal("list")
                                                        .executes(ACDimenSettingsCommands::executeList)
                                                )

                                                // set a boolean gamerule
                                                .then(Commands.literal("set")
                                                        .then(Commands.argument("gamerule", StringArgumentType.word())
                                                                .suggests((ctx, builder) -> {
                                                                    List<String> all = new ArrayList<>(BOOL_GAMERULES);
                                                                    all.addAll(INT_GAMERULES);
                                                                    return SharedSuggestionProvider.suggest(all, builder);
                                                                })

                                                                // bool value
                                                                .then(Commands.argument("bool_value", BoolArgumentType.bool())
                                                                        .executes(ACDimenSettingsCommands::executeSetBool)
                                                                )

                                                                // int value
                                                                .then(Commands.argument("int_value", IntegerArgumentType.integer(0))
                                                                        .executes(ACDimenSettingsCommands::executeSetInt)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
        );

        Abysscore.LOGGER.info("[AbyssCore] Registered: /abysscore dimen settings <dim> <list|set>");
    }

    // ── list ──────────────────────────────────────────────────────────────────

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ResourceLocation dimId = ResourceLocationArgument.getId(ctx, "dimension");

        ServerLevel level = findLevel(source.getServer(), dimId.toString());
        if (level == null) {
            source.sendFailure(Component.translatable("message.abysscore.dimen.not_found_or_unloaded", dimId));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("§1[AbyssCore] §fGamerules for §e" + dimId + "§f:"),
                false
        );

        GameRules rules = level.getGameRules();

        // Visit all gamerule keys and display their current value for this dimension
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(
                    GameRules.Key<T> key, GameRules.Type<T> type) {
                T value = rules.getRule(key);
                source.sendSuccess(
                        () -> Component.literal("  " + key.getId() + " : " + value.serialize()),
                        false
                );
            }
        });

        return 1;
    }

    // ── set bool ──────────────────────────────────────────────────────────────

    private static int executeSetBool(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ResourceLocation dimId = ResourceLocationArgument.getId(ctx, "dimension");
        String ruleName = StringArgumentType.getString(ctx, "gamerule");
        boolean value = BoolArgumentType.getBool(ctx, "bool_value");

        ServerLevel level = findLevel(source.getServer(), dimId.toString());
        if (level == null) {
            source.sendFailure(Component.translatable("message.abysscore.dimen.not_found_or_unloaded", dimId));
            return 0;
        }

        // Find the gamerule key by name and set it
        boolean[] found = {false};
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key,
                                     GameRules.Type<GameRules.BooleanValue> type) {
                if (key.getId().equals(ruleName)) {
                    level.getGameRules().getRule(key).set(value, null);
                    found[0] = true;
                    source.sendSuccess(
                            () -> Component.literal(
                                    "§e[AbyssCore] §fSet §e" + ruleName + "§f to §a" + value
                                            + "§f in §e" + dimId + "§f only."
                            ),
                            true
                    );
                }
            }
        });

        if (!found[0]) {
            source.sendFailure(Component.literal(
                    "[AbyssCore] '" + ruleName + "' is not a boolean gamerule. Use an integer value instead."
            ));
            return 0;
        }

        Abysscore.LOGGER.info("[AbyssCore] Set gamerule {} = {} in dimension {}",
                ruleName, value, dimId);
        return 1;
    }

    // ── set int ───────────────────────────────────────────────────────────────

    private static int executeSetInt(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ResourceLocation dimId = ResourceLocationArgument.getId(ctx, "dimension");
        String ruleName = StringArgumentType.getString(ctx, "gamerule");
        int value = IntegerArgumentType.getInteger(ctx, "int_value");

        ServerLevel level = findLevel(source.getServer(), dimId.toString());
        if (level == null) {
            source.sendFailure(Component.translatable("message.abysscore.dimen.not_found_or_unloaded", dimId));
            return 0;
        }

        boolean[] found = {false};
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key,
                                     GameRules.Type<GameRules.IntegerValue> type) {
                if (key.getId().equals(ruleName)) {
                    level.getGameRules().getRule(key).set(value, null);
                    found[0] = true;
                    source.sendSuccess(
                            () -> Component.literal(
                                    "§e[AbyssCore] §fSet §e" + ruleName + "§f to §a" + value
                                            + "§f in §e" + dimId + "§f only."
                            ),
                            true
                    );
                }
            }
        });

        if (!found[0]) {
            source.sendFailure(Component.literal(
                    "[AbyssCore] '" + ruleName + "' is not an integer gamerule. Use a boolean value instead."
            ));
            return 0;
        }

        Abysscore.LOGGER.info("[AbyssCore] Set gamerule {} = {} in dimension {}",
                ruleName, value, dimId);
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ServerLevel findLevel(MinecraftServer server, String dimArg) {
        for (ServerLevel level : server.getAllLevels()) {
            String key = level.dimension().location().toString();
            if (key.equals(dimArg)) return level;
        }
        return null;
    }

    private static List<String> getAllLoadedDimKeys(MinecraftServer server) {
        List<String> keys = new ArrayList<>();
        server.getAllLevels().forEach(l -> keys.add(l.dimension().location().toString()));
        return keys;
    }
}
