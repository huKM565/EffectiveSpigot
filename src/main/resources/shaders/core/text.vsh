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

#ifdef IS_GUI
int readData(ivec2 texel, int offset) {
    ivec2 atlasSize = textureSize(Sampler0, 0);
    ivec3 value = ivec3(texelFetch(Sampler0, clamp(texel + ivec2(offset, 0), ivec2(0), atlasSize - 1), 0).rgb * 255.0 + 0.5);
    return (value.r << 16) | (value.g << 8) | value.b;
}
#endif

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vec4 color = Color;
    vec2 uv = UV0;

#ifdef IS_GUI
    ivec2 atlasSize = textureSize(Sampler0, 0);
    vec2 atlasPosition = UV0 * vec2(atlasSize);
    bool left = fract(atlasPosition.x) < 0.5;
    bool top = fract(atlasPosition.y) < 0.5;
    ivec2 texel = ivec2(floor(atlasPosition));
    ivec4 mark = ivec4(texelFetch(Sampler0, clamp(texel, ivec2(0), atlasSize - 1), 0) * 255.0 + 0.5);
    bool isImage = mark == ivec4(37, 13, 71, 1);
    bool isAnimation = mark == ivec4(37, 13, 70, 1);
    bool isText = mark.r == 37 && mark.g == 13 && mark.a == 1 && mark.b >= 100 && mark.b <= 140;
    bool isDialogFlag = mark == ivec4(37, 13, 72, 1);
    ivec3 c = ivec3(Color.rgb * 255.0 + 0.5);
    int colorData = (c.r << 16) | (c.g << 8) | c.b;
    if (isDialogFlag) {
        ivec2 centre = texel + ivec2(left ? 1 : -1, top ? 1 : -1);
        uv = (vec2(centre) + 0.5) / vec2(atlasSize);
        gl_Position = ProjMat * ModelViewMat * vec4(left ? 0.0 : 1.0, top ? 0.0 : 1.0, Position.z, 1.0);
        color = vec4(1.0);
    } else if (isImage || isAnimation) {
        int inward = left ? 1 : -1;
        int placement = isAnimation ? readData(texel, 5 * inward) : colorData;
        vec2 target = vec2(float(placement >> 15), float((placement >> 6) & 0x1FF)) / 511.0;
        int sizeStep = (placement & 0x3F) + 1;
        int cornerData = readData(texel, inward);
        vec2 picturePixel = vec2(float(cornerData >> 12), float(cornerData & 0xFFF)) - 1.0;
        float pictureWidth = float(readData(texel, 2 * inward));
        float pixelX = float(sizeStep) / 64.0 / pictureWidth;
        float pixelY = pixelX * abs(ProjMat[1][1] / ProjMat[0][0]);
        vec2 p = target + picturePixel * vec2(pixelX, pixelY);
        gl_Position.xy = vec2(p.x * 2.0 - 1.0, 1.0 - p.y * 2.0) * gl_Position.w;
        if (isAnimation) {
            int timing = readData(texel, 3 * inward);
            float framesPerSecond = float(readData(texel, 4 * inward));
            float frameCount = float(timing & 0xFFF);
            int playback = colorData >> 15;
            float elapsedTicks = mod(GameTime * 24000.0 - float(colorData & 0x7FFF), 24000.0);
            bool started = (playback < 2 || playback == 4) && elapsedTicks < 24000.0 - 200.0;
            float frame = playback == 3 ? frameCount - 1.0 : started ? floor(elapsedTicks / 20.0 * framesPerSecond) : 0.0;
            float currentFrame = playback == 0 ? mod(frame, frameCount) : min(frame, frameCount - 1.0);
            if (playback == 4) currentFrame = frameCount - 1.0 - currentFrame;
            if (currentFrame != float(timing >> 12)) {
                gl_Position = vec4(-2.0, -2.0, 0.0, 1.0);
            }
        }
        color = vec4(1.0, 1.0, 1.0, Color.a);
    } else if (isText) {
        vec2 target = vec2(float(colorData >> 15), float((colorData >> 6) & 0x1FF)) / 511.0;
        int sizeStep = (colorData & 0x3F) + 1;
        vec2 gui = 2.0 / abs(vec2(ProjMat[0][0], ProjMat[1][1]));
        float lineOffset = float(mark.b - 100);
        float lineY = top ? Position.y + lineOffset : Position.y - lineOffset;
        vec2 lineCentre = vec2(floor(gui.x / 2.0), lineY + 4.0);
        vec2 p = target * gui + (Position.xy - lineCentre) * (float(sizeStep) / 4.0);
        gl_Position = ProjMat * ModelViewMat * vec4(p, Position.z, 1.0);
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
