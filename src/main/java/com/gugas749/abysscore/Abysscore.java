package com.gugas749.abysscore;

import com.gugas749.abysscore.Features.Bulk.BulkCommandManager;
import com.gugas749.abysscore.Client.ClientTickHandler;
import com.gugas749.abysscore.Client.KeyBindings;
import com.gugas749.abysscore.Commands.ACModCommands;
import com.gugas749.abysscore.Features.Dimen.ACDimensionManager;
import com.gugas749.abysscore.Features.Vanish.ACSimpleVoiceChatIntegration;
import com.gugas749.abysscore.Features.Vanish.ACVanishListener;
import com.gugas749.abysscore.Network.PacketHandler;
import com.gugas749.abysscore.Features.Regions.ACBlockProtectionListener;
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
import org.slf4j.Logger;

@Mod(Abysscore.MODID)
public class Abysscore {

    public static final String MODID = "abysscore";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Abysscore(IEventBus modEventBus, ModContainer modContainer) {

        // ── Network ──────────────────────────────────────────────────────────
        modEventBus.addListener(PacketHandler::registerPayloads);

        // ── Client only ──────────────────────────────────────────────────────
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(KeyBindings::register);
            NeoForge.EVENT_BUS.register(new ClientTickHandler());
        }

        // ── Server ───────────────────────────────────────────────────────────
        NeoForge.EVENT_BUS.register(new ACModCommands());
        NeoForge.EVENT_BUS.register(new ACBlockProtectionListener());
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.register(new ACVanishListener());

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ACSimpleVoiceChatIntegration.init();
        event.enqueueWork(() -> LOGGER.info("[AbyssCore] Successfully loaded!"));
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void onServerStarting(ServerStartingEvent event) {
        BulkCommandManager.load();
        ACDimensionManager.load();   // load registry
        ACDimensionManager.onServerStarted(event.getServer());  // cleanup pending states
        LOGGER.info("[AbyssCore] Server started, bulk commands loaded.");
        LOGGER.info("[AbyssCore] Server started, Dimens registry loaded.");
    }
}
