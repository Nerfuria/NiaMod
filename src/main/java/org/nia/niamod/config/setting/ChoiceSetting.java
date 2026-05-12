package org.nia.niamod.config.setting;

import lombok.Getter;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

@Getter
public class ChoiceSetting extends ConfigSetting<String> {
    private final Supplier<List<String>> optionsSupplier;
    private final Function<String, String> labelResolver;

    public ChoiceSetting(
            String id,
            String title,
            String description,
            Supplier<String> getter,
            java.util.function.Consumer<String> setter,
            List<String> options,
            Function<String, String> labelResolver
    ) {
        this(id, title, description, getter, setter, () -> options, labelResolver);
    }

    public ChoiceSetting(
            String id,
            String title,
            String description,
            Supplier<String> getter,
            java.util.function.Consumer<String> setter,
            Supplier<List<String>> optionsSupplier,
            Function<String, String> labelResolver
    ) {
        super(id, title, description, SettingKind.CHOICE, getter, setter);
        this.optionsSupplier = optionsSupplier;
        this.labelResolver = labelResolver;
    }

    public List<String> getOptions() {
        return List.copyOf(optionsSupplier.get());
    }

    @Override
    public String format() {
        return displayValue(get());
    }

    @Override
    public boolean tryParseAndSet(String rawValue) {
        String resolved = resolveOption(rawValue);
        if (resolved == null) {
            return false;
        }

        set(resolved);
        return true;
    }

    public void next() {
        cycle(1);
    }

    public void previous() {
        cycle(-1);
    }

    public String displayValue(String value) {
        return labelResolver.apply(value);
    }

    private void cycle(int direction) {
        List<String> options = getOptions();
        if (options.isEmpty()) {
            return;
        }

        int currentIndex = options.indexOf(resolveOption(get()));
        if (currentIndex < 0) {
            currentIndex = 0;
        }

        int nextIndex = Math.floorMod(currentIndex + direction, options.size());
        set(options.get(nextIndex));
    }

    private String resolveOption(String rawValue) {
        List<String> options = getOptions();
        if (rawValue == null || rawValue.isBlank()) {
            return options.isEmpty() ? null : options.getFirst();
        }

        String normalised = normalise(rawValue);
        for (String option : options) {
            if (normalise(option).equals(normalised) || normalise(displayValue(option)).equals(normalised)) {
                return option;
            }
        }

        return null;
    }

    private String normalise(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }
}
