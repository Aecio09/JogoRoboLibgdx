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
    // Curvatura ajustada (+20% em relacao a versao anterior)
    uv.x *= 1.0 + pow((abs(uv.y) / 6.0), 2.0);
    uv.y *= 1.0 + pow((abs(uv.x) / 5.2), 2.0);
    uv = (uv / 2.0) + 0.5;
    uv = uv * 0.98 + 0.01;
    return uv;
}

void main() {
    // Curvatura aumentada em 20% (fator de mistura ajustado para 0.78)
    vec2 curved_uv = curve(v_texCoords);
    vec2 uv = mix(v_texCoords, curved_uv, 0.78);
    
    // Aberracao cromatica radial e natural (zero no centro da tela, evitando efeito de oculos 3D)
    vec2 delta = uv - 0.5;
    float distSq = dot(delta, delta);
    float r = texture2D(u_texture, uv + delta * (distSq * 0.003)).r;
    float g = texture2D(u_texture, uv).g;
    float b = texture2D(u_texture, uv - delta * (distSq * 0.003)).b;
    
    vec3 col = vec3(r, g, b);
    
    // Mascara de fosforo CRT sutil (subpixels RGB sem borrar ou criar fantasmas vermelhos/azuis)
    vec3 mask = vec3(1.0);
    float subpixel = mod(gl_FragCoord.x, 3.0);
    if (subpixel < 1.0) {
        mask = vec3(1.04, 0.96, 0.96);
    } else if (subpixel < 2.0) {
        mask = vec3(0.96, 1.04, 0.96);
    } else {
        mask = vec3(0.96, 0.96, 1.04);
    }
    col *= mask;
    
    // Scanlines horizontais suaves
    float scanline = sin(uv.y * 720.0 * 3.14159) * 0.03;
    col -= scanline;
    
    // Vinheta e escurecimento natural de borda do tubo catodico
    float vignette = uv.x * uv.y * (1.0 - uv.x) * (1.0 - uv.y);
    vignette = clamp(pow(16.0 * vignette, 0.25), 0.0, 1.0);
    col *= mix(1.0, vignette, 0.65);
    
    // Oscilacao sutil de brilho do tubo (flicker)
    col *= 1.0 + 0.006 * sin(110.0 * u_time);
    
    // Corte nas bordas curvas
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        col = vec3(0.0);
    }
    
    gl_FragColor = vec4(col, 1.0);
}
