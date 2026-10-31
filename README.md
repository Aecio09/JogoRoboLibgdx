# Robo Repair Shop (ProjetoAlgoritimos)

Jogo 2D desenvolvido em **Java 21** com **LibGDX**, no qual o jogador assume o papel de operador de uma oficina robótica. A cada dia é preciso reparar uma fila de robôs resolvendo minigames antes que o cronômetro chegue a zero.

## Como funciona

1. Digite o nome do operador e pressione `Enter` para acessar o terminal.
2. Cada dia começa com uma fila de **2 a 6 robôs** e um tempo limitado. O tempo base é de 90 segundos, com 15 segundos extras para cada robô adicional.
3. Cada robô apresenta um defeito:
    - **VÍRUS** → minigame de combate no terminal.
    - **CURTO-CIRCUITO** → minigame de conexão de circuitos.
4. Reparos concluídos com sucesso rendem **+30 segundos** ao dia.
5. Se o tempo acabar, a sessão termina e o resultado é salvo no ranking local.
6. Ao reparar todos os robôs do dia, a garagem fecha e **duas cartas de power-up** são sorteadas.
7. Escolha uma carta, avance para o próximo dia e use o power-up guardado no reparo correspondente.

## Controles

| Contexto | Ação | Controle |
| --- | --- | --- |
| Login | Digitar o nome | Teclado |
| Login | Apagar caractere | `Backspace` |
| Login | Confirmar | `Enter` |
| Painel do dia | Selecionar robô | Clique na ficha |
| Painel do dia | Iniciar reparo | Clicar novamente no robô selecionado ou `Enter` |
| Painel do dia | Abrir ranking | `Tab` |
| Minigame de vírus | Mover | `WASD` ou setas |
| Minigame de vírus | Atirar | `Espaço` |
| Minigame de vírus | Reiniciar desafio | `R` |
| Minigame de vírus | Sair do reparo | `Esc` |
| Minigame de circuito | Girar peça | Clique na peça |
| Minigame de circuito | Armar Chip Curinga | `C` (quando o power-up estiver ativo) |
| Minigame de circuito | Reiniciar desafio | `R` |
| Minigame de circuito | Sair do reparo | `Esc` |
| Fim do dia | Escolher carta | Clique, `1` ou `2` |
| Fim do dia | Continuar | `Espaço` após selecionar |
| Ranking | Voltar ao painel | `Enter` ou `Esc` |
| Game over | Reiniciar sessão | `Enter` |

## Minigames implementados

Atualmente, o jogo possui dois minigames jogáveis.

### Combate de Vírus

O jogador controla um pequeno robô dentro da tela do computador. É preciso destruir todos os vírus antes de perder as **3 vidas** disponíveis.

- Os vírus se movem e ricocheteiam pelas bordas da tela.
- Cada contato com um vírus custa uma vida e concede um curto período de invulnerabilidade.
- A quantidade e a velocidade dos vírus aumentam conforme o dia avança.
- Tiros e vírus usam listas encadeadas customizadas.

### Labirinto de Circuitos

O objetivo é conectar a **fonte de energia** ao **chip** do outro lado da placa.

- Clique nas peças para girar suas conexões.
- O tabuleiro cresce conforme a dificuldade, indo de `5x7` até `7x9`.
- A energia é propagada por busca em largura (BFS) a cada jogada.
- O puzzle é sempre gerado com um caminho solucionável.
- O chip é energizado quando a conexão chega até ele.

## Minigames futuros

Os seguintes minigames foram planejados para futuras atualizações do projeto, mas ainda não estão implementados.

### Conexão de Fios

Um desafio de identificação e conexão de fios, no qual o jogador precisa ligar corretamente os terminais correspondentes sem cometer erros.

### Engrenagens

Um puzzle mecânico em que o jogador precisa posicionar e conectar engrenagens para transmitir movimento até o mecanismo principal.

### Reinicialização de Memória

Um desafio de memória no qual o jogador precisa observar e reproduzir sequências de comandos para restaurar os dados do robô.

### Torre Matemática

Um puzzle inspirado em jogos de desafios numéricos, no qual o jogador precisa resolver operações matemáticas e encontrar combinações corretas para avançar.

## Power-ups

