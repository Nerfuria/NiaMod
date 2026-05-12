package org.nia.niamod.eventbus;

import lombok.experimental.UtilityClass;

import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
class CancelableState {
    private static final ConcurrentHashMap<Cancelable, Boolean> CANCELED_STATE = new ConcurrentHashMap<>();

    static void cancel(Cancelable cancelable) {
        CANCELED_STATE.putIfAbsent(cancelable, true);
    }

    static boolean isCanceled(Cancelable cancelable) {
        Boolean canceled = CANCELED_STATE.get(cancelable);
        return canceled != null && canceled;
    }

    static void clear(Cancelable cancelable) {
        CANCELED_STATE.remove(cancelable);
    }
}
