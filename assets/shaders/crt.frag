#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_time;
uniform vec2 u_resolution;

vec2 curve(vec2 uv) {
    uv = (uv - 0.5) * 2.0;
    uv.x *= 1.0 + pow((abs(uv.y) / 7.0), 2.0); // De 5.0 para 7.0 (menos curva)
    uv.y *= 1.0 + pow((abs(uv.x) / 6.0), 2.0); // De 4.0 para 6.0 (menos curva)
    uv = (uv / 2.0) + 0.5;
    uv = uv * 0.98 + 0.01; // Ocupa mais espaço na tela
    return uv;
}

void main() {
    vec2 uv = v_texCoords;
    uv = curve(uv);
    
    vec3 col;
    float r = texture2D(u_texture, vec2(uv.x + 0.003, uv.y)).r;
    float g = texture2D(u_texture, vec2(uv.x + 0.000, uv.y)).g;
    float b = texture2D(u_texture, vec2(uv.x - 0.003, uv.y)).b;
    
    col = vec3(r, g, b);
    
    float scanline = sin(uv.y * 800.0) * 0.04;
    col -= scanline;
    
    float vignette = uv.x * uv.y * (1.0 - uv.x) * (1.0 - uv.y);
    vignette = clamp(pow(16.0 * vignette, 0.3), 0.0, 1.0);
    col *= vignette;
    
    // Scanline effect 2
    col *= 1.0 + 0.01 * sin(110.0 * u_time);
    
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        col = vec3(0.0);
    }
    
    gl_FragColor = vec4(col, 1.0);
}
