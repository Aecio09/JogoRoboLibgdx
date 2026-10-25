package io.github.projetoalgoritmos.minigames;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import io.github.projetoalgoritmos.datastructures.CustomLinkedList;
import io.github.projetoalgoritmos.utils.GifDecoder;

/**
 * Minigame de combate aos vírus dentro do terminal do computador.
 */
public class VirusMinigame {

    // Representa um projétil disparado pelo jogador
    public static class Bullet {
        public float x, y;
        public float width = 6;
        public float height = 14;

        public Bullet(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    // Representa uma unidade de vírus inimigo
    public static class Virus {
        public float x, y;
        public float vx, vy;
        public float width = 48;
        public float height = 48;
        public int hp = 1;

        public Virus(float x, float y, float vx, float vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }

        public Rectangle getBounds() {
            // Hitbox ligeiramente ajustada ao corpo visível do vírus
            return new Rectangle(x + 6, y + 6, width - 12, height - 12);
        }
    }

    // Texturas e Animações
    private Texture computadorTexture;
    private Texture playerTexture;
    private Texture pixelTexture;
    private Texture glowTexture;
    private Texture heartFullTexture;
    private Texture heartEmptyTexture;
    private Animation<TextureRegion> virusAnimation;

    // Fontes TrueType customizadas (BoldPixels.ttf)
    private BitmapFont customFont;
    private BitmapFont customFontTitle;
    private BitmapFont controlsFont;
    private GlyphLayout glyphLayout = new GlyphLayout();
    private static final Color COLOR_CONTROLS_TEXT = new Color(0.22f, 0.22f, 0.22f, 1f);

    // Área do Computador e Retângulo Delimitador da Tela
    private float compX, compY, compWidth, compHeight;
    private Rectangle screenBounds;

    // Estado do Jogador
    private float playerX, playerY;
    private float playerWidth = 44;
    private float playerHeight = 44;
    private float playerSpeed = 260f;
    private int playerLives = 3;
    private final int MAX_LIVES = 3;
    private float invulnerabilityTimer = 0f;
    private float shootCooldown = 0f;

    // Listas encadeadas customizadas acadêmicas (sem java.util)
    private CustomLinkedList<Bullet> bullets;
    private CustomLinkedList<Virus> viruses;

    // Controle de estado
    private float stateTime = 0f;
    private boolean isFinished = false;
    private boolean isVictory = false;

    public VirusMinigame() {
        carregarRecursos();
        configurarDimensoes();
        reiniciar();
    }

    private void carregarRecursos() {
        computadorTexture = new Texture("computador-virus-arena-background.png");
        playerTexture = new Texture("player-virus-minigame.png");

        // Sprites de coração para a barra de vida
        heartFullTexture = new Texture("heart-Full.png");
        heartEmptyTexture = new Texture("heart-empty.png");

        // Gerar fontes a partir de BoldPixels.ttf com caracteres acentuados
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Fonts/BoldPixels.ttf"));

        FreeTypeFontParameter paramNormal = new FreeTypeFontParameter();
        paramNormal.size = 14;
        paramNormal.color = Color.WHITE;
        paramNormal.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "áéíóúÁÉÍÓÚãõÃÕâêîôûÂÊÎÔÛçÇ";
        paramNormal.minFilter = Texture.TextureFilter.Nearest;
        paramNormal.magFilter = Texture.TextureFilter.Nearest;
        customFont = generator.generateFont(paramNormal);

        FreeTypeFontParameter paramTitle = new FreeTypeFontParameter();
        paramTitle.size = 22;
        paramTitle.color = Color.WHITE;
        paramTitle.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "áéíóúÁÉÍÓÚãõÃÕâêîôûÂÊÎÔÛçÇ";
        paramTitle.minFilter = Texture.TextureFilter.Nearest;
        paramTitle.magFilter = Texture.TextureFilter.Nearest;
        customFontTitle = generator.generateFont(paramTitle);

        FreeTypeFontParameter paramControls = new FreeTypeFontParameter();
        paramControls.size = 18;
        paramControls.color = Color.WHITE;
        paramControls.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "áéíóúÁÉÍÓÚãõÃÕâêîôûÂÊÎÔÛçÇ";
        paramControls.minFilter = Texture.TextureFilter.Nearest;
        paramControls.magFilter = Texture.TextureFilter.Nearest;
        controlsFont = generator.generateFont(paramControls);

        generator.dispose();

        // Animação dos vírus a partir do GIF de 4 frames
        virusAnimation = GifDecoder.loadGIFAnimation(
            Animation.PlayMode.LOOP,
            Gdx.files.internal("virus-inimigo.gif").read()
        );

        // Textura 1x1 simples para desenhar projéteis no SpriteBatch
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(Color.WHITE);
        pix.fill();
        pixelTexture = new Texture(pix);
        pix.dispose();

        // Textura radial suave para o efeito de brilho/glow
        Pixmap glowPix = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 32; y++) {
                float dst = (float) Math.sqrt((x - 16) * (x - 16) + (y - 16) * (y - 16)) / 16f;
                if (dst < 1f) {
                    float alpha = (1f - dst) * (1f - dst);
                    glowPix.setColor(1f, 1f, 1f, alpha);
                    glowPix.drawPixel(x, y);
                }
            }
        }
        glowTexture = new Texture(glowPix);
        glowPix.dispose();
    }

