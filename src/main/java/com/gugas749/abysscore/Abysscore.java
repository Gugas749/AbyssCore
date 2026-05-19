package com.gugas749.abysscore;

import com.gugas749.abysscore.Bulk.BulkCommandManager;
import com.gugas749.abysscore.Client.ClientSetup;
import com.gugas749.abysscore.Commands.ACFiguraCommands;
import com.gugas749.abysscore.Commands.ACModCommands;
import com.gugas749.abysscore.Network.FiguraReloadPacket;
import com.gugas749.abysscore.Network.OpenBulkScreenPacket;
import com.gugas749.abysscore.Network.SubmitBulkCommandHandler;
import com.gugas749.abysscore.Network.SubmitBulkCommandPacket;
import com.gugas749.abysscore.Regions.ACBlockProtectionListener;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
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
    }

    private void onServerStarting(ServerStartingEvent event) {
        // Load persisted bulk commands from disk when the server starts
        BulkCommandManager.load();
        LOGGER.info("[AbyssCore] Server started, bulk commands loaded.");
    }
}
