package com.dtbcds.stacktonearbychests;

import com.dtbcds.stacktonearbychests.mixin.HandledScreenAccessor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static com.dtbcds.stacktonearbychests.StackToNearbyChests.LOGGER;
import static java.util.function.Predicate.not;

@Mod.EventBusSubscriber(modid = "stacktonearbychests", value = Dist.CLIENT)
public class LockedSlots {
    private static final Path LOCKED_SLOTS_FOLDER = ModOptions.MOD_OPTIONS_DIR.resolve("locked-slots");
    public static final List<ResourceLocation> FAVORITE_ITEM_TAGS = List.of(
            new ResourceLocation(ModOptions.MOD_ID, "gold_badge"),
            new ResourceLocation(ModOptions.MOD_ID, "red_background"),
            new ResourceLocation(ModOptions.MOD_ID, "gold_border"),
            new ResourceLocation(ModOptions.MOD_ID, "iron_border"));

    private static HashSet<Integer> currentLockedSlots = new HashSet<>();
    private static boolean movingFavoriteItemStack = false;
    private static Slot quickMoveDestination;
    @Nullable
    private static ClickType actionBeingExecuted;

    private static Optional<Path> currentLockedSlotsFilePath = Optional.empty();

    public static void init() {
        // Registered via @EventBusSubscriber
    }

    @SubscribeEvent
    public static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft client = Minecraft.getInstance();
        currentLockedSlotsFilePath = getLockedSlotsFilePath(client);

        if (isEnabled()) {
            currentLockedSlotsFilePath.ifPresentOrElse(LockedSlots::read,
                    () -> LOGGER.info("Locked slots file path is empty"));
        }
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        if (isEnabled()) {
            currentLockedSlotsFilePath.ifPresentOrElse(LockedSlots::write,
                    () -> LOGGER.info("Locked slots file path is empty"));
        }

