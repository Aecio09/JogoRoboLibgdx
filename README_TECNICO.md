# 🤖 Robo Repair Shop — Documentação Técnica

Documento técnico do **ProjetoAlgoritimos**, um jogo 2D feito com **Java 21 + LibGDX** no qual o jogador repara robôs em minigames antes que o tempo diário acabe.

O projeto também atende aos requisitos da disciplina de **Algoritmos e Estruturas de Dados**, utilizando estruturas lineares implementadas manualmente para a fila de robôs, o ranking e as entidades dos minigames.

---

## 1. Visão geral

O jogo é dividido em dias. Cada dia possui:

- uma fila de robôs;
- um cronômetro;
- dois tipos de defeito;
- dois minigames;
- cartas de power-up ao final do dia.

O jogador avança enquanto conseguir reparar robôs e manter o cronômetro acima de zero. O jogo termina quando o tempo diário chega a zero, momento em que a sessão é registrada no ranking local.

---

## 2. Stack e requisitos

| Item | Versão/configuração |
| --- | --- |
| Java | 21 |
| LibGDX | 1.14.0 |
| Backend desktop | LWJGL3 3.4.1 |
| Build | Gradle Wrapper |
| Android | `compileSdk 36`, `targetSdk 36`, `minSdk 21` |
| Fonte principal | `BoldPixels.ttf` via FreeType |
| Resolução virtual | `1280x720` com `FitViewport` |

O projeto está dividido em:

- `core`: lógica compartilhada, telas, minigames e modelos.
- `lwjgl3`: launcher desktop.
- `android`: launcher e empacotamento Android.
- `assets`: imagens, fontes, shaders, GIFs e arquivos locais.

---

## 3. Organização do código

| Pacote/arquivo | Responsabilidade |
| --- | --- |
| `io.github.projetoalgoritmos.Main` | Loop principal, estados da aplicação, telas, progressão, cartas de power-up, ranking e composição da UI. |
| `controller.GameController` | Fila de robôs, tempo restante, bônus, contadores de robôs/dias e conclusão do dia. |
| `model.Robo` | Dados de um robô: id, modelo, prioridade, sprite e defeito. |
| `model.TipoDefeito` | Enum com `VIRUS` e `CURTO_CIRCUITO`, cada um associado a um minigame. |
| `model.RankingEntry` | Entrada persistida do ranking: nome, robôs, dias e tempo. |
| `model.Powerup` | Catálogo, tipos, efeitos, descrições e sorteio das cartas. |
| `minigames.VirusMinigame` | Combate de vírus, vidas, tiros, colisões e efeitos dos power-ups. |
| `minigames.CircuitMinigame` | Geração do circuito, rotação de peças, BFS de energia e efeitos dos power-ups. |
| `minigames.MinigameAnimation` | Entrada/saída animada do computador e tremor de tela. |
| `datastructures.CustomLinkedList` | Lista simplesmente encadeada usada em filas, ranking e entidades dos minigames. |
| `datastructures.CustomStack` | Pilha manual disponível no projeto, mas não utilizada no fluxo atual de gameplay. |
| `datastructures.Node` | Nó genérico compartilhado pelas estruturas encadeadas. |
| `utils.GifDecoder` | Decodificação dos GIFs `Laboratory.gif` e `virus-inimigo.gif`. |

---

## 4. Fluxo de estados

Os estados são definidos no enum interno `Main.GameState`:

```text
LOGIN ── Enter ──► PLAYING
                     │
                     ├── Enter/clique no robô ──► MINIGAME_VIRUS
                     │                                  │
                     │                                  └── fim ──► PLAYING
                     │
                     ├── Enter/clique no robô ──► MINIGAME_CIRCUIT
                     │                                  │
                     │                                  └── fim ──► PLAYING
                     │
                     ├── Tab ──► RANKING ── Enter/Esc ──► PLAYING
                     │
                     └── tempo = 0 ──► GAMEOVER ── Enter ──► LOGIN
```

Ao concluir todos os robôs do dia, o estado continua em `PLAYING`, mas com `transicaoDia = true`. Nesse período a porta da garagem fecha, o cronômetro é pausado e as cartas de power-up são exibidas.

---

## 5. Progressão, tempo e dificuldade

### 5.1 Quantidade de robôs

A quantidade de robôs do dia é calculada em `Main.robosDoDia`:

```text
robos(dia) = min(2 + (dia - 1), 6)
```

Ou seja:

