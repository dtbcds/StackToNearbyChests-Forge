package com.dtbcds.stacktonearbychests.gui;

import com.dtbcds.stacktonearbychests.ModOptions;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class ModOptionsGui {

        public static Screen createConfigScreen(Screen parent) {
                ModOptions options = ModOptions.get();
                ConfigBuilder builder = ConfigBuilder.create()
                                .setParentScreen(parent)
                                .setTitle(Component.translatable("stacktonearbychests.options.title"));

                ConfigEntryBuilder entryBuilder = builder.entryBuilder();

                // Appearance Category
                ConfigCategory appearance = builder
                                .getOrCreateCategory(Component.translatable("stacktonearbychests.options.appearance"));

                appearance.addEntry(entryBuilder
                                .startBooleanToggle(Component.translatable(
                                                "stacktonearbychests.options.showStackToNearbyContainersButton"),
                                                options.appearance.showStackToNearbyContainersButton.booleanValue())
                                .setDefaultValue(true)
                                .setSaveConsumer(newValue -> options.appearance.showStackToNearbyContainersButton
                                                .setValue(newValue))
                                .build());

                appearance.addEntry(entryBuilder
                                .startBooleanToggle(Component.translatable(
                                                "stacktonearbychests.options.showRestockFromNearbyContainersButton"),
                                                options.appearance.showRestockFromNearbyContainersButton.booleanValue())
                                .setDefaultValue(true)
                                .setSaveConsumer(newValue -> options.appearance.showRestockFromNearbyContainersButton
                                                .setValue(newValue))
                                .build());

                appearance.addEntry(entryBuilder
                                .startBooleanToggle(
                                                Component.translatable(
                                                                "stacktonearbychests.options.showQuickStackButton"),
                                                options.appearance.showQuickStackButton.booleanValue())
                                .setDefaultValue(true)
                                .setSaveConsumer(newValue -> options.appearance.showQuickStackButton.setValue(newValue))
                                .build());

                appearance.addEntry(entryBuilder
                                .startBooleanToggle(
                                                Component.translatable("stacktonearbychests.options.showRestockButton"),
                                                options.appearance.showRestockButton.booleanValue())
                                .setDefaultValue(true)
                                .setSaveConsumer(newValue -> options.appearance.showRestockButton.setValue(newValue))
                                .build());

                appearance.addEntry(entryBuilder.startBooleanToggle(Component
                                .translatable("stacktonearbychests.options.showTheButtonsOnTheCreativeInventoryScreen"),
                                options.appearance.showTheButtonsOnTheCreativeInventoryScreen.booleanValue())
                                .setDefaultValue(true)
                                .setSaveConsumer(
                                                newValue -> options.appearance.showTheButtonsOnTheCreativeInventoryScreen
                                                                .setValue(newValue))
                                .build());

                appearance.addEntry(entryBuilder
                                .startBooleanToggle(
                                                Component.translatable("stacktonearbychests.options.showButtonTooltip"),
                                                options.appearance.showButtonTooltip.booleanValue())
                                .setDefaultValue(true)
                                .setSaveConsumer(newValue -> options.appearance.showButtonTooltip.setValue(newValue))
                                .build());

                appearance.addEntry(entryBuilder
                                .startBooleanToggle(Component.translatable(
                                                "stacktonearbychests.options.alwaysShowMarkersForFavoritedItems"),
                                                options.appearance.alwaysShowMarkersForFavoritedItems.booleanValue())
                                .setDefaultValue(true)
                                .setTooltip(Component.translatable(
                                                "stacktonearbychests.options.alwaysShowMarkersForFavoritedItems.tooltip"))
                                .setSaveConsumer(newValue -> options.appearance.alwaysShowMarkersForFavoritedItems
                                                .setValue(newValue))
                                .build());

                appearance.addEntry(entryBuilder
                                .startSelector(Component.translatable("stacktonearbychests.options.favoriteItemStyle"),
                                                List.of(
                                                                ResourceLocation.fromNamespaceAndPath(ModOptions.MOD_ID, "gold_badge"),
                                                                ResourceLocation.fromNamespaceAndPath(ModOptions.MOD_ID,
                                                                                "red_background"),
                                                                ResourceLocation.fromNamespaceAndPath(ModOptions.MOD_ID, "gold_border"),
                                                                ResourceLocation.fromNamespaceAndPath(ModOptions.MOD_ID, "iron_border"))
                                                                .toArray(new ResourceLocation[0]),
                                                options.appearance.favoriteItemStyle)
                                .setDefaultValue(ResourceLocation.fromNamespaceAndPath(ModOptions.MOD_ID, "gold_badge"))
                                .setNameProvider(id -> Component.literal(id.getPath().replace("_", " ")))
                                .setSaveConsumer(newValue -> options.appearance.favoriteItemStyle = newValue)
                                .build());

                appearance.addEntry(entryBuilder
                                .startIntField(Component.translatable(
                                                "stacktonearbychests.options.stackToNearbyContainersButtonPosX"),
                                                options.appearance.stackToNearbyContainersButtonPosX.intValue())
                                .setDefaultValue(140)
                                .setSaveConsumer(newValue -> options.appearance.stackToNearbyContainersButtonPosX
                                                .setValue(newValue))
                                .build());

                appearance.addEntry(entryBuilder
                                .startIntField(Component.translatable(
                                                "stacktonearbychests.options.stackToNearbyContainersButtonPosY"),
                                                options.appearance.stackToNearbyContainersButtonPosY.intValue())
                                .setDefaultValue(170)
                                .setSaveConsumer(newValue -> options.appearance.stackToNearbyContainersButtonPosY
                                                .setValue(newValue))
                                .build());

                // ... Add other position fields similarly ...
                appearance.addEntry(entryBuilder
                                .startIntField(Component.translatable(
                                                "stacktonearbychests.options.restockFromNearbyContainersButtonPosX"),
                                                options.appearance.restockFromNearbyContainersButtonPosX.intValue())
                                .setDefaultValue(160)
                                .setSaveConsumer(newValue -> options.appearance.restockFromNearbyContainersButtonPosX
                                                .setValue(newValue))
                                .build());

                appearance.addEntry(entryBuilder
                                .startIntField(Component.translatable(
                                                "stacktonearbychests.options.restockFromNearbyContainersButtonPosY"),
                                                options.appearance.restockFromNearbyContainersButtonPosY.intValue())
                                .setDefaultValue(170)
                                .setSaveConsumer(newValue -> options.appearance.restockFromNearbyContainersButtonPosY
                                                .setValue(newValue))
                                .build());

                // Behavior Category
                ConfigCategory behavior = builder
                                .getOrCreateCategory(Component.translatable("stacktonearbychests.options.behavior"));

                behavior.addEntry(entryBuilder
                                .startIntField(Component.translatable("stacktonearbychests.options.searchInterval"),
                                                options.behavior.searchInterval.intValue())
                                .setDefaultValue(0)
                                .setTooltip(Component
                                                .translatable("stacktonearbychests.options.searchInterval.tooltip"))
                                .setSaveConsumer(newValue -> options.behavior.searchInterval.setValue(newValue))
                                .build());

                behavior.addEntry(entryBuilder
                                .startBooleanToggle(Component.translatable(
                                                "stacktonearbychests.options.supportForContainerEntities"),
                                                options.behavior.supportForContainerEntities.booleanValue())
                                .setDefaultValue(true)
                                .setSaveConsumer(newValue -> options.behavior.supportForContainerEntities
                                                .setValue(newValue))
                                .build());

                behavior.addEntry(entryBuilder
                                .startBooleanToggle(Component.translatable(
                                                "stacktonearbychests.options.doNotQuickStackItemsFromTheHotbar"),
                                                options.behavior.doNotQuickStackItemsFromTheHotbar.booleanValue())
                                .setDefaultValue(false)
                                .setSaveConsumer(newValue -> options.behavior.doNotQuickStackItemsFromTheHotbar
                                                .setValue(newValue))
                                .build());

                behavior.addEntry(entryBuilder
                                .startBooleanToggle(
                                                Component.translatable(
                                                                "stacktonearbychests.options.enableItemFavoriting"),
                                                options.behavior.enableItemFavoriting.booleanValue())
                                .setDefaultValue(true)
                                .setSaveConsumer(newValue -> options.behavior.enableItemFavoriting.setValue(newValue))
                                .build());

                // Lists
                behavior.addEntry(entryBuilder
                                .startStrList(Component.translatable("stacktonearbychests.options.stackingTargets"),
                                                List.copyOf(options.behavior.stackingTargets))
                                .setDefaultValue(List.copyOf(ModOptions.getDefault().behavior.stackingTargets))
                                .setSaveConsumer(newValue -> options.behavior.stackingTargets = java.util.Set
                                                .copyOf(newValue))
                                .build());

                behavior.addEntry(entryBuilder
                                .startStrList(Component
                                                .translatable("stacktonearbychests.options.stackingTargetEntities"),
                                                List.copyOf(options.behavior.stackingTargetEntities))
                                .setDefaultValue(List.copyOf(ModOptions.getDefault().behavior.stackingTargetEntities))
                                .setSaveConsumer(newValue -> options.behavior.stackingTargetEntities = java.util.Set
                                                .copyOf(newValue))
                                .build());

                // ... Add other lists similarly ...

                builder.setSavingRunnable(options::write);

                return builder.build();
        }
}