        currentLockedSlots.clear();
        currentLockedSlotsFilePath = Optional.empty();
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> handledScreen) {
            if (!isEnabled()) {
                currentLockedSlots.clear();
            }
            refresh(handledScreen.getMenu());
            movingFavoriteItemStack = false;
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?>) {
            movingFavoriteItemStack = false;
        }
    }

    @SubscribeEvent
    public static void onContainerRenderBackground(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        for (Slot slot : screen.getMenu().slots) {
            drawFavoriteItemStyle(event.getGuiGraphics(), slot, false, screen.getGuiLeft(), screen.getGuiTop());
        }
    }

    @SubscribeEvent
    public static void onContainerRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        // Foreground event usually has matrix stack translated to leftPos, topPos?
        // Let's check. Render.Foreground is in renderLabels.
        // renderLabels is usually called with matrix stack translated.
        // So we might not need offsets if we use slot.x/y directly?
        // Wait, slot.x/y are relative to leftPos/topPos.
        // If matrix is translated, slot.x/y are correct.
        // But Render.Background is NOT translated (it's in renderBg or after).
        // renderBg is usually not translated?

        // Actually, let's look at AbstractContainerScreen.render:
        // renderBackground(guiGraphics);
        // super.render(guiGraphics, mouseX, mouseY, partialTick); -> calls renderBg
        // renderTooltip...

        // renderBg is abstract. Implementations usually draw texture.
        // It is usually called with identity matrix.

        // So for Background event (which fires after renderBg), we need offsets.

        // For Foreground event (which fires in renderLabels), renderLabels is called
        // from render?
        // No, renderLabels is called from super.render?
        // Wait, AbstractContainerScreen.render calls renderBg, then super.render?
        // No.
        // AbstractContainerScreen.render:
        // this.renderBackground(guiGraphics);
        // super.render(guiGraphics, mouseX, mouseY, partialTick);
        // this.renderTooltip(guiGraphics, mouseX, mouseY);

        // super.render (Screen.render) does nothing?
        // AbstractContainerScreen overrides render?
        // Yes.
        // It calls renderBg.
        // Then it loops slots and calls renderSlot.
        // Then it calls renderLabels.

        // So Render.Background fires after renderBg?
        // Forge injects Render.Background in renderBg? No.
        // Forge injects Render.Background at the end of renderBg?
        // Or in render?

        // Actually, ContainerScreenEvent.Render.Background is fired in
        // AbstractContainerScreen.render AFTER renderBg and BEFORE slots.
        // So it is at 0,0.

        // ContainerScreenEvent.Render.Foreground is fired in
        // AbstractContainerScreen.render AFTER slots and BEFORE renderLabels?
        // Or inside renderLabels?
        // It's fired in renderLabels.
        // renderLabels is usually called with matrix translated to (leftPos, topPos).
        // So for Foreground, we don't need offsets if we use slot.x/y.

        // However, drawFavoriteItemStyle needs to know if it should add offsets.
        // I'll update the method signature.

        for (Slot slot : screen.getMenu().slots) {
            drawFavoriteItemStyle(event.getGuiGraphics(), slot, true, 0, 0);
        }
    }

    // Called from StackToNearbyChests or Mixin
    public static void handleMarkAsFavorite(AbstractContainerScreen<?> screen) {
        Minecraft client = Minecraft.getInstance();
        double x = client.mouseHandler.xpos() * (double) client.getWindow().getGuiScaledWidth()
                / (double) client.getWindow().getScreenWidth();
        double y = client.mouseHandler.ypos() * (double) client.getWindow().getGuiScaledHeight()
                / (double) client.getWindow().getScreenHeight();
        Slot slot = ((HandledScreenAccessor) screen).invokeGetSlotAt(x, y);
        if (!unLock(slot) && slot != null && slot.hasItem()) {
            lock(slot);
        }
    }

    // Called from Mixin
    public static InteractionResult onBeforeClickSlot(int slotId, int button, ClickType actionType, Player player) {
        @Nullable
        Slot slot = slotId < 0 ? null : player.containerMenu.getSlot(slotId);
        if (isLocked(slot) && (actionType == ClickType.PICKUP
                && ModOptions.get().behavior.favoriteItemsCannotBePickedUp.booleanValue()
                || actionType == ClickType.QUICK_MOVE
                        && ModOptions.get().behavior.favoriteItemStacksCannotBeQuickMoved.booleanValue()
                || actionType == ClickType.SWAP
                        && ModOptions.get().behavior.favoriteItemStacksCannotBeSwapped.booleanValue()
                || actionType == ClickType.THROW
                        && ModOptions.get().behavior.favoriteItemStacksCannotBeThrown.booleanValue())) {
            return InteractionResult.FAIL;
        }

        if (slot != null && slot.hasItem()
                && ModOptions.get().keymap.markAsFavoriteKey.matches(button - KeySequence.MOUSE_BUTTON_CODE_OFFSET)) {
            if (isLocked(slot)) {
                unLock(slot);
            } else {
                lock(slot);
            }
            return InteractionResult.FAIL;
        }

        actionBeingExecuted = actionType;

        return InteractionResult.PASS;
    }

    // Called from Mixin
    public static void onAfterClickSlot(int slotId, int button, ClickType actionType, Player player) {
        AbstractContainerMenu screenHandler = player.containerMenu;
        @Nullable
        Slot slot = slotId < 0 ? null : screenHandler.getSlot(slotId);
        switch (actionType) {
            case PICKUP -> {
                if (slotId == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE) { // Throw
                    movingFavoriteItemStack = false;
                }
                if (slot == null) {
                    break;
                }

                ItemStack cursorStack = screenHandler.getCarried();
                ItemStack slotStack = slot.getItem();
                if (movingFavoriteItemStack) {
                    if (cursorStack.isEmpty()) {
                        lock(slot);
                        movingFavoriteItemStack = false;
                    } else if (!ItemStack.isSameItemSameTags(cursorStack, slotStack)) { // Swap the slot with the cursor
                        if (!isLocked(slot)) {
                            movingFavoriteItemStack = false;
                        }
                        lock(slot);
                    }
                } else {
                    if (isLocked(slot) && slotStack.isEmpty()) {
                        unLock(slot);
                        movingFavoriteItemStack = true;
                    } else if (!cursorStack.isEmpty()
                            && !ItemStack.isSameItemSameTags(cursorStack, slotStack)) { // Swap the slot with the cursor
                        if (isLocked(slot)) {
                            movingFavoriteItemStack = true;
                        }
                        unLock(slot);
                    }
                }
            }
            case QUICK_MOVE -> {
                if (slot == null) {
                    break;
                }
                if (isLocked(slot) && !slot.hasItem()) {
                    unLock(slot);
                    lock(quickMoveDestination);
                }
            }
            case SWAP -> {
                boolean isFromSlotLocked = unLock(slot);
                boolean isToSlotLocked = unLock(button); // The variable button holds the index of hotbar slot when
                                                         // swapping
                if (isFromSlotLocked) {
                    lock(button);
                }
                if (isToSlotLocked) {
                    lock(slot);
                }
            }
            case QUICK_CRAFT -> {
                if (screenHandler.getCarried().isEmpty()) {
                    movingFavoriteItemStack = false;
                } else if (movingFavoriteItemStack) {
                    lock(slot);
                }
            }
        }

        actionBeingExecuted = null;
    }

    private static void read(Path path) {
        LOGGER.info("Reading locked slot indices from {}", path.getFileName());

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            Type type = new TypeToken<HashSet<Integer>>() {
            }.getType();
            currentLockedSlots = new Gson().fromJson(reader, type);
        } catch (NoSuchFileException e) {
            LOGGER.info("Locked slots file does not exist");
        } catch (IOException e) {
            LOGGER.error("Failed to read locked slots file", e);
        }
    }

    private static void write(Path path) {
        LOGGER.info("Writing locked slot indices to {}", path.getFileName());

        try {
            Files.createDirectories(LOCKED_SLOTS_FOLDER);
            String json = new Gson().toJson(currentLockedSlots);
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to write locked slots file", e);
        }
    }

    private static Optional<Path> getLockedSlotsFilePath(Minecraft client) {
        var integratedServer = client.getSingleplayerServer();
        ServerData currentServerEntry = client.getCurrentServer();
        String fileName;

        if (integratedServer != null) {
            // For singleplayer, use the level name from the client's connection
            // This avoids the ClassCastException with IntegratedServer
            var connection = client.getConnection();
            if (connection != null && connection.getLevel() != null) {
                // Use the level's dimension location as a unique identifier
                fileName = connection.getLevel().dimension().location().toString().replace(":", "_") + ".json";
            } else {
                // Fallback: use a generic singleplayer filename
                fileName = "singleplayer.json";
            }
        } else if (currentServerEntry != null) {
            fileName = currentServerEntry.ip.concat(".json").replace(":", "colon");
        } else {
            LOGGER.info("Could not get level name or server address");
            return Optional.empty();
        }

        return Optional.of(LOCKED_SLOTS_FOLDER.resolve(fileName));
    }

    private static boolean isEnabled() {
        return ModOptions.get().behavior.enableItemFavoriting.booleanValue();
    }

    private static boolean lock(@Nullable Slot slot) {
        return isLockable(slot) && lock(slot.getSlotIndex());
    }

    private static boolean lock(int slotIndex) {
        return isLockable(slotIndex) && currentLockedSlots.add(slotIndex);
    }

    private static boolean unLock(@Nullable Slot slot) {
        return isLockable(slot) && unLock(slot.getSlotIndex());
    }

    private static boolean unLock(int slotIndex) {
        return isLockable(slotIndex) && currentLockedSlots.remove(slotIndex);
    }

    private static boolean setLocked(int slotIndex, boolean locked) {
        return locked ? lock(slotIndex) : unLock(slotIndex);
    }

    public static boolean isLocked(@Nullable Slot slot) {
        return isLockable(slot) && isLocked(slot.getSlotIndex());
    }

    public static boolean isLocked(int slotIndex) {
        return isLockable(slotIndex) && currentLockedSlots.contains(slotIndex);
    }

    private static boolean isLockable(@Nullable Slot slot) {
        return isEnabled()
                && slot != null
                && slot.container instanceof Inventory
                && !Minecraft.getInstance().player.getAbilities().instabuild;
    }

    private static boolean isLockable(int slotIndex) {
        return isEnabled()
                && slotIndex >= 0
                && slotIndex != 39 // Head
                && slotIndex != 38 // Chest
                && slotIndex != 37 // Legs
                && slotIndex != 36;// Feet
    }

    public static void onSetStack(int slotIndex, ItemStack stack) {
        if (stack.isEmpty()) {
            if (actionBeingExecuted == null) {
                if (StackToNearbyChests.IS_EASY_SHULKER_BOXES_MOD_LOADED
                        || StackToNearbyChests.IS_METAL_BUNDLES_MOD_LOADED) {
                    return;
                }

                unLock(slotIndex);
            } else if (actionBeingExecuted == ClickType.THROW) {
                unLock(slotIndex);
            } else if (actionBeingExecuted == ClickType.PICKUP_ALL) {
                if (unLock(slotIndex)) {
                    movingFavoriteItemStack = true;
                }
            }
        }
    }

    public static void onInsertItem(Slot destination) {
        quickMoveDestination = destination;
    }

    private static boolean refresh(AbstractContainerMenu screenHandler) {
        return screenHandler.slots.stream()
                .filter(not(Slot::hasItem))
                .map(LockedSlots::unLock)
                .reduce(Boolean::logicalOr)
                .orElse(false);
    }

    public static void onSetGameMode(GameType gameMode) {
        if (gameMode == GameType.CREATIVE) {
            currentLockedSlots.clear();
        }
    }

    public static void drawFavoriteItemStyle(GuiGraphics context, Slot slot, boolean isForeground, int xOffset,
            int yOffset) {
        ModOptions options = ModOptions.get();

        if (!(options.appearance.alwaysShowMarkersForFavoritedItems.booleanValue()
                || options.keymap.showMarkersForFavoritedItemsKey.isPressed()
                || options.keymap.markAsFavoriteKey.isPressed())) {
            return;
        }

        ResourceLocation id = options.appearance.favoriteItemStyle;
        if (isLocked(slot)) {
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(new ResourceLocation(id.getNamespace(), "item/" + id.getPath()));
            RenderSystem.setShaderTexture(0, sprite.atlasLocation());
            if (isForeground && id.getPath().equals("gold_badge")) {
                context.blit(xOffset + slot.x, yOffset + slot.y, 400, 16, 16, sprite);
            } else {
                context.blit(xOffset + slot.x, yOffset + slot.y, 0, 16, 16, sprite);
            }
        }
    }

    public static InteractionResult beforeDropSelectedItem(int selectedSlotIndex) {
        if (isLocked(selectedSlotIndex) && ModOptions.get().behavior.favoriteItemStacksCannotBeThrown.booleanValue()) {
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }

    public static void afterDropSelectedItem(int selectedSlotIndex) {
        if (isLocked(selectedSlotIndex)
                && !Minecraft.getInstance().player.inventoryMenu.slots.get(selectedSlotIndex).hasItem()) {
            unLock(selectedSlotIndex);
        }
    }

    public static InteractionResult onSwapItemWithOffhand() {
        int selectedSlotIndex = Minecraft.getInstance().player.getInventory().selected;
        boolean isSelectedSlotLocked = isLocked(selectedSlotIndex);

        if (isSelectedSlotLocked && ModOptions.get().behavior.favoriteItemsCannotBeSwappedWithOffhand.booleanValue()) {
            return InteractionResult.FAIL;
        }

        setLocked(selectedSlotIndex, isLocked(Inventory.SLOT_OFFHAND));
        setLocked(Inventory.SLOT_OFFHAND, isSelectedSlotLocked);

        return InteractionResult.PASS;
    }
}