Ao final do dia, duas cartas aleatórias aparecem sobre a garagem fechada. Elas flutuam, têm brilho pulsante e podem ser escolhidas com o mouse ou pelas teclas `1` e `2`. Depois da escolha, use `Espaço` para continuar.

O jogo guarda **um power-up por tipo de minigame**. O item é consumido quando um reparo daquele tipo começa. Se outro power-up do mesmo tipo for escolhido, ele substitui o anterior.

### Minigame de Vírus

| Power-up | Efeito |
| --- | --- |
| Antivírus em Leque | Dispara 3 projéteis em leque durante o reparo. |
| Overclock de Tiro | Aumenta a cadência e a velocidade dos tiros. |
| Firewall Pessoal | Absorve 1 hit e concede invulnerabilidade temporária. |
| Criogenia | Deixa todos os vírus 45% mais lentos. |
| Drone de Backup | Um drone orbita o jogador e atira automaticamente por 8 segundos. |

### Minigame de Circuito

| Power-up | Efeito |
| --- | --- |
| Diagnóstico de Rota | Destaca o caminho correto da fonte ao chip por 8 segundos. |
| Multímetro | Marca o próximo ponto de quebra do circuito por 8 segundos. |
| Chave de Precisão | Os próximos 3 cliques alinham peças do caminho à rotação correta. |
| Chip Curinga | Pressione `C` e clique em uma peça para transformá-la em uma cruz com 4 conexões (1 uso). |
| Fusível de Tempo | Adiciona 10 segundos ao cronômetro do dia. |

## Ranking

Ao final de uma sessão, o jogo registra:

- Nome do operador;
- Total de robôs consertados;
- Total de dias concluídos;
- Tempo total de sessão.

O histórico é salvo localmente em `ranking_dias.txt` e exibido no menu de ranking. As entradas são carregadas nas próximas execuções.

## Como executar

### Pré-requisitos

- **JDK 21** instalado e configurado no `PATH`.
- O projeto usa o Gradle Wrapper incluído no repositório.

### Desktop

Executar o jogo:

```sh
./gradlew :lwjgl3:run
```

Para gerar um JAR executável:

```sh
./gradlew :lwjgl3:jar
```

O arquivo é gerado em `lwjgl3/build/libs/ProjetoAlgoritimos-1.0.0.jar`.

### Android

Pré-requisitos adicionais:

- Android SDK com `platforms;android-36` e `build-tools;36.0.0`.
- Configure o SDK em `ANDROID_HOME`, `ANDROID_SDK_ROOT` ou em um arquivo `local.properties` na raiz:

```properties
sdk.dir=/caminho/para/o/android-sdk
```

Gerar e instalar o APK de debug:

```sh
./gradlew :android:assembleDebug
adb install -r android/build/outputs/apk/debug/android-debug.apk
```

Ou instalar e abrir diretamente em um dispositivo/emulador conectado:

```sh
./gradlew :android:run
```

> A versão Android compila e empacota o jogo, mas os minigames foram projetados para teclado e mouse. O controle por toque ainda não está completo.

## Estrutura do projeto

```text
core/       lógica compartilhada do jogo
lwjgl3/     launcher desktop (LWJGL3)
android/    launcher e empacotamento Android
assets/     imagens, fontes, shaders e GIFs
```

Principais arquivos:

- `core/src/main/java/io/github/projetoalgoritmos/Main.java` — loop principal, telas, dias, cartas e ranking.
- `core/src/main/java/io/github/projetoalgoritmos/controller/GameController.java` — fila de robôs, tempo e progresso.
- `core/src/main/java/io/github/projetoalgoritmos/minigames/` — minigames de vírus e circuito.
- `core/src/main/java/io/github/projetoalgoritmos/model/Powerup.java` — catálogo e sorteio dos power-ups.
- `core/src/main/java/io/github/projetoalgoritmos/datastructures/` — estruturas de dados implementadas manualmente.

## Tecnologias

- Java 21
- LibGDX 1.14.0
- LWJGL3
- Gradle
- FreeType para as fontes `BoldPixels.ttf`
- Shader CRT customizado
- Android SDK 36 / minSdk 21

Para detalhes de arquitetura, regras internas e decisões de implementação, consulte o [`README_TECNICO.md`](README_TECNICO.md).
