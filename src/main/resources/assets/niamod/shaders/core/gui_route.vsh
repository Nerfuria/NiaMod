#version 330

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec4 vertexColor;
out vec2 routePosition;
out float ribbonHalfWidth;
flat out float routeLength;
flat out float routePhase;
flat out float routeSpacing;
flat out vec4 lineStyleParams;
flat out vec3 dotColor;
flat out vec4 dotStyleParams;
flat out float lightHalfLength;

const float ROUTE_LENGTH_PACK_SCALE = 10.0;
const float SPACING_PACK_SCALE = 10.0;
const float MAX_CORE_RADIUS = 4.0;
const float MAX_GLOW_RADIUS = 8.0;
const float MAX_LIGHT_HALF_LENGTH = 16.0;
const float COLOR_CHANNEL_MAX = 255.0;
const int PHASE_BITS = 8;
const int PHASE_STEPS = 1 << PHASE_BITS;
const int PHASE_MASK = PHASE_STEPS - 1;
const int DOT_RADIUS_BITS = 7;
const int DOT_RADIUS_MASK = (1 << DOT_RADIUS_BITS) - 1;
const int SPACING_BITS = 10;
const int SPACING_MASK = (1 << SPACING_BITS) - 1;
const int DOT_GLOW_RADIUS_BITS = 5;
const int DOT_GLOW_RADIUS_MASK = (1 << DOT_GLOW_RADIUS_BITS) - 1;
const int OPACITY_BITS = 5;
const int OPACITY_MASK = (1 << OPACITY_BITS) - 1;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    vertexColor = Color;
    routePosition = UV0;
    ribbonHalfWidth = abs(UV0.y);
    routeLength = float(UV1.x) / ROUTE_LENGTH_PACK_SCALE;
    routeSpacing = float(UV2.x & SPACING_MASK) / SPACING_PACK_SCALE;
    routePhase = float(UV1.y & PHASE_MASK) / float(PHASE_STEPS) * routeSpacing;
    lineStyleParams = vec4(
        Normal.xy,
        float(UV2.y & OPACITY_MASK) / float(OPACITY_MASK),
        Normal.z
    );
    lightHalfLength = round(Color.a * COLOR_CHANNEL_MAX) / COLOR_CHANNEL_MAX * MAX_LIGHT_HALF_LENGTH;

    uint packedDotColor = uint(round(Position.z));
    dotColor = vec3(
        float((packedDotColor >> 16u) & 0xFFu),
        float((packedDotColor >> 8u) & 0xFFu),
        float(packedDotColor & 0xFFu)
    ) / 255.0;

    dotStyleParams = vec4(
        float((UV1.y >> PHASE_BITS) & DOT_RADIUS_MASK) / float(DOT_RADIUS_MASK) * MAX_CORE_RADIUS,
        float((UV2.x >> SPACING_BITS) & DOT_GLOW_RADIUS_MASK) / float(DOT_GLOW_RADIUS_MASK) * MAX_GLOW_RADIUS,
        float((UV2.y >> OPACITY_BITS) & OPACITY_MASK) / float(OPACITY_MASK),
        float((UV2.y >> (OPACITY_BITS * 2)) & OPACITY_MASK) / float(OPACITY_MASK)
    );
}
