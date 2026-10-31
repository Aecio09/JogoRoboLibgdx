package io.github.projetoalgoritmos;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.projetoalgoritmos.controller.GameController;
import io.github.projetoalgoritmos.datastructures.CustomLinkedList;
import io.github.projetoalgoritmos.model.*;
import io.github.projetoalgoritmos.utils.GifDecoder;
import io.github.projetoalgoritmos.minigames.CircuitMinigame;
import io.github.projetoalgoritmos.minigames.VirusMinigame;

public class Main extends ApplicationAdapter {
    private enum GameState { LOGIN, PLAYING, MINIGAME_VIRUS, MINIGAME_CIRCUIT, GAMEOVER, RANKING }
    private GameState currentState = GameState.LOGIN;
    private VirusMinigame virusMinigame;
    private CircuitMinigame circuitMinigame;
    private String playerName = "";

    private static final float TEMPO_BASE_DIA = 90f;
    private static final float BONUS_POR_REPARO = 30f;
    private static final float DURACAO_PORTA_GARAGEM = 1.15f;
    private static final int TAMANHO_FONTE_INTERFACE = 20;
    private static final int ROBOS_BASE = 2;
    private static final int ROBOS_MAX_DIA = 6;
    private static final String[] MODELOS = {
        "INDUSTRIAL_LIFTER", "SCOUT_DRONE", "LOGISTICS_UNIT",
        "BOMB_DISPOSAL", "CLAW_UNIT", "DEFENSE_BOT"
    };
    private static final String[] PRIORIDADES = {"LOW RISK", "STANDARD", "EMERGENCY"};
    private static final float CARTA_LARGURA = 320f;
    private static final float CARTA_ALTURA = 230f;
    private static final float CARTA_ESPACO = 40f;
    private static final float CARTA_Y_BASE = 220f;

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Animation<TextureRegion> backgroundAnimation;
    private Animation<TextureRegion>[] robotAnimations;
    
    private Texture deathSheet;
    private Animation<TextureRegion> deathAnimation;
    
    private GameController gameController;
    private Robo activeRobo;
    private Robo roboEmReparo;
    private float stateTime;
    private float sessionTimer;
    private float gameOverTime;
    private boolean transicaoDia = false;
    private float transicaoTimer = 0f;
    private String notificacao = "";
    private float notificacaoTimer = 0f;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    // Power-ups: um slot por tipo de minigame e as duas cartas do fim do dia.
    private Powerup powerupVirus;
    private Powerup powerupCircuito;
    private Powerup[] cartasPowerup = new Powerup[2];
    private int cartaSelecionada = -1;
    
    private CustomLinkedList<RankingEntry> rankingList = new CustomLinkedList<>();
    // Novo formato: nome,robos,dias,tempo. O arquivo antigo media componentes.
    private final String RANKING_FILE = "ranking_dias.txt";
    
    private FrameBuffer fbo;
    private ShaderProgram crtShader;
    private TextureRegion fboRegion;

    // desenha o jogo sempre em 1280x720, ajustando ao tamanho real da tela
    private final FitViewport viewport = new FitViewport(1280, 720);
    private final Matrix4 fboProjection = new Matrix4().setToOrtho2D(0, 0, 1280, 720);
    private final Vector2 touchPoint = new Vector2();

    private final Color COLOR_BG = new Color(0.047f, 0.055f, 0.067f, 1f);
    private final Color COLOR_SURFACE = new Color(0.067f, 0.075f, 0.086f, 1f);
    private final Color COLOR_CYAN = new Color(0f, 0.941f, 1f, 1f);
    private final Color COLOR_HAZARD = new Color(0.996f, 0.718f, 0f, 1f);
    private final Color COLOR_EMERGENCY = new Color(0.757f, 0f, 0.078f, 1f);
    private final Color COLOR_ALERT_TEXT = new Color(1f, 0.42f, 0.32f, 1f);

