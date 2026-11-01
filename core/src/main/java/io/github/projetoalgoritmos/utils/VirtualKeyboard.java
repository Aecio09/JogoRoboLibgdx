package io.github.projetoalgoritmos.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class VirtualKeyboard {

    public static class Key {
        public String label;
        public String value;
        public Rectangle bounds;
        public Color bgColor;
        public Color borderColor;
        public Color textColor;

        public Key(String label, String value, float x, float y, float w, float h,
                   Color bg, Color border, Color text) {
            this.label = label;
            this.value = value;
            this.bounds = new Rectangle(x, y, w, h);
            this.bgColor = bg;
            this.borderColor = border;
            this.textColor = text;
        }
    }

    private Key[] keys;
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private String lastPressedKey = null;
    private float pressFeedbackTimer = 0f;

    // Paleta de cores temática
    private static final Color COLOR_KEY_BG = new Color(0.08f, 0.10f, 0.12f, 0.95f);
    private static final Color COLOR_KEY_BORDER = new Color(0f, 0.75f, 0.85f, 0.50f);
    private static final Color COLOR_KEY_TEXT = Color.WHITE;

    private static final Color COLOR_ENTER_BG = new Color(0.05f, 0.28f, 0.20f, 0.95f);
    private static final Color COLOR_ENTER_BORDER = new Color(0.10f, 0.95f, 0.50f, 0.90f);
    private static final Color COLOR_ENTER_TEXT = new Color(0.20f, 1.0f, 0.60f, 1f);

    private static final Color COLOR_DEL_BG = new Color(0.25f, 0.08f, 0.08f, 0.95f);
    private static final Color COLOR_DEL_BORDER = new Color(0.95f, 0.35f, 0.25f, 0.75f);
    private static final Color COLOR_DEL_TEXT = new Color(1f, 0.55f, 0.45f, 1f);

    private static final Color COLOR_PRESSED_BG = new Color(0f, 0.55f, 0.70f, 0.90f);

    public VirtualKeyboard() {
        criarTeclas();
    }

    private void criarTeclas() {
        // Layout do teclado: largura total ~840px, centralizado em 1280 (x=220)
        float startX = 220f;
        float totalWidth = 840f;
        float keyH = 50f;
        float gapX = 8f;
        float gapY = 8f;

        // Linhas de baixo para cima:
        // Linha 4 (y=50): LIMPAR, ESPAÇO, AUTORIZAR
        // Linha 3 (y=108): Z, X, C, V, B, N, M, DEL
        // Linha 2 (y=166): A, S, D, F, G, H, J, K, L
        // Linha 1 (y=224): Q, W, E, R, T, Y, U, I, O, P
        // Linha 0 (y=282): 1, 2, 3, 4, 5, 6, 7, 8, 9, 0

        String[] row0 = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        String[] row1 = {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"};
        String[] row2 = {"A", "S", "D", "F", "G", "H", "J", "K", "L"};
        String[] row3 = {"Z", "X", "C", "V", "B", "N", "M"};

        int totalKeys = row0.length + row1.length + row2.length + row3.length + 1 + 3;
        keys = new Key[totalKeys];
        int idx = 0;

        float keyW10 = (totalWidth - (9 * gapX)) / 10f; // ~76.8px

        // Linha 0: Números
        float y0 = 282f;
        for (int i = 0; i < row0.length; i++) {
            float x = startX + i * (keyW10 + gapX);
            keys[idx++] = new Key(row0[i], row0[i], x, y0, keyW10, keyH,
                COLOR_KEY_BG, COLOR_KEY_BORDER, COLOR_KEY_TEXT);
        }

        // Linha 1: QWERTY
        float y1 = 224f;
        for (int i = 0; i < row1.length; i++) {
            float x = startX + i * (keyW10 + gapX);
            keys[idx++] = new Key(row1[i], row1[i], x, y1, keyW10, keyH,
                COLOR_KEY_BG, COLOR_KEY_BORDER, COLOR_KEY_TEXT);
        }

        // Linha 2: ASDFGHJKL (9 teclas centralizadas)
        float y2 = 166f;
        float row2Width = 9 * keyW10 + 8 * gapX;
        float row2StartX = startX + (totalWidth - row2Width) / 2f;
        for (int i = 0; i < row2.length; i++) {
            float x = row2StartX + i * (keyW10 + gapX);
            keys[idx++] = new Key(row2[i], row2[i], x, y2, keyW10, keyH,
                COLOR_KEY_BG, COLOR_KEY_BORDER, COLOR_KEY_TEXT);
        }

        // Linha 3: ZXCVBNM + DEL
        float y3 = 108f;
        float x3 = startX;
        for (int i = 0; i < row3.length; i++) {
            float x = x3 + i * (keyW10 + gapX);
            keys[idx++] = new Key(row3[i], row3[i], x, y3, keyW10, keyH,
                COLOR_KEY_BG, COLOR_KEY_BORDER, COLOR_KEY_TEXT);
        }
        float delX = x3 + row3.length * (keyW10 + gapX);
        float delW = (startX + totalWidth) - delX;
        keys[idx++] = new Key("<- DEL", "BACKSPACE", delX, y3, delW, keyH,
            COLOR_DEL_BG, COLOR_DEL_BORDER, COLOR_DEL_TEXT);

        // Linha 4: LIMPAR + ESPACO + AUTORIZAR
        float y4 = 50f;
        float clearW = 140f;
        float enterW = 240f;
        float spaceW = totalWidth - clearW - enterW - (2 * gapX);

        keys[idx++] = new Key("LIMPAR", "CLEAR", startX, y4, clearW, keyH,
            COLOR_DEL_BG, COLOR_DEL_BORDER, COLOR_DEL_TEXT);

        float spaceX = startX + clearW + gapX;
        keys[idx++] = new Key("ESPACO", "SPACE", spaceX, y4, spaceW, keyH,
            COLOR_KEY_BG, COLOR_KEY_BORDER, COLOR_KEY_TEXT);

        float enterX = spaceX + spaceW + gapX;
        keys[idx++] = new Key("AUTORIZAR", "ENTER", enterX, y4, enterW, keyH,
            COLOR_ENTER_BG, COLOR_ENTER_BORDER, COLOR_ENTER_TEXT);
    }

    public void update(float dt) {
        if (pressFeedbackTimer > 0f) {
            pressFeedbackTimer -= dt;
            if (pressFeedbackTimer <= 0f) {
                lastPressedKey = null;
            }
        }
    }

    /**
     * Processa um toque nas coordenadas desprojetadas (1280x720).
     * Retorna a ação ou caractere tocado, ou null se fora do teclado.
     */
    public String handleTouch(float tx, float ty) {
        for (Key key : keys) {
            if (key.bounds.contains(tx, ty)) {
                lastPressedKey = key.value;
                pressFeedbackTimer = 0.12f;
                return key.value;
            }
        }
        return null;
    }

    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font) {
        // 1. Desenha o fundo e bordas de todas as teclas
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Key key : keys) {
            boolean isPressed = key.value.equals(lastPressedKey) && pressFeedbackTimer > 0f;
            Color bg = isPressed ? COLOR_PRESSED_BG : key.bgColor;
            shapeRenderer.setColor(bg);
            shapeRenderer.rect(key.bounds.x, key.bounds.y, key.bounds.width, key.bounds.height);

            // Borda da tecla
            shapeRenderer.setColor(isPressed ? Color.WHITE : key.borderColor);
            float bw = isPressed ? 2.5f : 1.5f;
            shapeRenderer.rect(key.bounds.x, key.bounds.y, key.bounds.width, bw);
            shapeRenderer.rect(key.bounds.x, key.bounds.y + key.bounds.height - bw, key.bounds.width, bw);
            shapeRenderer.rect(key.bounds.x, key.bounds.y, bw, key.bounds.height);
            shapeRenderer.rect(key.bounds.x + key.bounds.width - bw, key.bounds.y, bw, key.bounds.height);
        }
        shapeRenderer.end();

        // 2. Desenha os rótulos de texto centralizados em cada tecla
        batch.begin();
        for (Key key : keys) {
            boolean isPressed = key.value.equals(lastPressedKey) && pressFeedbackTimer > 0f;
            font.setColor(isPressed ? Color.WHITE : key.textColor);

            float scale = (key.label.length() > 2) ? 0.65f : 0.85f;
            font.getData().setScale(scale);

            glyphLayout.setText(font, key.label);
            float textX = key.bounds.x + (key.bounds.width - glyphLayout.width) / 2f;
            float textY = key.bounds.y + (key.bounds.height + glyphLayout.height) / 2f;
            font.draw(batch, key.label, textX, textY);
        }
        batch.end();
    }
}
