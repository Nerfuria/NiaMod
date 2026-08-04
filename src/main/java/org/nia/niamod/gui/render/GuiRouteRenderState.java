package org.nia.niamod.gui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.nia.niamod.render.TerritoryRouteStyle;

import java.util.ArrayList;
import java.util.List;

public final class GuiRouteRenderState implements GuiElementRenderState {
    private static final float MAX_CORE_RADIUS = 4.0F;
    private static final float MAX_GLOW_RADIUS = 8.0F;
    private static final float MAX_LIGHT_HALF_LENGTH = 16.0F;
    private static final float CURVATURE_WIDTH_RATIO = 0.85F;
    private static final float MAX_MITER_SCALE = 2.4F;
    private static final float MIN_DIRECTION_LENGTH_SQUARED = 0.0001F;
    private static final float MIN_MITER_DENOMINATOR = 0.001F;
    private static final float ROUTE_LENGTH_PACK_SCALE = 10.0F;
    private static final float SPACING_PACK_SCALE = 10.0F;
    private static final int PHASE_BITS = 8;
    private static final int PHASE_STEPS = 1 << PHASE_BITS;
    private static final int PHASE_MASK = PHASE_STEPS - 1;
    private static final int DOT_RADIUS_BITS = 7;
    private static final int DOT_RADIUS_MASK = (1 << DOT_RADIUS_BITS) - 1;
    private static final int SPACING_BITS = 10;
    private static final int SPACING_MASK = (1 << SPACING_BITS) - 1;
    private static final int DOT_GLOW_RADIUS_BITS = 5;
    private static final int DOT_GLOW_RADIUS_MASK = (1 << DOT_GLOW_RADIUS_BITS) - 1;
    private static final int OPACITY_BITS = 5;
    private static final int OPACITY_MASK = (1 << OPACITY_BITS) - 1;
    private static final int LIGHT_HALF_LENGTH_MASK = 0xFF;

    private final TextureSetup textureSetup = TextureSetup.noTexture();
    private final Matrix3x2f pose;
    private final List<RibbonPoint> points;
    private final float routeLength;
    private final float encodedLineCoreRadius;
    private final float encodedLineGlowRadius;
    private final float lineGlowOpacity;
    private final int lineColor;
    private final int dotColor;
    private final int packedPhaseAndDotRadius;
    private final int packedSpacingAndDotGlowRadius;
    private final int packedOpacities;
    private final int guiWidth;
    private final int guiHeight;
    private final ScreenRectangle bounds;
    private final ScreenRectangle scissorArea;

    public GuiRouteRenderState(
            Matrix3x2fc pose,
            List<Vector2f> routePoints,
            TerritoryRouteStyle style,
            float phase,
            float spacing,
            int guiWidth,
            int guiHeight,
            ScreenRectangle scissorArea
    ) {
        this.pose = new Matrix3x2f(pose);
        int encodedLightHalfLength = encodeBits(style.dotLength() * 0.5F, MAX_LIGHT_HALF_LENGTH, LIGHT_HALF_LENGTH_MASK);
        this.lineColor = (encodedLightHalfLength << 24) | (style.lineColor() & 0xFFFFFF);
        this.dotColor = style.dotColor() & 0xFFFFFF;
        this.encodedLineCoreRadius = encodeUnit(style.lineWidth() * 0.5F, MAX_CORE_RADIUS);
        this.encodedLineGlowRadius = encodeUnit(style.lineGlowRadius(), MAX_GLOW_RADIUS);
        this.lineGlowOpacity = Math.clamp(style.lineGlowOpacity(), 0.0F, 1.0F);
        this.packedPhaseAndDotRadius = packPhaseAndDotRadius(phase, spacing, style);
        this.packedSpacingAndDotGlowRadius = packSpacingAndDotGlowRadius(spacing, style);
        this.packedOpacities = packOpacities(style);
        this.guiWidth = Math.max(1, guiWidth);
        this.guiHeight = Math.max(1, guiHeight);
        this.scissorArea = scissorArea;

        RouteGeometry geometry = createGeometry(routePoints, style.ribbonHalfWidth(), style.coreHalfWidth());
        this.points = geometry.points();
        this.routeLength = geometry.length();
        this.bounds = calculateBounds(this.points);
    }

    private static float encodeUnit(float value, float maximum) {
        return Math.clamp(value / maximum, 0.0F, 1.0F);
    }

