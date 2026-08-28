package org.nia.niamod.render;

import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.nia.niamod.gui.render.*;
import org.nia.niamod.mixin.GuiGraphicsAccessor;
import org.joml.Vector2f;

import java.util.List;

@UtilityClass
public class Render2D {

    public static void tooltipLine(GuiGraphics guiGraphics, int xOffset, LocalFloatRef renderYOffset, String text) {
        renderYOffset.set(renderYOffset.get() + 10.0F);
        FontRenderer.getInstance()
                .renderText(
                        guiGraphics,
                        StyledText.fromString(text),
                        10 + xOffset,
                        10.0F + renderYOffset.get(),
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);
    }

    public static void fill(GuiGraphics g, UiRect rect, int color) {
        g.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), color);
    }

    public static void surface(GuiGraphics g, UiRect rect, int fillColor, int borderColor) {
        fill(g, rect, fillColor);
        outline(g, rect, borderColor);
    }

    public static void outline(GuiGraphics g, UiRect rect, int color) {
        g.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, color);
        g.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), color);
        g.fill(rect.x(), rect.y() + 1, rect.x() + 1, rect.bottom() - 1, color);
        g.fill(rect.right() - 1, rect.y() + 1, rect.right(), rect.bottom() - 1, color);
    }

    public static void horizontalLine(GuiGraphics g, int x, int y, int width, int color) {
        g.fill(x, y, x + width, y + 1, color);
    }

    public static void verticalLine(GuiGraphics g, int x, int y, int height, int color) {
        g.fill(x, y, x + 1, y + height, color);
    }

    public static void accentStrip(GuiGraphics g, UiRect rect, int color, int width) {
        g.fill(rect.x(), rect.y(), rect.x() + width, rect.bottom(), color);
    }

    public static void toggle(GuiGraphics g, UiRect rect, boolean enabled, int onColor, int offColor, int knobColor) {
        int radius = Math.min(rect.height() / 2, 6);
        roundedRect(g, rect.x(), rect.y(), rect.width(), rect.height(), radius, enabled ? onColor : offColor);

        int knobSize = Math.max(8, rect.height() - 4);
        int knobY = rect.y() + (rect.height() - knobSize) / 2;
        int knobX = enabled ? rect.right() - knobSize - 2 : rect.x() + 2;
        roundedRect(g, knobX, knobY, knobSize, knobSize, Math.max(3, knobSize / 2), knobColor);
    }

    public static void pill(GuiGraphics g, Font font, UiRect rect, String label, int fillColor, int textColor) {
        fill(g, rect, fillColor);
        int textWidth = font.width(label);
        g.drawString(font, label, rect.x() + (rect.width() - textWidth) / 2, rect.y() + 4, textColor, false);
    }

    public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        if (radius <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        radius = Math.min(radius, Math.min(w / 2, h / 2));
        if (h > radius * 2) {
            g.fill(x, y + radius, x + w, y + h - radius, color);
        }

        float r = radius + 0.5f;
        for (int row = 0; row < radius; row++) {
            int dy = radius - row;
            int inset = radius - (int) Math.sqrt(r * r - (double) dy * dy);
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            g.fill(x + inset, y + h - 1 - row, x + w - inset, y + h - row, color);
        }
    }

    public static void circle(GuiGraphics g, int cx, int cy, int diameter, int color) {
        int r = diameter / 2;
        roundedRect(g, cx - r, cy - r, diameter, diameter, r, color);
    }

    public static void dropShadow(GuiGraphics g, int x, int y, int w, int h, int loops, int opacity, int edgeRadius) {
        for (int i = 0; i <= loops; i++) {
            float alpha = Math.max(0.5f, (opacity - i * 1.2f) / 5.5f);
            int a = Math.clamp(Math.round(alpha), 0, 255);
            if (a == 0) continue;
            int halfMargin = Math.round(i / 2.0f);
            int color = (a << 24);
            roundedRect(g, x - halfMargin, y - halfMargin, w + halfMargin * 2, h + halfMargin * 2, edgeRadius, color);
        }
    }

    public static void dropShadow(GuiGraphics g, UiRect rect, int layers, int baseColor, int radius) {
        int baseAlpha = (baseColor >>> 24) & 0xFF;
        for (int i = layers; i >= 1; i--) {
            float strength = (layers - i + 1) / (float) (layers + 2);
            int alpha = Math.round(baseAlpha * strength);
            int color = (alpha << 24) | (baseColor & 0x00FFFFFF);
            roundedRect(
                    g,
                    rect.x() - i,
                    rect.y() - i,
                    rect.width() + i * 2,
                    rect.height() + i * 2,
                    radius + i,
                    color
            );
        }
    }

    public static void horizontalGradient(GuiGraphics g, int x, int y, int w, int h, int colorLeft, int colorRight) {
        int aL = (colorLeft >>> 24) & 0xFF, rL = (colorLeft >> 16) & 0xFF, gL = (colorLeft >> 8) & 0xFF, bL = colorLeft & 0xFF;
        int aR = (colorRight >>> 24) & 0xFF, rR = (colorRight >> 16) & 0xFF, gR = (colorRight >> 8) & 0xFF, bR = colorRight & 0xFF;

        for (int col = 0; col < w; col++) {
            float t = (float) col / Math.max(1, w - 1);
            int a = Math.round(aL + (aR - aL) * t);
            int r = Math.round(rL + (rR - rL) * t);
            int gr = Math.round(gL + (gR - gL) * t);
            int b = Math.round(bL + (bR - bL) * t);
            int color = (a << 24) | (r << 16) | (gr << 8) | b;
            g.fill(x + col, y, x + col + 1, y + h, color);
        }
    }

    public static void slider(GuiGraphics g, UiRect track, float progress, int trackColor, int filledColor, int grabberColor) {
        g.fill(track.x(), track.y(), track.right(), track.bottom(), trackColor);
        int filledWidth = Math.round(track.width() * progress);
        if (filledWidth > 0) {
            g.fill(track.x(), track.y(), track.x() + filledWidth, track.bottom(), filledColor);
        }
        int grabX = track.x() + filledWidth - 3;
        g.fill(grabX, track.y() - 2, grabX + 6, track.bottom() + 2, grabberColor);
    }

    public static void glow(GuiGraphics g, UiRect rect, int color, int spread) {
        int alpha = ((color >>> 24) & 0xFF) / 3;
        int glowColor = (alpha << 24) | (color & 0x00FFFFFF);
        UiRect expanded = new UiRect(rect.x() - spread, rect.y() - spread,
                rect.width() + spread * 2, rect.height() + spread * 2);
        fill(g, expanded, glowColor);
    }

    public static void softGlow(GuiGraphics g, UiRect rect, int layers, int color, int radius) {
        int baseAlpha = (color >>> 24) & 0xFF;
        for (int i = layers; i >= 1; i--) {
            int alpha = Math.max(0, Math.round(baseAlpha * (i / (float) (layers + 1))));
            if (alpha == 0) {
                continue;
            }

            roundedRect(
                    g,
                    rect.x() - i,
                    rect.y() - i,
                    rect.width() + i * 2,
                    rect.height() + i * 2,
                    radius + i,
                    (alpha << 24) | (color & 0x00FFFFFF)
            );
        }
    }

    public static int hsvToRgb(float h, float s, float v) {
        return java.awt.Color.HSBtoRGB(h / 360.0f, s, v) & 0xFFFFFF;
    }

    public static float[] rgbToHsv(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int gr = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float[] hsb = java.awt.Color.RGBtoHSB(r, gr, b, null);
        return new float[]{hsb[0] * 360.0f, hsb[1], hsb[2]};
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    public static int lerpColor(int a, int b, float t) {
        int aA = (a >>> 24) & 0xFF, aR = (a >> 16) & 0xFF, aG = (a >> 8) & 0xFF, aB = a & 0xFF;
        int bA = (b >>> 24) & 0xFF, bR = (b >> 16) & 0xFF, bG = (b >> 8) & 0xFF, bB = b & 0xFF;
        return (Math.round(aA + (bA - aA) * t) << 24)
                | (Math.round(aR + (bR - aR) * t) << 16)
                | (Math.round(aG + (bG - aG) * t) << 8)
                | Math.round(aB + (bB - aB) * t);
    }

    public static void clippedRect(GuiGraphics g, int left, int top, int right, int bottom, UiRect clip, int color) {
        int x1 = Math.max(left, clip.x());
        int y1 = Math.max(top, clip.y());
        int x2 = Math.min(right, clip.right());
        int y2 = Math.min(bottom, clip.bottom());
        if (x2 > x1 && y2 > y1) {
            g.fill(x1, y1, x2, y2, color);
        }
    }

    public static void horizontalMultiGradient(
            GuiGraphics g,
            int left,
            int top,
            int right,
            int bottom,
            int gradientLeft,
            int gradientWidth,
            int[] colors
    ) {
        if (right <= left || bottom <= top || colors.length < 2) {
            return;
        }

        int width = Math.max(1, gradientWidth);
        int stopCount = colors.length;
        for (int i = 0; i < stopCount - 1; i++) {
            int stopLeft = gradientLeft + width * i / (stopCount - 1);
            int stopRight = gradientLeft + width * (i + 1) / (stopCount - 1);
            int bandLeft = Math.max(left, stopLeft);
            int bandRight = Math.min(right, stopRight);
            if (bandRight <= bandLeft) {
                continue;
            }

            double stopWidth = Math.max(1.0, stopRight - stopLeft);
            int leftColor = lerpColor(colors[i], colors[i + 1], (float) ((bandLeft - stopLeft) / stopWidth));
            int rightColor = lerpColor(colors[i], colors[i + 1], (float) ((bandRight - stopLeft) / stopWidth));
            g.guiRenderState.submitGuiElement(new HorizontalGradientQuad(
                    g.pose(),
                    bandLeft,
                    bandRight,
                    top,
                    bottom,
                    g.scissorStack.peek(),
                    leftColor,
                    rightColor
            ));
        }
    }

    public static void dashedLine(
            GuiGraphics g,
            double x1,
            double y1,
            double x2,
            double y2,
            double phase,
            int dashLength,
            int gapLength,
            int maxDashes,
            DashColorProvider colorProvider
    ) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0) {
            return;
        }

        double unitX = dx / length;
        double unitY = dy / length;
        double cycle = cappedCycle(length, dashLength + gapLength, maxDashes);
        double offset = positiveModulo(phase, cycle);
        double visibleDashLength = Math.min(dashLength, Math.max(2.0, cycle * 0.65));

        for (double dashStart = -offset; dashStart < length; dashStart += cycle) {
            double start = Math.max(0.0, dashStart);
            double end = Math.min(length, dashStart + visibleDashLength);
            if (end <= start) {
                continue;
            }

            double mid = (start + end) / 2.0;
            drawStroke(
                    g,
                    x1 + unitX * start,
                    y1 + unitY * start,
                    x1 + unitX * end,
                    y1 + unitY * end,
                    colorProvider.color(mid, length)
            );
        }
    }

    public static void dashedLineClipped(
            GuiGraphics g,
            double x1,
            double y1,
            double x2,
            double y2,
            UiRect clip,
            int padding,
            double phase,
            int dashLength,
            int gapLength,
            int maxDashes,
            DashColorProvider colorProvider
    ) {
        double[] clipped = clipLine(x1, y1, x2, y2, clip, padding);
        if (clipped == null) {
            return;
        }
        dashedLine(g, clipped[0], clipped[1], clipped[2], clipped[3], phase, dashLength, gapLength, maxDashes, colorProvider);
    }

    public static double[] clipLine(double x1, double y1, double x2, double y2, UiRect rect, int padding) {
        double minX = rect.x() - padding;
        double minY = rect.y() - padding;
        double maxX = rect.right() + padding;
        double maxY = rect.bottom() + padding;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double t0 = 0.0;
        double t1 = 1.0;

        double[] clipped = clipEdge(-dx, x1 - minX, t0, t1);
        if (clipped == null) return null;
        t0 = clipped[0];
        t1 = clipped[1];

        clipped = clipEdge(dx, maxX - x1, t0, t1);
        if (clipped == null) return null;
        t0 = clipped[0];
        t1 = clipped[1];

        clipped = clipEdge(-dy, y1 - minY, t0, t1);
        if (clipped == null) return null;
        t0 = clipped[0];
        t1 = clipped[1];

        clipped = clipEdge(dy, maxY - y1, t0, t1);
        if (clipped == null) return null;
        t0 = clipped[0];
        t1 = clipped[1];

        return new double[]{
                x1 + dx * t0,
                y1 + dy * t0,
                x1 + dx * t1,
                y1 + dy * t1
        };
    }

    private static void drawStroke(GuiGraphics g, double x1, double y1, double x2, double y2, int color) {
        if (Math.abs(y2 - y1) < 0.5) {
            int left = (int) Math.round(Math.min(x1, x2));
            int right = (int) Math.round(Math.max(x1, x2));
            int y = (int) Math.round(y1);
            g.fill(left, y - 1, Math.max(left + 1, right), y + 1, color);
            return;
        }

        if (Math.abs(x2 - x1) < 0.5) {
            int top = (int) Math.round(Math.min(y1, y2));
            int bottom = (int) Math.round(Math.max(y1, y2));
            int x = (int) Math.round(x1);
            g.fill(x - 1, top, x + 1, Math.max(top + 1, bottom), color);
            return;
        }

        double dx = x2 - x1;
        double dy = y2 - y1;
        int length = Math.max(1, (int) Math.round(Math.sqrt(dx * dx + dy * dy)));
        float angle = (float) Math.atan2(dy, dx);

        g.pose().pushMatrix();
        try {
            g.pose().translate((float) x1, (float) y1);
            g.pose().rotate(angle);
            g.fill(0, -1, length, 1, color);
        } finally {
            g.pose().popMatrix();
        }
    }

    private static double positiveModulo(double value, double modulo) {
        double result = value % modulo;
        return result < 0 ? result + modulo : result;
    }

    private static double cappedCycle(double length, double baseCycle, int maxDashes) {
        if (maxDashes <= 0) {
            return baseCycle;
        }
        return Math.max(baseCycle, length / maxDashes);
    }

    private static double[] clipEdge(double p, double q, double t0, double t1) {
        if (p == 0.0) {
            return q < 0.0 ? null : new double[]{t0, t1};
        }

        double r = q / p;
        if (p < 0.0) {
            if (r > t1) return null;
            if (r > t0) t0 = r;
        } else {
            if (r < t0) return null;
            if (r < t1) t1 = r;
        }
        return new double[]{t0, t1};
    }

    public static void shaderRoundedRect(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (!submitGuiShaderRect(g, RenderPipelines.GUI_ROUNDED_RECT, x, y, w, h, color, radius, 0)) {
            roundedRect(g, x, y, w, h, radius, color);
        }
    }

    public static boolean shaderRoute(GuiGraphics g, List<Vector2f> points, TerritoryRouteStyle style, float phase, float spacing) {
        if (!(g instanceof GuiGraphicsAccessor accessor) || points.size() < 2) {
            return false;
        }

        GuiRenderState renderState = accessor.niamod$getGuiRenderState();
        if (renderState == null) {
            return false;
        }

        renderState.submitGuiElement(new GuiRouteRenderState(
                g.pose(),
                points,
                style,
                phase,
                spacing,
                g.guiWidth(),
                g.guiHeight(),
                currentScissorArea(accessor)
        ));
        return true;
    }

    public static void shaderRoundedSurface(GuiGraphics g, int x, int y, int w, int h, int radius, int fillColor, int borderColor) {
        if (w <= 0 || h <= 0) {
            return;
        }

        if (((borderColor >>> 24) & 0xFF) > 0) {
            shaderRoundedRect(g, x, y, w, h, radius, borderColor);
            if (w <= 2 || h <= 2) {
                return;
            }
            shaderRoundedRect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fillColor);
            return;
        }

        shaderRoundedRect(g, x, y, w, h, radius, fillColor);
    }

    public static void shaderPortalOverlay(GuiGraphics g, int x, int y, int w, int h, int color, float progress) {
        int encodedProgress = Math.clamp(Math.round(progress * 1000.0f), 0, 1000);
        int time = (int) ((System.currentTimeMillis() / 4L) % 60000L);
        if (!submitGuiShaderRect(g, RenderPipelines.GUI_PORTAL_OVERLAY, x, y, w, h, color, encodedProgress, time)) {
            int alpha = Math.clamp(Math.round(((color >>> 24) & 0xFF) * progress), 0, 255);
            roundedRect(g, x, y, w, h, Math.max(6, Math.min(w, h) / 5), withAlpha(color, alpha));
        }
    }

    public static void shaderPortalCapture(
            GuiGraphics g,
            RenderTarget snapshot,
            int x,
            int y,
            int w,
            int h,
            float progress,
            float seedX,
            float seedY,
            int color,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
        shaderWindowCapture(
                g,
                RenderPipelines.GUI_PORTAL_CAPTURE,
                snapshot,
                x,
                y,
                w,
                h,
                progress,
                seedX,
                seedY,
                color,
                sourceX,
                sourceY,
                sourceWidth,
                sourceHeight
        );
    }

    public static void shaderIncinerateCapture(
            GuiGraphics g,
            RenderTarget snapshot,
            int x,
            int y,
            int w,
            int h,
            float progress,
            float seedX,
            float seedY,
            int color,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
        shaderWindowCapture(
                g,
                RenderPipelines.GUI_INCINERATE_CAPTURE,
                snapshot,
                x,
                y,
                w,
                h,
                progress,
                seedX,
                seedY,
                color,
                sourceX,
                sourceY,
                sourceWidth,
                sourceHeight
        );
    }

    public static void shaderMushroomCapture(
            GuiGraphics g,
            RenderTarget snapshot,
            int x,
            int y,
            int w,
            int h,
            float progress,
            float seedX,
            float seedY,
            int color,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
        shaderWindowCapture(
                g,
                RenderPipelines.GUI_MUSHROOM_CAPTURE,
                snapshot,
                x,
                y,
                w,
                h,
                progress,
                seedX,
                seedY,
                color,
                sourceX,
                sourceY,
                sourceWidth,
                sourceHeight
        );
    }

    private static void shaderWindowCapture(
            GuiGraphics g,
            com.mojang.blaze3d.pipeline.RenderPipeline pipeline,
            RenderTarget snapshot,
            int x,
            int y,
            int w,
            int h,
            float progress,
            float seedX,
            float seedY,
            int color,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
        if (snapshot == null || snapshot.getColorTextureView() == null) {
            return;
        }

        TextureSetup textureSetup = TextureSetup.singleTexture(
                snapshot.getColorTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
        );

        if (!(g instanceof GuiGraphicsAccessor accessor)) {
            return;
        }

        GuiRenderState renderState = accessor.niamod$getGuiRenderState();
        if (renderState == null) {
            return;
        }
        ScreenRectangle scissorArea = currentScissorArea(accessor);

        renderState.submitGuiElement(new GuiPortalRectRenderState(
                pipeline,
                textureSetup,
                g.pose(),
                x,
                y,
                w,
                h,
                sourceX,
                sourceY,
                sourceWidth,
                sourceHeight,
                color,
                progress,
                seedX,
                seedY,
                g.guiWidth(),
                g.guiHeight(),
                scissorArea
        ));
    }

    private static boolean submitGuiShaderRect(
            GuiGraphics g,
            com.mojang.blaze3d.pipeline.RenderPipeline pipeline,
            int x,
            int y,
            int w,
            int h,
            int color,
            int param0,
            int param1
    ) {
        if (!(g instanceof GuiGraphicsAccessor accessor) || w <= 0 || h <= 0) {
            return false;
        }

        GuiRenderState renderState = accessor.niamod$getGuiRenderState();
        if (renderState == null) {
            return false;
        }
        ScreenRectangle scissorArea = currentScissorArea(accessor);

        renderState.submitGuiElement(new GuiShaderRectRenderState(
                pipeline,
                g.pose(),
                x,
                y,
                w,
                h,
                color,
                param0,
                param1,
                g.guiWidth(),
                g.guiHeight(),
                scissorArea
        ));
        return true;
    }

    private static ScreenRectangle currentScissorArea(GuiGraphicsAccessor accessor) {
        if (accessor instanceof GuiGraphicsScissorState scissorState) {
            return scissorState.niamod$getCurrentScissorArea();
        }
        return null;
    }

    @FunctionalInterface
    public interface DashColorProvider {
        int color(double distance, double length);
    }
}
