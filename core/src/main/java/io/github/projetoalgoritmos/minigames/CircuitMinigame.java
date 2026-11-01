package io.github.projetoalgoritmos.minigames;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.projetoalgoritmos.model.Powerup;

/**
 * Minigame de labirinto de circuitos (Pipe Puzzle).
 * O jogador rotaciona peças clicando nelas para conectar
 * a FONTE DE ENERGIA ao COMPONENTE alvo.
 */
public class CircuitMinigame {

    // Direções de conexão de cada peça
    private static final int UP    = 0;
    private static final int RIGHT = 1;
    private static final int DOWN  = 2;
    private static final int LEFT  = 3;

    // Tipos de peça do circuito
    public enum TipoPeca {
        VAZIO,        // Sem conexões
        RETA,         // 2 conexões opostas (reta)
        CURVA,        // 2 conexões adjacentes (curva 90°)
        T_PECA,       // 3 conexões (T)
        CRUZ,         // 4 conexões (cruzamento)
        FONTE,        // Ponto de partida da energia (fixo)
        DESTINO       // Componente alvo (fixo)
    }

    // Cada célula do tabuleiro
    public static class Celula {
        public TipoPeca tipo;
        public int rotacao;       // 0, 1, 2, 3 (em sentidos de 90°)
        public int rotacaoSolucao; // rotação correta antes do embaralhamento
        public boolean fixa;      // Se não pode ser rotacionada
        public boolean energizada; // Se a energia está fluindo por aqui

        public Celula(TipoPeca tipo, int rotacao, boolean fixa) {
            this.tipo = tipo;
            this.rotacao = rotacao;
            this.rotacaoSolucao = rotacao;
            this.fixa = fixa;
            this.energizada = false;
        }

        // Retorna true se esta peça tem conexão na direção informada
        public boolean temConexao(int direcao) {
            boolean[] base = getConexoesBase();
            // Aplica rotação: a direção real = (direcao - rotacao + 4) % 4
            int dirLocal = (direcao - rotacao + 4) % 4;
            return base[dirLocal];
        }

        // Conexões base (sem rotação) de cada tipo
        private boolean[] getConexoesBase() {
            switch (tipo) {
                case RETA:    return new boolean[]{true, false, true, false};   // UP, DOWN
                case CURVA:   return new boolean[]{true, true, false, false};   // UP, RIGHT
                case T_PECA:  return new boolean[]{true, true, true, false};    // UP, RIGHT, DOWN
                case CRUZ:    return new boolean[]{true, true, true, true};     // Todas
                case FONTE:   return new boolean[]{true, true, true, true};     // Todas
                case DESTINO: return new boolean[]{true, true, true, true};     // Todas
                default:      return new boolean[]{false, false, false, false};
            }
        }

        public void rotacionar() {
            if (!fixa) {
                rotacao = (rotacao + 1) % 4;
            }
        }
    }

    // Dimensões do tabuleiro (variam com a dificuldade)
    private int cols = 7;
    private int rows = 5;

    // Direções: UP, RIGHT, DOWN, LEFT
    // A linha 0 fica embaixo e cresce para cima (igual ao desenho da grade).
    private static final int[][] DIRS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    private static final int[] OPOSTOS = {DOWN, LEFT, UP, RIGHT};

    // Tipos usados no preenchimento aleatório (com pesos diferentes)
    private static final TipoPeca[] TIPOS_ALEATORIOS = {
        TipoPeca.RETA, TipoPeca.CURVA, TipoPeca.CURVA,
        TipoPeca.T_PECA, TipoPeca.CRUZ
    };

    // Tabuleiro
    private Celula[][] tabuleiro;
    private int fonteCol, fonteRow;
    private int destinoCol, destinoRow;

    // Caminho garantido da fonte até o destino
    private boolean[][] noCaminho;
    private boolean[][] visitado;
    private int[] caminhoR;
    private int[] caminhoC;
    private int caminhoLen;

