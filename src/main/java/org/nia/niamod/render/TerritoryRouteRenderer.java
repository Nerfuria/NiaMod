package org.nia.niamod.render;

import com.wynntils.core.components.Models;
import com.wynntils.services.map.pois.TerritoryPoi;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.MapRenderer;
import com.wynntils.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector2f;
import org.nia.niamod.models.events.HoveredTerritoryInfoRenderEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TerritoryRouteRenderer {
    private static final float MIN_SEGMENT_LENGTH_SQUARED = 0.0001F;
    private static final float ROUTE_SAMPLE_LENGTH = 6.0F;
    private static final float MIN_CORNER_RADIUS = 10.0F;
    private static final float CORNER_RIBBON_RADIUS_MULTIPLIER = 1.25F;
    private static final float CORNER_SAMPLE_LENGTH = 3.0F;
    private static final float MAX_CORNER_SEGMENT_FRACTION = 0.45F;
    private static final float ENDPOINT_JOIN_LENGTH = 14.0F;
    private static final long ANIMATION_RESET_GAP_NANOS = Duration.ofMillis(500).toNanos();
    private static final double NANOS_PER_SECOND = Duration.ofSeconds(1).toNanos();

    private String animatedTerritory;
    private long animationStartNanos;
    private long lastRenderNanos;

    public void render(
            HoveredTerritoryInfoRenderEvent event,
            String territoryName,
            List<String> headquartersToTerritory,
            TerritoryRouteStyle headquartersToTerritoryStyle,
            List<String> territoryToHeadquarters,
            TerritoryRouteStyle territoryToHeadquartersStyle
    ) {
        double elapsedSeconds = animationElapsedSeconds(territoryName);
        renderRoute(event, headquartersToTerritory, headquartersToTerritoryStyle, elapsedSeconds);
        renderRoute(event, territoryToHeadquarters, territoryToHeadquartersStyle, elapsedSeconds);
    }

    private double animationElapsedSeconds(String territoryName) {
        long now = System.nanoTime();
        if (!Objects.equals(animatedTerritory, territoryName) || now - lastRenderNanos > ANIMATION_RESET_GAP_NANOS) {
            animatedTerritory = territoryName;
            animationStartNanos = now;
        }
        lastRenderNanos = now;
        return (now - animationStartNanos) / NANOS_PER_SECOND;
    }

    private void renderRoute(
            HoveredTerritoryInfoRenderEvent event,
            List<String> territoryNames,
            TerritoryRouteStyle style,
            double elapsedSeconds
    ) {
        if (!style.enabled() || territoryNames.size() < 2) {
            return;
        }

        List<Vector2f> points = buildRoute(event, territoryNames, style);
        if (points.size() < 2) {
            return;
        }

        float spacing = fittedSpacing(points, style.dotSpacing());
        float phase = animationPhase(elapsedSeconds, style, spacing);
        if (!Render2D.shaderRoute(event.guiGraphics(), points, style, phase, spacing)) {
            renderFallback(event.guiGraphics(), points, style);
        }
    }

    private List<Vector2f> buildRoute(
            HoveredTerritoryInfoRenderEvent event,
            List<String> territoryNames,
            TerritoryRouteStyle style
    ) {
        List<Vector2f> mapPoints = new ArrayList<>(territoryNames.size());
        for (String territoryName : territoryNames) {
            TerritoryPoi poi = Models.Territory.getTerritoryPoiFromAdvancement(territoryName);
            if (poi == null) {
                return List.of();
            }

            mapPoints.add(new Vector2f(
                    MapRenderer.getRenderX(poi, event.mapCenterX(), event.centerX(), event.zoomRenderScale()),
                    MapRenderer.getRenderZ(poi, event.mapCenterZ(), event.centerZ(), event.zoomRenderScale())
            ));
        }

        float cornerRadius = Math.max(MIN_CORNER_RADIUS, style.ribbonHalfWidth() * CORNER_RIBBON_RADIUS_MULTIPLIER);
        return removeDuplicatePoints(offsetPoints(densifyPoints(roundCorners(mapPoints, cornerRadius)), style.offset()));
    }

    private List<Vector2f> offsetPoints(List<Vector2f> points, float offset) {
        List<Vector2f> result = new ArrayList<>(points.size());
        float routeLength = routeLength(points);
        float routeDistance = 0.0F;
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                routeDistance += points.get(i).distance(points.get(i - 1));
            }
            Vector2f normal = new Vector2f();
            if (i > 0) {
                addSegmentNormal(normal, points.get(i - 1), points.get(i));
            }
            if (i + 1 < points.size()) {
                addSegmentNormal(normal, points.get(i), points.get(i + 1));
            }
            if (normal.lengthSquared() > MIN_SEGMENT_LENGTH_SQUARED) {
                float endpointDistance = Math.min(routeDistance, routeLength - routeDistance);
                normal.normalize(offset * smoothstep(endpointDistance / ENDPOINT_JOIN_LENGTH));
            }

            result.add(new Vector2f(points.get(i)).add(normal));
        }
        return result;
    }

    private List<Vector2f> densifyPoints(List<Vector2f> points) {
        if (points.size() < 2) {
            return List.copyOf(points);
        }

        List<Vector2f> result = new ArrayList<>();
        result.add(new Vector2f(points.getFirst()));
        for (int i = 1; i < points.size(); i++) {
            Vector2f start = points.get(i - 1);
            Vector2f end = points.get(i);
            int steps = Math.max(1, (int) Math.ceil(start.distance(end) / ROUTE_SAMPLE_LENGTH));
            for (int step = 1; step <= steps; step++) {
                result.add(new Vector2f(start).lerp(end, (float) step / steps));
            }
        }
        return result;
    }

    private List<Vector2f> roundCorners(List<Vector2f> points, float requestedRadius) {
        if (points.size() < 3) {
            return List.copyOf(points);
        }

        List<Vector2f> result = new ArrayList<>();
        result.add(new Vector2f(points.getFirst()));
        for (int i = 1; i + 1 < points.size(); i++) {
            Vector2f previous = points.get(i - 1);
            Vector2f corner = points.get(i);
            Vector2f next = points.get(i + 1);
            float incomingLength = previous.distance(corner);
            float outgoingLength = corner.distance(next);
            if (incomingLength * incomingLength <= MIN_SEGMENT_LENGTH_SQUARED || outgoingLength * outgoingLength <= MIN_SEGMENT_LENGTH_SQUARED) {
                result.add(new Vector2f(corner));
                continue;
            }

            float radius = Math.min(requestedRadius, Math.min(incomingLength, outgoingLength) * MAX_CORNER_SEGMENT_FRACTION);
            Vector2f entry = new Vector2f(corner).sub(new Vector2f(corner).sub(previous).normalize(radius));
            Vector2f exit = new Vector2f(corner).add(new Vector2f(next).sub(corner).normalize(radius));
            result.add(entry);
            int steps = Math.max(2, (int) Math.ceil(radius / CORNER_SAMPLE_LENGTH));
            for (int step = 1; step <= steps; step++) {
                float progress = (float) step / steps;
                Vector2f incoming = new Vector2f(entry).lerp(corner, progress);
                Vector2f outgoing = new Vector2f(corner).lerp(exit, progress);
                result.add(incoming.lerp(outgoing, progress));
            }
        }
        result.add(new Vector2f(points.getLast()));
        return result;
    }

    private float fittedSpacing(List<Vector2f> points, float requestedSpacing) {
        float length = routeLength(points);
        int lightCount = Math.max(1, Math.round(length / requestedSpacing));
        return length / lightCount;
    }

    private float animationPhase(double elapsedSeconds, TerritoryRouteStyle style, float spacing) {
        double cycle = elapsedSeconds * style.dotSpeed() / style.dotSpacing();
        return (float) ((cycle - Math.floor(cycle)) * spacing);
    }

    private float routeLength(List<Vector2f> points) {
        float length = 0.0F;
        for (int i = 1; i < points.size(); i++) {
            length += points.get(i).distance(points.get(i - 1));
        }
        return length;
    }

    private float smoothstep(float value) {
        float clamped = Math.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private void addSegmentNormal(Vector2f accumulator, Vector2f start, Vector2f end) {
        float deltaX = end.x - start.x;
        float deltaY = end.y - start.y;
        float lengthSquared = deltaX * deltaX + deltaY * deltaY;
        if (lengthSquared <= MIN_SEGMENT_LENGTH_SQUARED) {
            return;
        }

        float inverseLength = (float) (1.0 / Math.sqrt(lengthSquared));
        accumulator.add(-deltaY * inverseLength, deltaX * inverseLength);
    }

    private List<Vector2f> removeDuplicatePoints(List<Vector2f> points) {
        List<Vector2f> result = new ArrayList<>(points.size());
        for (Vector2f point : points) {
            if (result.isEmpty() || result.getLast().distanceSquared(point) > MIN_SEGMENT_LENGTH_SQUARED) {
                result.add(point);
            }
        }
        return List.copyOf(result);
    }

    private void renderFallback(
            GuiGraphics guiGraphics,
            List<Vector2f> points,
            TerritoryRouteStyle style
    ) {
        CustomColor fallbackColor = CustomColor.fromARGBInt(style.lineArgb());
        for (int i = 1; i < points.size(); i++) {
            Vector2f start = points.get(i - 1);
            Vector2f end = points.get(i);
            RenderUtils.drawLine(
                    guiGraphics,
                    fallbackColor,
                    start.x,
                    start.y,
                    end.x,
                    end.y,
                    style.lineWidth()
            );
        }
    }
}