    private void configurarDimensoes() {
        // Posicionamento do monitor centralizado na resolução 1280x720
        compWidth = 720;
        compHeight = 720;
        compX = (1280 - compWidth) / 2f; // 280
        compY = 0;

        // Proporções exatas da telinha dentro da textura 256x256 do monitor
        float screenX = compX + compWidth * (28f / 256f);
        float screenWidth = compWidth * (200f / 256f);
        float screenY = compY + compHeight * (104f / 256f);
        float screenHeight = compHeight * (128f / 256f);

        screenBounds = new Rectangle(screenX, screenY, screenWidth, screenHeight);
    }

    public void reiniciar() {
        // Posiciona o player na base da telinha do monitor
        playerX = screenBounds.x + (screenBounds.width - playerWidth) / 2f;
        playerY = screenBounds.y + 20;
        playerLives = MAX_LIVES;
        invulnerabilityTimer = 0f;
        shootCooldown = 0f;
        isFinished = false;
        isVictory = false;

        bullets = new CustomLinkedList<>();
        viruses = new CustomLinkedList<>();

        // Spawna 5 vírus em posições e velocidades variadas
        float[][] pos = {
            {screenBounds.x + 40, screenBounds.y + screenBounds.height - 70, 130, -70},
            {screenBounds.x + 140, screenBounds.y + screenBounds.height - 110, -110, 80},
            {screenBounds.x + 250, screenBounds.y + screenBounds.height - 65, 140, -90},
            {screenBounds.x + 360, screenBounds.y + screenBounds.height - 105, -120, -75},
            {screenBounds.x + 460, screenBounds.y + screenBounds.height - 75, 135, 85}
        };

        for (int i = 0; i < pos.length; i++) {
            viruses.add(new Virus(pos[i][0], pos[i][1], pos[i][2], pos[i][3]));
        }
    }

    public void update(float delta) {
        stateTime += delta;

        // Tecla R reinicia a qualquer momento
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            reiniciar();
            return;
        }

