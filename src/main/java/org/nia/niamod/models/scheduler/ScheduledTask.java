package org.nia.niamod.models.scheduler;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public final class ScheduledTask {
    private final Runnable task;
    private final IntSupplier intervalTicks;
    private final BooleanSupplier cancelCondition;
    private final ScheduleMode mode;
    private int ticksRemaining;

    private ScheduledTask(Runnable task, int delayTicks, IntSupplier intervalTicks, BooleanSupplier cancelCondition, ScheduleMode mode) {
        this.task = task;
        this.ticksRemaining = delayTicks;
        this.intervalTicks = intervalTicks;
        this.cancelCondition = cancelCondition;
        this.mode = mode;
    }

    public static ScheduledTask delayed(Runnable task, int delayTicks) {
        return new ScheduledTask(task, delayTicks, () -> 0, () -> false, ScheduleMode.ONE_SHOT);
    }

    public static ScheduledTask repeating(Runnable task, int delayTicks, IntSupplier intervalTicks, BooleanSupplier cancelCondition) {
        return new ScheduledTask(task, delayTicks, intervalTicks, cancelCondition, ScheduleMode.REPEATING);
    }

    public boolean tick() {
        if (cancelCondition.getAsBoolean()) {
            return true;
        }

        if (ticksRemaining > 0) {
            ticksRemaining--;
        }

        if (ticksRemaining > 0) {
            return false;
        }

        task.run();
        if (mode == ScheduleMode.ONE_SHOT) {
            return true;
        }

        ticksRemaining = Math.max(0, intervalTicks.getAsInt());
        return false;
    }
}
