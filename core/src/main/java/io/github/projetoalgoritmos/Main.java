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
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
    private static final int ROBOS_BASE = 2;
    private static final int ROBOS_MAX_DIA = 6;
    private static final String[] MODELOS = {
        "INDUSTRIAL_LIFTER", "SCOUT_DRONE", "LOGISTICS_UNIT",
        "BOMB_DISPOSAL", "CLAW_UNIT", "DEFENSE_BOT"
    };
    private static final String[] PRIORIDADES = {"LOW RISK", "STANDARD", "EMERGENCY"};

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

    private String inputBuffer = "";

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        
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
                    if (transicaoDia) return true;
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
                        cancelarReparo();
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
        notificacao = "";
        notificacaoTimer = 0f;
        sessionTimer = 0f;
        gameOverTime = 0f;
        inputBuffer = "";
    }

    private void iniciarDia(int dia) {
        int quantidade = Math.min(ROBOS_BASE + (dia - 1), ROBOS_MAX_DIA);
        float tempoInicial = TEMPO_BASE_DIA + (quantidade - ROBOS_BASE) * 15f;
        gameController.iniciarDia(dia, quantidade, tempoInicial);

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
            virusMinigame.reiniciar();
            currentState = GameState.MINIGAME_VIRUS;
        } else {
            circuitMinigame.configurarDificuldade(gameController.getDiaAtual());
            circuitMinigame.reiniciar();
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
                transicaoTimer = 3f;
                activeRobo = null;
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
                transicaoTimer -= dt;
                if (transicaoTimer <= 0f) {
                    transicaoDia = false;
                    iniciarDia(gameController.getDiaAtual() + 1);
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
            gameController.atualizarTempo(dt);
            if (gameController.isTempoEsgotado()) {
                triggerGameOver();
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
                drawMinigameHud();
            }
        } else if (currentState == GameState.MINIGAME_CIRCUIT) {
            circuitMinigame.update(dt, viewport);
            if (circuitMinigame.isFinished()) {
                finalizarReparo(circuitMinigame.isVictory());
                if (currentState == GameState.PLAYING) drawInterface();
            } else {
                circuitMinigame.render(batch, font);
                drawMinigameHud();
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
        font.setColor(COLOR_HAZARD);
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
        
        font.setColor(COLOR_HAZARD);
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
        font.setColor(COLOR_EMERGENCY);
        font.draw(batch, "[ TEMPO ESGOTADO ]", 430, 450);
        
        font.getData().setScale(1.1f);
        font.setColor(Color.WHITE);
        font.draw(batch, "OPERATOR: " + playerName.toUpperCase(), 380, 400);
        
        font.setColor(COLOR_HAZARD);
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
        Color corTempo = tempo <= 15f ? COLOR_EMERGENCY : COLOR_CYAN;

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
        font.setColor(COLOR_HAZARD);
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
        font.setColor(COLOR_HAZARD);
        font.draw(batch, "DIA " + String.format("%02d", dia), 525, 700);
        font.setColor(tempo <= 15f ? COLOR_EMERGENCY : COLOR_CYAN);
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
            font.setColor(r.getDefeito() == TipoDefeito.VIRUS ? COLOR_HAZARD : COLOR_CYAN);
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
            font.setColor(COLOR_HAZARD);
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
            font.setColor(COLOR_HAZARD);
            font.draw(batch, activeRobo.getDefeito().getNome(), 1020, 595);
            font.setColor(Color.WHITE);
            font.draw(batch, activeRobo.getDefeito().getDescricao(), 1020, 570);
            font.setColor(COLOR_CYAN);
            font.draw(batch, "MINIGAME:", 1020, 525);
            font.setColor(Color.WHITE);
            font.draw(batch, activeRobo.getDefeito() == TipoDefeito.VIRUS
                ? "COMBATE DE VIRUS" : "LABIRINTO DE CIRCUITOS", 1020, 505);
        }

        font.getData().setScale(0.8f);
        font.setColor(COLOR_HAZARD);
        font.draw(batch, "BONUS POR REPARO: +" + (int) BONUS_POR_REPARO + "s", 1020, 440);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "BONUS NO DIA: +" + (int) gameController.getTempoBonusAcumulado() + "s", 1020, 415);
        font.setColor(Color.WHITE);
        font.draw(batch, "ROBOS RESTANTES: " + restantes, 1020, 370);
        font.draw(batch, "DIA ATUAL: " + dia, 1020, 345);

        // Rodapé
        font.getData().setScale(0.8f);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "[CLIQUE] Selecionar   [ENTER] Iniciar reparo   [TAB] Ranking", 430, 80);
        font.getData().setScale(0.7f);
        font.setColor(COLOR_HAZARD);
        font.draw(batch, "TEMPO ESGOTADO ENCERRA O TURNO", 430, 110);

        if (notificacaoTimer > 0f && !notificacao.isEmpty()) {
            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.85f);
            shapeRenderer.rect(440, 145, 420, 36);
            shapeRenderer.end();
            batch.begin();

            font.getData().setScale(0.85f);
            font.setColor(COLOR_HAZARD);
            font.draw(batch, notificacao, 470, 170);
        }

        if (transicaoDia) {
            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.02f, 0.03f, 0.05f, 0.94f);
            shapeRenderer.rect(390, 250, 500, 230);
            shapeRenderer.setColor(COLOR_CYAN);
            shapeRenderer.rect(390, 478, 500, 2);
            shapeRenderer.end();
            batch.begin();

            font.getData().setScale(1.6f);
            font.setColor(COLOR_CYAN);
            font.draw(batch, "DIA " + dia + " CONCLUIDO", 435, 440);
            font.getData().setScale(0.9f);
            font.setColor(Color.WHITE);
            font.draw(batch, "ROBOS REPARADOS: " + gameController.getTotalRobosConsertados(), 430, 395);
            font.draw(batch, "BONUS ACUMULADO: +" + (int) gameController.getTempoBonusAcumulado() + "s", 430, 365);
            font.setColor(COLOR_HAZARD);
            font.draw(batch, "PROXIMO DIA EM " + (int) Math.ceil(transicaoTimer) + "s", 430, 320);
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
