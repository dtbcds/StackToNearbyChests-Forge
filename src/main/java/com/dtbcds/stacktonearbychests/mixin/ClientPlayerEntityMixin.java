package com.dtbcds.stacktonearbychests.mixin;

import com.mojang.authlib.GameProfile;
import com.dtbcds.stacktonearbychests.LockedSlots;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer {

    public ClientPlayerEntityMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void beforeDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (LockedSlots.beforeDropSelectedItem(getInventory().selected) == InteractionResult.FAIL) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "drop", at = @At("TAIL"))
    private void afterDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        LockedSlots.afterDropSelectedItem(getInventory().selected);
    }

}
