package com.gugas749.abysscore;

import com.gugas749.abysscore.Bulk.BulkCommandManager;
import com.gugas749.abysscore.Client.ClientSetup;
import com.gugas749.abysscore.Client.ClientTickHandler;
import com.gugas749.abysscore.Client.KeyBindings;
import com.gugas749.abysscore.Commands.ACModCommands;
import com.gugas749.abysscore.Network.Binds.KeyPressHandler;
import com.gugas749.abysscore.Network.Binds.KeyPressPacket;
import com.gugas749.abysscore.Network.Bulk.SubmitBulkCommandHandler;
import com.gugas749.abysscore.Network.Bulk.SubmitBulkCommandPacket;
import com.gugas749.abysscore.Regions.ACBlockProtectionListener;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(Abysscore.MODID)
public class Abysscore {

    public static final String MODID = "abysscore";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Abysscore(IEventBus modEventBus, ModContainer modContainer) {
        // ── Network packets ──────────────────────────────────────────────────
        modEventBus.addListener(this::registerPayloadHandlers);

        // ── Client setup ──────────────
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientSetup::registerPacketHandlers);
            modEventBus.addListener(KeyBindings::register);
            NeoForge.EVENT_BUS.register(new ClientTickHandler());
        }

        // ── Server-side events ───────────────────────────────────────────────
        NeoForge.EVENT_BUS.register(new ACModCommands());
        NeoForge.EVENT_BUS.register(new ACBlockProtectionListener());
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);

        //--------
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("[AbyssCore] Succefully loaded!");
        });
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        // C2S — client submits a new bulk command definition
        registrar.playToServer(
                SubmitBulkCommandPacket.TYPE,
                SubmitBulkCommandPacket.CODEC,
                SubmitBulkCommandHandler::handle
        );

        registrar.playToServer(
                KeyPressPacket.TYPE,
                KeyPressPacket.CODEC,
                KeyPressHandler::handle
        );
    }

    private void onServerStarting(ServerStartingEvent event) {
        // Load persisted bulk commands from disk when the server starts
        BulkCommandManager.load();
        LOGGER.info("[AbyssCore] Server started, bulk commands loaded.");
    }
}
