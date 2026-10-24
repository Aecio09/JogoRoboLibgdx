# 🤖 Robot Repair Shop - Technical Documentation

Este projeto foi desenvolvido para a disciplina de **Algoritmos e Estruturas de Dados**, utilizando a biblioteca **LibGDX** para a interface gráfica e implementações manuais de estruturas de dados lineares.

---

## 🏗️ Estruturas de Dados Customizadas

Seguindo rigorosamente as diretrizes acadêmicas, o projeto **não utiliza** o framework Java Collections (`java.util.*`). Todas as estruturas foram implementadas do zero no pacote `io.github.projetoalgoritmos.datastructures`.

### 🔗 Lista Simplesmente Encadeada (`CustomLinkedList<T>`)
*   **Papel no Jogo:** Gerencia a fila de robôs na oficina e o armazenamento do ranking global.
*   **Operações Manuais:** Inserção, remoção por objeto, busca por índice e controle de tamanho através da manipulação de nós (`Node<T>`).
*   **Destaque Acadêmico:** A navegação pela fila de espera no painel lateral é feita percorrendo-se manualmente os ponteiros da lista.

### 📚 Pilha (`CustomStack<T>`)
*   **Papel no Jogo:** Cada robô possui uma pilha de componentes defeituosos.
*   **Lógica LIFO (Last-In, First-Out):** O técnico só pode reparar o componente que está no topo. O próximo item da lista de diagnóstico só é revelado após a remoção (`pop`) do anterior.
*   **Simulação de Reparo:** Representa a ordem técnica de desmontagem e montagem de uma unidade robótica.

---

## 🎨 Integração Visual e Gameplay

A interface foi projetada para simular um **Terminal de Operações Futurista (Cyberpunk)**, integrando a lógica de dados com feedback visual em tempo real.

### 🎮 Terminal de Comandos (Mecânica Alfanumérica)
Em vez de cliques simples, o jogador deve interagir com o **Terminal de Entrada** na parte inferior da tela. 
*   O sistema lê o componente no topo da pilha do robô ativo.
*   O jogador deve digitar o código identificador (ex: `CPU`, `MEMORIA_RAM`) e pressionar `ENTER`.
*   A validação é feita comparando o `inputBuffer` do teclado com o dado armazenado na estrutura de pilha.

### 🖥️ Efeito CRT e Shaders
Para uma imersão completa, foi implementado um **Shader de Pós-processamento (GLSL)**:
*   **Curvatura de Lente:** Simula a distorção física de monitores antigos.
*   **Scanlines:** Adiciona linhas de varredura horizontais.
*   **Aberração Cromática:** Leve separação de cores (RGB) nas bordas.
*   **Flicker:** Oscilação sutil de brilho.

### 🔬 Gerenciamento de Assets
*   **Background Animado:** Utilização de um `GifDecoder` customizado para processar o background `Laboratory.gif` frame a frame, contornando limitações nativas do LibGDX.
*   **Variedade de Unidades:** Sistema de animação que suporta 7 modelos diferentes de robôs, cada um com sua própria escala e velocidade de animação.

---

## 📊 Estatísticas e Persistência
Ao final de cada sessão (quando a oficina atinge o limite de 8 robôs), o jogo captura:
1.  **Total de Robôs Consertados.**
2.  **Total de Componentes Substituídos.**
3.  **Tempo Total de Sessão.**

Os dados são salvos em um arquivo local `ranking.txt`, utilizando a `CustomLinkedList` para organizar e exibir o histórico de desempenho dos operadores.

---

### 🚀 Como Executar
1.  Certifique-se de ter o Java 21+ instalado.
2.  No terminal, execute: `./gradlew :lwjgl3:run`
3.  Para gerar o executável (.jar): `./gradlew :lwjgl3:jar`