    // Texturas
    private Texture computadorTexture;
    private Texture pixelTexture;

    // Fontes
    private BitmapFont customFont;
    private BitmapFont customFontTitle;
    private BitmapFont controlsFont;
    private GlyphLayout glyphLayout = new GlyphLayout();

    // Cores temáticas de PCB
    private static final Color COR_TRILHA       = new Color(0.72f, 0.58f, 0.20f, 1f); // Dourado das trilhas de cobre
    private static final Color COR_TRILHA_ENER  = new Color(0.10f, 1.0f, 0.45f, 1f);  // Verde neon quando energizada
    private static final Color COR_FUNDO_CELULA = new Color(0.14f, 0.42f, 0.30f, 1f); // Verde PCB escuro
    private static final Color COR_GRADE        = new Color(0.10f, 0.32f, 0.22f, 1f); // Linhas da grade
    private static final Color COR_FONTE        = new Color(1.0f, 0.85f, 0.0f, 1f);   // Amarelo energia
    private static final Color COR_DESTINO      = new Color(0.0f, 0.75f, 1.0f, 1f);   // Ciano componente
    private static final Color COR_FIXA         = new Color(0.55f, 0.45f, 0.15f, 1f); // Dourado escuro (peça soldada)
    private static final Color COLOR_CONTROLS   = new Color(0.22f, 0.22f, 0.22f, 1f);

    // Layout do computador
    private float compX, compY, compWidth, compHeight;
    private Rectangle screenBounds;

    // Layout da grade dentro da tela
    private float gridX, gridY, cellSize;

    // Estado
    private float stateTime = 0f;
    private boolean resultReady = false;
    private boolean isVictory = false;
    private final MinigameAnimation animation = new MinigameAnimation();

    // Power-up ativo neste reparo
    private Powerup powerupAtivo;
    private float diagnosticoTimer = 0f;
    private float multimetroTimer = 0f;
    private int precisaoClicks = 0;
    private int curingaCargas = 0;
    private boolean curingaArmado = false;

    public CircuitMinigame() {
        carregarRecursos();
        configurarDimensoes();
        reiniciar();
    }

    private void carregarRecursos() {
        computadorTexture = new Texture("computador-virus-connection-background.png");

        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(Color.WHITE);
        pix.fill();
        pixelTexture = new Texture(pix);
        pix.dispose();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Fonts/BoldPixels.ttf"));
        String chars = FreeTypeFontGenerator.DEFAULT_CHARS + "áéíóúÁÉÍÓÚãõÃÕâêîôûÂÊÎÔÛçÇ";

        FreeTypeFontParameter p1 = new FreeTypeFontParameter();
        p1.size = 14; p1.color = Color.WHITE; p1.characters = chars;
        p1.minFilter = Texture.TextureFilter.Nearest;
        p1.magFilter = Texture.TextureFilter.Nearest;
        customFont = generator.generateFont(p1);

        FreeTypeFontParameter p2 = new FreeTypeFontParameter();
        p2.size = 22; p2.color = Color.WHITE; p2.characters = chars;
        p2.minFilter = Texture.TextureFilter.Nearest;
        p2.magFilter = Texture.TextureFilter.Nearest;
        customFontTitle = generator.generateFont(p2);

        FreeTypeFontParameter p3 = new FreeTypeFontParameter();
        p3.size = 18; p3.color = Color.WHITE; p3.characters = chars;
        p3.minFilter = Texture.TextureFilter.Nearest;
        p3.magFilter = Texture.TextureFilter.Nearest;
        controlsFont = generator.generateFont(p3);

        generator.dispose();
    }

