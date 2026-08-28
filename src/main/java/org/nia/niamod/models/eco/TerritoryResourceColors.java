package org.nia.niamod.models.eco;

import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.render.Render2D;

public final class TerritoryResourceColors {
    private static final int HEADQUARTER_COLOR = 0xFFD4AF37;
    private static final int[] RAINBOW_STOPS = {
            0xFFFF2D2D,
            0xFFFF8C1A,
            0xFFFFE45C,
            0xFF33D17A,
            0xFF2DD4BF,
            0xFF4D8DFF,
            0xFFC061CB
    };

    private TerritoryResourceColors() {
    }

    public static int headquarterColor() {
        return HEADQUARTER_COLOR;
    }

    public static int cityColor() {
        return 0xFF000000 | NyahConfig.getData().getEcoCityColor();
    }

    public static int configuredColor(ResourceKind kind) {
        return switch (kind) {
            case EMERALDS -> cityColor();
            case CROPS -> 0xFF000000 | NyahConfig.getData().getEcoCropsColor();
            case WOOD -> 0xFF000000 | NyahConfig.getData().getEcoWoodColor();
            case ORE, ALL -> 0xFF000000 | NyahConfig.getData().getEcoOreColor();
            case FISH -> 0xFF000000 | NyahConfig.getData().getEcoFishColor();
            case NONE -> 0xFF000000 | NyahConfig.getData().getEcoNoneColor();
        };
    }

    public static int connectionColor() {
        return 0xFF000000 | NyahConfig.getData().getEcoConnectionColor();
    }

    public static int rainbowColor(double progress, int alpha) {
        int[] stops = configuredRainbowStops();
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        double scaled = clamped * (stops.length - 1);
        int leftIndex = Math.min(stops.length - 2, (int) Math.floor(scaled));
        float localProgress = (float) (scaled - leftIndex);
        int color = Render2D.lerpColor(stops[leftIndex], stops[leftIndex + 1], localProgress);
        return Render2D.withAlpha(color, alpha);
    }

    public static int[] rainbowStops(int alpha) {
        int[] stops = configuredRainbowStops();
        int[] colors = new int[stops.length];
        for (int i = 0; i < stops.length; i++) {
            colors[i] = Render2D.withAlpha(stops[i], alpha);
        }
        return colors;
    }

    private static int[] configuredRainbowStops() {
        return RAINBOW_STOPS;
    }
}
