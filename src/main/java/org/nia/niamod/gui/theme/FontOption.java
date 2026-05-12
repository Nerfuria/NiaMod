package org.nia.niamod.gui.theme;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum FontOption {
    MINECRAFT_UNIFORM("minecraft_uniform", "Minecraft Uniform", "minecraft", "uniform"),
    MINECRAFT_DEFAULT("minecraft_default", "Minecraft Default", "minecraft", "default"),
    COMFORTAA("comfortaa", "Comfortaa", "niamod", "comfortaa"),
    ADWAITA_SANS("adwaita_sans", "Adwaita Sans", "niamod", "adwaita_sans"),
    LIBERATION_SANS("liberation_sans", "Liberation Sans", "niamod", "liberation_sans"),
    ADWAITA_MONO("adwaita_mono", "Adwaita Mono", "niamod", "adwaita_mono"),
    JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono", "niamod", "jetbrains_mono");

    private final String key;
    private final String label;
    private final String namespace;
    private final String fontId;

    public static FontOption resolve(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return MINECRAFT_DEFAULT;
        }

        String normalised = rawValue.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');

        return Arrays.stream(values())
                .filter(option -> option.key.equals(normalised))
                .findFirst()
                .orElse(MINECRAFT_DEFAULT);
    }

    public static List<String> keys() {
        return Arrays.stream(values())
                .map(FontOption::getKey)
                .toList();
    }

    public static String labelFor(String key) {
        return resolve(key).getLabel();
    }

    public Identifier fontDescriptionId() {
        return Identifier.fromNamespaceAndPath(namespace, fontId);
    }
}
