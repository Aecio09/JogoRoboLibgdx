package io.github.projetoalgoritmos.model;

public class Robo {
    private int id;
    private String modelo;
    private String prioridade;
    private int spriteType;
    private TipoDefeito defeito;

    public Robo(int id, String modelo, String prioridade, TipoDefeito defeito) {
        this.id = id;
        this.modelo = modelo;
        this.prioridade = prioridade;
        this.defeito = defeito;
        this.spriteType = 0;
    }

    public void setSpriteType(int type) { this.spriteType = type; }
    public int getSpriteType() { return spriteType; }

    public int getId() { return id; }
    public String getModelo() { return modelo; }
    public String getPrioridade() { return prioridade; }
    public TipoDefeito getDefeito() { return defeito; }
}
