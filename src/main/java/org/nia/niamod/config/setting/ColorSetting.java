package org.nia.niamod.config.setting;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorSetting extends ConfigSetting<Integer> {
    public ColorSetting(String id, String title, String description, Supplier<Integer> getter, Consumer<Integer> setter) {
        super(id, title, description, SettingKind.COLOR, getter, setter);
    }

    @Override
    public String format() {
        return String.format(Locale.ROOT, "%06X", get() & 0xFFFFFF);
    }

    @Override
    public boolean tryParseAndSet(String rawValue) {
        String normalised = rawValue.trim().replace("#", "");
        if (normalised.length() != 6) {
            return false;
        }

        try {
            set(Integer.parseInt(normalised, 16));
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
