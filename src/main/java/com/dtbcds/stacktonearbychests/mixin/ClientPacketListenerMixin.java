package com.dtbcds.stacktonearbychests.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dtbcds.stacktonearbychests.ForEachContainerTask;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleOpenScreen", at = @At("TAIL"))
    private void onHandleContainerOpen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        // Still schedule on the client thread, but check first
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> { // still on render thread
            ForEachContainerTask task = ForEachContainerTask.getCurrentTask();
            if (task == null)
                return;
            if (mc.player == null)
                return;
            task.onInventory(mc.player.containerMenu); // real, live menu
        });
    }

}