| Dia | Robôs |
| --- | --- |
| 1 | 2 |
| 2 | 3 |
| 3 | 4 |
| 4 | 5 |
| 5 em diante | 6 |

### 5.2 Tempo

- Tempo base: **90 segundos**.
- Bônus inicial por quantidade: **+15 segundos** para cada robô acima de 2.
- Cada reparo bem-sucedido concede **+30 segundos**.
- O tempo é descontado durante o painel do dia e durante os minigames.
- Durante as animações de entrada/saída dos minigames, o cronômetro diário fica pausado.
- O jogo termina quando o tempo chega a zero.

Fórmula do tempo inicial:

```text
tempoInicial = 90 + (robos - 2) * 15
```

### 5.3 Progresso do dia

- Um reparo com vitória remove o robô da fila e decrementa `robosRestantesNoDia`.
- Um reparo com derrota mantém o robô na fila.
- Ao reparar todos os robôs, `concluirDia()` incrementa o total de dias concluídos.
- Não há limite de dias: a sessão segue até o tempo acabar.
- A oficina tem capacidade para 8 robôs, embora cada dia gere no máximo 6.

### 5.4 Dificuldade dos minigames

A dificuldade é configurada com o número do dia atual:

- **Vírus:** quantidade de inimigos e velocidade aumentam com o dia.
- **Circuito:** o tabuleiro cresce com o dia.

---

## 6. Minigames

## 6.1 Combate de Vírus

Arquivo: `core/src/main/java/io/github/projetoalgoritmos/minigames/VirusMinigame.java`

O jogador controla um pequeno robô dentro da tela de um computador e deve eliminar todos os vírus.

### Regras

- O jogador começa com **3 vidas**.
- Ao encostar em um vírus, perde 1 vida e recebe invulnerabilidade temporária de **1,3s**.
- Se as vidas chegarem a zero, o reparo falha.
- Se todos os vírus forem eliminados, o reparo é concluído.
- Os vírus ricocheteiam nas bordas da área jogável.
- O jogador pode se mover com `WASD` ou setas e atirar com `Espaço`.
- `R` reinicia o desafio; `Esc` cancela o reparo antes do resultado.

### Dificuldade

```text
quantidadeDeVirus = min(2 + dificuldade, 8)
multiplicadorDeVelocidade = 1 + (dificuldade - 1) * 0.10
```

A dificuldade recebida é o dia atual.

### Implementação

- Jogador, projéteis, vidas e vírus são atualizados a cada frame.
- Os projéteis e vírus são armazenados em `CustomLinkedList`.
- A colisão usa `Rectangle.overlaps`.
- O tiro padrão tem velocidade 460 e cooldown de 0,22s.
- A animação de entrada/saída e o tremor de tela são fornecidos por `MinigameAnimation`.

## 6.2 Labirinto de Circuitos

Arquivo: `core/src/main/java/io/github/projetoalgoritmos/minigames/CircuitMinigame.java`

O jogador deve conectar a fonte de energia ao chip girando peças de circuito.

### Peças

| Tipo | Conexões |
| --- | --- |
| `RETA` | Duas conexões opostas. |
| `CURVA` | Duas conexões adjacentes. |
| `T_PECA` | Três conexões. |
| `CRUZ` | Quatro conexões. |
| `FONTE` | Ponto inicial fixo. |
| `DESTINO` | Chip fixo. |
| `VAZIO` | Sem conexões. |

Cada peça possui uma rotação de `0` a `3`, aplicada em incrementos de 90 graus.

### Tamanho do tabuleiro

| Dia | Grade |
| --- | --- |
| 1 | `5x7` |
| 2 | `5x8` |
| 3 | `6x8` |
| 4 | `6x9` |
| 5 em diante | `7x9` |

### Geração do puzzle

1. A fonte fica no meio da borda esquerda.
2. O destino fica no meio da borda direita.
3. Uma busca em profundidade aleatória (DFS) encontra um caminho simples entre os dois.
4. As peças do caminho são criadas com conexões compatíveis.
5. O restante do tabuleiro é preenchido com peças aleatórias.
6. A rotação correta de cada peça é armazenada em `rotacaoSolucao`.
7. As rotações das peças móveis são embaralhadas.

Isso garante que o puzzle sempre tenha solução.

### Propagação de energia

- A energia começa na fonte.
- Uma BFS percorre o tabuleiro usando arrays de fila.
- Uma célula só é energizada se houver conexão mútua com um vizinho energizado.
- O jogador vence quando a célula de destino é energizada.

