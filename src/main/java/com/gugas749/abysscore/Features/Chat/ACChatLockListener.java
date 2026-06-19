package com.gugas749.abysscore.Features.Chat;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.Set;

public class ACChatLockListener {

    private static final Set<String> BLOCKED_COMMANDS = Set.of(
        "say", "tell", "msg", "w", "me", "teammsg", "tm"
    );

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChat(ServerChatEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !player.hasPermissions(2)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onCommand(CommandEvent event) {
        var source = event.getParseResults().getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        if (player.hasPermissions(2)) return;

        String input = event.getParseResults().getReader().getString().trim();
        if (input.startsWith("/")) input = input.substring(1);
        String commandName = input.split(" ")[0].toLowerCase();

        if (BLOCKED_COMMANDS.contains(commandName)) {
            event.setCanceled(true);
        }
    }
}
