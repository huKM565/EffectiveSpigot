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

#ifdef IS_GUI
#include <minecraft:globals.glsl>
uniform sampler2D Sampler0;
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
    vec2 uv = UV0;

#ifdef IS_GUI
    int corner = gl_VertexIndex % 4;
    bool left = corner == 0 || corner == 1;
    bool top = corner == 0 || corner == 3;
    ivec2 atlasSize = textureSize(Sampler0, 0);
    ivec2 texel = ivec2(round(UV0 * vec2(atlasSize))) - ivec2(left ? 0 : 1, top ? 0 : 1);
    ivec4 mark = ivec4(texelFetch(Sampler0, clamp(texel, ivec2(0), atlasSize - 1), 0) * 255.0 + 0.5);
    bool isImage = mark == ivec4(37, 13, 71, 1);
    bool isAnimation = mark == ivec4(37, 13, 70, 1);
    bool isText = mark.r == 37 && mark.g == 13 && mark.a == 1 && mark.b >= 100 && mark.b <= 140;
    bool isDialogFlag = mark == ivec4(37, 13, 72, 1);
    if (isDialogFlag) {
        ivec2 centre = texel + ivec2(left ? 1 : -1, top ? 1 : -1);
        uv = (vec2(centre) + 0.5) / vec2(atlasSize);
        gl_Position = ProjMat * ModelViewMat * vec4(left ? 0.0 : 1.0, top ? 0.0 : 1.0, Position.z, 1.0);
        color = vec4(1.0);
    } else if (isImage || isAnimation || isText) {
        ivec3 c = ivec3(Color.rgb * 255.0 + 0.5);
        int packed = (c.r << 16) | (c.g << 8) | c.b;
        vec2 target = vec2(float(packed >> 15), float((packed >> 6) & 0x1FF)) / 511.0;
        int sizeStep = (packed & 0x3F) + 1;
        if (isImage || isAnimation) {
            int inward = left ? 1 : -1;
            ivec3 corner = ivec3(texelFetch(Sampler0, clamp(texel + ivec2(inward, 0), ivec2(0), atlasSize - 1), 0).rgb * 255.0 + 0.5);
            ivec3 widthData = ivec3(texelFetch(Sampler0, clamp(texel + ivec2(2 * inward, 0), ivec2(0), atlasSize - 1), 0).rgb * 255.0 + 0.5);
            int cornerData = (corner.r << 16) | (corner.g << 8) | corner.b;
            vec2 picturePixel = vec2(float(cornerData >> 12), float(cornerData & 0xFFF)) - 1.0;
            float pictureWidth = float((widthData.r << 16) | (widthData.g << 8) | widthData.b);
            float pixelX = float(sizeStep) / 64.0 / pictureWidth;
            float pixelY = pixelX * abs(ProjMat[1][1] / ProjMat[0][0]);
            vec2 p = target + picturePixel * vec2(pixelX, pixelY);
            gl_Position.xy = vec2(p.x * 2.0 - 1.0, 1.0 - p.y * 2.0) * gl_Position.w;
            if (isAnimation) {
                ivec3 timing = ivec3(texelFetch(Sampler0, clamp(texel + ivec2(3 * inward, 0), ivec2(0), atlasSize - 1), 0).rgb * 255.0 + 0.5);
                ivec3 rateData = ivec3(texelFetch(Sampler0, clamp(texel + ivec2(4 * inward, 0), ivec2(0), atlasSize - 1), 0).rgb * 255.0 + 0.5);
                int timingData = (timing.r << 16) | (timing.g << 8) | timing.b;
                float framesPerSecond = float((rateData.r << 16) | (rateData.g << 8) | rateData.b);
                float frameCount = float(timingData & 0xFFF);
                float currentFrame = mod(floor(GameTime * 1200.0 * framesPerSecond), frameCount);
                if (currentFrame != float(timingData >> 12)) {
                    gl_Position = vec4(-2.0, -2.0, 0.0, 1.0);
                }
            }
        } else {
            vec2 gui = 2.0 / abs(vec2(ProjMat[0][0], ProjMat[1][1]));
            float lineOffset = float(mark.b - 100);
            float lineY = top ? Position.y + lineOffset : Position.y - lineOffset;
            vec2 lineCentre = vec2(floor(gui.x / 2.0), lineY + 4.0);
            vec2 p = target * gui + (Position.xy - lineCentre) * (float(sizeStep) / 4.0);
            gl_Position = ProjMat * ModelViewMat * vec4(p, Position.z, 1.0);
        }
        color = vec4(1.0, 1.0, 1.0, Color.a);
    }
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = color * sample_lightmap(Sampler2, UV2);
#else
    vertexColor = color;
#endif
    texCoord0 = uv;
}