### Controles

- Clique em uma peça não fixa para girar 90 graus.
- `R` reinicia o puzzle.
- `Esc` cancela o reparo.
- `C` arma o Chip Curinga, quando esse power-up está ativo.

---

## 7. Power-ups

Arquivo: `core/src/main/java/io/github/projetoalgoritmos/model/Powerup.java`

### 7.1 Catálogo

O catálogo é estático e contém 10 power-ups, divididos em dois tipos:

- `Powerup.Tipo.VIRUS`
- `Powerup.Tipo.CIRCUITO`

`Powerup.sortearDuas()` escolhe dois índices diferentes do catálogo. As duas cartas são sempre power-ups distintos, mas podem pertencer ao mesmo minigame.

### 7.2 Escolha e armazenamento

- As cartas só aparecem quando a animação da porta da garagem termina.
- O jogador escolhe com clique, `1` ou `2`.
- `Espaço` só avança depois de uma seleção.
- `Main` mantém um slot para vírus e um para circuito.
- Um novo power-up do mesmo tipo substitui o anterior.
- O power-up é consumido no início do reparo correspondente, mesmo que o jogador falhe ou cancele depois.
- Power-ups guardados são perdidos ao iniciar uma nova sessão.

### 7.3 Efeitos

| Efeito | Tipo | Implementação |
| --- | --- | --- |
| `LEQUE` | Vírus | Dispara 3 projéteis com velocidades horizontais `-110`, `0` e `110`. |
| `OVERCLOCK` | Vírus | Aumenta a velocidade do tiro para 575 e reduz o cooldown para 0,10s. |
| `FIREWALL` | Vírus | Escudo que absorve 1 hit e concede 1,3s de invulnerabilidade. |
| `CRIOGENIA` | Vírus | Multiplica `vx` e `vy` dos vírus por 0,55. |
| `DRONE` | Vírus | Drone orbita o jogador por 8s e atira automaticamente a cada 0,9s. |
| `DIAGNOSTICO` | Circuito | Destaca as células do caminho correto por 8s. |
| `MULTIMETRO` | Circuito | Marca por 8s uma célula não energizada adjacente à fronteira de energia. |
| `PRECISAO` | Circuito | 3 cliques em células do caminho aplicam `rotacaoSolucao`. |
| `CURINGA` | Circuito | 1 carga; `C` arma e o próximo clique transforma a peça em `CRUZ`. |
| `TEMPO_EXTRA` | Circuito | Adiciona 10s ao cronômetro no início do próximo reparo de circuito. |

### 7.4 Apresentação das cartas

- Tamanho: `320x230`.
- Duas cartas centralizadas na tela.
- Flutuação vertical suave com variação de aproximadamente 7 pixels.
- Brilho pulsante desenhado com camadas de `ShapeRenderer`.
- Cores de destaque próprias para cada power-up.
- Nome, tipo, descrição e atalho de seleção são desenhados com `BoldPixels.ttf`.

---

## 8. Estruturas de dados

### 8.1 `CustomLinkedList<T>`

Lista simplesmente encadeada implementada manualmente.

Operações disponíveis:

- `add(T)`
- `remove(T)`
- `get(int)`
- `clear()`
- `size()`
- `isEmpty()`

Usos atuais:

- fila de robôs em `GameController`;
- lista de entradas do ranking em `Main`;
- projéteis em `VirusMinigame`;
- vírus em `VirusMinigame`.

As operações de busca, inserção no fim e remoção são lineares, coerentes com uma lista encadeada sem índice.

### 8.2 `CustomStack<T>`

Pilha com `push`, `pop`, `peek`, `isEmpty` e `size`.

A classe está implementada no pacote de estruturas, mas **não é utilizada pelo fluxo atual do jogo**. A versão anterior da documentação descrevia uma etapa de digitação de componentes e uso de pilha que não existe mais no gameplay.

### 8.3 Arrays e BFS

O `CircuitMinigame` usa arrays de inteiros como fila circular para a BFS de propagação de energia e matrizes para o tabuleiro.

### 8.4 Exceção ao “sem `java.util`”

A lógica do jogo usa estruturas próprias e arrays. A única exceção é o `GifDecoder`, que usa `java.util.Vector` internamente para a decodificação dos GIFs.

---

## 9. Renderização e interface

- Resolução lógica fixa de `1280x720` com `FitViewport`.
- Renderização do jogo em um `FrameBuffer`, seguida de um passe com shader CRT.
- Efeitos do shader:
  - curvatura de lente;
  - scanlines;
  - aberração cromática;
  - flicker.