    private String inputBuffer = "";

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = criarFonteInterface();
        
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, 1280, 720, false);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);

        ShaderProgram.pedantic = false;
        crtShader = new ShaderProgram(
            Gdx.files.internal("shaders/crt.vert"),
            Gdx.files.internal("shaders/crt.frag")
        );

        backgroundAnimation = GifDecoder.loadGIFAnimation(Animation.PlayMode.LOOP, Gdx.files.internal("Laboratory.gif").read());
        
        loadRobotAnimations();

        deathSheet = new Texture("death.png");
        TextureRegion[][] deathTmp = TextureRegion.split(deathSheet, 70, 64);
        TextureRegion[] deathFrames = new TextureRegion[8];
        for (int i = 0; i < 8; i++) deathFrames[i] = deathTmp[0][i];
        deathAnimation = new Animation<>(0.375f, deathFrames);
        
        loadRanking();
        initNewSession();
        virusMinigame = new VirusMinigame();
        circuitMinigame = new CircuitMinigame();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                if (currentState != GameState.LOGIN) return false;
                if (character >= 32 && character <= 126) {
                    inputBuffer += character;
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (currentState == GameState.LOGIN) {
                    if (keycode == Input.Keys.BACKSPACE && inputBuffer.length() > 0) {
                        inputBuffer = inputBuffer.substring(0, inputBuffer.length() - 1);
                        return true;
                    }
                    if (keycode == Input.Keys.ENTER) {
                        if (!inputBuffer.trim().isEmpty()) {
                            playerName = inputBuffer.trim();
                            inputBuffer = "";
                            currentState = GameState.PLAYING;
                            iniciarDia(1);
                        }
                        return true;
                    }
                    return false;
                }

                if (currentState == GameState.PLAYING) {
                    if (transicaoDia) {
                        if (transicaoTimer >= DURACAO_PORTA_GARAGEM) {
                            if (keycode == Input.Keys.NUM_1) {
                                selecionarCarta(0);
                                return true;
                            }
                            if (keycode == Input.Keys.NUM_2) {
                                selecionarCarta(1);
                                return true;
                            }
                            if (keycode == Input.Keys.SPACE && cartaSelecionada >= 0) {
                                aplicarCartaSelecionada();
                                int proximoDia = gameController.getDiaAtual() + 1;
                                transicaoDia = false;
                                transicaoTimer = 0f;
                                iniciarDia(proximoDia);
                            }
                        }
                        return true;
                    }
                    if (keycode == Input.Keys.ENTER) {
                        iniciarReparo();
                        return true;
                    }
                    if (keycode == Input.Keys.TAB) {
                        currentState = GameState.RANKING;
                        return true;
                    }
                    return false;
                }

                if (currentState == GameState.MINIGAME_VIRUS || currentState == GameState.MINIGAME_CIRCUIT) {
                    if (keycode == Input.Keys.ESCAPE) {
                        boolean resultadoPronto = currentState == GameState.MINIGAME_VIRUS
                            ? virusMinigame.isResultReady()
                            : circuitMinigame.isResultReady();
                        if (!resultadoPronto) {
                            cancelarReparo();
                        }
                        return true;
                    }
                    return false;
                }

                if (currentState == GameState.RANKING) {
                    if (keycode == Input.Keys.ENTER || keycode == Input.Keys.ESCAPE) {
                        currentState = GameState.PLAYING;
                        return true;
                    }
                    return false;
                }

                if (currentState == GameState.GAMEOVER && keycode == Input.Keys.ENTER) {
                    currentState = GameState.LOGIN;
                    initNewSession();
                    return true;
                }

                return false;
            }
        });
    }

    private BitmapFont criarFonteInterface() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal("Fonts/BoldPixels.ttf")
        );

        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.size = TAMANHO_FONTE_INTERFACE;
        param.color = Color.WHITE;
        param.characters = FreeTypeFontGenerator.DEFAULT_CHARS
            + "áéíóúÁÉÍÓÚãõÃÕâêîôûÂÊÎÔÛçÇ";
        param.minFilter = Texture.TextureFilter.Nearest;
        param.magFilter = Texture.TextureFilter.Nearest;

        BitmapFont fonte = generator.generateFont(param);
        generator.dispose();
        return fonte;
    }

    private void loadRobotAnimations() {
        robotAnimations = new Animation[7];
        
        Texture t0 = new Texture("Idle.png");
        TextureRegion[][] tmp0 = TextureRegion.split(t0, 112, 80);
        robotAnimations[0] = new Animation<>(0.15f, tmp0[0]);

        robotAnimations[1] = createAnimFromFolder("blockbot/blockbott", 4, 0.15f);
        robotAnimations[2] = createAnimFromFolder("bombbot/bomb", 4, 0.15f);
        robotAnimations[3] = createAnimFromFolder("clawbot1/botzpurple", 4, 0.15f);
        robotAnimations[4] = createAnimFromFolder("clawbot2/botzgreen", 4, 0.15f);
        robotAnimations[5] = createAnimFromFolder("gunbot1/botred", 4, 0.15f);
        robotAnimations[6] = createAnimFromFolder("gunbot2/botorange", 4, 0.15f);
    }

    private Animation<TextureRegion> createAnimFromFolder(String pathPrefix, int frames, float duration) {
        TextureRegion[] tr = new TextureRegion[frames];
        for(int i=0; i<frames; i++) {
            tr[i] = new TextureRegion(new Texture(pathPrefix + (i+1) + ".png"));
        }
        return new Animation<>(duration, tr);
    }

    private void initNewSession() {
        gameController = new GameController(8);
        activeRobo = null;
        roboEmReparo = null;
        transicaoDia = false;
        transicaoTimer = 0f;
        powerupVirus = null;
        powerupCircuito = null;
        cartasPowerup[0] = null;
        cartasPowerup[1] = null;
        cartaSelecionada = -1;
        notificacao = "";
        notificacaoTimer = 0f;
        sessionTimer = 0f;
        gameOverTime = 0f;
        inputBuffer = "";
    }

    private void iniciarDia(int dia) {
        int quantidade = robosDoDia(dia);
        float tempoInicial = TEMPO_BASE_DIA + (quantidade - ROBOS_BASE) * 15f;
        gameController.iniciarDia(dia, quantidade, tempoInicial);
        cartasPowerup[0] = null;
        cartasPowerup[1] = null;
        cartaSelecionada = -1;

        // Alterna os defeitos para o jogador conhecer os dois minigames.
        int offsetDefeito = MathUtils.random(TipoDefeito.values().length - 1);

        for (int i = 0; i < quantidade; i++) {
            int id = 100 + (dia - 1) * 10 + i + 1;
            String modelo = MODELOS[MathUtils.random(MODELOS.length - 1)];
            String prioridade = PRIORIDADES[MathUtils.random(PRIORIDADES.length - 1)];
            TipoDefeito defeito = TipoDefeito.values()[
                (offsetDefeito + i) % TipoDefeito.values().length
            ];

            Robo r = new Robo(id, modelo, prioridade, defeito);
            r.setSpriteType(MathUtils.random(6));
            gameController.adicionarRobo(r);
        }

        activeRobo = gameController.getOficina().isEmpty()
            ? null : gameController.getOficina().get(0);
        notificar("DIA " + dia + " INICIADO", 3f);
    }

    private void iniciarReparo() {
        if (activeRobo == null || transicaoDia) return;

        roboEmReparo = activeRobo;

        if (activeRobo.getDefeito() == TipoDefeito.VIRUS) {
            virusMinigame.configurarDificuldade(gameController.getDiaAtual());
            virusMinigame.aplicarPowerup(powerupVirus);
            virusMinigame.reiniciar();
            powerupVirus = null;
            currentState = GameState.MINIGAME_VIRUS;
        } else {
            circuitMinigame.configurarDificuldade(gameController.getDiaAtual());
            if (powerupCircuito != null
                && powerupCircuito.getEfeito() == Powerup.Efeito.TEMPO_EXTRA) {
                gameController.adicionarTempoBonus(10f);
                circuitMinigame.aplicarPowerup(null);
            } else {
                circuitMinigame.aplicarPowerup(powerupCircuito);
            }
            circuitMinigame.reiniciar();
            powerupCircuito = null;
            currentState = GameState.MINIGAME_CIRCUIT;
        }
    }

    private void cancelarReparo() {
        if (roboEmReparo != null) {
            activeRobo = roboEmReparo;
            roboEmReparo = null;
        }
        notificar("REPARO CANCELADO", 2f);
        currentState = GameState.PLAYING;
    }

    private void finalizarReparo(boolean vitoria) {
        Robo robo = roboEmReparo;
        roboEmReparo = null;

        if (vitoria && robo != null) {
            gameController.processarConserto(robo);
            gameController.adicionarTempoBonus(BONUS_POR_REPARO);
            notificar("REPARO CONCLUIDO  +" + (int) BONUS_POR_REPARO + "s", 2.5f);

            if (gameController.isDiaConcluido()) {
                gameController.concluirDia();
                transicaoDia = true;
                transicaoTimer = 0f;
                activeRobo = null;
                cartasPowerup = Powerup.sortearDuas();
                cartaSelecionada = -1;
            } else {
                activeRobo = gameController.getOficina().isEmpty()
                    ? null : gameController.getOficina().get(0);
            }
        } else {
            activeRobo = (robo != null) ? robo : activeRobo;
            notificar("REPARO FALHOU  TEMPO PERDIDO", 2.5f);
        }

        currentState = GameState.PLAYING;
    }

    private void notificar(String texto, float duracao) {
        notificacao = texto;
        notificacaoTimer = duracao;
    }

    private void triggerGameOver() {
        if (currentState == GameState.GAMEOVER) return;

        currentState = GameState.GAMEOVER;
        gameOverTime = 0f;
        rankingList.add(new RankingEntry(
            playerName,
            gameController.getTotalRobosConsertados(),
            gameController.getTotalDiasConcluidos(),
            sessionTimer
        ));
        saveRanking();
    }

    private String formatarTempo(float segundos) {
        int total = Math.max(0, (int) Math.ceil(segundos));
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private void saveRanking() {
        FileHandle file = Gdx.files.local(RANKING_FILE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rankingList.size(); i++) {
            RankingEntry entry = rankingList.get(i);
            sb.append(entry.getName()).append(",")
              .append(entry.getRobotsRepaired()).append(",")
              .append(entry.getDaysCompleted()).append(",")
              .append(entry.getTime()).append("\n");
        }
        file.writeString(sb.toString(), false);
    }

    private void loadRanking() {
        FileHandle file = Gdx.files.local(RANKING_FILE);
        if (file.exists()) {
            String[] lines = file.readString().split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    rankingList.add(new RankingEntry(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Float.parseFloat(parts[3])));
                }
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        fbo.begin();
        ScreenUtils.clear(COLOR_BG);
        batch.setProjectionMatrix(fboProjection);
        shapeRenderer.setProjectionMatrix(fboProjection);
        float dt = Gdx.graphics.getDeltaTime();
        stateTime += dt;

        if (notificacaoTimer > 0f) {
            notificacaoTimer -= dt;
        }

        if (currentState == GameState.PLAYING) {
            if (transicaoDia) {
                transicaoTimer += dt;
                if (transicaoTimer >= DURACAO_PORTA_GARAGEM && Gdx.input.justTouched()) {
                    touchPoint.set(Gdx.input.getX(), Gdx.input.getY());
                    viewport.unproject(touchPoint);
                    for (int i = 0; i < cartasPowerup.length; i++) {
                        if (cartaContem(i, touchPoint.x, touchPoint.y)) {
                            selecionarCarta(i);
                            break;
                        }
                    }
                }
            } else {
                sessionTimer += dt;
                gameController.atualizarTempo(dt);

                if (gameController.isTempoEsgotado()) {
                    triggerGameOver();
                } else if (Gdx.input.justTouched()) {
                    touchPoint.set(Gdx.input.getX(), Gdx.input.getY());
                    viewport.unproject(touchPoint);
                    float mx = touchPoint.x;
                    float my = touchPoint.y;

                    if (mx >= 80 && mx <= 360) {
                        for (int i = 0; i < gameController.getOficina().size(); i++) {
                            float ry = 570 - (i * 75);
                            if (my >= ry && my <= ry + 70) {
                                Robo selecionado = gameController.getOficina().get(i);
                                if (selecionado == activeRobo) {
                                    iniciarReparo();
                                } else {
                                    activeRobo = selecionado;
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } else if (currentState == GameState.MINIGAME_VIRUS || currentState == GameState.MINIGAME_CIRCUIT) {
            sessionTimer += dt;
            boolean emAnimacao = currentState == GameState.MINIGAME_VIRUS
                ? virusMinigame.isAnimating()
                : circuitMinigame.isAnimating();
            if (!emAnimacao) {
                gameController.atualizarTempo(dt);
                if (gameController.isTempoEsgotado()) {
                    triggerGameOver();
                }
            }
        } else if (currentState == GameState.GAMEOVER) {
            gameOverTime += dt;
        }

        batch.begin();
        batch.setColor(1, 1, 1, 0.2f);
        batch.draw(backgroundAnimation.getKeyFrame(stateTime), 0, 0, 1280, 720);
        batch.setColor(Color.WHITE);
        batch.end();

        if (currentState == GameState.LOGIN) {
            drawLoginScreen();
        } else if (currentState == GameState.PLAYING) {
            drawInterface();
        } else if (currentState == GameState.GAMEOVER) {
            drawGameOverScreen();
        } else if (currentState == GameState.RANKING) {
            drawRankingMenu();
        } else if (currentState == GameState.MINIGAME_VIRUS) {
            virusMinigame.update(dt);
            if (virusMinigame.isFinished()) {
                finalizarReparo(virusMinigame.isVictory());
                if (currentState == GameState.PLAYING) drawInterface();
            } else {
                virusMinigame.render(batch, font);
                if (!virusMinigame.isAnimating()) drawMinigameHud();
            }
        } else if (currentState == GameState.MINIGAME_CIRCUIT) {
            circuitMinigame.update(dt, viewport);
            if (circuitMinigame.isFinished()) {
                finalizarReparo(circuitMinigame.isVictory());
                if (currentState == GameState.PLAYING) drawInterface();
            } else {
                circuitMinigame.render(batch, font);
                if (!circuitMinigame.isAnimating()) drawMinigameHud();
            }
        }

        fbo.end();

        ScreenUtils.clear(0, 0, 0, 1);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.setShader(crtShader);
        batch.begin();
        crtShader.setUniformf("u_time", stateTime);
        batch.draw(fboRegion, 0, 0, 1280, 720);
        batch.end();
        batch.setShader(null);
    }

    private void drawLoginScreen() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(COLOR_SURFACE);
        shapeRenderer.rect(440, 300, 400, 200);
        shapeRenderer.setColor(COLOR_CYAN);
        shapeRenderer.rect(440, 498, 400, 2);
        shapeRenderer.end();

        batch.begin();
        font.getData().setScale(1.2f);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "[ SYSTEM ACCESS REQUIRED ]", 480, 470);
        font.getData().setScale(0.8f);
        font.draw(batch, "ENTER OPERATOR NAME:", 460, 420);
        font.getData().setScale(1.5f);
        font.draw(batch, "> " + inputBuffer + "_", 460, 380);
        font.getData().setScale(0.7f);
        font.draw(batch, "PRESS ENTER TO AUTHORIZE", 540, 320);
        font.setColor(Color.WHITE);
        font.draw(batch, "PROTOTIPO: REPAROS VIA MINIGAMES", 490, 265);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "DEFEITOS: VIRUS  |  CURTO-CIRCUITO", 485, 240);
        batch.end();
    }

    private void drawRankingMenu() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(COLOR_SURFACE);
        shapeRenderer.rect(340, 150, 600, 450);
        shapeRenderer.setColor(COLOR_CYAN);
        shapeRenderer.rect(340, 598, 600, 2);
        shapeRenderer.end();

        batch.begin();
        font.getData().setScale(1.8f);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "[ GLOBAL RANKING DATABASE ]", 380, 560);
        
        font.getData().setScale(1.0f);
        for (int i = 0; i < rankingList.size() && i < 10; i++) {
            font.setColor(Color.WHITE);
            font.draw(batch, (i+1) + ". " + rankingList.get(i).toString(), 400, 500 - (i * 32));
        }
        
        font.setColor(Color.WHITE);
        font.getData().setScale(0.8f);
        font.draw(batch, "PRESS ENTER OR ESC TO RETURN", 540, 170);
        batch.end();
    }

    private void drawGameOverScreen() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(COLOR_BG);
        shapeRenderer.rect(340, 100, 600, 520);
        shapeRenderer.setColor(COLOR_EMERGENCY);
        shapeRenderer.rect(340, 618, 600, 2);
        shapeRenderer.end();

        batch.begin();
        TextureRegion currentDeathFrame = deathAnimation.getKeyFrame(gameOverTime, false);
        batch.draw(currentDeathFrame, 540, 480, 210, 192);

        font.getData().setScale(2.0f);
        font.setColor(COLOR_ALERT_TEXT);
        font.draw(batch, "[ TEMPO ESGOTADO ]", 430, 450);
        
        font.getData().setScale(1.1f);
        font.setColor(Color.WHITE);
        font.draw(batch, "OPERATOR: " + playerName.toUpperCase(), 380, 400);
        
        font.setColor(Color.WHITE);
        font.draw(batch, "ROBOTS REPAIRED: " + gameController.getTotalRobosConsertados(), 380, 360);
        font.draw(batch, "DAYS COMPLETED: " + gameController.getTotalDiasConcluidos(), 380, 330);
        font.draw(batch, "DAY REACHED: " + gameController.getDiaAtual(), 380, 300);
        font.draw(batch, "TOTAL SESSION TIME: " + (int)sessionTimer + "s", 380, 270);
        
        font.setColor(COLOR_CYAN);
        font.draw(batch, "[ SESSION RANKINGS ]", 380, 240);
        for (int i = 0; i < rankingList.size() && i < 4; i++) {
            font.draw(batch, (i+1) + ". " + rankingList.get(i).toString(), 400, 205 - (i * 25));
        }
        
        font.getData().setScale(0.8f);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "PRESS ENTER TO REBOOT TERMINAL", 540, 110);
        batch.end();
    }

    private void drawMinigameHud() {
        float tempo = gameController.getTempoRestanteDia();
        Color corTempo = tempo <= 15f ? COLOR_ALERT_TEXT : COLOR_CYAN;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.78f);
        shapeRenderer.rect(18, 666, 330, 40);
        shapeRenderer.setColor(corTempo);
        shapeRenderer.rect(18, 666, 330, 2);
        shapeRenderer.end();

        batch.begin();
        font.getData().setScale(0.75f);
        font.setColor(corTempo);
        font.draw(batch, "DIA " + gameController.getDiaAtual()
            + "  |  TEMPO " + formatarTempo(tempo), 28, 694);
        font.setColor(Color.WHITE);
        font.draw(batch, "REPARO: UNIT_" + (roboEmReparo != null ? roboEmReparo.getId() : "---"), 28, 676);
        batch.end();
    }

    private void drawInterface() {
        int dia = gameController.getDiaAtual();
        int fila = gameController.getOficina().size();
        int restantes = gameController.getRobosRestantesNoDia();
        int reparados = gameController.getTotalRobosConsertados();
        float tempo = gameController.getTempoRestanteDia();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(COLOR_SURFACE);
        shapeRenderer.rect(0, 660, 1280, 60);
        shapeRenderer.setColor(COLOR_CYAN);
        shapeRenderer.rect(0, 660, 1280, 2);
        shapeRenderer.setColor(new Color(0.02f, 0.03f, 0.05f, 1f));
        shapeRenderer.rect(0, 0, 80, 660);
        shapeRenderer.setColor(COLOR_SURFACE);
        shapeRenderer.rect(80, 0, 320, 660);
        shapeRenderer.setColor(COLOR_SURFACE);
        shapeRenderer.rect(1000, 140, 270, 520);
        shapeRenderer.setColor(COLOR_CYAN);
        shapeRenderer.rect(1000, 658, 270, 2);
        shapeRenderer.setColor(COLOR_CYAN.cpy().mul(1,1,1,0.2f));
        shapeRenderer.rect(1000, 140, 2, 520);
        shapeRenderer.setColor(COLOR_SURFACE);
        shapeRenderer.rect(400, 0, 880, 140);
        shapeRenderer.setColor(COLOR_CYAN);
        shapeRenderer.rect(400, 138, 880, 2);
        shapeRenderer.setColor(COLOR_HAZARD);
        shapeRenderer.rect(400, 130, 880, 8);
        shapeRenderer.end();

        batch.begin();

        // Barra superior
        font.getData().setScale(0.9f);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "TERMINAL_OS_v4.2  |  OPERATOR: " + playerName.toUpperCase(), 25, 700);
        font.setColor(Color.WHITE);
        font.draw(batch, "DIA " + String.format("%02d", dia), 525, 700);
        font.setColor(tempo <= 15f ? COLOR_ALERT_TEXT : COLOR_CYAN);
        font.draw(batch, "TEMPO " + formatarTempo(tempo), 620, 700);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "FILA " + restantes + "  |  REPARADOS " + reparados, 810, 700);

        // Fila de robôs
        font.setColor(COLOR_CYAN);
        font.draw(batch, "[ REPAIR_QUEUE ]", 100, 648);

        for (int i = 0; i < fila; i++) {
            Robo r = gameController.getOficina().get(i);
            float y = 570 - (i * 75);

            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(r == activeRobo ? new Color(0.1f, 0.2f, 0.3f, 1f) : COLOR_BG);
            shapeRenderer.rect(85, y, 280, 70);
            if (r.getPrioridade().equals("EMERGENCY")) shapeRenderer.setColor(COLOR_EMERGENCY);
            else if (r.getPrioridade().equals("STANDARD")) shapeRenderer.setColor(COLOR_HAZARD);
            else shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1f);
            shapeRenderer.rect(355, y, 10, 70);
            shapeRenderer.end();
            batch.begin();

            font.getData().setScale(0.72f);
            font.setColor(Color.WHITE);
            font.draw(batch, "ID: " + r.getId() + "  " + r.getModelo(), 95, y + 58);
            font.setColor(r.getDefeito() == TipoDefeito.VIRUS ? COLOR_ALERT_TEXT : COLOR_CYAN);
            font.draw(batch, "DEFEITO: " + r.getDefeito().getNome(), 95, y + 38);
            font.setColor(COLOR_CYAN.cpy().mul(1,1,1,0.6f));
            font.draw(batch, "PRIORIDADE: " + r.getPrioridade(), 95, y + 18);
        }

        // Robô selecionado
        if (activeRobo != null) {
            Animation<TextureRegion> anim = robotAnimations[activeRobo.getSpriteType()];
            TextureRegion currentFrame = anim.getKeyFrame(stateTime, true);
            float drawW = 448, drawH = 320;
            if (activeRobo.getSpriteType() == 1) { drawW = 300; drawH = 360; }
            else if (activeRobo.getSpriteType() == 2) { drawW = 270; drawH = 215; }
            batch.draw(currentFrame, 700 - drawW / 2, 240, drawW, drawH);

            font.getData().setScale(1.15f);
            font.setColor(Color.WHITE);
            font.draw(batch, "UNIT_" + activeRobo.getId() + ": AGUARDANDO REPARO", 475, 640);

            font.getData().setScale(0.9f);
            font.setColor(COLOR_ALERT_TEXT);
            font.draw(batch, "DEFEITO: " + activeRobo.getDefeito().getNome(), 475, 605);
            font.setColor(COLOR_CYAN);
            font.draw(batch, activeRobo.getDefeito().getDescricao(), 475, 580);

            font.getData().setScale(1.0f);
            font.setColor(Color.WHITE);
            font.draw(batch, "[ ENTER ] INICIAR REPARO", 530, 205);
        } else {
            font.getData().setScale(1.1f);
            font.setColor(COLOR_CYAN);
            font.draw(batch, "SEM ROBOS NA FILA", 570, 380);
        }

        // Painel de detalhes
        font.getData().setScale(0.9f);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "[ DEFEITO ATUAL ]", 1020, 630);

        if (activeRobo != null) {
            font.getData().setScale(0.8f);
            font.setColor(COLOR_ALERT_TEXT);
            font.draw(batch, activeRobo.getDefeito().getNome(), 1020, 595);
            font.setColor(Color.WHITE);
            font.getData().setScale(0.72f);
            font.draw(batch, activeRobo.getDefeito().getDescricao(), 1020, 570);
            font.getData().setScale(0.8f);
            font.setColor(COLOR_CYAN);
            font.draw(batch, "MINIGAME:", 1020, 525);
            font.setColor(Color.WHITE);
            font.draw(batch, activeRobo.getDefeito() == TipoDefeito.VIRUS
                ? "COMBATE DE VIRUS" : "LABIRINTO DE CIRCUITOS", 1020, 505);
        }

        font.getData().setScale(0.8f);
        font.setColor(Color.WHITE);
        font.draw(batch, "BONUS POR REPARO: +" + (int) BONUS_POR_REPARO + "s", 1020, 440);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "BONUS NO DIA: +" + (int) gameController.getTempoBonusAcumulado() + "s", 1020, 415);
        font.setColor(Color.WHITE);
        font.draw(batch, "ROBOS RESTANTES: " + restantes, 1020, 370);
        font.draw(batch, "DIA ATUAL: " + dia, 1020, 345);
        font.getData().setScale(0.68f);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "POWER-UP VIRUS: "
            + (powerupVirus != null ? powerupVirus.getNomeCurto() : "---"), 1020, 310);
        font.draw(batch, "POWER-UP CIRC: "
            + (powerupCircuito != null ? powerupCircuito.getNomeCurto() : "---"), 1020, 290);

        // Rodapé
        font.getData().setScale(0.8f);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "[CLIQUE] Selecionar   [ENTER] Iniciar reparo   [TAB] Ranking", 430, 80);
        font.getData().setScale(0.7f);
        font.setColor(COLOR_ALERT_TEXT);
        font.draw(batch, "TEMPO ESGOTADO ENCERRA O TURNO", 430, 110);

        if (notificacaoTimer > 0f && !notificacao.isEmpty()) {
            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.85f);
            shapeRenderer.rect(440, 145, 420, 36);
            shapeRenderer.end();
            batch.begin();

            font.getData().setScale(0.85f);
            font.setColor(Color.WHITE);
            font.draw(batch, notificacao, 470, 170);
        }

        batch.end();
        if (transicaoDia) drawPortaGaragem();
    }

    private int robosDoDia(int dia) {
        return Math.min(ROBOS_BASE + (dia - 1), ROBOS_MAX_DIA);
    }

    private void drawPortaGaragem() {
        float progresso = MathUtils.clamp(transicaoTimer / DURACAO_PORTA_GARAGEM, 0f, 1f);
        float eased = 1f - (1f - progresso) * (1f - progresso);
        float alturaPorta = 720f * eased;
        float topoPorta = 720f - alturaPorta;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        int totalSlats = 24;
        float slatH = 720f / totalSlats;
        for (int i = 0; i < totalSlats; i++) {
            float slatY = 720f - (i + 1) * slatH;
            float y = Math.max(slatY, topoPorta);
            float h = slatY + slatH - y;
            if (h <= 0f) continue;

            if (i % 2 == 0) {
                shapeRenderer.setColor(0.12f, 0.14f, 0.16f, 1f);
            } else {
                shapeRenderer.setColor(0.16f, 0.18f, 0.21f, 1f);
            }
            shapeRenderer.rect(0, y, 1280, h);

            shapeRenderer.setColor(0.06f, 0.07f, 0.08f, 1f);
            shapeRenderer.rect(0, slatY, 1280, 2f);
            shapeRenderer.setColor(0.24f, 0.27f, 0.30f, 1f);
            shapeRenderer.rect(0, slatY + 2f, 1280, 1f);
        }

        shapeRenderer.setColor(0.07f, 0.08f, 0.09f, 1f);
        shapeRenderer.rect(320f, topoPorta, 3f, alturaPorta);
        shapeRenderer.rect(957f, topoPorta, 3f, alturaPorta);

        if (alturaPorta > 0f) {
            shapeRenderer.setColor(0.04f, 0.05f, 0.06f, 1f);
            shapeRenderer.rect(0, topoPorta, 1280, 14f);

            for (int x = 0; x < 1280; x += 40) {
                if ((x / 40) % 2 == 0) {
                    shapeRenderer.setColor(COLOR_HAZARD);
                } else {
                    shapeRenderer.setColor(0.09f, 0.10f, 0.11f, 1f);
                }
                shapeRenderer.rect(x, topoPorta + 3f, 20f, 8f);
            }

            shapeRenderer.setColor(COLOR_CYAN);
            shapeRenderer.rect(0, topoPorta + 14f, 1280, 2f);
            shapeRenderer.setColor(0.28f, 0.30f, 0.33f, 1f);
            shapeRenderer.rect(0, 716f, 1280, 4f);
        }

        shapeRenderer.end();

        if (progresso >= 1f) {
            int dia = gameController.getDiaAtual();

            batch.begin();
            font.getData().setScale(0.9f);
            font.setColor(Color.WHITE);
            desenharCentralizado("[ SETOR DE REPAROS FECHADO ]", 0, 665, 1280);

            font.getData().setScale(1.5f);
            font.setColor(COLOR_CYAN);
            desenharCentralizado("DIA " + dia + " CONCLUIDO", 0, 615, 1280);

            font.getData().setScale(0.88f);
            font.setColor(Color.WHITE);
            desenharCentralizado("ROBOS REPARADOS NO DIA: " + robosDoDia(dia), 0, 565, 1280);
            desenharCentralizado("BONUS DE TEMPO: +" + (int) gameController.getTempoBonusAcumulado() + "s", 0, 540, 1280);
            desenharCentralizado("PROXIMO DIA: " + (dia + 1), 0, 515, 1280);
            batch.end();

            drawCartasPowerup();
        }
    }

    private void selecionarCarta(int indice) {
        if (indice < 0 || indice >= cartasPowerup.length) return;
        if (cartasPowerup[indice] == null) return;
        cartaSelecionada = indice;
    }

    private void aplicarCartaSelecionada() {
        if (cartaSelecionada < 0 || cartaSelecionada >= cartasPowerup.length) return;
        Powerup escolhido = cartasPowerup[cartaSelecionada];
        if (escolhido == null) return;

        if (escolhido.getTipo() == Powerup.Tipo.VIRUS) {
            powerupVirus = escolhido;
        } else {
            powerupCircuito = escolhido;
        }
    }

    private boolean cartaContem(int indice, float x, float y) {
        if (indice < 0 || indice >= cartasPowerup.length || cartasPowerup[indice] == null) {
            return false;
        }
        float cx = posicaoCartaX(indice);
        float cy = posicaoCartaY(indice);
        return x >= cx && x <= cx + CARTA_LARGURA
            && y >= cy && y <= cy + CARTA_ALTURA;
    }

    private float posicaoCartaX(int indice) {
        float total = CARTA_LARGURA * 2f + CARTA_ESPACO;
        return (1280f - total) / 2f + indice * (CARTA_LARGURA + CARTA_ESPACO);
    }

    private float posicaoCartaY(int indice) {
        return CARTA_Y_BASE + MathUtils.sin(stateTime * 2.2f + indice * 1.7f) * 7f;
    }

    private void desenharCentralizado(String texto, float x, float y, float largura) {
        glyphLayout.setText(font, texto);
        font.draw(batch, texto, x + (largura - glyphLayout.width) / 2f, y);
    }

    private void drawCartasPowerup() {
        if (cartasPowerup[0] == null || cartasPowerup[1] == null) return;

        float[] alturas = new float[cartasPowerup.length];

        // Brilho pulsante e corpo das cartas.
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < cartasPowerup.length; i++) {
            Powerup carta = cartasPowerup[i];
            float x = posicaoCartaX(i);
            float y = posicaoCartaY(i);
            alturas[i] = y;

            Color cor = carta.getCor();
            boolean selecionada = i == cartaSelecionada;
            float pulso = 0.5f + 0.5f * MathUtils.sin(stateTime * 3.2f + i * 2.0f);

            shapeRenderer.setColor(cor.r, cor.g, cor.b,
                (selecionada ? 0.20f : 0.10f) + 0.10f * pulso);
            shapeRenderer.rect(x - 14, y - 14, CARTA_LARGURA + 28, CARTA_ALTURA + 28);
            shapeRenderer.setColor(cor.r, cor.g, cor.b, 0.04f + 0.05f * pulso);
            shapeRenderer.rect(x - 28, y - 28, CARTA_LARGURA + 56, CARTA_ALTURA + 56);

            shapeRenderer.setColor(0.035f, 0.045f, 0.055f, 0.97f);
            shapeRenderer.rect(x, y, CARTA_LARGURA, CARTA_ALTURA);

            shapeRenderer.setColor(cor);
            shapeRenderer.rect(x, y + CARTA_ALTURA - 4f, CARTA_LARGURA, 4f);

            float borda = selecionada ? 3f : 1.5f;
            shapeRenderer.setColor(cor.r, cor.g, cor.b, selecionada ? 1f : 0.65f);
            shapeRenderer.rect(x, y, CARTA_LARGURA, borda);
            shapeRenderer.rect(x, y + CARTA_ALTURA - borda, CARTA_LARGURA, borda);
            shapeRenderer.rect(x, y, borda, CARTA_ALTURA);
            shapeRenderer.rect(x + CARTA_LARGURA - borda, y, borda, CARTA_ALTURA);
        }
        shapeRenderer.end();

        batch.begin();
        for (int i = 0; i < cartasPowerup.length; i++) {
            Powerup carta = cartasPowerup[i];
            float x = posicaoCartaX(i);
            float y = alturas[i];
            Color cor = carta.getCor();
            boolean selecionada = i == cartaSelecionada;

            font.getData().setScale(0.58f);
            font.setColor(cor);
            desenharCentralizado("POWER-UP // " + carta.getTipo().name(), x, y + CARTA_ALTURA - 30f, CARTA_LARGURA);

            font.getData().setScale(0.9f);
            font.setColor(Color.WHITE);
            desenharCentralizado(carta.getNome(), x, y + CARTA_ALTURA - 66f, CARTA_LARGURA);

            font.getData().setScale(0.60f);
            font.setColor(0.78f, 0.86f, 0.90f, 1f);
            String[] descricao = carta.getDescricao();
            for (int linha = 0; linha < descricao.length; linha++) {
                desenharCentralizado(descricao[linha], x, y + CARTA_ALTURA - 112f - linha * 22f, CARTA_LARGURA);
            }

            if (selecionada) {
                font.getData().setScale(0.58f);
                font.setColor(cor);
                desenharCentralizado("[ SELECIONADA ]", x, y + 26f, CARTA_LARGURA);
            } else {
                font.getData().setScale(0.55f);
                font.setColor(0.50f, 0.60f, 0.65f, 1f);
                desenharCentralizado("[ " + (i + 1) + " ] ESCOLHER", x, y + 26f, CARTA_LARGURA);
            }
        }

        float blink = 0.55f + 0.45f * MathUtils.sin(stateTime * 4f);
        font.getData().setScale(0.78f);
        if (cartaSelecionada < 0) {
            font.setColor(COLOR_CYAN.r, COLOR_CYAN.g, COLOR_CYAN.b, Math.max(0.35f, blink));
            desenharCentralizado("ESCOLHA UMA CARTA  [1] [2]  OU CLIQUE", 0, 155, 1280);
        } else {
            font.setColor(COLOR_CYAN);
            desenharCentralizado("POWER-UP SELECIONADO: "
                + cartasPowerup[cartaSelecionada].getNome()
                + "  [ESPACO] CONTINUAR", 0, 155, 1280);
        }
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        for (int i = 0; i < robotAnimations.length; i++) {
            Animation<TextureRegion> anim = robotAnimations[i];
            Object[] frames = anim.getKeyFrames();
            for (Object frameObj : frames) {
                TextureRegion frame = (TextureRegion) frameObj;
                frame.getTexture().dispose();
            }
        }
        deathSheet.dispose();
        fbo.dispose();
        crtShader.dispose();
        Object[] backgroundFrames = backgroundAnimation.getKeyFrames();
        for (Object frameObj : backgroundFrames) {
            TextureRegion frame = (TextureRegion) frameObj;
            frame.getTexture().dispose();
        }
        if (virusMinigame != null) {
            virusMinigame.dispose();
        }
        if (circuitMinigame != null) {
            circuitMinigame.dispose();
        }
    }
}
