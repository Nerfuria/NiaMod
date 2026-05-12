package org.nia.niamod.config.setting;

import org.nia.niamod.config.choices.SettingCategory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record SettingSection(String id, String title, String description, SettingCategory category,
                             Supplier<Boolean> enabledGetter, Consumer<Boolean> enabledSetter,
                             List<ConfigSetting<?>> settings) {

    public static SettingSection standard(
            String id,
            String title,
            String description,
            SettingCategory category,
            Supplier<Boolean> enabledGetter,
            Consumer<Boolean> enabledSetter,
            List<ConfigSetting<?>> settings
    ) {
        return new SettingSection(id, title, description, category, enabledGetter, enabledSetter, settings);
    }


    public boolean hasToggle() {
        return enabledGetter != null && enabledSetter != null;
    }

    public boolean isEnabled() {
        return enabledGetter != null && Boolean.TRUE.equals(enabledGetter.get());
    }

    public void setEnabled(boolean enabled) {
        if (enabledSetter != null) {
            enabledSetter.accept(enabled);
        }
    }

}
