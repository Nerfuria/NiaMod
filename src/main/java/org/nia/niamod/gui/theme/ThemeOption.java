package org.nia.niamod.gui.theme;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum ThemeOption {
    DEFAULT("default", "Default Dark", GuiTheme.defaultTheme()),
    OCEAN("ocean", "Ocean", GuiTheme.builder()
            .background(0xFF0F172A)
            .secondary(0xFF1E293B)
            .textColor(0xFFF8FAFC)
            .secondaryText(0xFFCBD5E1)
            .trinaryText(0xFF94A3B8)
            .overlay(0x26000000)
            .accentColor(0xFF0EA5E9)
            .shadowColor(0x18000000)
            .sliderTrack(0xFF0F172A)
            .scrollbarColor(0x30FFFFFF)
            .build()),
    CHERRY("cherry", "Cherry Blossom", GuiTheme.builder()
            .background(0xFF2D1B2E)
            .secondary(0xFF3D263F)
            .textColor(0xFFFDF2F8)
            .secondaryText(0xFFFBCFE8)
            .trinaryText(0xFFF472B6)
            .overlay(0x26000000)
            .accentColor(0xFFEC4899)
            .shadowColor(0x18000000)
            .sliderTrack(0xFF2D1B2E)
            .scrollbarColor(0x30FFFFFF)
            .build()),
    CUSTOM("custom", "Custom Colors", GuiTheme.defaultTheme());

    private final String key;
    private final String label;
    private final GuiTheme theme;

    public static ThemeOption resolve(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return DEFAULT;
        String normalised = rawValue.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return Arrays.stream(values()).filter(o -> o.key.equals(normalised)).findFirst().orElse(DEFAULT);
    }

    public static List<String> keys() {
        return Arrays.stream(values())
                .map(ThemeOption::getKey)
                .toList();
    }

    public static String labelFor(String key) {
        return resolve(key).getLabel();
    }
}
