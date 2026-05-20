package com.gugas749.abysscore.Mixins;

import com.sk89q.worldedit.neoforge.NeoForgeWorldEdit;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NeoForgeWorldEdit.class, remap = false)
public class NeoForgePlayerMixin {

    @Inject(method = "onRightClickBlock", at = @At("HEAD"), cancellable = true)
    private void abysscore$guardFakePlayer(PlayerInteractEvent.RightClickBlock event, CallbackInfo ci) {
        // Create Deployer fake player has no active game packet listener.
        // ServerPlayer.hasDisconnected() returns true when connection is absent,
        // which is always the case for fake/simulated players.
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        if (sp.hasDisconnected()) {
            ci.cancel();
        }
    }
}

