package org.nia.niamod.config.setting;

import lombok.Getter;

@Getter
public class ButtonSetting extends ConfigSetting<Runnable> {
    private final Runnable action;
    private final String buttonText;

    public ButtonSetting(String id, String title, String description, Runnable action, String buttonText) {
        super(id, title, description, SettingKind.BUTTON, () -> action, a -> {
        });
        this.action = action;
        this.buttonText = buttonText;
    }

    @Override
    public Runnable get() {
        return super.get();
    }

    @Override
    public String format() {
        return "";
    }

    @Override
    public boolean tryParseAndSet(String rawValue) {
        return false;
    }
}
