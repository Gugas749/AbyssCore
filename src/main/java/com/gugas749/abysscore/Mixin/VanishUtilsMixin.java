package com.gugas749.abysscore.Mixin;

import com.gugas749.abysscore.Features.Vanish.ACVanishExtras;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "redstonedubstep.mods.vanishmod.VanishUtil", remap = false)
public class VanishUtilsMixin {

    @Inject(
            method = "playerAllowedToSeeOther(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void abysscore$customVisibility(
            Entity subject,
            Entity otherPlayer,
            boolean isSubjectVanished,
            boolean isOtherVanished,
            CallbackInfoReturnable<Boolean> cir) {

        if (!isOtherVanished) return;

        Boolean override = ACVanishExtras.checkVisibility(
                otherPlayer.getUUID(),  // the vanished player
                subject.getUUID()       // the viewer trying to see them
        );

        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