    private static int packPhaseAndDotRadius(float phase, float spacing, TerritoryRouteStyle style) {
        float phaseFraction = phase / Math.max(spacing, 1.0F);
        int encodedPhase = Math.clamp((int) (phaseFraction * PHASE_STEPS), 0, PHASE_MASK);
        int encodedDotRadius = encodeBits(style.dotSize() * 0.5F, MAX_CORE_RADIUS, DOT_RADIUS_MASK);
        return encodedPhase | encodedDotRadius << PHASE_BITS;
    }

    private static int packSpacingAndDotGlowRadius(float spacing, TerritoryRouteStyle style) {
        int encodedSpacing = Math.clamp(Math.round(spacing * SPACING_PACK_SCALE), 0, SPACING_MASK);
        int encodedDotGlowRadius = encodeBits(style.dotGlowRadius(), MAX_GLOW_RADIUS, DOT_GLOW_RADIUS_MASK);
        return encodedSpacing | encodedDotGlowRadius << SPACING_BITS;
    }

    private static int packOpacities(TerritoryRouteStyle style) {
        int encodedLineOpacity = encodeBits(style.lineOpacity(), 1.0F, OPACITY_MASK);
        int encodedLightOpacity = encodeBits(style.dotOpacity(), 1.0F, OPACITY_MASK);
        int encodedLightGlowOpacity = encodeBits(style.dotGlowOpacity(), 1.0F, OPACITY_MASK);
        return encodedLineOpacity | encodedLightOpacity << OPACITY_BITS | encodedLightGlowOpacity << (OPACITY_BITS * 2);
    }

    private static int encodeBits(float value, float maximum, int mask) {
        return Math.clamp(Math.round(value / maximum * mask), 0, mask);
    }

    private static RouteGeometry createGeometry(List<Vector2f> source, float halfRibbonWidth, float minimumHalfRibbonWidth) {
        if (source.size() < 2) {
            return new RouteGeometry(List.of(), 0.0F);
        }

        List<Float> distances = new ArrayList<>(source.size());
        distances.add(0.0F);
        float totalLength = 0.0F;
        for (int i = 1; i < source.size(); i++) {
            totalLength += source.get(i).distance(source.get(i - 1));
            distances.add(totalLength);
        }

        List<RibbonPoint> result = new ArrayList<>(source.size());
        for (int i = 0; i < source.size(); i++) {
            Vector2f point = source.get(i);
            Vector2f previousDirection = i > 0 ? direction(source.get(i - 1), point) : null;
            Vector2f nextDirection = i + 1 < source.size() ? direction(point, source.get(i + 1)) : null;

            Vector2f center = new Vector2f(point);
            float routeDistance = distances.get(i);
            float pointHalfRibbonWidth = curvatureLimitedWidth(source, i, halfRibbonWidth, minimumHalfRibbonWidth);
            Vector2f offset;
            if (previousDirection == null) {
                center.sub(new Vector2f(nextDirection).mul(pointHalfRibbonWidth));
                routeDistance -= pointHalfRibbonWidth;
                offset = normal(nextDirection).mul(pointHalfRibbonWidth);
            } else if (nextDirection == null) {
                center.add(new Vector2f(previousDirection).mul(pointHalfRibbonWidth));
                routeDistance += pointHalfRibbonWidth;
                offset = normal(previousDirection).mul(pointHalfRibbonWidth);
            } else {
                Vector2f tangent = new Vector2f(previousDirection).add(nextDirection);
                if (tangent.lengthSquared() < MIN_DIRECTION_LENGTH_SQUARED) {
                    offset = normal(nextDirection).mul(pointHalfRibbonWidth);
                } else {
                    tangent.normalize();
                    Vector2f miter = normal(tangent);
                    float denominator = miter.dot(normal(nextDirection));
                    float scale = Math.abs(denominator) < MIN_MITER_DENOMINATOR ? pointHalfRibbonWidth : pointHalfRibbonWidth / denominator;
                    scale = Math.clamp(scale, -pointHalfRibbonWidth * MAX_MITER_SCALE, pointHalfRibbonWidth * MAX_MITER_SCALE);
                    offset = miter.mul(scale);
                }
            }

            result.add(new RibbonPoint(new Vector2f(center), new Vector2f(center).add(offset), new Vector2f(center).sub(offset), routeDistance));
        }

        return new RouteGeometry(List.copyOf(result), totalLength);
    }

