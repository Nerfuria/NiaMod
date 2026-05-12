package org.nia.niamod.managers;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.nia.niamod.models.scheduler.ScheduledTask;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

@UtilityClass
public class SchedulerManager {
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
    private static ConcurrentLinkedDeque<ScheduledTask> tasks;

    public static void init() {
        tasks = new ConcurrentLinkedDeque<>();
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static void schedule(Runnable task, int delayTicks) {
        tasks.add(ScheduledTask.delayed(task, delayTicks));
    }

    public static void scheduleAsync(Runnable task, int delayTicks) {
        tasks.add(ScheduledTask.delayed(() -> EXECUTOR_SERVICE.execute(task), delayTicks));
    }

    public static void scheduleRepeating(Runnable task, int delayTicks, int intervalTicks, BooleanSupplier cancelCondition) {
        tasks.add(ScheduledTask.repeating(task, delayTicks, () -> intervalTicks, cancelCondition));
    }

    public static void scheduleRepeating(Runnable task, IntSupplier intervalTicks, BooleanSupplier cancelCondition) {
        tasks.add(ScheduledTask.repeating(task, 0, intervalTicks, cancelCondition));
    }

    public static void tick() {
        tasks.removeIf(ScheduledTask::tick);
    }
}
