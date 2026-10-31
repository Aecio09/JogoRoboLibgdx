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
import io.github.projetoalgoritmos.model.Powerup;
import io.github.projetoalgoritmos.utils.GifDecoder;

/**
 * Minigame de combate aos vírus dentro do terminal do computador.
 */
public class VirusMinigame {

    // Representa um projétil disparado pelo jogador
    public static class Bullet {
        public float x, y;
        public float vx, vy;
        public float width = 6;
        public float height = 14;

        public Bullet(float x, float y) {
            this(x, y, 0f, 460f);
        }

        public Bullet(float x, float y, float vx, float vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
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
    private Powerup powerupAtivo;
    private boolean escudoAtivo = false;
    private float droneTempoRestante = 0f;
    private float droneTiroCooldown = 0f;
    private float droneAngulo = 0f;
    private float droneX, droneY;

    // Listas encadeadas customizadas acadêmicas (sem java.util)
    private CustomLinkedList<Bullet> bullets;
    private CustomLinkedList<Virus> viruses;

    // Controle de estado
    private float stateTime = 0f;
    private boolean resultReady = false;
    private boolean isVictory = false;
    private int dificuldade = 1;
    private final MinigameAnimation animation = new MinigameAnimation();

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

    public void configurarDificuldade(int nivel) {
        this.dificuldade = Math.max(1, nivel);
    }

    public void aplicarPowerup(Powerup powerup) {
        this.powerupAtivo = (powerup != null && powerup.getTipo() == Powerup.Tipo.VIRUS)
            ? powerup : null;
    }

    private boolean temEfeito(Powerup.Efeito efeito) {
        return powerupAtivo != null && powerupAtivo.getEfeito() == efeito;
    }

    public boolean isFinished() {
        return resultReady && animation.isOutroFinished();
    }

    public boolean isVictory() { return isVictory; }
    public boolean isResultReady() { return resultReady; }
    public boolean isIntroActive() { return animation.isIntroActive(); }
    public boolean isAnimating() { return animation.isAnimating(); }

    public void reiniciar() {
        // Posiciona o player na base da telinha do monitor
        playerX = screenBounds.x + (screenBounds.width - playerWidth) / 2f;
        playerY = screenBounds.y + 20;
        playerLives = MAX_LIVES;
        invulnerabilityTimer = 0f;
        shootCooldown = 0f;
        escudoAtivo = temEfeito(Powerup.Efeito.FIREWALL);
        droneTempoRestante = temEfeito(Powerup.Efeito.DRONE) ? 8f : 0f;
        droneTiroCooldown = 0f;
        droneAngulo = 0f;
        droneX = playerX + playerWidth / 2f;
        droneY = playerY + playerHeight / 2f;
        stateTime = 0f;
        resultReady = false;
        isVictory = false;
        animation.reiniciar();

        bullets = new CustomLinkedList<>();
        viruses = new CustomLinkedList<>();

        // A dificuldade aumenta a quantidade e a velocidade dos vírus.
        int quantidade = Math.min(2 + dificuldade, 8);
        float multiplicador = 1f + (dificuldade - 1) * 0.10f;

        for (int i = 0; i < quantidade; i++) {
            float x = screenBounds.x + 30 + MathUtils.random(screenBounds.width - 100);
            float y = screenBounds.y + screenBounds.height * 0.55f
                + MathUtils.random(screenBounds.height * 0.35f);
            float vx = MathUtils.random(70f, 130f) * (MathUtils.randomBoolean() ? 1 : -1) * multiplicador;
            float vy = MathUtils.random(50f, 100f) * (MathUtils.randomBoolean() ? 1 : -1) * multiplicador;
            viruses.add(new Virus(x, y, vx, vy));
        }

        if (temEfeito(Powerup.Efeito.CRIOGENIA)) {
            for (int i = 0; i < viruses.size(); i++) {
                Virus v = viruses.get(i);
                v.vx *= 0.55f;
                v.vy *= 0.55f;
            }
        }
    }

    public void update(float delta) {
        stateTime += delta;
        animation.update(delta);

        // Tecla R reinicia enquanto o resultado ainda não foi definido
        if (!resultReady && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            reiniciar();
            return;
        }

        if (resultReady) return;
        if (animation.isIntroActive()) return;

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

        // 2.1 Drone de backup: orbita o jogador e atira no vírus mais próximo
        if (droneTempoRestante > 0f) {
            droneTempoRestante -= delta;
            droneAngulo += delta * 2.2f;
            droneX = playerX + playerWidth / 2f + MathUtils.cos(droneAngulo) * 36f;
            droneY = playerY + playerHeight / 2f + MathUtils.sin(droneAngulo) * 22f;
            droneTiroCooldown -= delta;

            if (droneTiroCooldown <= 0f) {
                Virus alvo = encontrarVirusMaisProximo(droneX, droneY);
                if (alvo != null) {
                    float alvoX = alvo.x + alvo.width / 2f;
                    float alvoY = alvo.y + alvo.height / 2f;
                    float dx = alvoX - droneX;
                    float dy = alvoY - droneY;
                    float distancia = (float) Math.sqrt(dx * dx + dy * dy);

                    if (distancia > 0.01f) {
                        float velocidade = 380f;
                        bullets.add(new Bullet(
                            droneX, droneY,
                            dx / distancia * velocidade,
                            dy / distancia * velocidade
                        ));
                    }
                    droneTiroCooldown = 0.9f;
                } else {
                    droneTiroCooldown = 0.25f;
                }
            }
        }

        // 3. Disparo do jogador (Espaço)
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && shootCooldown <= 0) {
            float bulletX = playerX + (playerWidth / 2f) - 3;
            float bulletY = playerY + playerHeight - 6;
            float bulletSpeed = temEfeito(Powerup.Efeito.OVERCLOCK) ? 575f : 460f;

            if (temEfeito(Powerup.Efeito.LEQUE)) {
                bullets.add(new Bullet(bulletX, bulletY, -110f, bulletSpeed));
                bullets.add(new Bullet(bulletX, bulletY, 0f, bulletSpeed));
                bullets.add(new Bullet(bulletX, bulletY, 110f, bulletSpeed));
            } else {
                bullets.add(new Bullet(bulletX, bulletY, 0f, bulletSpeed));
            }

            shootCooldown = temEfeito(Powerup.Efeito.OVERCLOCK) ? 0.10f : 0.22f;
        }

        // 4. Atualizar Tiros e remover os que saíram da tela
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            b.x += b.vx * delta;
            b.y += b.vy * delta;

            if (b.y > screenBounds.y + screenBounds.height
                || b.y + b.height < screenBounds.y
                || b.x + b.width < screenBounds.x
                || b.x > screenBounds.x + screenBounds.width) {
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
                    invulnerabilityTimer = 1.3f;
                    if (escudoAtivo) {
                        escudoAtivo = false;
                        animation.shake(0.36f, 13f);
                    } else {
                        playerLives--;
                        animation.shake(playerLives <= 0 ? 0.65f : 0.36f,
                            playerLives <= 0 ? 22f : 13f);
                        if (playerLives <= 0) {
                            resultReady = true;
                            isVictory = false;
                            animation.startOutro();
                        }
                    }
                    break;
                }
            }
        }

