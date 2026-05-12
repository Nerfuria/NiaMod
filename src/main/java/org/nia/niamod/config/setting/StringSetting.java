package org.nia.niamod.config.setting;

import lombok.Getter;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
public class StringSetting extends ConfigSetting<String> {
    private final int maxLength;

    public StringSetting(String id, String title, String description, Supplier<String> getter, Consumer<String> setter) {
        this(id, title, description, getter, setter, 256);
    }

    public StringSetting(String id, String title, String description, Supplier<String> getter, Consumer<String> setter, int maxLength) {
        super(id, title, description, SettingKind.STRING, getter, setter);
        this.maxLength = maxLength;
    }

    @Override
    public String format() {
        return get();
    }

    @Override
    public boolean tryParseAndSet(String rawValue) {
        set(rawValue);
        return true;
    }
}
