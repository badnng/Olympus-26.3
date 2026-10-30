#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:globals.glsl>

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(location = 0) out vec4 fragColor;

// Based off my own implementation of a screen post shader for an electrical stun effect
// https://github.com/Xylonity/Hostiles/blob/v1.20.1/common/src/main/resources/assets/hostiles/shaders/program/polarity/polarity.vsh
void main() {

    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    float time = GameTime * 24000.0;
    float pulse = 0.5 + 0.5 * sin(time * 0.74 + sin(time * 0.19) * 2.0);

    vec2 shakePixels = vec2(
        sin(time * 2.83) + sin(time * 6.17 + 1.4) * 0.45,
        sin(time * 3.47 + 2.1) + sin(time * 7.31) * 0.35
    ) * (1.35 + pulse * 1.65);

    vec2 uv = texCoord + shakePixels * texel;

    float splitAngle = time * 0.91 + sin(time * 0.37) * 0.8;
    vec2 splitDirection = vec2(cos(splitAngle), sin(splitAngle));
    vec2 splitOffset = splitDirection * texel * (2.6 + pulse * 3.0);

    vec3 aberrated = vec3(
        texture(InSampler, uv + splitOffset).r,
        texture(InSampler, uv).g,
        texture(InSampler, uv - splitOffset).b
    );

    vec2 blurOffset = vec2(-splitDirection.y, splitDirection.x) * texel * (1.4 + pulse * 1.6);
    vec3 blurred = texture(InSampler, uv + blurOffset).rgb + texture(InSampler, uv - blurOffset).rgb;
    vec3 color = aberrated * 0.68 + blurred * 0.16;

    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(color, vec3(luminance), 0.08 + pulse * 0.1);
    color += vec3(1.0, 0.95, 0.12) * (0.018 + pulse * 0.022);

    vec2 centered = (texCoord - 0.5) * vec2(OutSize.x / max(OutSize.y, 1.0), 1.0);
    float vignette = smoothstep(0.28, 0.92, length(centered));
    color *= 1.0 - vignette * (0.07 + pulse * 0.035);

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
