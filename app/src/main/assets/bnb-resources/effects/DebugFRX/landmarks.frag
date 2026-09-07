#include <bnb/glsl.frag>

void main()
{
    vec2 cxy = 2.0 * gl_PointCoord - 1.0;
    float r = 1.25 * dot(cxy, cxy);
    float delta = fwidth(r);
    float alpha = 1.0 - smoothstep(1.0 - delta, 1.0 + delta, r);
    
    bnb_FragColor = vec4(1.0, 0.0, 0.0, 1.0) * alpha;
}