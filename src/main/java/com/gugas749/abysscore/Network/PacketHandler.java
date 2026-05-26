package com.gugas749.abysscore.Network;

import com.gugas749.abysscore.Network.Binds.KeyPressHandler;
import com.gugas749.abysscore.Network.Binds.KeyPressPacket;
import com.gugas749.abysscore.Network.Bulk.OpenBulkScreenPacket;
import com.gugas749.abysscore.Network.Bulk.SubmitBulkCommandHandler;
import com.gugas749.abysscore.Network.Bulk.SubmitBulkCommandPacket;
import com.gugas749.abysscore.Client.ACVanishHudHandler;
import com.gugas749.abysscore.Network.Vanish.VanishStateSyncPacket;
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
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── Handlers ─────────────────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    private static void handleOpenBulkScreenClient(OpenBulkScreenPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                net.minecraft.client.Minecraft.getInstance()
                        .setScreen(new com.gugas749.abysscore.Client.BulkCommandScreen())
        );
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleOpenDimenCreateClient(OpenDimenCreateScreenPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                net.minecraft.client.Minecraft.getInstance()
                        .setScreen(new com.gugas749.abysscore.Client.DimenCreateScreen())
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