- `SpriteBatch` para sprites, GIFs e textos.
- `ShapeRenderer` para painéis, barras, glows e cartas.
- `FreeTypeFontGenerator` com `BoldPixels.ttf`.
- Fonte da interface principal: tamanho 20.
- Fontes dos minigames: 14, 18 e 22, mantidas nos tamanhos atuais.
- `GifDecoder` processa `Laboratory.gif` e `virus-inimigo.gif` frame a frame.
- `MinigameAnimation` controla:
  - entrada do computador: 0,9s;
  - saída: 0,8s;
  - tremor de tela em colisões e ações.

Estados visuais principais:

- Login do operador.
- Painel da oficina com fila, robô selecionado e detalhes.
- HUD do minigame com dia, tempo e unidade em reparo.
- Ranking global.
- Game over com estatísticas da sessão.
- Tela de fim de dia com a porta fechada e as cartas.

---

## 10. Ranking e persistência

Arquivo local:

```text
ranking_dias.txt
```

Formato por linha:

```text
nome,robosConsertados,diasConcluidos,tempoTotal
```

Comportamento:

- O ranking é carregado na inicialização.
- Uma nova entrada é salva quando o tempo chega a zero.
- O menu de ranking exibe até 10 entradas.
- A tela de game over exibe até 4 entradas.
- As entradas são exibidas na ordem em que foram inseridas; não há ordenação por pontuação.

No desktop, ao executar via Gradle, o diretório de trabalho é `assets/`, então o arquivo é criado nessa pasta. Em outras distribuições, vale o diretório local da aplicação.

O arquivo `assets/ranking.txt` é legado e não é lido pelo fluxo atual.

---

## 11. Assets principais

| Asset | Uso |
| --- | --- |
| `Laboratory.gif` | Background animado do jogo. |
| `Idle.png` e pastas `blockbot`, `bombbot`, `clawbot*`, `gunbot*` | Animações dos robôs. |
| `death.png` | Animação de game over. |
| `computador-virus-arena-background.png` | Cenário do minigame de vírus. |
| `computador-virus-connection-background.png` | Cenário do minigame de circuito. |
| `player-virus-minigame.png` | Jogador do minigame de vírus. |
| `virus-inimigo.gif` | Animação dos vírus. |
| `heart-Full.png` / `heart-empty.png` | Indicadores de vida. |
| `Fonts/BoldPixels.ttf` | Fonte principal. |
| `shaders/crt.vert` / `crt.frag` | Pós-processamento CRT. |

Arquivos como `assets/components/`, `assets/guns.zip` e `assets/ranking.txt` são legados e não são referenciados pelo código atual.

---

## 12. Build e execução

### Desktop

```sh
./gradlew :lwjgl3:run
```

### JAR desktop

```sh
./gradlew :lwjgl3:jar
```

Saída:

```text
lwjgl3/build/libs/ProjetoAlgoritimos-1.0.0.jar
```

### Android

```sh
./gradlew :android:assembleDebug
```

APK:

```text
android/build/outputs/apk/debug/android-debug.apk
```

Instalação:

```sh
adb install -r android/build/outputs/apk/debug/android-debug.apk
```

Instalação e execução em um dispositivo/emulador conectado:

```sh
./gradlew :android:run
```

### Verificação de compilação

```sh
./gradlew core:compileJava lwjgl3:classes
```

Em ambientes com pouca memória, pode ser usado:

```sh
./gradlew core:compileJava lwjgl3:classes --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx512m
```

---

## 13. Limitações e observações

- O protótipo não utiliza áudio; o launcher desktop desativa a inicialização de OpenAL.
- A versão Android está configurada para build, mas os minigames foram desenhados para teclado e mouse. O controle por toque ainda não cobre todo o fluxo.
- A prioridade do robô (`LOW RISK`, `STANDARD`, `EMERGENCY`) é usada apenas como indicação visual na fila; não altera tempo, dificuldade ou recompensa.
- `CustomStack` está implementada, mas reservada para evoluções futuras; não faz parte do loop atual.
- Power-ups não persistem entre sessões.
- O ranking local não é ordenado por desempenho.
- As cartas podem sortear dois power-ups do mesmo tipo de minigame, embora os efeitos sejam sempre diferentes.

---

## 14. Documentos relacionados

- [`README.md`](README.md) — visão geral, controles e instruções de execução.
