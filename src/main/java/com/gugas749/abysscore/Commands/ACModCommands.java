package com.gugas749.abysscore.Commands;

import com.gugas749.abysscore.Abysscore;
import com.gugas749.abysscore.Commands.SubRegisters.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class ACModCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        Abysscore.LOGGER.info("[AbyssCore] Registering commands...");

        ACTagFlagCommands.register(event.getDispatcher());
        ACRegionCommands.register(event.getDispatcher());
        ACBulkCommands.register(event.getDispatcher());
        ACBindCommands.register(event.getDispatcher());
        //ACStaffCommands.register(event.getDispatcher());
        ACDimenCommands.register(event.getDispatcher());
        ACDimenSettingsCommands.register(event.getDispatcher());
        ACHelpCommands.register(event.getDispatcher());
        ACVanishCommands.register(event.getDispatcher());
        ACGodCommands.register(event.getDispatcher());
    }
}