    private static float curvatureLimitedWidth(List<Vector2f> points, int index, float requestedWidth, float minimumWidth) {
        if (index == 0 || index + 1 == points.size()) {
            return requestedWidth;
        }

        Vector2f incoming = direction(points.get(index - 1), points.get(index));
        Vector2f outgoing = direction(points.get(index), points.get(index + 1));
        float turnSinHalf = (float) Math.sqrt(Math.max(0.0F, (1.0F - Math.clamp(incoming.dot(outgoing), -1.0F, 1.0F)) * 0.5F));
        if (turnSinHalf * turnSinHalf <= MIN_DIRECTION_LENGTH_SQUARED) {
            return requestedWidth;
        }

        float adjacentLength = Math.min(points.get(index).distance(points.get(index - 1)), points.get(index).distance(points.get(index + 1)));
        float curvatureRadius = adjacentLength / (2.0F * turnSinHalf);
        return Math.clamp(curvatureRadius * CURVATURE_WIDTH_RATIO, minimumWidth, requestedWidth);
    }

    private static Vector2f direction(Vector2f start, Vector2f end) {
        Vector2f result = new Vector2f(end).sub(start);
        return result.lengthSquared() < MIN_DIRECTION_LENGTH_SQUARED ? new Vector2f(1.0F, 0.0F) : result.normalize();
    }

    private static Vector2f normal(Vector2f direction) {
        return new Vector2f(-direction.y, direction.x);
    }

    private ScreenRectangle calculateBounds(List<RibbonPoint> ribbonPoints) {
        if (ribbonPoints.isEmpty()) {
            return ScreenRectangle.empty();
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (RibbonPoint point : ribbonPoints) {
            Vector2f transformedLeft = pose.transformPosition(point.left(), new Vector2f());
            Vector2f transformedRight = pose.transformPosition(point.right(), new Vector2f());
            minX = Math.min(minX, Math.min(transformedLeft.x, transformedRight.x));
            minY = Math.min(minY, Math.min(transformedLeft.y, transformedRight.y));
            maxX = Math.max(maxX, Math.max(transformedLeft.x, transformedRight.x));
            maxY = Math.max(maxY, Math.max(transformedLeft.y, transformedRight.y));
        }

        int left = (int) Math.floor(minX);
        int top = (int) Math.floor(minY);
        int right = (int) Math.ceil(maxX);
        int bottom = (int) Math.ceil(maxY);
        return new ScreenRectangle(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
    }

    @Override
    public void buildVertices(@NotNull VertexConsumer consumer) {
        int packedLength = Math.round(routeLength * ROUTE_LENGTH_PACK_SCALE);
        for (int i = 1; i < points.size(); i++) {
            RibbonPoint start = points.get(i - 1);
            RibbonPoint end = points.get(i);
            Vector2f segmentDirection = direction(start.center(), end.center());
            Vector2f segmentNormal = normal(segmentDirection);
            submitVertex(consumer, start, start.left(), segmentNormal, packedLength);
            submitVertex(consumer, end, end.left(), segmentNormal, packedLength);
            submitVertex(consumer, end, end.right(), segmentNormal, packedLength);
            submitVertex(consumer, start, start.right(), segmentNormal, packedLength);
        }
    }

    private void submitVertex(VertexConsumer consumer, RibbonPoint point, Vector2f position, Vector2f segmentNormal, int packedLength) {
        Vector2f transformed = pose.transformPosition(position, new Vector2f());
        Vector2f offset = new Vector2f(position).sub(point.center());
        float sideDistance = offset.dot(segmentNormal);
        float clipX = transformed.x / guiWidth * 2.0F - 1.0F;
        float clipY = 1.0F - transformed.y / guiHeight * 2.0F;

        consumer.addVertex(clipX, clipY, dotColor)
                .setColor((lineColor >> 16) & 0xFF, (lineColor >> 8) & 0xFF, lineColor & 0xFF, (lineColor >>> 24) & 0xFF)
                .setUv(point.distance(), sideDistance)
                .setUv1(packedLength, packedPhaseAndDotRadius)
                .setUv2(packedSpacingAndDotGlowRadius, packedOpacities)
                .setNormal(encodedLineCoreRadius, encodedLineGlowRadius, lineGlowOpacity);
    }

    @Override
    public @NotNull RenderPipeline pipeline() {
        return RenderPipelines.GUI_ROUTE;
    }

    @Override
    public @NotNull TextureSetup textureSetup() {
        return textureSetup;
    }

    @Override
    public ScreenRectangle scissorArea() {
        return scissorArea;
    }

    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }

    private record RibbonPoint(Vector2f center, Vector2f left, Vector2f right, float distance) {}

    private record RouteGeometry(List<RibbonPoint> points, float length) {}
}