    private void configurarDimensoes() {
        compWidth = 720;
        compHeight = 720;
        compX = (1280 - compWidth) / 2f;
        compY = 0;

        float screenX = compX + compWidth * (28f / 256f);
        float screenWidth = compWidth * (200f / 256f);
        float screenY = compY + compHeight * (104f / 256f);
        float screenHeight = compHeight * (128f / 256f);
        screenBounds = new Rectangle(screenX, screenY, screenWidth, screenHeight);

        // Calcula tamanho das células para caber na tela com margem
        float margemH = 30f;
        float margemV = 40f;
        float areaW = screenBounds.width - margemH * 2;
        float areaH = screenBounds.height - margemV * 2;
        cellSize = Math.min(areaW / cols, areaH / rows);

        // Centraliza a grade na tela do monitor
        float gridTotalW = cellSize * cols;
        float gridTotalH = cellSize * rows;
        gridX = screenBounds.x + (screenBounds.width - gridTotalW) / 2f;
        gridY = screenBounds.y + (screenBounds.height - gridTotalH) / 2f;
    }

    public void configurarDificuldade(int nivel) {
        if (nivel <= 1) {
            rows = 5; cols = 7;
        } else if (nivel == 2) {
            rows = 5; cols = 8;
        } else if (nivel == 3) {
            rows = 6; cols = 8;
        } else if (nivel == 4) {
            rows = 6; cols = 9;
        } else {
            rows = 7; cols = 9;
        }
        configurarDimensoes();
    }

    public void aplicarPowerup(Powerup powerup) {
        this.powerupAtivo = (powerup != null && powerup.getTipo() == Powerup.Tipo.CIRCUITO)
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
        resultReady = false;
        isVictory = false;

        int tentativas = 0;
        do {
            gerarPuzzle();
            propagarEnergia();
        } while (isVictory && ++tentativas < 10);

        // Caso raríssimo: o tabuleiro já nasceu resolvido. Desalinha uma peça.
        for (int i = 0; i < 20 && isVictory; i++) {
            int r = MathUtils.random(rows - 1);
            int c = MathUtils.random(cols - 1);
            if (!tabuleiro[r][c].fixa) {
                tabuleiro[r][c].rotacionar();
                propagarEnergia();
            }
        }

        stateTime = 0f;
        resultReady = false;
        isVictory = false;
        diagnosticoTimer = temEfeito(Powerup.Efeito.DIAGNOSTICO) ? 8f : 0f;
        multimetroTimer = temEfeito(Powerup.Efeito.MULTIMETRO) ? 8f : 0f;
        precisaoClicks = temEfeito(Powerup.Efeito.PRECISAO) ? 3 : 0;
        curingaCargas = temEfeito(Powerup.Efeito.CURINGA) ? 1 : 0;
        curingaArmado = false;
        animation.reiniciar();
    }

    /**
     * Gera um puzzle sempre solucionável:
     * 1. Escolhe um caminho aleatório (DFS) da fonte até o destino
     * 2. Monta as peças desse caminho com conexões compatíveis
     * 3. Preenche o resto com peças aleatórias
     * 4. Embaralha as rotações de todas as peças móveis
     */
    private void gerarPuzzle() {
        tabuleiro = new Celula[rows][cols];
        noCaminho = new boolean[rows][cols];
        visitado = new boolean[rows][cols];
        caminhoR = new int[rows * cols];
        caminhoC = new int[rows * cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tabuleiro[r][c] = new Celula(TipoPeca.VAZIO, 0, false);
            }
        }

        // Fonte na esquerda, destino na direita
        fonteRow = rows / 2;
        fonteCol = 0;
        destinoRow = rows / 2;
        destinoCol = cols - 1;

        // Caminho aleatório garantido entre fonte e destino
        caminhoLen = 0;
        buscarCaminho(fonteRow, fonteCol);

        // Fallback de segurança (não deve acontecer em uma grade conectada)
        if (caminhoLen == 0) {
            for (int c = 0; c < cols; c++) {
                caminhoR[caminhoLen] = fonteRow;
                caminhoC[caminhoLen] = c;
                caminhoLen++;
            }
        }

