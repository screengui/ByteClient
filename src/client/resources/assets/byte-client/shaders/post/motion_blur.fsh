#version 330

uniform sampler2D MainSampler;
uniform sampler2D MainDepthSampler;

layout(std140) uniform MotionBlurUniforms {
    mat4 mvInverse;
    mat4 projInverse;
    mat4 prevModelView;
    mat4 prevProjection;
    vec3 cameraDelta;
    vec2 view_res;
    float blendFactor;
    int sampleCount;
    int fullVelocity;
    int debugMode;
    int useDepth;
};

in vec2 texCoord;
out vec4 color;

vec3 reproject(vec3 screenPos) {
    vec3 ndc = screenPos * 2.0 - 1.0;
    vec4 viewPos = projInverse * vec4(ndc, 1.0);
    vec3 worldPos = (mvInverse * vec4(viewPos.xyz / viewPos.w, 1.0)).xyz + cameraDelta;
    vec4 previousClip = prevProjection * (prevModelView * vec4(worldPos, 1.0));
    return (previousClip.xyz / previousClip.w) * 0.5 + 0.5;
}

vec2 clampLength(vec2 velocity) {
    float lengthSquared = dot(velocity, velocity);
    return lengthSquared > 0.16 ? velocity * (0.4 * inversesqrt(lengthSquared)) : velocity;
}

float noise(vec2 position) {
    return fract(52.9829189 * fract(0.06711056 * position.x + 0.00583715 * position.y));
}

void main() {
    ivec2 texel = ivec2(gl_FragCoord.xy);
    float depth = texelFetch(MainDepthSampler, texel, 0).x;
    vec2 velocity;
    if (useDepth == 1) {
        if (depth < 0.56) {
            color = texture(MainSampler, texCoord);
            return;
        }
        float dilatedDepth = min(depth, texelFetch(MainDepthSampler, texel + ivec2(1, 0), 0).x);
        dilatedDepth = min(dilatedDepth, texelFetch(MainDepthSampler, texel + ivec2(-1, 0), 0).x);
        dilatedDepth = min(dilatedDepth, texelFetch(MainDepthSampler, texel + ivec2(0, 1), 0).x);
        dilatedDepth = min(dilatedDepth, texelFetch(MainDepthSampler, texel + ivec2(0, -1), 0).x);
        velocity = clampLength(texCoord - reproject(vec3(texCoord, dilatedDepth)).xy);
    } else {
        velocity = clampLength(texCoord - reproject(vec3(texCoord, 1.0)).xy);
    }
    float speed = length(velocity);
    if (speed < 0.0001) {
        color = texture(MainSampler, texCoord);
        return;
    }
    int samples = clamp(int(ceil(speed * blendFactor * float(sampleCount))), 4, sampleCount);
    vec2 stepUv = (blendFactor * velocity) / float(samples);
    float centerOffset = -float(samples) * 0.5;
    vec2 seed = texCoord * view_res;
    vec3 sum = vec3(0.0);
    for (int index = 0; index < samples; index++) {
        float sampleIndex = float(index);
        float jitter = noise(seed + vec2(sampleIndex, sampleIndex * 1.4));
        vec2 position = texCoord + (sampleIndex + centerOffset + jitter) * stepUv;
        vec3 sampleColor = texture(MainSampler, position).rgb;
        sum += sampleColor * sampleColor;
    }
    color = vec4(sqrt(sum / float(samples)), 1.0);
}