        // 8. Checagem de Vitória
        if (!resultReady && viruses.isEmpty()) {
            resultReady = true;
            isVictory = true;
            animation.startOutro();
        }
    }

    private Virus encontrarVirusMaisProximo(float x, float y) {
        Virus maisProximo = null;
        float menorDistancia = Float.MAX_VALUE;

        for (int i = 0; i < viruses.size(); i++) {
            Virus v = viruses.get(i);
            float dx = (v.x + v.width / 2f) - x;
            float dy = (v.y + v.height / 2f) - y;
            float distancia = dx * dx + dy * dy;
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                maisProximo = v;
            }
        }

        return maisProximo;
    }

    public void render(SpriteBatch batch, BitmapFont defaultFont) {
        animation.apply(batch);
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
        if (escudoAtivo) {
            float pulsoEscudo = 0.35f + 0.20f * MathUtils.sin(stateTime * 6f);
            batch.setColor(0.20f, 0.70f, 1f, pulsoEscudo);
            batch.draw(glowTexture, playerX - 14, playerY - 14, playerWidth + 28, playerHeight + 28);
        }
        if (invulnerabilityTimer <= 0 || ((int)(stateTime * 10) % 2 == 0)) {
            batch.setColor(Color.WHITE);
            batch.draw(playerTexture, playerX, playerY, playerWidth, playerHeight);
        }

        // 4.1 Drone de backup
        if (droneTempoRestante > 0f) {
            batch.setColor(0.25f, 1f, 0.55f, 0.75f);
            batch.draw(glowTexture, droneX - 12, droneY - 12, 24, 24);
            batch.setColor(Color.WHITE);
            batch.draw(pixelTexture, droneX - 5, droneY - 5, 10, 10);
            batch.setColor(0.25f, 1f, 0.55f, 1f);
            batch.draw(pixelTexture, droneX - 3, droneY - 3, 6, 6);
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
        customFont.draw(batch, "NIVEL " + dificuldade + "  VIRUS: " + viruses.size(),
            screenBounds.x + screenBounds.width - 160, screenBounds.y + screenBounds.height - 18);

        // 7. Mensagens de Vitória / Derrota durante a saída do computador
        if (resultReady) {
            String title;
            Color titleColor;
            if (isVictory) {
                title = "SISTEMA DESINFECTADO!";
                titleColor = Color.GREEN;
            } else {
                title = "INVASAO CRITICA DE VIRUS!";
                titleColor = Color.RED;
            }
            String subtitle = "RETORNANDO AO TERMINAL...";

            glyphLayout.setText(customFontTitle, title);
            float titleX = screenBounds.x + (screenBounds.width - glyphLayout.width) / 2f;
            customFontTitle.setColor(titleColor);
            customFontTitle.draw(batch, title, titleX, screenBounds.y + screenBounds.height / 2f + 25);

            glyphLayout.setText(customFont, subtitle);
            float subX = screenBounds.x + (screenBounds.width - glyphLayout.width) / 2f;
            customFont.setColor(Color.WHITE);
            customFont.draw(batch, subtitle, subX, screenBounds.y + screenBounds.height / 2f - 15);
        }

        // 8. Rodapé informativo com fonte maior, cor preta clareada e centralizado no computador
        String controlsText = "[WASD / Setas]: Mover   [ESPACO]: Atirar   [R]: Reiniciar   [ESC]: Sair";
        glyphLayout.setText(controlsFont, controlsText);
        float controlsX = compX + (compWidth - glyphLayout.width) / 2f;
        controlsFont.setColor(COLOR_CONTROLS_TEXT);
        controlsFont.draw(batch, controlsText, controlsX, screenBounds.y - 12);

        batch.setColor(Color.WHITE);
        batch.end();
        animation.reset(batch);
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
