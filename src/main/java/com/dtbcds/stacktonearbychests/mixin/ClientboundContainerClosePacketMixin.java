package com.dtbcds.stacktonearbychests.mixin;

import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.dtbcds.stacktonearbychests.ForEachContainerTask.isRunning;

@Mixin(ClientboundContainerClosePacket.class)
public class ClientboundContainerClosePacketMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void quickstack$suppressScreenClear(CallbackInfo ci) {
        if (isRunning()) {
            // send packet to server but do NOT run the default handler
            // that calls minecraft.setScreen(null)
            ci.cancel();
        }
    }
}