        if (isFinished) return;

        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer -= delta;
        }

        if (shootCooldown > 0) {
            shootCooldown -= delta;
        }

        // 1. Movimento do jogador
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            playerX -= playerSpeed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            playerX += playerSpeed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            playerY += playerSpeed * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            playerY -= playerSpeed * delta;
        }

        // 2. Colisão com as bordas da tela do monitor (Clamping)
        playerX = MathUtils.clamp(playerX, screenBounds.x, screenBounds.x + screenBounds.width - playerWidth);
        playerY = MathUtils.clamp(playerY, screenBounds.y, screenBounds.y + screenBounds.height - playerHeight);

        // 3. Disparo do jogador (Espaço)
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && shootCooldown <= 0) {
            float bulletX = playerX + (playerWidth / 2f) - 3;
            float bulletY = playerY + playerHeight - 6;
            bullets.add(new Bullet(bulletX, bulletY));
            shootCooldown = 0.22f;
        }

        // 4. Atualizar Tiros e colisão com topo
        float bulletSpeed = 460f;
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            b.y += bulletSpeed * delta;

            if (b.y > screenBounds.y + screenBounds.height) {
                bullets.remove(b);
                i--;
            }
        }

        // 5. Atualizar Vírus e rebote nas paredes da telinha
        for (int i = 0; i < viruses.size(); i++) {
            Virus v = viruses.get(i);
            v.x += v.vx * delta;
            v.y += v.vy * delta;

            // Rebate horizontal
            if (v.x <= screenBounds.x) {
                v.x = screenBounds.x;
                v.vx = -v.vx;
            } else if (v.x + v.width >= screenBounds.x + screenBounds.width) {
                v.x = screenBounds.x + screenBounds.width - v.width;
                v.vx = -v.vx;
            }

            // Rebate vertical
            if (v.y <= screenBounds.y) {
                v.y = screenBounds.y;
                v.vy = -v.vy;
            } else if (v.y + v.height >= screenBounds.y + screenBounds.height) {
                v.y = screenBounds.y + screenBounds.height - v.height;
                v.vy = -v.vy;
            }
        }

        // 6. Colisão: Tiros vs Vírus
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            Rectangle bRect = b.getBounds();

            for (int j = 0; j < viruses.size(); j++) {
                Virus v = viruses.get(j);
                if (bRect.overlaps(v.getBounds())) {
                    v.hp--;
                    bullets.remove(b);
                    i--;

                    if (v.hp <= 0) {
                        viruses.remove(v);
                    }
                    break;
                }
            }
        }

        // 7. Colisão: Player vs Vírus
        Rectangle playerHitbox = new Rectangle(playerX + 8, playerY + 8, playerWidth - 16, playerHeight - 16);
        if (invulnerabilityTimer <= 0) {
            for (int i = 0; i < viruses.size(); i++) {
                Virus v = viruses.get(i);
                if (playerHitbox.overlaps(v.getBounds())) {
                    playerLives--;
                    invulnerabilityTimer = 1.3f;
                    if (playerLives <= 0) {
                        isFinished = true;
                        isVictory = false;
                    }
                    break;
                }
            }
        }

        // 8. Checagem de Vitória
        if (viruses.isEmpty()) {
            isFinished = true;
            isVictory = true;
        }
    }

    public void render(SpriteBatch batch, BitmapFont defaultFont) {
        batch.begin();

        // 1. Desenha a moldura e fundo do computador
        batch.setColor(Color.WHITE);
        batch.draw(computadorTexture, compX, compY, compWidth, compHeight);

        // --- EFEITOS DE BRILHO (GLOW) NA TELA DO COMPUTADOR ---
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE); // Modo aditivo

        // A. Brilho ambiente da tela CRT (leve pulso elétrico de fósforo)
        float pulse = 0.06f + 0.025f * MathUtils.sin(stateTime * 3.0f);
        batch.setColor(0.12f, 0.65f, 0.85f, pulse);
        batch.draw(pixelTexture, screenBounds.x, screenBounds.y, screenBounds.width, screenBounds.height);

        // B. Vazamento sutil de luz nas bordas plásticas internas do monitor
        batch.setColor(0.1f, 0.5f, 0.7f, 0.04f);
        batch.draw(pixelTexture, screenBounds.x - 3, screenBounds.y - 3, screenBounds.width + 6, screenBounds.height + 6);

        // C. Reflexo de curvatura no vidro do monitor (CRT Glass Specular Glare)
        batch.setColor(1f, 1f, 1f, 0.07f);
        batch.draw(pixelTexture, screenBounds.x + 8, screenBounds.y + screenBounds.height - 35, screenBounds.width - 16, 12);
        batch.setColor(1f, 1f, 1f, 0.035f);
        batch.draw(pixelTexture, screenBounds.x + 16, screenBounds.y + screenBounds.height - 52, screenBounds.width - 32, 10);

        // 2. Desenha os projéteis (tiros) com núcleo branco e halo neon brilhante
        batch.setColor(0f, 0.95f, 1f, 0.65f);
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            batch.draw(glowTexture, b.x - 7, b.y - 7, b.width + 14, b.height + 14);
        }
        batch.setColor(Color.WHITE);
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            batch.draw(pixelTexture, b.x, b.y, b.width, b.height);
        }

        // Restaura o modo normal de blend para os demais sprites
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // 3. Desenha os vírus com animação fluida
        batch.setColor(Color.WHITE);
        TextureRegion virusFrame = virusAnimation.getKeyFrame(stateTime, true);
        for (int i = 0; i < viruses.size(); i++) {
            Virus v = viruses.get(i);
            batch.draw(virusFrame, v.x, v.y, v.width, v.height);
        }

        // 4. Desenha o jogador (com efeito de piscar quando invulnerável)
        if (invulnerabilityTimer <= 0 || ((int)(stateTime * 10) % 2 == 0)) {
            batch.setColor(Color.WHITE);
            batch.draw(playerTexture, playerX, playerY, playerWidth, playerHeight);
        }

        // 5. Interface da telinha: Corações lado a lado (Full e Empty)
        float heartSize = 26f;
        float heartSpacing = 30f;
        float heartsStartX = screenBounds.x + 16f;
        float heartsY = screenBounds.y + screenBounds.height - 36f;

        batch.setColor(Color.WHITE);
        for (int i = 0; i < MAX_LIVES; i++) {
            Texture heartTex = (i < playerLives) ? heartFullTexture : heartEmptyTexture;
            batch.draw(heartTex, heartsStartX + (i * heartSpacing), heartsY, heartSize, heartSize);
        }

        // 6. Contador de Vírus com a fonte BoldPixels.ttf
        customFont.setColor(Color.WHITE);
        customFont.draw(batch, "VIRUS: " + viruses.size(), screenBounds.x + screenBounds.width - 120, screenBounds.y + screenBounds.height - 18);

        // 7. Mensagens de Vitória / Derrota perfeitamente centralizadas na tela do computador
        if (isFinished) {
            if (isVictory) {
                String title = "SISTEMA DESINFECTADO!";
                String subtitle = "Pressione [R] para reiniciar ou [ESC] para sair";

                glyphLayout.setText(customFontTitle, title);
                float titleX = screenBounds.x + (screenBounds.width - glyphLayout.width) / 2f;
                customFontTitle.setColor(Color.GREEN);
                customFontTitle.draw(batch, title, titleX, screenBounds.y + screenBounds.height / 2f + 25);

                glyphLayout.setText(customFont, subtitle);
                float subX = screenBounds.x + (screenBounds.width - glyphLayout.width) / 2f;
                customFont.setColor(Color.WHITE);
                customFont.draw(batch, subtitle, subX, screenBounds.y + screenBounds.height / 2f - 15);
            } else {
                String title = "INVASAO CRITICA DE VIRUS!";
                String subtitle = "Pressione [R] para tentar novamente ou [ESC] para sair";

                glyphLayout.setText(customFontTitle, title);
                float titleX = screenBounds.x + (screenBounds.width - glyphLayout.width) / 2f;
                customFontTitle.setColor(Color.RED);
                customFontTitle.draw(batch, title, titleX, screenBounds.y + screenBounds.height / 2f + 25);

                glyphLayout.setText(customFont, subtitle);
                float subX = screenBounds.x + (screenBounds.width - glyphLayout.width) / 2f;
                customFont.setColor(Color.WHITE);
                customFont.draw(batch, subtitle, subX, screenBounds.y + screenBounds.height / 2f - 15);
            }
        }

        // 8. Rodapé informativo com fonte maior, cor preta clareada e centralizado no computador
        String controlsText = "[WASD / Setas]: Mover   [ESPACO]: Atirar   [R]: Reiniciar   [ESC]: Sair";
        glyphLayout.setText(controlsFont, controlsText);
        float controlsX = compX + (compWidth - glyphLayout.width) / 2f;
        controlsFont.setColor(COLOR_CONTROLS_TEXT);
        controlsFont.draw(batch, controlsText, controlsX, screenBounds.y - 12);

        batch.setColor(Color.WHITE);
        batch.end();
    }

    public void dispose() {
        if (computadorTexture != null) computadorTexture.dispose();
        if (playerTexture != null) playerTexture.dispose();
        if (pixelTexture != null) pixelTexture.dispose();
        if (glowTexture != null) glowTexture.dispose();
        if (heartFullTexture != null) heartFullTexture.dispose();
        if (heartEmptyTexture != null) heartEmptyTexture.dispose();
        if (customFont != null) customFont.dispose();
        if (customFontTitle != null) customFontTitle.dispose();
        if (controlsFont != null) controlsFont.dispose();
    }
}
