package com.dtbcds.stacktonearbychests;

import com.dtbcds.stacktonearbychests.gui.ModOptionsGui;
import com.dtbcds.stacktonearbychests.gui.PosUpdatableButtonWidget;
import com.dtbcds.stacktonearbychests.mixin.CreativeModeInventoryScreenAccessor;
import com.dtbcds.stacktonearbychests.mixin.HandledScreenAccessor;
import com.dtbcds.stacktonearbychests.mixin.HorseInventoryScreenAccessor;
import com.dtbcds.stacktonearbychests.mixin.RecipeBookComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

@Mod("stacktonearbychests")
public class StackToNearbyChests {
    public static final Logger LOGGER = LogManager.getLogger("StackToNearbyChests");

    public static final boolean IS_IPN_MOD_LOADED = ModList.get().isLoaded("inventoryprofilesnext");
    public static final boolean IS_EASY_SHULKER_BOXES_MOD_LOADED = ModList.get().isLoaded("easyshulkerboxes");
    public static final boolean IS_METAL_BUNDLES_MOD_LOADED = ModList.get().isLoaded("metalbundles");

    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(ModOptions.MOD_ID,
            "textures/buttons.png");

    public static Optional<PosUpdatableButtonWidget> currentStackToNearbyContainersButton = Optional.empty();

