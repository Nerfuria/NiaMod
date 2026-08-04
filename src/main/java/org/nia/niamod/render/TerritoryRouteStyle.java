package org.nia.niamod.render;

public record TerritoryRouteStyle(
        boolean enabled,
        int lineColor,
        float lineWidth,
        float glowStrength,
        float lightLength,
        float lightSpacing,
        float lightSpeed
) {
    private static final float ROUTE_OFFSET = 1.6F;
    private static final float LINE_OPACITY = 0.85F;
    private static final float LINE_GLOW_RADIUS = 1.8F;
    private static final float LINE_GLOW_STRENGTH_MULTIPLIER = 0.56F;
    private static final float LIGHT_COLOR_BLEND = 0.5F;
    private static final float MIN_LIGHT_WIDTH = 2.4F;
    private static final float LIGHT_WIDTH_MULTIPLIER = 1.5F;
    private static final float LIGHT_OPACITY = 0.92F;
    private static final float MIN_LIGHT_GLOW_RADIUS = 2.5F;
    private static final float MIN_RIBBON_HALF_WIDTH = 2.0F;
    private static final float CORE_EDGE_PADDING = 1.0F;
    private static final float GLOW_EXTENT_MULTIPLIER = 3.0F;

    public float offset() {
        return ROUTE_OFFSET;
    }

    public float lineOpacity() {
        return LINE_OPACITY;
    }

    public float lineGlowRadius() {
        return LINE_GLOW_RADIUS;
    }

    public float lineGlowOpacity() {
        return glowStrength * LINE_GLOW_STRENGTH_MULTIPLIER;
    }

    public int dotColor() {
        return blendTowardWhite(lineColor, LIGHT_COLOR_BLEND);
    }

    public float dotSize() {
        return Math.max(MIN_LIGHT_WIDTH, lineWidth * LIGHT_WIDTH_MULTIPLIER);
    }

    public float dotLength() {
        return lightLength;
    }

    public float dotOpacity() {
        return LIGHT_OPACITY;
    }

    public float dotGlowRadius() {
        return Math.max(MIN_LIGHT_GLOW_RADIUS, lineWidth * LIGHT_WIDTH_MULTIPLIER);
    }

    public float dotGlowOpacity() {
        return glowStrength;
    }

    public float dotSpacing() {
        return lightSpacing;
    }

    public float dotSpeed() {
        return lightSpeed;
    }

    public float coreHalfWidth() {
        return Math.max(lineWidth * 0.5F, dotSize() * 0.5F) + CORE_EDGE_PADDING;
    }

    public float ribbonHalfWidth() {
        float glowHalfWidth = Math.max(lineGlowRadius(), dotGlowRadius()) * GLOW_EXTENT_MULTIPLIER;
        return Math.max(MIN_RIBBON_HALF_WIDTH, Math.max(coreHalfWidth(), glowHalfWidth));
    }

    public int lineArgb() {
        int alpha = Math.clamp(Math.round(LINE_OPACITY * 255.0F), 0, 255);
        return (alpha << 24) | (lineColor & 0xFFFFFF);
    }

    private static int blendTowardWhite(int color, float amount) {
        int red = blendChannel(color >> 16 & 0xFF, amount);
        int green = blendChannel(color >> 8 & 0xFF, amount);
        int blue = blendChannel(color & 0xFF, amount);
        return red << 16 | green << 8 | blue;
    }

    private static int blendChannel(int channel, float amount) {
        return Math.round(channel + (255 - channel) * amount);
    }
}
