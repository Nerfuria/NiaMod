package org.nia.niamod.config.setting;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ConfigSetting<T> {
    private final String id;
    private final String title;
    private final String description;
    private final SettingKind kind;
    private final Supplier<T> getter;
    private final Consumer<T> setter;

    public T get() {
        return getter.get();
    }

    public void set(T value) {
        setter.accept(value);
    }

    public abstract String format();

    public abstract boolean tryParseAndSet(String rawValue);

    public boolean isBoolean() {
        return kind == SettingKind.BOOLEAN;
    }
}
