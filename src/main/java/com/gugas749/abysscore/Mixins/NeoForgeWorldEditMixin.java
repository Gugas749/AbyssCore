package com.gugas749.abysscore.Mixins;

import com.sk89q.worldedit.neoforge.NeoForgeWorldEdit;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes a crash caused by Create mod's Deployer fake player interacting with WorldEdit.
 */
@Mixin(value = NeoForgeWorldEdit.class, remap = false)
public class NeoForgeWorldEditMixin {

    @Inject(method = "onRightClickBlock", at = @At("HEAD"), cancellable = true)
    private void abysscore$guardFakePlayer(PlayerInteractEvent.RightClickBlock event, CallbackInfo ci) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)) return;

        // ServerPlayer.connection is null for fake players (Create Deployer)«
        if (sp.connection == null) {
            ci.cancel();
        }
    }
}
