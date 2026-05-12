package org.nia.niamod.config.setting;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanSetting extends ConfigSetting<Boolean> {
    public BooleanSetting(String id, String title, String description, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(id, title, description, SettingKind.BOOLEAN, getter, setter);
    }

    @Override
    public String format() {
        return Boolean.toString(get());
    }

    @Override
    public boolean tryParseAndSet(String rawValue) {
        if (!"true".equalsIgnoreCase(rawValue) && !"false".equalsIgnoreCase(rawValue)) {
            return false;
        }
        set(Boolean.parseBoolean(rawValue));
        return true;
    }
}
