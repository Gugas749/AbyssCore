package com.gugas749.abysscore.Network;

import com.gugas749.abysscore.Client.screens.*;
import com.gugas749.abysscore.Features.Regions.ACNoEntryListener;
import com.gugas749.abysscore.Features.Regions.ACRegionSavedData;
import com.gugas749.abysscore.Network.Binds.KeyPressHandler;
import com.gugas749.abysscore.Network.Binds.KeyPressPacket;
import com.gugas749.abysscore.Network.Blind.BlindSyncPacket;
import com.gugas749.abysscore.Network.Bulk.OpenBulkScreenPacket;
import com.gugas749.abysscore.Network.Bulk.SubmitBulkCommandHandler;
import com.gugas749.abysscore.Network.Bulk.SubmitBulkCommandPacket;
import com.gugas749.abysscore.Client.ACVanishHudHandler;
import com.gugas749.abysscore.Network.Vanish.VanishStateSyncPacket;
import com.gugas749.abysscore.Network.menu.MenuActionPacket;
import com.gugas749.abysscore.Network.menu.MenuPacketHandlers;
import com.gugas749.abysscore.Network.menu.OpenMainMenuPacket;
import com.gugas749.abysscore.Network.region.*;
import com.gugas749.abysscore.Network.title.SaveTitlePacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.gugas749.abysscore.Network.Dimen.DimenPacketHandlers;
import com.gugas749.abysscore.Network.Dimen.OpenDimenCreateScreenPacket;
import com.gugas749.abysscore.Network.Dimen.SubmitDimenCreatePacket;

public class PacketHandler {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        PayloadRegistrar optional = event.registrar("2").optional();

        // ── S2C (server → client) ────────────────────────────────────────────

        // OpenBulkScreen: dist-safe handler — only opens screen on client dist
        registrar.playToClient(
                OpenBulkScreenPacket.TYPE,
                OpenBulkScreenPacket.CODEC,
                FMLEnvironment.dist == Dist.CLIENT
                        ? PacketHandler::handleOpenBulkScreenClient
                        : (pkt, ctx) -> {}
        );

        // open dimension creation screen
        registrar.playToClient(
                OpenDimenCreateScreenPacket.TYPE,
                OpenDimenCreateScreenPacket.CODEC,
                FMLEnvironment.dist == Dist.CLIENT
                        ? PacketHandler::handleOpenDimenCreateClient
                        : (pkt, ctx) -> {}
        );

        // VANISH
        registrar.playToClient(
                VanishStateSyncPacket.TYPE,
                VanishStateSyncPacket.CODEC,
                FMLEnvironment.dist == Dist.CLIENT
                        ? ACVanishHudHandler::handleSync
                        : (pkt, ctx) -> {}
        );

        // REGION MANAGER SCREEN
        registrar.playToClient(
                OpenRegionScreenPacket.TYPE,
                OpenRegionScreenPacket.CODEC,
                FMLEnvironment.dist == Dist.CLIENT
                        ? PacketHandler::handleOpenRegionScreenClient
                        : (pkt, ctx) -> {}
        );

        // no entry - related
        registrar.playToClient(
                NoEntryPacket.TYPE,
                NoEntryPacket.CODEC,
                FMLEnvironment.dist == Dist.CLIENT
                        ? NoEntryHandler::handlePacket
                        : (pkt, ctx) -> {}
        );

        // blind packet
        registrar.playToClient(
                BlindSyncPacket.TYPE,
                BlindSyncPacket.CODEC,
                FMLEnvironment.dist == Dist.CLIENT
                        ? BlindScreen::handleSync
                        : (pkt, ctx) -> {}
        );

        // main menu
        registrar.playToClient(
                OpenMainMenuPacket.TYPE,
                OpenMainMenuPacket.CODEC,
                FMLEnvironment.dist == Dist.CLIENT
                        ? PacketHandler::handleOpenMainMenu
                        : (pkt, ctx) -> {}
        );

        // ── C2S (client → server) ────────────────────────────────────────────

        registrar.playToServer(
                KeyPressPacket.TYPE,
                KeyPressPacket.CODEC,
                KeyPressHandler::handle
        );

        // submit new bulk command creation
        registrar.playToServer(
                SubmitBulkCommandPacket.TYPE,
                SubmitBulkCommandPacket.CODEC,
                SubmitBulkCommandHandler::handle
        );

        // submit new dimension creation
        registrar.playToServer(
                SubmitDimenCreatePacket.TYPE,
                SubmitDimenCreatePacket.CODEC,
                DimenPacketHandlers::handleCreate
        );

        //submit region change
        registrar.playToServer(
                SubmitRegionUpdatePacket.TYPE,
                SubmitRegionUpdatePacket.CODEC,
                RegionScreenPacketHandlers::handleRegionUpdate
        );

        // request tp - no entry
        registrar.playToServer(
                ReadyToTeleportPacket.TYPE,
                ReadyToTeleportPacket.CODEC,
                ACNoEntryListener::handleReadyToTeleport
        );

        // menu action
        registrar.playToServer(
                MenuActionPacket.TYPE,
                MenuActionPacket.CODEC,
                MenuPacketHandlers::handleAction
        );

        // save title
        registrar.playToServer(
                SaveTitlePacket.TYPE,
                SaveTitlePacket.CODEC,
                MenuPacketHandlers::handleSaveTitle
        );

        // request region screen from menu
        registrar.playToServer(
                RequestRegionScreenPacket.TYPE,
                RequestRegionScreenPacket.CODEC,
                (pkt, ctx) -> ctx.enqueueWork(() -> {
                    if (!(ctx.player() instanceof ServerPlayer op)) return;
                    if (!op.hasPermissions(2)) return;
                    var data = ACRegionSavedData.get(op.serverLevel());
                    var entries = new java.util.ArrayList<OpenRegionScreenPacket.RegionEntry>();
                    for (var region : data.regions()) {
                        entries.add(new OpenRegionScreenPacket.RegionEntry(
                                region.name(), region.dimension(),
                                region.minX(), region.minY(), region.minZ(),
                                region.maxX(), region.maxY(), region.maxZ(),
                                region.tags(), region.entryFilterTag()
                        ));
                    }
                    PacketDistributor.sendToPlayer(op,
                            new OpenRegionScreenPacket(entries));
                })
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── Handlers ─────────────────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    private static void handleOpenBulkScreenClient(OpenBulkScreenPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                net.minecraft.client.Minecraft.getInstance()
                        .setScreen(new BulkCommandScreen())
        );
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleOpenDimenCreateClient(OpenDimenCreateScreenPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                net.minecraft.client.Minecraft.getInstance()
                        .setScreen(new DimenCreateScreen())
        );
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleOpenRegionScreenClient(OpenRegionScreenPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                net.minecraft.client.Minecraft.getInstance()
                        .setScreen(new RegionManagerScreen(packet.regions()))
        );
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleOpenMainMenu(OpenMainMenuPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                net.minecraft.client.Minecraft.getInstance()
                        .setScreen(new AbyssCoreMenuScreen(packet))
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── Helpers ──────────────────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