        // Monta as peças do caminho com conexões garantidas
        for (int i = 0; i < caminhoLen; i++) {
            int r = caminhoR[i];
            int c = caminhoC[i];
            noCaminho[r][c] = true;

            if (i == 0) {
                tabuleiro[r][c] = new Celula(TipoPeca.FONTE, 0, true);
            } else if (i == caminhoLen - 1) {
                tabuleiro[r][c] = new Celula(TipoPeca.DESTINO, 0, true);
            } else {
                int entrada = direcaoEntre(r, c, caminhoR[i - 1], caminhoC[i - 1]);
                int saida = direcaoEntre(r, c, caminhoR[i + 1], caminhoC[i + 1]);
                tabuleiro[r][c] = criarPecaDoCaminho(entrada, saida);
            }
        }

        // Preenche o resto do tabuleiro com peças aleatórias
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!noCaminho[r][c]) {
                    tabuleiro[r][c] = criarPecaAleatoria();
                }
            }
        }

        // Guarda a rotação correta (antes de embaralhar) para a Chave de Precisão.
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tabuleiro[r][c].rotacaoSolucao = tabuleiro[r][c].rotacao;
            }
        }

        // Embaralha as rotações das peças móveis para o puzzle começar "bagunçado"
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!tabuleiro[r][c].fixa) {
                    tabuleiro[r][c].rotacao = MathUtils.random(3);
                }
            }
        }
    }

    // Busca em profundidade aleatória: encontra um caminho simples até o destino
    private boolean buscarCaminho(int r, int c) {
        visitado[r][c] = true;
        caminhoR[caminhoLen] = r;
        caminhoC[caminhoLen] = c;
        caminhoLen++;

        if (r == destinoRow && c == destinoCol) return true;

        int[] ordem = {UP, RIGHT, DOWN, LEFT};
        for (int i = ordem.length - 1; i > 0; i--) {
            int j = MathUtils.random(i);
            int tmp = ordem[i];
            ordem[i] = ordem[j];
            ordem[j] = tmp;
        }

        for (int d : ordem) {
            int nr = r + DIRS[d][0];
            int nc = c + DIRS[d][1];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visitado[nr][nc]) {
                if (buscarCaminho(nr, nc)) return true;
            }
        }

        caminhoLen--;
        return false;
    }

    private int direcaoEntre(int r, int c, int nr, int nc) {
        int dr = nr - r;
        int dc = nc - c;
        for (int d = 0; d < 4; d++) {
            if (DIRS[d][0] == dr && DIRS[d][1] == dc) return d;
        }
        return UP;
    }

    // Escolhe um tipo de peça que consiga ligar as duas direções do caminho
    private Celula criarPecaDoCaminho(int entrada, int saida) {
        boolean opostas = (entrada + 2) % 4 == saida;
        int sorteio = MathUtils.random(99);

        TipoPeca tipo;
        if (opostas) {
            if (sorteio < 65)      tipo = TipoPeca.RETA;
            else if (sorteio < 88) tipo = TipoPeca.T_PECA;
            else                   tipo = TipoPeca.CRUZ;
        } else {
            if (sorteio < 65)      tipo = TipoPeca.CURVA;
            else if (sorteio < 88) tipo = TipoPeca.T_PECA;
            else                   tipo = TipoPeca.CRUZ;
        }

        return new Celula(tipo, rotacaoQueConecta(tipo, entrada, saida), false);
    }

    // Procura uma rotação que faça a peça conectar as duas direções pedidas
    private int rotacaoQueConecta(TipoPeca tipo, int d1, int d2) {
        int[] validas = new int[4];
        int total = 0;

        for (int rot = 0; rot < 4; rot++) {
            Celula teste = new Celula(tipo, rot, false);
            if (teste.temConexao(d1) && teste.temConexao(d2)) {
                validas[total++] = rot;
            }
        }

        return total > 0 ? validas[MathUtils.random(total - 1)] : 0;
    }

    private Celula criarPecaAleatoria() {
        TipoPeca tipo = TIPOS_ALEATORIOS[MathUtils.random(TIPOS_ALEATORIOS.length - 1)];
        return new Celula(tipo, MathUtils.random(3), false);
    }

    /**
     * Propaga energia a partir da fonte usando busca em largura (BFS manual).
     * Uma célula recebe energia se tem conexão mútua com um vizinho energizado.
     */
    private void propagarEnergia() {
        // Limpa energização
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tabuleiro[r][c].energizada = false;
            }
        }

        // BFS simples sem java.util (usa arrays como fila circular)
        int[] filaR = new int[rows * cols];
        int[] filaC = new int[rows * cols];
        int inicio = 0, fim = 0;

        tabuleiro[fonteRow][fonteCol].energizada = true;
        filaR[fim] = fonteRow;
        filaC[fim] = fonteCol;
        fim++;

        int[][] dirs = DIRS;
        int[] opostos = OPOSTOS;

        while (inicio < fim) {
            int r = filaR[inicio];
            int c = filaC[inicio];
            inicio++;

            for (int d = 0; d < 4; d++) {
                int nr = r + dirs[d][0];
                int nc = c + dirs[d][1];

                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !tabuleiro[nr][nc].energizada) {
                    // Conexão mútua: esta célula conecta na direção d E a vizinha conecta na direção oposta
                    if (tabuleiro[r][c].temConexao(d) && tabuleiro[nr][nc].temConexao(opostos[d])) {
                        tabuleiro[nr][nc].energizada = true;
                        filaR[fim] = nr;
                        filaC[fim] = nc;
                        fim++;
                    }
                }
            }
        }

        // Verifica vitória
        isVictory = tabuleiro[destinoRow][destinoCol].energizada;
    }

    private final Vector2 tempTouch = new Vector2();

    public void update(float delta) {
        update(delta, null);
    }

    public void update(float delta, Viewport viewport) {
        stateTime += delta;
        animation.update(delta);

        if (diagnosticoTimer > 0f) {
            diagnosticoTimer = Math.max(0f, diagnosticoTimer - delta);
        }
        if (multimetroTimer > 0f) {
            multimetroTimer = Math.max(0f, multimetroTimer - delta);
        }

        if (!resultReady && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            reiniciar();
            return;
        }

        if (resultReady) return;
        if (animation.isIntroActive()) return;

        if (curingaCargas > 0 && Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            curingaArmado = !curingaArmado;
        }

        // Clique do mouse para rotacionar peças
        if (Gdx.input.justTouched()) {
            float mx, my;
            if (viewport != null) {
                tempTouch.set(Gdx.input.getX(), Gdx.input.getY());
                viewport.unproject(tempTouch);
                mx = tempTouch.x;
                my = tempTouch.y;
            } else {
                mx = Gdx.input.getX() * 1280f / Gdx.graphics.getWidth();
                my = 720f - (Gdx.input.getY() * 720f / Gdx.graphics.getHeight());
            }

            // Verifica se tocou no botão de Curinga (Mobile)
            if (curingaCargas > 0 && my <= screenBounds.y - 14 && my >= screenBounds.y - 48
                && mx >= compX + 100 && mx <= compX + compWidth - 100) {
                curingaArmado = !curingaArmado;
                animation.shake(0.12f, 3f);
            }
            // Verifica se está dentro da grade
            else if (mx >= gridX && mx < gridX + cellSize * cols &&
                my >= gridY && my < gridY + cellSize * rows) {

                int col = (int) ((mx - gridX) / cellSize);
                int row = (int) ((my - gridY) / cellSize);

                if (col >= 0 && col < cols && row >= 0 && row < rows) {
                    Celula cel = tabuleiro[row][col];
                    if (!cel.fixa) {
                        if (curingaArmado && curingaCargas > 0) {
                            cel.tipo = TipoPeca.CRUZ;
                            cel.rotacao = 0;
                            curingaCargas--;
                            curingaArmado = false;
                            animation.shake(0.20f, 6f);
                        } else if (precisaoClicks > 0 && noCaminho[row][col]) {
                            if (cel.rotacao != cel.rotacaoSolucao) {
                                cel.rotacao = cel.rotacaoSolucao;
                                precisaoClicks--;
                            }
                            animation.shake(0.16f, 4f);
                        } else {
                            cel.rotacionar();
                            animation.shake(0.16f, 4f);
                        }

                        propagarEnergia();
                        if (isVictory) {
                            resultReady = true;
                            animation.startOutro();
                        }
                    }
                }
            }
        }
    }

    private int[] encontrarFalha() {
        int[] fallback = null;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!noCaminho[r][c] || tabuleiro[r][c].energizada) continue;
                if (fallback == null) {
                    fallback = new int[]{r, c};
                }

                for (int d = 0; d < 4; d++) {
                    int nr = r + DIRS[d][0];
                    int nc = c + DIRS[d][1];
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                        && tabuleiro[nr][nc].energizada) {
                        return new int[]{r, c};
                    }
                }
            }
        }

        return fallback;
    }

    public void render(SpriteBatch batch, BitmapFont defaultFont) {
        animation.apply(batch);
        batch.begin();

        // 1. Desenha o monitor
        batch.setColor(Color.WHITE);
        batch.draw(computadorTexture, compX, compY, compWidth, compHeight);

        // 2. Desenha o fundo das células da grade
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float cx = gridX + c * cellSize;
                float cy = gridY + r * cellSize;

                // Fundo da célula
                batch.setColor(COR_FUNDO_CELULA);
                batch.draw(pixelTexture, cx + 1, cy + 1, cellSize - 2, cellSize - 2);
            }
        }

        // 3. Desenha as trilhas/conexões de cada peça
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Celula cel = tabuleiro[r][c];
                if (cel.tipo == TipoPeca.VAZIO) continue;

                float cx = gridX + c * cellSize;
                float cy = gridY + r * cellSize;
                float meio = cellSize / 2f;
                float espessura = cellSize * 0.18f;

                Color corTrilha = cel.energizada ? COR_TRILHA_ENER : COR_TRILHA;

                if (cel.tipo == TipoPeca.FONTE) {
                    corTrilha = cel.energizada ? COR_TRILHA_ENER : COR_FONTE;
                } else if (cel.tipo == TipoPeca.DESTINO) {
                    corTrilha = cel.energizada ? COR_TRILHA_ENER : COR_DESTINO;
                }

                batch.setColor(corTrilha);

                // Centro da peça (nó de solda)
                float nodeSize = espessura * 1.4f;
                batch.draw(pixelTexture, cx + meio - nodeSize / 2, cy + meio - nodeSize / 2, nodeSize, nodeSize);

                // Trilhas para cada direção conectada
                if (cel.temConexao(UP)) {
                    batch.draw(pixelTexture, cx + meio - espessura / 2, cy + meio, espessura, meio);
                }
                if (cel.temConexao(DOWN)) {
                    batch.draw(pixelTexture, cx + meio - espessura / 2, cy, espessura, meio);
                }
                if (cel.temConexao(RIGHT)) {
                    batch.draw(pixelTexture, cx + meio, cy + meio - espessura / 2, meio, espessura);
                }
                if (cel.temConexao(LEFT)) {
                    batch.draw(pixelTexture, cx, cy + meio - espessura / 2, meio, espessura);
                }

                // Indicadores visuais para FONTE e DESTINO
                if (cel.tipo == TipoPeca.FONTE) {
                    float iconSize = cellSize * 0.35f;
                    batch.setColor(COR_FONTE);
                    // Símbolo de raio (simplificado como quadrado brilhante)
                    batch.draw(pixelTexture, cx + meio - iconSize / 2, cy + meio - iconSize / 2, iconSize, iconSize);
                } else if (cel.tipo == TipoPeca.DESTINO) {
                    float iconSize = cellSize * 0.3f;
                    batch.setColor(COR_DESTINO);
                    batch.draw(pixelTexture, cx + meio - iconSize / 2, cy + meio - iconSize / 2, iconSize, iconSize);
                }
            }
        }

        // 4. Linhas da grade por cima
        batch.setColor(COR_GRADE);
        for (int r = 0; r <= rows; r++) {
            batch.draw(pixelTexture, gridX, gridY + r * cellSize, cellSize * cols, 1);
        }
        for (int c = 0; c <= cols; c++) {
            batch.draw(pixelTexture, gridX + c * cellSize, gridY, 1, cellSize * rows);
        }

        // 5. Glow aditivo nas peças energizadas
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Celula cel = tabuleiro[r][c];
                if (cel.energizada && cel.tipo != TipoPeca.VAZIO) {
                    float cx = gridX + c * cellSize;
                    float cy = gridY + r * cellSize;
                    float pulse = 0.08f + 0.04f * MathUtils.sin(stateTime * 4f);
                    batch.setColor(0.1f, 1f, 0.5f, pulse);
                    batch.draw(pixelTexture, cx + 2, cy + 2, cellSize - 4, cellSize - 4);
                }
            }
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // 5.1 Efeitos dos power-ups de informação
        if (diagnosticoTimer > 0f) {
            float alpha = 0.45f + 0.25f * MathUtils.sin(stateTime * 6f);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(0.10f, 0.90f, 1f, alpha);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (!noCaminho[r][c]) continue;
                    float cx = gridX + c * cellSize;
                    float cy = gridY + r * cellSize;
                    float borda = 2f;
                    batch.draw(pixelTexture, cx, cy, cellSize, borda);
                    batch.draw(pixelTexture, cx, cy + cellSize - borda, cellSize, borda);
                    batch.draw(pixelTexture, cx, cy, borda, cellSize);
                    batch.draw(pixelTexture, cx + cellSize - borda, cy, borda, cellSize);
                }
            }
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        if (multimetroTimer > 0f) {
            int[] falha = encontrarFalha();
            if (falha != null) {
                float cx = gridX + falha[1] * cellSize;
                float cy = gridY + falha[0] * cellSize;
                float pulso = 0.55f + 0.45f * MathUtils.sin(stateTime * 9f);
                float borda = 3f;
                float meio = cellSize / 2f;

                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                batch.setColor(1f, 0.30f, 0.20f, pulso);
                batch.draw(pixelTexture, cx, cy, cellSize, borda);
                batch.draw(pixelTexture, cx, cy + cellSize - borda, cellSize, borda);
                batch.draw(pixelTexture, cx, cy, borda, cellSize);
                batch.draw(pixelTexture, cx + cellSize - borda, cy, borda, cellSize);
                batch.draw(pixelTexture, cx + meio - 1.5f, cy + 6, 3, cellSize - 12);
                batch.draw(pixelTexture, cx + 6, cy + meio - 1.5f, cellSize - 12, 3);
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }

        if (curingaArmado) {
            float pulso = 0.45f + 0.35f * MathUtils.sin(stateTime * 8f);
            float borda = 3f;
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(1f, 0.45f, 0.95f, pulso);
            batch.draw(pixelTexture, gridX, gridY, cellSize * cols, borda);
            batch.draw(pixelTexture, gridX, gridY + cellSize * rows - borda, cellSize * cols, borda);
            batch.draw(pixelTexture, gridX, gridY, borda, cellSize * rows);
            batch.draw(pixelTexture, gridX + cellSize * cols - borda, gridY, borda, cellSize * rows);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        batch.setColor(Color.WHITE);

        // 6. Labels FONTE e CHIP
        customFont.setColor(Color.WHITE);
        glyphLayout.setText(customFont, "FONTE");
        customFont.draw(batch, "FONTE", gridX - glyphLayout.width - 12, gridY + fonteRow * cellSize + cellSize / 2f + 5);

        customFont.setColor(COR_DESTINO);
        glyphLayout.setText(customFont, "CHIP");
        customFont.draw(batch, "CHIP", gridX + cols * cellSize + 12, gridY + destinoRow * cellSize + cellSize / 2f + 5);

        // 7. Mensagem de vitória durante a saída do computador
        if (resultReady && isVictory) {
            String title = "CIRCUITO CONECTADO!";
            String subtitle = "RETORNANDO AO TERMINAL...";

            glyphLayout.setText(customFontTitle, title);
            float titleX = screenBounds.x + (screenBounds.width - glyphLayout.width) / 2f;
            customFontTitle.setColor(COR_TRILHA_ENER);
            customFontTitle.draw(batch, title, titleX, screenBounds.y + screenBounds.height - 14);

            glyphLayout.setText(customFont, subtitle);
            float subX = screenBounds.x + (screenBounds.width - glyphLayout.width) / 2f;
            customFont.setColor(Color.WHITE);
            customFont.draw(batch, subtitle, subX, screenBounds.y + 18);
        }

        // 8. Rodapé com controles
        String controlsText = "[TOQUE / CLIQUE]: Rotacionar peca";
        glyphLayout.setText(controlsFont, controlsText);
        float controlsX = compX + (compWidth - glyphLayout.width) / 2f;
        controlsFont.setColor(COLOR_CONTROLS);
        controlsFont.draw(batch, controlsText, controlsX, screenBounds.y - 12);

        String powerupText = "";
        if (curingaCargas > 0) {
            powerupText = curingaArmado
                ? "CURINGA ARMADO: TOQUE UMA PECA"
                : "CHIP CURINGA: [TOQUE AQUI OU 'C' PARA ARMAR]";
        } else if (precisaoClicks > 0) {
            powerupText = "CHAVE DE PRECISAO: " + precisaoClicks + " CLIQUES RESTANTES";
        }
        if (!powerupText.isEmpty()) {
            glyphLayout.setText(customFont, powerupText);
            float powerupX = compX + (compWidth - glyphLayout.width) / 2f;

            if (curingaCargas > 0) {
                float boxW = glyphLayout.width + 20f;
                float boxH = 24f;
                float boxX = powerupX - 10f;
                float boxY = screenBounds.y - 48f;
                batch.setColor(curingaArmado ? new Color(1f, 0.45f, 0.95f, 0.25f) : new Color(0.1f, 0.35f, 0.25f, 0.45f));
                batch.draw(pixelTexture, boxX, boxY, boxW, boxH);
                batch.setColor(curingaArmado ? new Color(1f, 0.45f, 0.95f, 0.85f) : COR_TRILHA_ENER);
                batch.draw(pixelTexture, boxX, boxY, boxW, 1.5f);
                batch.draw(pixelTexture, boxX, boxY + boxH - 1.5f, boxW, 1.5f);
                batch.draw(pixelTexture, boxX, boxY, 1.5f, boxH);
                batch.draw(pixelTexture, boxX + boxW - 1.5f, boxY, 1.5f, boxH);
            }

            customFont.setColor(curingaArmado ? new Color(1f, 0.55f, 0.95f, 1f) : COR_TRILHA_ENER);
            customFont.draw(batch, powerupText, powerupX, screenBounds.y - 31);
        }

        batch.setColor(Color.WHITE);
        batch.end();
        animation.reset(batch);
    }

    public void dispose() {
        if (computadorTexture != null) computadorTexture.dispose();
        if (pixelTexture != null) pixelTexture.dispose();
        if (customFont != null) customFont.dispose();
        if (customFontTitle != null) customFontTitle.dispose();
        if (controlsFont != null) controlsFont.dispose();
    }
}
