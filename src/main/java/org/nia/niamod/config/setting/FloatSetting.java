package org.nia.niamod.config.setting;

import lombok.Getter;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
public class FloatSetting extends ConfigSetting<Float> {
    private final float min;
    private final float max;

    public FloatSetting(String id, String title, String description, float min, float max, Supplier<Float> getter, Consumer<Float> setter) {
        super(id, title, description, SettingKind.FLOAT, getter, setter);
        this.min = min;
        this.max = max;
    }

    @Override
    public String format() {
        return String.format(Locale.ROOT, "%.2f", get());
    }

    @Override
    public boolean tryParseAndSet(String rawValue) {
        try {
            float value = Float.parseFloat(rawValue.trim());
            set(Math.max(min, Math.min(max, value)));
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
