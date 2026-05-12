package org.nia.niamod.config.setting;

import lombok.Getter;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
public class IntSetting extends ConfigSetting<Integer> {
    private final int min;
    private final int max;

    public IntSetting(String id, String title, String description, int min, int max, Supplier<Integer> getter, Consumer<Integer> setter) {
        super(id, title, description, SettingKind.INTEGER, getter, setter);
        this.min = min;
        this.max = max;
    }

    @Override
    public String format() {
        return Integer.toString(get());
    }

    @Override
    public boolean tryParseAndSet(String rawValue) {
        try {
            int value = Integer.parseInt(rawValue.trim());
            set(Math.max(min, Math.min(max, value)));
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
