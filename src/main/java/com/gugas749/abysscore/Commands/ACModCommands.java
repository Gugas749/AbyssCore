package com.gugas749.abysscore.Commands;

import com.gugas749.abysscore.Abysscore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class ACModCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        Abysscore.LOGGER.info("[AbyssCore] Registering commands...");

        ACFiguraCommands.register(event.getDispatcher());
        ACTagFlagCommands.register(event.getDispatcher());
        ACRegionCommands.register(event.getDispatcher());
    }
}
