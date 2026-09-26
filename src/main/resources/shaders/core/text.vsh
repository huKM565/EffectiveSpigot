#version 330
#extension GL_ARB_separate_shader_objects : require

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#include <minecraft:fog.glsl>
#include <minecraft:sample_lightmap.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
layout(location = 3) in ivec2 UV2;
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
#endif

layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vec4 color = Color;

#ifdef IS_GUI
    ivec3 c = ivec3(Color.rgb * 255.0 + 0.5);
    if ((c.b & 0xE0) == 0xC0) {
        float x = float(c.r) / 255.0;
        float y = float(c.g) / 255.0;
        float w = float((c.b & 0x1F) + 1) / 32.0;
        float h = w * abs(ProjMat[1][1] / ProjMat[0][0]);
        int corner = gl_VertexIndex % 4;
        vec2 p = vec2(
            (corner == 0 || corner == 1) ? x : x + w,
            (corner == 0 || corner == 3) ? y : y + h
        );
        gl_Position.xy = vec2(p.x * 2.0 - 1.0, 1.0 - p.y * 2.0) * gl_Position.w;
        color = vec4(1.0);
    } else if ((c.b & 0xE0) == 0x80) {
        float x = float(c.r) / 255.0;
        float y = float(c.g) / 255.0;
        float scale = float((c.b & 0x1F) + 1) / 8.0;
        vec4 origin = ProjMat * ModelViewMat * vec4(0.0, 0.0, 0.0, 1.0);
        vec2 offset = (gl_Position.xy - origin.xy) / gl_Position.w;
        gl_Position.xy = (vec2(x * 2.0 - 1.0, 1.0 - y * 2.0) + offset * scale) * gl_Position.w;
        color = vec4(1.0);
    }
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = color * sample_lightmap(Sampler2, UV2);
#else
    vertexColor = color;
#endif
    texCoord0 = UV0;
}
