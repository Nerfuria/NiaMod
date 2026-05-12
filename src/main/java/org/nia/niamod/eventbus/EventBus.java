package org.nia.niamod.eventbus;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.nia.niamod.features.Feature;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
@RequiredArgsConstructor
public final class EventBus implements EventDispatcher {
    private final ConcurrentHashMap<Class<?>, ConcurrentLinkedDeque<ListenerInfo>> listeners =
            new ConcurrentHashMap<>();
    private final Consumer<Runnable> mainThreadConsumer;

    public void subscribe(Object listener) {
        Set<Class<?>> hierarchy = new LinkedHashSet<>();
        Class<?> current = listener.getClass();
        while (current != null) {
            hierarchy.add(current);
            current = current.getSuperclass();
        }
        hierarchy.stream()
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                .filter(
                        method ->
                                method.getParameterCount() == 1 && method.isAnnotationPresent(Subscribe.class))
                .forEach(method -> subscribeMethod(listener, method));
    }

    private void subscribeMethod(Object listener, Method method) {
        if (!method.canAccess(listener)) {
            method.setAccessible(true);
        }
        Parameter parameter = method.getParameters()[0];
        Class<?> eventType = parameter.getType();
        Subscribe subscribe = method.getAnnotation(Subscribe.class);
        EventInfo eventInfo = eventType.getAnnotation(EventInfo.class);
        Preference preference = eventInfo != null ? eventInfo.preference() : subscribe.value();
        listeners.compute(
                eventType,
                (type, currentListeners) -> {
                    ConcurrentLinkedDeque<ListenerInfo> next =
                            currentListeners == null ? new ConcurrentLinkedDeque<>() : currentListeners;
                    ListenerInfo info =
                            new ListenerInfo(
                                    listener,
                                    method,
                                    eventType,
                                    preference,
                                    Cancelable.class.isAssignableFrom(eventType),
                                    subscribe.priority());
                    if (next.stream().noneMatch(existing -> existing.sameTarget(info))) {
                        next.add(info);
                    }
                    return next.stream()
                            .sorted((left, right) -> Integer.compare(right.priority(), left.priority()))
                            .collect(Collectors.toCollection(ConcurrentLinkedDeque::new));
                });
    }

    public void unsubscribe(Object listener) {
        listeners.values().stream()
                .flatMap(Collection::stream)
                .forEach(
                        listenerInfo -> {
                            if (!listener.equals(listenerInfo.target())) {
                                return;
                            }
                            listeners.computeIfPresent(
                                    listenerInfo.eventType(),
                                    (type, currentListeners) -> {
                                        currentListeners.remove(listenerInfo);
                                        return currentListeners;
                                    });
                        });
    }

    @Override
    public void dispatch(Object event, CancelableCallback callback) {
        ConcurrentLinkedDeque<ListenerInfo> matchedListeners = listeners.get(event.getClass());
        if (matchedListeners == null || matchedListeners.isEmpty()) {
            matchedListeners = listeners.get(Object.class);
        }
        if (matchedListeners == null || matchedListeners.isEmpty()) {
            return;
        }
        for (ListenerInfo listenerInfo : matchedListeners) {
            switch (listenerInfo.preference()) {
                case MAIN -> mainThreadConsumer.accept(() -> dispatchToListener(event, listenerInfo));
                case DISPATCH, CALLER -> dispatchToListener(event, listenerInfo);
                case POOL -> {
                    if (listenerInfo.cancelable()) {
                        dispatchToListener(event, listenerInfo);
                    } else {
                        ForkJoinPool.commonPool().submit(() -> dispatchToListener(event, listenerInfo));
                    }
                }
            }
            if (listenerInfo.cancelable() && CancelableState.isCanceled((Cancelable) event)) {
                callback.canceled(event);
                break;
            }
        }
        if (event instanceof Cancelable cancelable) {
            CancelableState.clear(cancelable);
        }
    }

    @Override
    public void dispatch(Object event) {
        dispatch(
                event,
                ignored ->
                        log.warning(
                                "Event " + event.getClass().getName() + " was canceled without a callback"));
    }

    private void dispatchToListener(Object event, ListenerInfo listenerInfo) {
        if (listenerInfo.target() instanceof Feature feature && feature.isDisabled()) {
            return;
        }
        try {
            listenerInfo.method().invoke(listenerInfo.target(), event);
        } catch (Throwable exception) {
            Throwable cause = exception instanceof java.lang.reflect.InvocationTargetException invocationTargetException
                    ? invocationTargetException.getCause()
                    : exception;
            if (listenerInfo.target() instanceof Feature feature) {
                feature.disableAfterCrash(listenerInfo.method().getName(), cause);
                return;
            }
            log.log(Level.SEVERE, "Problem invoking listener", cause);
        }
    }

    private record ListenerInfo(
            Object target,
            Method method,
            Class<?> eventType,
            Preference preference,
            boolean cancelable,
            int priority) {
        private boolean sameTarget(ListenerInfo other) {
            return Objects.equals(target, other.target) && Objects.equals(method, other.method);
        }
    }
}
