package com.dtbcds.stacktonearbychests.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractContainerScreen.class)
public interface HandledScreenAccessor {

    @Accessor(value = "leftPos")
    int getX();

    @Accessor(value = "leftPos")
    void setX(int x);

    @Accessor(value = "topPos")
    int getY();

    @Accessor(value = "topPos")
    void setY(int y);

    @Accessor(value = "hoveredSlot")
    @Nullable
    Slot getFocusedSlot();

    @Accessor(value = "imageWidth")
    int getBackgroundWidth();

    @Accessor(value = "imageHeight")
    int getBackgroundHeight();

    @Invoker(value = "findSlot")
    @Nullable
    Slot invokeGetSlotAt(double x, double y);
}
