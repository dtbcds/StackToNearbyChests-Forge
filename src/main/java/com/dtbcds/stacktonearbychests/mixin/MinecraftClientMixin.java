package com.dtbcds.stacktonearbychests.mixin;

import com.dtbcds.stacktonearbychests.LockedSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;<init>(Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V"), cancellable = true)
    private void onSwapItemWithOffhand(CallbackInfo ci) {
        if (LockedSlots.onSwapItemWithOffhand() == InteractionResult.FAIL) {
            ci.cancel();
        }
    }
}
