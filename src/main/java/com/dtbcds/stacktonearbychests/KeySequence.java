package com.dtbcds.stacktonearbychests;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class KeySequence {
    public static final int MOUSE_BUTTON_CODE_OFFSET = 100;

    private static final IntArrayList PRESSING_KEYS = new IntArrayList();

    private List<Integer> keys;
    private final List<Integer> defaultKeys;

    public KeySequence(List<Integer> keys) {
        this.keys = new ArrayList<>(keys);
        this.defaultKeys = new ArrayList<>(keys);
    }

    public static KeySequence empty() {
        return new KeySequence(new ArrayList<>());
    }

    public static void init() {
        // No-op, we handle events in StackToNearbyChests
    }

    public static void updateKey(int key, int action) {
        if (action == GLFW.GLFW_PRESS) {
            if (!PRESSING_KEYS.contains(key)) {
                PRESSING_KEYS.add(key);
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            PRESSING_KEYS.rem(key);
        }
    }

    // Fix the problem that all key bindings cannot be triggered because other mods
    // cancel the key release event
    public static void reCheckPressedKeys() {
        for (int i = 0; i < PRESSING_KEYS.size(); i++) {
            int key = PRESSING_KEYS.getInt(i);
            if (!isKeyPressed(key)) {
                PRESSING_KEYS.rem(key);
                i--;
            }
        }
    }

    public static boolean isKeyPressed(int key) {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return isMouseButton(key)
                ? GLFW.glfwGetMouseButton(window, key + MOUSE_BUTTON_CODE_OFFSET) == GLFW.GLFW_PRESS
                : InputConstants.isKeyDown(window, key);
    }

    private static boolean isMouseButton(int key) {
        return key < -1;
    }

    public void addKey(int key) {
        if (keys.size() >= 3) {
            keys.clear();
        }
        if (!keys.contains(key)) {
            keys.add(key);
        }
    }

    public boolean isEmpty() {
        return keys.isEmpty();
    }

    public boolean isPressed() {
        return !isEmpty() && PRESSING_KEYS.equals(keys);
    }

    public void addMouseButton(int button) {
        addKey(button - MOUSE_BUTTON_CODE_OFFSET);
    }

    public void clear() {
        keys.clear();
    }

    public void reset() {
        keys = new ArrayList<>(defaultKeys);
    }

    public Component getLocalizedText() {
        if (keys.isEmpty()) {
            return Component.translatable("key.keyboard.unknown");
        }

        String localized = keys.stream()
                .map(key -> {
                    if (isMouseButton(key)) {
                        return InputConstants.Type.MOUSE.getOrCreate(key + MOUSE_BUTTON_CODE_OFFSET);
                    } else {
                        return InputConstants.Type.KEYSYM.getOrCreate(key);
                    }
                })
                .map(key -> key.getDisplayName().getString())
                .collect(Collectors.joining(" + "));
        return Component.literal(localized);
    }

    public boolean testThenRun(Runnable action) {
        if (isPressed()) {
            action.run();
            return true;
        }

        return false;
    }

    public boolean matches(int key) {
        if (keys.isEmpty()) {
            return false;
        }

        int lastKey = keys.get(keys.size() - 1);
        if (lastKey != key) {
            return false;
        }

        for (int i = 0; i < keys.size() - 1; i++) {
            if (!PRESSING_KEYS.contains(keys.get(i).intValue())) {
                return false;
            }
        }

        return true;
    }

    // These register methods were used to register callbacks.
    // In Forge, we'll call checkAndRun from the event handler.

    public void registerNotOnScreen(Runnable action, InteractionResult result) {
        // This was used to register a callback.
        // We can't easily register a callback to a static list without managing it.
        // Instead, we will hardcode the checks in StackToNearbyChests for the known
        // keys.
        // Or we can add a list of listeners here.

        // For now, I will modify StackToNearbyChests to call the action directly.
    }
}