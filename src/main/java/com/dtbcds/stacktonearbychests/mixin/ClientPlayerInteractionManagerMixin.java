package com.dtbcds.stacktonearbychests.mixin;

import com.dtbcds.stacktonearbychests.InventoryActions;
import com.dtbcds.stacktonearbychests.LockedSlots;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "handleInventoryMouseClick", at = @At(value = "HEAD"), cancellable = true)
    private void beforeClickSlot(int containerId, int slotId, int button, ClickType clickType, Player player,
            CallbackInfo ci) {
        if (player instanceof LocalPlayer localPlayer) {
            if (InventoryActions.onBeforeClickSlot(slotId, button, clickType, localPlayer) == InteractionResult.FAIL) {
                ci.cancel();
                return;
            }
            if (LockedSlots.onBeforeClickSlot(slotId, button, clickType, localPlayer) == InteractionResult.FAIL) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleInventoryMouseClick", at = @At(value = "RETURN"))
    private void afterClickSlot(int containerId, int slotId, int button, ClickType clickType, Player player,
            CallbackInfo ci) {
        if (player instanceof LocalPlayer) {
            LockedSlots.onAfterClickSlot(slotId, button, clickType, player);
        }
    }

    @Inject(method = "setLocalMode", at = @At("TAIL"))
    private void onSetGameMode(GameType gameMode, GameType previousGameMode, CallbackInfo ci) {
        LockedSlots.onSetGameMode(gameMode);
    }
}
