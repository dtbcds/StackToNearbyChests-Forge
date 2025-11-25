package com.dtbcds.stacktonearbychests;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = "stacktonearbychests", value = Dist.CLIENT)
public abstract class ForEachContainerTask {

    private static final ScheduledThreadPoolExecutor TIMER = new ScheduledThreadPoolExecutor(1);
    private static ForEachContainerTask currentTask;

    protected final Minecraft client;
    protected final LocalPlayer player;
    protected final Consumer<AbstractContainerMenu> action;

    private boolean interrupted;
    private final int searchInterval;

    private int waitTicks = 0;
    private AbstractContainerMenu pendingScreen = null;
    private int timeoutTicks = 0;

    @Nullable
    private ForEachContainerTask after;

    public ForEachContainerTask(Minecraft client, LocalPlayer player, Consumer<AbstractContainerMenu> action) {
        this.client = client;
        this.player = player;
        this.action = action;
        searchInterval = ModOptions.get().behavior.searchInterval.intValue();
    }

    public static void init() {
        // Registered via @EventBusSubscriber
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (isRunning()) {
            if (event.getScreen() instanceof DeathScreen) {
                currentTask.interrupt();
            } else {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (isRunning() && event.getKey() == GLFW.GLFW_KEY_ESCAPE && event.getAction() == GLFW.GLFW_PRESS) {
            currentTask.interrupt();
            // We can't cancel KeyInputEvent in a way that stops other mods effectively if
            // they use raw input,
            // but we can try. However, usually we just handle it.
        }
    }

    @SubscribeEvent
    public static void onClientChatReceived(ClientChatReceivedEvent event) {
        if (isRunning()
                && event.getMessage().getContents() instanceof TranslatableContents translatable
                && translatable.getKey().equals("container.isLocked")) {
            getCurrentTask().openNextContainer();
        }
    }

    public static ForEachContainerTask getCurrentTask() {
        return currentTask;
    }

    public static boolean isRunning() {
        return currentTask != null;
    }

    public void start() {
        currentTask = this;
        openNextContainerExceptionHandled();
    }

    protected void stop() {
        TIMER.getQueue().clear();

        currentTask = null;

        // Open the inventory screen to keep it visible and interactable
        client.execute(() -> {
            if (player != null && client.screen != null) {
                // Wait one tick for server to process the close
                client.tell(() -> {
                    // Open the inventory screen
                    player.containerMenu = player.inventoryMenu;
                });
            }
        });
    }

    public void interrupt() {
        sendChatMessage("stacktonearbychests.message.actionInterrupted");
        interrupted = true;
    }

    public void onInventory(AbstractContainerMenu screenHandler) {
        clearTimeout();
        // Delay processing to allow items to sync (tick-based)
        this.pendingScreen = screenHandler;
        this.waitTicks = 1; // 1 ticks = ~50ms
    }

    private void openNextContainer() {
        if (interrupted) {
            stop();
            return;
        }

        if (searchInterval == 0) {
            openNextContainerExceptionHandled();
        } else {
            TIMER.schedule(() -> client.execute(this::openNextContainerExceptionHandled), searchInterval,
                    TimeUnit.MILLISECONDS);
        }
    }

    private void openNextContainerExceptionHandled() {
        // This method may be submitted to MinecraftClient for execution, so exceptions
        // need to be handled here
        try {
            if (findAndOpenNextContainer()) {
                setTimeout();
            } else if (after != null) {
                after.start();
            } else {
                stop();
            }
        } catch (Exception e) {
            sendChatMessage("stacktonearbychests.message.exceptionOccurred");
            StackToNearbyChests.LOGGER.error("An exception occurred", e);
            stop();
        }
    }

    /**
     * Open the next container.
     * 
     * @return {@code true} if successfully found and interacted with an eligible
     *         container
     */
    protected abstract boolean findAndOpenNextContainer();

    private void setTimeout() {
        this.timeoutTicks = 40; // 2 seconds at 20 tps
    }

    private void clearTimeout() {
        this.timeoutTicks = 0;
    }

    public static void handleTick() {
        ForEachContainerTask task = getCurrentTask();
        if (task != null) {
            task.onTick();
        }
    }

    public void onTick() {
        if (waitTicks > 0) {
            waitTicks--;
            if (waitTicks == 0 && pendingScreen != null) {
                // Verify the screen is still open and matches
                if (client.player != null && client.player.containerMenu == pendingScreen) {
                    action.accept(pendingScreen);
                    openNextContainer();
                } else {
                    // Screen changed during delay, just move on
                    openNextContainer();
                }
                pendingScreen = null;
            }
        }

        if (timeoutTicks > 0) {
            timeoutTicks--;
            if (timeoutTicks == 0) {
                sendChatMessage("stacktonearbychests.message.interruptedByTimeout");
                stop();
            }
        }
    }

    public void thenStart(ForEachContainerTask after) {
        this.after = after;
    }

    private void sendChatMessage(String key) {
        Minecraft.getInstance().gui.getChat().addMessage(Component.translatable(key));
    }
}
