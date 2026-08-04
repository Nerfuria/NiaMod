#version 330

in vec4 vertexColor;
in vec2 routePosition;
in float ribbonHalfWidth;
flat in float routeLength;
flat in float routePhase;
flat in float routeSpacing;
flat in vec4 lineStyleParams;
flat in vec3 dotColor;
flat in vec4 dotStyleParams;
flat in float lightHalfLength;

out vec4 fragColor;

const float MAX_CORE_RADIUS = 4.0;
const float MAX_GLOW_RADIUS = 8.0;
const float MIN_DOT_SPACING = 1.0;
const float LINE_AA_SCALE = 1.25;
const float DOT_AA_SCALE = 1.2;
const float MIN_AA_WIDTH = 0.45;
const float MIN_SIGMA = 0.001;
const float MIN_VISIBLE_ALPHA = 0.002;
const float MIN_DIVISOR = 0.001;
const float GLOW_EDGE_PADDING = 0.5;
const float MIN_GLOW_EDGE_WIDTH = 0.5;

float gaussian(float distanceFromCenter, float sigma) {
    float normalized = distanceFromCenter / max(sigma, MIN_SIGMA);
    return exp(-0.5 * normalized * normalized);
}

float antialiasedCore(float distanceFromCenter, float radius, float scale) {
    float aa = max(fwidth(distanceFromCenter) * scale, MIN_AA_WIDTH);
    return 1.0 - smoothstep(radius - aa, radius + aa, distanceFromCenter);
}

void main() {
    float outsideRoute = max(max(-routePosition.x, routePosition.x - routeLength), 0.0);
    float distanceFromLine = length(vec2(outsideRoute, abs(routePosition.y)));
    float alongRoute = clamp(routePosition.x, 0.0, routeLength);
    float lineCoreRadius = lineStyleParams.x * MAX_CORE_RADIUS;
    float lineGlowRadius = lineStyleParams.y * MAX_GLOW_RADIUS;
    float glowEdgeStart = max(lineCoreRadius, dotStyleParams.x) + GLOW_EDGE_PADDING;
    float glowEdgeEnd = max(ribbonHalfWidth, glowEdgeStart + MIN_GLOW_EDGE_WIDTH);
    float glowEdgeFade = 1.0 - smoothstep(glowEdgeStart, glowEdgeEnd, distanceFromLine);
    float lineCore = antialiasedCore(distanceFromLine, lineCoreRadius, LINE_AA_SCALE);
    float lineGlow = gaussian(distanceFromLine, lineGlowRadius) * glowEdgeFade;
    float lineAlpha = clamp(lineCore * lineStyleParams.z + lineGlow * lineStyleParams.w, 0.0, 1.0);

    float spacing = max(routeSpacing, MIN_DOT_SPACING);
    float cycleDistance = mod(alongRoute - routePhase + spacing * 0.5, spacing) - spacing * 0.5;
    float distanceAlongLight = max(abs(cycleDistance) - lightHalfLength, 0.0);
    float distanceFromDot = length(vec2(distanceAlongLight, distanceFromLine));
    float dotCore = antialiasedCore(distanceFromDot, dotStyleParams.x, DOT_AA_SCALE);
    float dotGlow = gaussian(distanceFromDot, dotStyleParams.y) * glowEdgeFade;
    float dotAlpha = clamp(dotCore * dotStyleParams.z + dotGlow * dotStyleParams.w, 0.0, 1.0);

    float alpha = dotAlpha + lineAlpha * (1.0 - dotAlpha);
    if (alpha <= MIN_VISIBLE_ALPHA) {
        discard;
    }

    vec3 premultipliedColor = dotColor * dotAlpha + vertexColor.rgb * lineAlpha * (1.0 - dotAlpha);
    fragColor = vec4(premultipliedColor / max(alpha, MIN_DIVISOR), alpha);
}
