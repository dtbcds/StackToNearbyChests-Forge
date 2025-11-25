package com.dtbcds.stacktonearbychests;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedList;
import java.util.Queue;

@Mod.EventBusSubscriber(modid = "stacktonearbychests", value = Dist.CLIENT)
public class EndWorldTickExecutor {

    private static final Queue<Runnable> tasks = new LinkedList<>();

    public static void init() {
        // Registered via @EventBusSubscriber
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side.isClient()) {
            while (tasks.peek() != null) {
                tasks.poll().run();
            }
        }
    }

    public static void execute(Runnable task) {
        tasks.add(task);
    }
}
