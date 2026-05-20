package com.gugas749.abysscore.Network;

import com.gugas749.abysscore.Network.Binds.KeyPressHandler;
import com.gugas749.abysscore.Network.Binds.KeyPressPacket;
import com.gugas749.abysscore.Network.Bulk.OpenBulkScreenHandler;
import com.gugas749.abysscore.Network.Bulk.OpenBulkScreenPacket;
import com.gugas749.abysscore.Network.Bulk.SubmitBulkCommandHandler;
import com.gugas749.abysscore.Network.Bulk.SubmitBulkCommandPacket;
import com.gugas749.abysscore.Network.Figura.FiguraReloadPacket;
import com.gugas749.abysscore.Network.Figura.FiguraReloadPacketHandler;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        PayloadRegistrar optional = event.registrar("2").optional();

        // ── S2C (server → client) ────────────────────────────────────────────

        // FiguraReload: optional because Figura is client-only
        optional.playToClient(
                FiguraReloadPacket.TYPE,
                FiguraReloadPacket.CODEC,
                FMLEnvironment.dist == Dist.CLIENT
                        ? FiguraReloadPacketHandler::handle
                        : (pkt, ctx) -> {}
        );

        // OpenBulkScreen: dist-safe handler — only opens screen on client dist
        registrar.playToClient(
                OpenBulkScreenPacket.TYPE,
                OpenBulkScreenPacket.CODEC,
                FMLEnvironment.dist == Dist.CLIENT
                        ? PacketHandler::handleOpenBulkScreenClient
                        : (pkt, ctx) -> {}
        );

        // ── C2S (client → server) ────────────────────────────────────────────

        registrar.playToServer(
                KeyPressPacket.TYPE,
                KeyPressPacket.CODEC,
                KeyPressHandler::handle
        );

        registrar.playToServer(
                SubmitBulkCommandPacket.TYPE,
                SubmitBulkCommandPacket.CODEC,
                SubmitBulkCommandHandler::handle
        );
    }

    // Separated into its own method so the BulkCommandScreen/Minecraft references
    // are only resolved when this method is actually called (client dist only).
    @net.neoforged.api.distmarker.OnlyIn(Dist.CLIENT)
    private static void handleOpenBulkScreenClient(OpenBulkScreenPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                net.minecraft.client.Minecraft.getInstance()
                        .setScreen(new com.gugas749.abysscore.Client.BulkCommandScreen())
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
