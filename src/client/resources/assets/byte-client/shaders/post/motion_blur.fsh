#version 330

uniform sampler2D InSampler;
uniform sampler2D HistorySampler;

layout(std140) uniform MotionConfig {
    float HistoryWeight;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 current = texture(InSampler, texCoord);
    vec4 history = texture(HistorySampler, texCoord);
    float hasHistory = step(0.01, history.a);
    float weight = clamp(HistoryWeight * hasHistory, 0.0, 0.75);
    fragColor = vec4(mix(current.rgb, history.rgb, weight), 1.0);
}
