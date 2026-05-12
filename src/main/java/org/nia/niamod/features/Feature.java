package org.nia.niamod.features;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

import static org.nia.niamod.NiamodClient.LOGGER;

public abstract class Feature {
    @Getter
    @Setter
    protected boolean enabled = true;

    public abstract void init();

    public String getFeatureName() {
        return getClass().getSimpleName();
    }

    public boolean isDisabled() {
        return !enabled;
    }

    public final void runSafe(String action, Runnable runnable) {
        if (isDisabled()) {
            return;
        }

        try {
            runnable.run();
        } catch (Throwable throwable) {
            disableAfterCrash(action, throwable);
        }
    }

    public final <T> T callSafe(String action, Supplier<T> supplier, T fallback) {
        if (isDisabled()) {
            return fallback;
        }

        try {
            return supplier.get();
        } catch (Throwable throwable) {
            disableAfterCrash(action, throwable);
            return fallback;
        }
    }

    protected final Runnable safeRunnable(String action, Runnable runnable) {
        return () -> runSafe(action, runnable);
    }

    public final void disableAfterCrash(String action, Throwable throwable) {
        LOGGER.error("Feature {} crashed during {} and is being disabled.", getFeatureName(), action, throwable);
        setEnabled(false);
    }
}
