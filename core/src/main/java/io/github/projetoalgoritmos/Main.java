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
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.projetoalgoritmos.controller.GameController;
import io.github.projetoalgoritmos.datastructures.CustomLinkedList;
import io.github.projetoalgoritmos.model.*;
import io.github.projetoalgoritmos.utils.GifDecoder;

public class Main extends ApplicationAdapter {
    private enum GameState { LOGIN, PLAYING, GAMEOVER, RANKING }
    private GameState currentState = GameState.LOGIN;
    private String playerName = "";

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Animation<TextureRegion> backgroundAnimation;
    private Animation<TextureRegion>[] robotAnimations;
    
    private Texture deathSheet;
    private Animation<TextureRegion> deathAnimation;
    
    private GameController gameController;
    private Robo activeRobo;
    private float stateTime;
    private float sessionTimer;
    private float arrivalTimer;
    private float gameOverTime;
    private int totalComponentsSubstituted = 0;
    
    private CustomLinkedList<RankingEntry> rankingList = new CustomLinkedList<>();
    private final String RANKING_FILE = "ranking.txt";
    
    private FrameBuffer fbo;
    private ShaderProgram crtShader;
    private TextureRegion fboRegion;

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

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                if (character >= 32 && character <= 126) {
                    inputBuffer += character;
                    return true;
                }
                return false;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.BACKSPACE && inputBuffer.length() > 0) {
                    inputBuffer = inputBuffer.substring(0, inputBuffer.length() - 1);
                    return true;
                }
                if (keycode == Input.Keys.ENTER) {
                    if (currentState == GameState.LOGIN) {
                        if (!inputBuffer.trim().isEmpty()) {
                            playerName = inputBuffer.trim();
                            inputBuffer = "";
                            currentState = GameState.PLAYING;
                        }
                    } else if (currentState == GameState.PLAYING) {
                        processCommand();
                    } else if (currentState == GameState.GAMEOVER || currentState == GameState.RANKING) {
                        if (currentState == GameState.GAMEOVER) {
                            currentState = GameState.LOGIN;
                            initNewSession();
                        } else {
                            currentState = GameState.PLAYING;
                        }
                    }
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE && currentState == GameState.RANKING) {
                    currentState = GameState.PLAYING;
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
        totalComponentsSubstituted = 0;
        arrivalTimer = 0;
        sessionTimer = 0;
        inputBuffer = "";
    }

    private void processCommand() {
        String input = inputBuffer.trim().toLowerCase();
        if (input.equals("/rank")) {
            currentState = GameState.RANKING;
            inputBuffer = "";
            return;
        }
        input = input.toUpperCase().replace(" ", "_");
        if (activeRobo != null && !activeRobo.getPilhaComponentes().isEmpty()) {
            String target = activeRobo.getPilhaComponentes().peek().getTipo().name();
            if (input.equals(target)) {
                activeRobo.removerComponente();
                totalComponentsSubstituted++;
                if (activeRobo.isConsertado()) {
                    gameController.processarConserto(activeRobo);
                    activeRobo = (gameController.getOficina().isEmpty()) ? null : gameController.getOficina().get(0);
                }
            }
        }
        inputBuffer = "";
    }

    private void spawnRobo() {
        int id = (int) (Math.random() * 900) + 100;
        String[] models = {"INDUSTRIAL_LIFTER", "SCOUT_DRONE", "LOGISTICS_UNIT", "BOMB_DISPOSAL", "CLAW_UNIT", "DEFENSE_BOT"};
        String[] prios = {"LOW RISK", "STANDARD", "EMERGENCY"};
        
        Robo r = new Robo(id, models[(int)(Math.random()*models.length)], prios[(int)(Math.random()*3)]);
        r.setSpriteType((int)(Math.random() * 7));
        
        int numComps = (int)(Math.random() * 3) + 1;
        boolean[] used = new boolean[TipoComponente.values().length];
        int added = 0;
        while (added < numComps) {
            int randomIndex = (int)(Math.random() * TipoComponente.values().length);
            if (!used[randomIndex]) {
                r.adicionarComponente(new Componente(TipoComponente.values()[randomIndex]));
                used[randomIndex] = true;
                added++;
            }
        }
        if (gameController.adicionarRobo(r)) {
            if (activeRobo == null) activeRobo = r;
        } else {
            rankingList.add(new RankingEntry(playerName, gameController.getTotalRobosConsertados(), totalComponentsSubstituted, sessionTimer));
            saveRanking();
            currentState = GameState.GAMEOVER;
            gameOverTime = 0;
        }
    }

    private void saveRanking() {
        FileHandle file = Gdx.files.local(RANKING_FILE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rankingList.size(); i++) {
            RankingEntry entry = rankingList.get(i);
            sb.append(entry.getName()).append(",")
              .append(entry.getRobotsRepaired()).append(",")
              .append(entry.getTotalComponents()).append(",")
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
    public void render() {
        fbo.begin();
        ScreenUtils.clear(COLOR_BG);
        float dt = Gdx.graphics.getDeltaTime();
        stateTime += dt;

        if (currentState == GameState.PLAYING) {
            sessionTimer += dt;
            arrivalTimer += dt;
            if (arrivalTimer >= 3.0f) {
                spawnRobo();
                arrivalTimer = 0;
            }
            
            if (Gdx.input.justTouched()) {
                float mx = Gdx.input.getX();
                float my = Gdx.graphics.getHeight() - Gdx.input.getY();
                if (mx >= 80 && mx <= 360) {
                    for (int i = 0; i < gameController.getOficina().size(); i++) {
                        float ry = 600 - (i * 75);
                        if (my >= ry && my <= ry + 70) {
                            activeRobo = gameController.getOficina().get(i);
                            break;
                        }
                    }
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
        }
        
        fbo.end();

        ScreenUtils.clear(0, 0, 0, 1);
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
            font.draw(batch, (i+1) + ". " + rankingList.get(i).toString(), 400, 500 - (i * 35));
        }
        
        font.setColor(COLOR_HAZARD);
        font.getData().setScale(0.8f);
        font.draw(batch, "PRESS ENTER OR ESC TO RETURN", 540, 180);
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
        font.draw(batch, "[ SYSTEM FAILURE: OFFICE FULL ]", 380, 450);
        
        font.getData().setScale(1.1f);
        font.setColor(Color.WHITE);
        font.draw(batch, "OPERATOR: " + playerName.toUpperCase(), 380, 400);
        
        font.setColor(COLOR_HAZARD);
        font.draw(batch, "ROBOTS REPAIRED: " + gameController.getTotalRobosConsertados(), 380, 360);
        font.draw(batch, "COMPONENTS REPLACED: " + totalComponentsSubstituted, 380, 330);
        font.draw(batch, "TOTAL SESSION TIME: " + (int)sessionTimer + "s", 380, 300);
        
        font.setColor(COLOR_CYAN);
        font.draw(batch, "[ SESSION RANKINGS ]", 380, 250);
        for (int i = 0; i < rankingList.size() && i < 5; i++) {
            font.draw(batch, (i+1) + ". " + rankingList.get(i).toString(), 400, 210 - (i * 30));
        }
        
        font.getData().setScale(0.8f);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "PRESS ENTER TO REBOOT TERMINAL", 540, 120);
        batch.end();
    }

    private void drawInterface() {
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
        font.getData().setScale(1.0f);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "TERMINAL_OS_v4.2  |  OPERATOR: " + playerName.toUpperCase(), 100, 700);
        font.draw(batch, "UPTIME: " + (int)sessionTimer + "s", 1050, 700);
        font.draw(batch, "[ REPAIR_QUEUE ]", 100, 640);
        
        for(int i=0; i<gameController.getOficina().size(); i++) {
            Robo r = gameController.getOficina().get(i);
            float y = 600 - (i * 75);
            batch.end();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            if (r == activeRobo) shapeRenderer.setColor(0.1f, 0.2f, 0.3f, 1f);
            else shapeRenderer.setColor(COLOR_BG);
            shapeRenderer.rect(85, y, 280, 70);
            if(r.getPrioridade().equals("EMERGENCY")) shapeRenderer.setColor(COLOR_EMERGENCY);
            else if(r.getPrioridade().equals("STANDARD")) shapeRenderer.setColor(COLOR_HAZARD);
            else shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1f);
            shapeRenderer.rect(355, y, 10, 70);
            shapeRenderer.end();
            batch.begin();
            font.getData().setScale(0.8f);
            font.draw(batch, "ID: " + r.getId(), 95, y + 60);
            font.draw(batch, r.getModelo(), 95, y + 40);
            font.draw(batch, r.getPrioridade(), 95, y + 20);
        }

        if(activeRobo != null) {
            Animation<TextureRegion> anim = robotAnimations[activeRobo.getSpriteType()];
            TextureRegion currentFrame = anim.getKeyFrame(stateTime, true);
            float drawW = 448, drawH = 320;
            if (activeRobo.getSpriteType() == 1) { drawW = 300; drawH = 360; }
            else if (activeRobo.getSpriteType() == 2) { drawW = 270; drawH = 215; }
            batch.draw(currentFrame, 700 - drawW/2, 240, drawW, drawH);
            
            font.getData().setScale(1.3f);
            font.draw(batch, "UNIT_" + activeRobo.getId() + ": STATUS_CRITICAL", 520, 640);
            font.getData().setScale(0.9f);
            font.setColor(COLOR_CYAN);
            font.draw(batch, "[ STACK_DIAGNOSTIC ]", 1020, 630);
            int stackSize = activeRobo.getPilhaComponentes().size();
            if (stackSize > 0) {
                batch.end();
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                shapeRenderer.setColor(new Color(0.0f, 0.1f, 0.3f, 1.0f));
                shapeRenderer.rect(1010, 550, 250, 50);
                shapeRenderer.end();
                batch.begin();
                font.setColor(Color.WHITE);
                font.draw(batch, "ACTIVE_TASK:", 1020, 590);
                font.draw(batch, "> " + activeRobo.getPilhaComponentes().peek().getTipo().name(), 1020, 570);
                font.setColor(COLOR_CYAN.cpy().mul(1,1,1,0.5f));
                font.draw(batch, "PENDING_QUEUE:", 1020, 520);
                for(int i=1; i<stackSize; i++) {
                    font.draw(batch, "- COMPONENT_OFFLINE_" + i, 1030, 500 - (i*25));
                }
            }
        }

        font.getData().setScale(1.8f);
        font.setColor(COLOR_CYAN);
        font.draw(batch, "> " + inputBuffer + "_", 430, 80);
        font.getData().setScale(0.9f);
        font.draw(batch, "INPUT_SEQUENCE_REQUIRED", 430, 115);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        for (Animation<TextureRegion> anim : robotAnimations) {
            for (TextureRegion frame : anim.getKeyFrames()) frame.getTexture().dispose();
        }
        deathSheet.dispose();
        fbo.dispose();
        crtShader.dispose();
        for (TextureRegion frame : backgroundAnimation.getKeyFrames()) {
            frame.getTexture().dispose();
        }
    }
}
