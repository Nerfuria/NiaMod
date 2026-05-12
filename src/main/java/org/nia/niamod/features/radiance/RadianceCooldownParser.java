package org.nia.niamod.features.radiance;

import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class RadianceCooldownParser {
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile(
            "(?i)\\bradiance\\b[^0-9]*\\(?\\s*(\\d{1,2})\\s*:\\s*(\\d{2})\\s*\\)?");

    public static Double remainingSeconds(String text) {
        String cleaned = sanitize(text);
        if (cleaned.isBlank()) {
            return null;
        }

        for (String line : cleaned.split("\\n")) {
            Double seconds = remainingSecondsFromLine(line);
            if (seconds != null) {
                return seconds;
            }
        }

        return null;
    }

    private static Double remainingSecondsFromLine(String line) {
        if (line.isBlank()) {
            return null;
        }

        Matcher matcher = COOLDOWN_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        int minutes = parseIntSafe(matcher.group(1));
        int seconds = parseIntSafe(matcher.group(2));
        return minutes >= 0 && seconds >= 0 ? minutes * 60.0 + seconds : null;
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.replace("\u0000", "");
        cleaned = cleaned.replaceAll("(?i)Â§[0-9a-fk-or]", "");
        cleaned = cleaned.replaceAll("(?i)&[0-9a-fk-or]", "");
        cleaned = cleaned.replaceAll("&\\{[^}]*}", "");
        cleaned = cleaned.replaceAll("&\\[[^]]*]", "");
        cleaned = cleaned.replaceAll("&<[^>]*>", "");
        return cleaned.trim();
    }

    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
