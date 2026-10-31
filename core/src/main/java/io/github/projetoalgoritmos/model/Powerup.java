package io.github.projetoalgoritmos.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class Powerup {

    public enum Tipo {
        VIRUS,
        CIRCUITO
    }

    public enum Efeito {
        LEQUE,
        OVERCLOCK,
        FIREWALL,
        CRIOGENIA,
        DRONE,
        DIAGNOSTICO,
        MULTIMETRO,
        PRECISAO,
        CURINGA,
        TEMPO_EXTRA
    }

    private static final Powerup[] POOL = {
        new Powerup(Efeito.LEQUE, Tipo.VIRUS, "ANTIVIRUS EM LEQUE", "LEQUE",
            new String[]{"3 TIROS EM LEQUE", "DURANTE O REPARO"},
            new Color(0.10f, 0.85f, 1.00f, 1f)),

        new Powerup(Efeito.OVERCLOCK, Tipo.VIRUS, "OVERCLOCK DE TIRO", "OVERCLOCK",
            new String[]{"TIRO MUITO MAIS", "RAPIDO"},
            new Color(1.00f, 0.55f, 0.15f, 1f)),

        new Powerup(Efeito.FIREWALL, Tipo.VIRUS, "FIREWALL PESSOAL", "FIREWALL",
            new String[]{"ABSORVE 1 HIT", "COM INVULNERABILIDADE"},
            new Color(0.30f, 0.65f, 1.00f, 1f)),

        new Powerup(Efeito.CRIOGENIA, Tipo.VIRUS, "CRIOGENIA", "CRIOGENIA",
            new String[]{"VIRUS 45% MAIS", "LENTOS"},
            new Color(0.65f, 0.90f, 1.00f, 1f)),

        new Powerup(Efeito.DRONE, Tipo.VIRUS, "DRONE DE BACKUP", "DRONE",
            new String[]{"DRONE ALIADO ATIRA", "AUTOMATICAMENTE"},
            new Color(0.25f, 1.00f, 0.55f, 1f)),

        new Powerup(Efeito.DIAGNOSTICO, Tipo.CIRCUITO, "DIAGNOSTICO DE ROTA", "DIAGNOSTICO",
            new String[]{"REVELA O CAMINHO", "DA FONTE AO CHIP"},
            new Color(0.10f, 0.90f, 1.00f, 1f)),

        new Powerup(Efeito.MULTIMETRO, Tipo.CIRCUITO, "MULTIMETRO", "MULTIMETRO",
            new String[]{"MARCA O PROXIMO", "PONTO DE QUEBRA"},
            new Color(0.35f, 1.00f, 0.55f, 1f)),

        new Powerup(Efeito.PRECISAO, Tipo.CIRCUITO, "CHAVE DE PRECISAO", "PRECISAO",
            new String[]{"3 CLIQUES GIRAM", "PARA A POSICAO CERTA"},
            new Color(0.75f, 0.55f, 1.00f, 1f)),

        new Powerup(Efeito.CURINGA, Tipo.CIRCUITO, "CHIP CURINGA", "CURINGA",
            new String[]{"1 PECA VIRA CRUZ", "COM 4 CONEXOES"},
            new Color(1.00f, 0.45f, 0.95f, 1f)),

        new Powerup(Efeito.TEMPO_EXTRA, Tipo.CIRCUITO, "FUSIVEL DE TEMPO", "+10s",
            new String[]{"+10 SEGUNDOS", "NO CRONOMETRO DO DIA"},
            new Color(0.45f, 0.75f, 1.00f, 1f))
    };

    private final Efeito efeito;
    private final Tipo tipo;
    private final String nome;
    private final String nomeCurto;
    private final String[] descricao;
    private final Color cor;

    private Powerup(Efeito efeito, Tipo tipo, String nome, String nomeCurto,
                    String[] descricao, Color cor) {
        this.efeito = efeito;
        this.tipo = tipo;
        this.nome = nome;
        this.nomeCurto = nomeCurto;
        this.descricao = descricao;
        this.cor = cor;
    }

    /** Sorteia duas cartas diferentes para a tela de fim de dia. */
    public static Powerup[] sortearDuas() {
        int primeiro = MathUtils.random(POOL.length - 1);
        int segundo = MathUtils.random(POOL.length - 2);
        if (segundo >= primeiro) {
            segundo++;
        }
        return new Powerup[]{POOL[primeiro], POOL[segundo]};
    }

    public Efeito getEfeito() { return efeito; }
    public Tipo getTipo() { return tipo; }
    public String getNome() { return nome; }
    public String getNomeCurto() { return nomeCurto; }
    public String[] getDescricao() { return descricao; }
    public Color getCor() { return cor; }
}
