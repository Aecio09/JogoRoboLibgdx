package io.github.projetoalgoritmos.model;

/**
 * Defeitos que um robô pode apresentar. Cada defeito está ligado
 * a um dos minigames disponíveis no protótipo.
 */
public enum TipoDefeito {
    VIRUS("VIRUS", "COMBATE DE VIRUS NO TERMINAL"),
    CURTO_CIRCUITO("CURTO-CIRCUITO", "RECONECTE AS TRILHAS DE ENERGIA");

    private final String nome;
    private final String descricao;

    TipoDefeito(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
}