    public StackToNearbyChests() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onInitializeClient);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void onInitializeClient(FMLClientSetupEvent event) {
        KeySequence.init();
        LockedSlots.init();
        InventoryActions.init();
        EndWorldTickExecutor.init();
        ForEachContainerTask.init();

        // Key registration should be handled in a separate KeyMapping registry or
        // similar,
        // but for now we'll assume the existing KeySequence/InventoryActions logic
        // handles the logic,
        // and we just need to hook into events.

        // Note: Key registration in Forge is typically done via
        // RegisterKeyMappingsEvent.
        // However, the original code seems to use a custom KeySequence system.
        // I will keep the init calls but I might need to adapt how keys are polled or
        // registered.
    }

    @SubscribeEvent
    public void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            KeySequence.reCheckPressedKeys();
            ForEachContainerTask.handleTick();
        }
    }

    @SubscribeEvent
    public void onKeyInput(net.minecraftforge.client.event.InputEvent.Key event) {
        KeySequence.updateKey(event.getKey(), event.getAction());
        checkGlobalKeys();
    }

    @SubscribeEvent
    public void onMouseInput(net.minecraftforge.client.event.InputEvent.MouseButton event) {
        KeySequence.updateKey(event.getButton() - KeySequence.MOUSE_BUTTON_CODE_OFFSET, event.getAction());
        checkGlobalKeys();
    }

    private void checkGlobalKeys() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen == null && client.level != null) {
            ModOptions.get().keymap.stackToNearbyContainersKey.testThenRun(InventoryActions::stackToNearbyContainers);
            ModOptions.get().keymap.restockFromNearbyContainersKey
                    .testThenRun(InventoryActions::restockFromNearbyContainers);

            if (ModOptions.get().keymap.openModOptionsScreenKey.isPressed()) {
                client.setScreen(ModOptionsGui.createConfigScreen(null));
            }
        }
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        Minecraft client = Minecraft.getInstance();
        Screen screen = event.getScreen();
        addButtonsAndKeys(client, screen);
    }

    @SubscribeEvent
    public void onScreenKeyPressed(ScreenEvent.KeyPressed.Post event) {
        KeySequence.updateKey(event.getKeyCode(), org.lwjgl.glfw.GLFW.GLFW_PRESS); // Screen events don't always give
                                                                                   // action, but Post usually implies
                                                                                   // press?
        // Wait, KeyPressed is for press. KeyReleased is for release.
        // We need to handle release too to update KeySequence correctly.

        handleScreenInput(event.getScreen());
    }

    @SubscribeEvent
    public void onScreenKeyReleased(ScreenEvent.KeyReleased.Post event) {
        KeySequence.updateKey(event.getKeyCode(), org.lwjgl.glfw.GLFW.GLFW_RELEASE);
    }

    @SubscribeEvent
    public void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Post event) {
        KeySequence.updateKey(event.getButton() - KeySequence.MOUSE_BUTTON_CODE_OFFSET, org.lwjgl.glfw.GLFW.GLFW_PRESS);
        handleScreenInput(event.getScreen());
    }

    @SubscribeEvent
    public void onScreenMouseReleased(ScreenEvent.MouseButtonReleased.Post event) {
        KeySequence.updateKey(event.getButton() - KeySequence.MOUSE_BUTTON_CODE_OFFSET,
                org.lwjgl.glfw.GLFW.GLFW_RELEASE);
    }

    private void handleScreenInput(Screen screen) {
        if (isTextFieldActive(screen) || isInventoryTabNotSelected(screen)) {
            return;
        }

        ModOptions.Keymap keymap = ModOptions.get().keymap;
        boolean triggered = false;

        if (screen instanceof AbstractContainerScreen<?> inventoryScreen) {
            Slot focusedSlot = getFocusedSlot(inventoryScreen);
            if (focusedSlot != null && focusedSlot.hasItem()) {
                triggered = keymap.quickStackItemsOfTheSameTypeAsTheOneUnderTheCursorToNearbyContainersKey
                        .testThenRun(() -> InventoryActions.stackToNearbyContainers(focusedSlot.getItem().getItem()));
            }

            if (!triggered) {
                keymap.stackToNearbyContainersKey.testThenRun(InventoryActions::stackToNearbyContainers);
            }

            keymap.restockFromNearbyContainersKey.testThenRun(InventoryActions::restockFromNearbyContainers);
        } else if (isContainerScreen(screen)) {
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                keymap.quickStackKey.testThenRun(() -> InventoryActions.quickStack(containerScreen.getMenu()));
                keymap.restockKey.testThenRun(() -> InventoryActions.restock(containerScreen.getMenu()));
            }
        }
    }

    private void addButtonsAndKeys(Minecraft client, Screen screen) {
        LocalPlayer player = client.player;
        if (player == null || player.isSpectator()) {
            return;
        }

        ModOptions.Appearance appearanceOption = ModOptions.get().appearance;
        boolean showButtonTooltip = appearanceOption.showButtonTooltip.booleanValue();

        if (screen instanceof AbstractContainerScreen<?> inventoryScreen) {

            if (screen instanceof InventoryScreen
                    || screen instanceof CreativeModeInventoryScreen
                            && appearanceOption.showTheButtonsOnTheCreativeInventoryScreen.booleanValue()) {
                addButtonsOnInventoryScreen(screen, inventoryScreen, showButtonTooltip, appearanceOption);
            }

            // Key events are handled in onKeyboardInput
        } else if (isContainerScreen(screen)) {
            AbstractContainerScreen<?> handledScreen = (AbstractContainerScreen<?>) screen;

            if (ModOptions.get().appearance.showQuickStackButton.booleanValue()) {
                // We need to add the button to the screen.
                // In Forge/Vanilla 1.20, we use screen.addRenderableWidget
                // But PosUpdatableButtonWidget adds itself to the screen in its constructor in
                // the original code?
                // "Screens.getButtons(parent).add(this);" -> This is Fabric API.
                // We need to change PosUpdatableButtonWidget to add itself or we add it here.
                // I will update PosUpdatableButtonWidget to NOT add itself, and add it here.

                PosUpdatableButtonWidget button = new PosUpdatableButtonWidget.Builder(handledScreen)
                        .setUV(32, 0)
                        .setTexture(BUTTON_TEXTURE, 64, 32)
                        .setTooltip(showButtonTooltip
                                ? Component.translatable("stacktonearbychests.tooltip.quickStackButton")
                                : null)
                        .setPosUpdater(parent -> getAbsolutePos(parent, appearanceOption.quickStackButtonPosX,
                                appearanceOption.quickStackButtonPosY))
                        .setPressAction(b -> InventoryActions.quickStack(handledScreen.getMenu()))
                        .build();

                addWidgetToScreen(screen, button);
            }

            if (ModOptions.get().appearance.showRestockButton.booleanValue()) {
                PosUpdatableButtonWidget button = new PosUpdatableButtonWidget.Builder(handledScreen)
                        .setUV(48, 0)
                        .setTexture(BUTTON_TEXTURE, 64, 32)
                        .setTooltip(showButtonTooltip
                                ? Component.translatable("stacktonearbychests.tooltip.restockButton")
                                : null)
                        .setPosUpdater(parent -> getAbsolutePos(parent, appearanceOption.restockButtonPosX,
                                appearanceOption.restockButtonPosY))
                        .setPressAction(b -> InventoryActions.restock(handledScreen.getMenu()))
                        .build();
                addWidgetToScreen(screen, button);
            }
        }
    }

    private static void addButtonsOnInventoryScreen(Screen screen, AbstractContainerScreen<?> inventoryScreen,
            boolean showButtonTooltip, ModOptions.Appearance appearanceOption) {
        if (ModOptions.get().appearance.showStackToNearbyContainersButton.booleanValue()) {
            var buttonWidget = new PosUpdatableButtonWidget.Builder(inventoryScreen)
                    .setUV(0, 0)
                    .setTexture(BUTTON_TEXTURE, 64, 32)
                    .setTooltip(showButtonTooltip
                            ? getTooltipWithHint("stacktonearbychests.tooltip.stackToNearbyContainersButton")
                            : null)
                    .setPosUpdater(parent -> new Vec2i(
                            parent.getX() + appearanceOption.stackToNearbyContainersButtonPosX.intValue(),
                            parent.getY() + appearanceOption.stackToNearbyContainersButtonPosY.intValue()))
                    .setPressAction(button -> {
                        AbstractContainerMenu screenHandler = inventoryScreen.getMenu();
                        ItemStack cursorStack = screenHandler.getCarried();
                        if (cursorStack.isEmpty()) {
                            InventoryActions.stackToNearbyContainers();
                        } else {
                            Item item = cursorStack.getItem();

                            screenHandler.slots.stream()
                                    .filter(slot -> slot.container instanceof Inventory)
                                    .filter(slot -> slot.getSlotIndex() < 36) // 36: feet, 37: legs, 38: chest, 39:
                                                                              // head, 40: offhand
                                    .filter(slot -> !LockedSlots.isLocked(slot))
                                    .filter(slot -> !slot.hasItem()
                                            || InventoryActions.canMerge(slot.getItem(), cursorStack))
                                    .peek(slot -> InventoryActions.pickup(screenHandler, slot))
                                    .anyMatch(slot -> cursorStack.isEmpty());

                            InventoryActions.stackToNearbyContainers(item);
                        }
                    })
                    .build();

            currentStackToNearbyContainersButton = Optional.ofNullable(buttonWidget);
            addWidgetToScreen(screen, buttonWidget);
        }

        if (ModOptions.get().appearance.showRestockFromNearbyContainersButton.booleanValue()) {
            var button = new PosUpdatableButtonWidget.Builder(inventoryScreen)
                    .setUV(16, 0)
                    .setTexture(BUTTON_TEXTURE, 64, 32)
                    .setTooltip(showButtonTooltip
                            ? getTooltipWithHint("stacktonearbychests.tooltip.restockFromNearbyContainersButton")
                            : null)
                    .setPosUpdater(parent -> new Vec2i(
                            parent.getX() + appearanceOption.restockFromNearbyContainersButtonPosX.intValue(),
                            parent.getY() + appearanceOption.restockFromNearbyContainersButtonPosY.intValue()))
                    .setPressAction(b -> InventoryActions.restockFromNearbyContainers())
                    .build();
            addWidgetToScreen(screen, button);
        }
    }

    private static void addWidgetToScreen(Screen screen, GuiEventListener widget) {
        try {
            var method = Screen.class.getDeclaredMethod("m_142416_", GuiEventListener.class);
            method.setAccessible(true);
            method.invoke(screen, widget);
        } catch (Exception ex) {
            LOGGER.error("Failed to add widget to screen", ex);
        }
    }

    private static boolean isTextFieldActive(Screen screen) {
        GuiEventListener focusedElement = screen.getFocused();

        if (focusedElement instanceof RecipeBookComponent recipeBook) {
            EditBox searchField = ((RecipeBookComponentAccessor) recipeBook).getSearchBox();
            if (searchField != null && searchField.isFocused()) {
                return true;
            }
        }

        return focusedElement instanceof EditBox textField && textField.isFocused();
    }

    private static boolean isInventoryTabNotSelected(Screen screen) {
        if (screen instanceof CreativeModeInventoryScreen creativeScreen) {
            CreativeModeTab selectedTab = getCreativeSelectedTab(creativeScreen);
            // If the selected tab is null or not the inventory tab, we consider it not
            // selected
            return selectedTab == null || selectedTab.getType() != CreativeModeTab.Type.INVENTORY;
        }
        return false;
    }

    /**
     * Safely obtains the selected creative tab from a CreativeModeInventoryScreen.
     * Uses the mixin accessor when available, otherwise falls back to reflection.
     */
    private static CreativeModeTab getCreativeSelectedTab(CreativeModeInventoryScreen screen) {
        try {
            return ((CreativeModeInventoryScreenAccessor) screen).getSelectedTab();
        } catch (ClassCastException e) {
            try {
                var field = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
                field.setAccessible(true);
                return (CreativeModeTab) field.get(screen);
            } catch (Exception ex) {
                LOGGER.error("Failed to get selected tab from CreativeModeInventoryScreen", ex);
                return null;
            }
        }
    }

    public static Vec2i getAbsolutePos(HandledScreenAccessor parent, ModOptions.IntOption x, ModOptions.IntOption y) {
        // We need to replace Vec2i.
        return new Vec2i(parent.getX() + parent.getBackgroundWidth() + x.intValue(),
                parent.getY() + parent.getBackgroundHeight() / 2 + y.intValue());
    }

    private static Component getTooltipWithHint(String translationKey) {
        return Component.translatable(translationKey)
                .append("\n")
                .append(Component.translatable("stacktonearbychests.tooltip.hint")
                        .setStyle(Style.EMPTY.withItalic(true).withColor(net.minecraft.ChatFormatting.DARK_GRAY)));
    }

    /**
     * Safely obtains the currently hovered slot from an
     * {@link AbstractContainerScreen}.
     * Uses the {@link HandledScreenAccessor} mixin when available, otherwise falls
     * back to reflection.
     */
    private static Slot getFocusedSlot(AbstractContainerScreen<?> screen) {
        try {
            return ((HandledScreenAccessor) screen).getFocusedSlot();
        } catch (ClassCastException e) {
            try {
                var field = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
                field.setAccessible(true);
                return (Slot) field.get(screen);
            } catch (Exception ex) {
                LOGGER.error("Failed to retrieve focused slot", ex);
                return null;
            }
        }
    }

    public static boolean isContainerScreen(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            return false;
        } else if (screen instanceof BeaconScreen
                || screen instanceof GrindstoneScreen
                || screen instanceof CartographyTableScreen
                || screen instanceof CraftingScreen
                || screen instanceof LoomScreen
                || screen instanceof EnchantmentScreen
                || screen instanceof MerchantScreen
                || screen instanceof AnvilScreen
                || screen instanceof StonecutterScreen
                || screen instanceof InventoryScreen
                || screen instanceof CreativeModeInventoryScreen) {
            return false;
        } else if (screen instanceof HorseInventoryScreen horseScreen) {
            AbstractHorse horse = ((HorseInventoryScreenAccessor) horseScreen).getHorse();
            return horse instanceof AbstractChestedHorse chestedHorse && chestedHorse.hasChest();
        }

        return true;
    }
}