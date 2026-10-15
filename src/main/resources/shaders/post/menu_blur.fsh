#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:globals.glsl>

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform MenuBlurConfig {
    vec2 BlurDir;
    float Radius;
    float Darkness;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

bool isDialogFlag(ivec2 pixel) {
    return ivec3(texelFetch(InSampler, pixel, 0).rgb * 255.0 + 0.5) == ivec3(37, 13, 72);
}

void main() {
    ivec2 size = textureSize(InSampler, 0);
    if (isDialogFlag(ivec2(0, 0)) || isDialogFlag(ivec2(0, size.y - 1))) {
        fragColor = texture(InSampler, texCoord);
        return;
    }

    vec2 oneTexel = 1.0 / InSize;
    vec2 sampleStep = oneTexel * BlurDir;

    vec4 blurred = vec4(0.0);
    float actualRadius = Radius >= 0.5 ? round(Radius) : float(MenuBlurRadius);
    for (float a = -actualRadius + 0.5; a <= actualRadius; a += 2.0) {
        blurred += texture(InSampler, texCoord + sampleStep * a);
    }
    blurred += texture(InSampler, texCoord + sampleStep * actualRadius) / 2.0;
    fragColor = blurred / (actualRadius + 0.5);
    fragColor.rgb *= 1.0 - Darkness;
}
