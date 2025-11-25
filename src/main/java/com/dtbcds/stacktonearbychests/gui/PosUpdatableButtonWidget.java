package com.dtbcds.stacktonearbychests.gui;

import com.dtbcds.stacktonearbychests.Vec2i;
import com.dtbcds.stacktonearbychests.mixin.HandledScreenAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class PosUpdatableButtonWidget extends Button {
    private final AbstractContainerScreen<?> parent;
    private final Optional<Function<HandledScreenAccessor, Vec2i>> posUpdater;
    private final int u;
    private final int v;
    private final int hoveredVOffset;
    private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;

    private PosUpdatableButtonWidget(int x,
            int y,
            int width,
            int height,
            int u,
            int v,
            int hoveredVOffset,
            ResourceLocation texture,
            int textureWidth,
            int textureHeight,
            OnPress pressAction,
            Component text,
            AbstractContainerScreen<?> parent,
            Optional<Function<HandledScreenAccessor, Vec2i>> posUpdater) {
        super(x, y, width, height, text, pressAction, DEFAULT_NARRATION);
        this.u = u;
        this.v = v;
        this.hoveredVOffset = hoveredVOffset;
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.parent = parent;
        this.posUpdater = posUpdater;
        // Do not add to parent here, let the caller do it.
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        posUpdater.ifPresent(updater -> {
            if (parent instanceof HandledScreenAccessor accessor) {
                setPos(updater.apply(accessor));
            }
        });

        RenderSystem.setShaderTexture(0, texture);
        int i = v;
        if (!this.isActive()) {
            // i = v + hoveredVOffset * 2; // Assuming standard button texture layout?
            // Actually the original code had isNarratable check?
            // "if (!this.isNarratable())" -> usually means disabled?
            // But standard button logic is:
            // v = 46 + (isHovered ? 20 : 0);

            // Original code:
            // if (!this.isNarratable()) i = v + hoveredVOffset * 2;
            // else if (this.isHovered()) i = v + hoveredVOffset;

            // I will stick to original logic but map isNarratable to isActive?
            // isNarratable is usually true for buttons.
            // Maybe it meant !isActive()?
            // Let's assume !isActive() -> disabled state.
            i = v + hoveredVOffset * 2;
        } else if (this.isHovered()) {
            i = v + hoveredVOffset;
        }

        RenderSystem.enableDepthTest();
        context.blit(texture, getX(), getY(), u, i, width, height, textureWidth, textureHeight);
    }

    public void setPos(Vec2i pos) {
        setX(pos.x());
        setY(pos.y());
    }

    public static class Builder {
        private int x = 0;
        private int y = 0;
        private int width = 16;
        private int height = 16;
        private int u = 0;
        private int v = 0;
        private int hoveredVOffset = 16;
        private ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("minecraft", "missing");
        private int textureWidth = 16;
        private int textureHeight = 16;
        private OnPress pressAction = button -> {
        };
        @Nullable
        private Tooltip tooltip;
        private Component text = CommonComponents.EMPTY;
        private AbstractContainerScreen<?> parent;
        private Optional<Function<HandledScreenAccessor, Vec2i>> posUpdater = Optional.empty();

        public Builder(AbstractContainerScreen<?> parent) {
            this.parent = parent;
        }

        public Builder setPos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder setSize(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder setUV(int u, int v) {
            this.u = u;
            this.v = v;
            return this;
        }

        public Builder setHoveredVOffset(int hoveredVOffset) {
            this.hoveredVOffset = hoveredVOffset;
            return this;
        }

        public Builder setTexture(ResourceLocation texture, int textureWidth, int textureHeight) {
            this.texture = texture;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            return this;
        }

        public Builder setPressAction(OnPress pressAction) {
            this.pressAction = pressAction;
            return this;
        }

        public Builder setTooltip(@Nullable Component content) {
            if (content != null) {
                this.tooltip = Tooltip.create(content);
            }
            return this;
        }

        public Builder setTooltip(@Nullable Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder setText(Component text) {
            this.text = text;
            return this;
        }

        public Builder setPosUpdater(Function<HandledScreenAccessor, Vec2i> posUpdater) {
            this.posUpdater = Optional.ofNullable(posUpdater);
            return this;
        }

        public PosUpdatableButtonWidget build() {
            PosUpdatableButtonWidget button = new PosUpdatableButtonWidget(x,
                    y,
                    width,
                    height,
                    u,
                    v,
                    hoveredVOffset,
                    texture,
                    textureWidth,
                    textureHeight,
                    pressAction,
                    text,
                    parent,
                    posUpdater);
            button.setTooltip(tooltip);
            return button;
        }
    }
}
