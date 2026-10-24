package io.github.projetoalgoritmos.model;

import io.github.projetoalgoritmos.datastructures.CustomStack;

public class Robo {
    private int id;
    private String modelo;
    private String prioridade; 
    private boolean consertado;
    private int spriteType;
    private CustomStack<Componente> pilhaComponentes;

    public Robo(int id, String modelo, String prioridade) {
        this.id = id;
        this.modelo = modelo;
        this.prioridade = prioridade;
        this.consertado = false;
        this.spriteType = 0; // Default
        this.pilhaComponentes = new CustomStack<>();
    }

    public void setSpriteType(int type) { this.spriteType = type; }
    public int getSpriteType() { return spriteType; }

    public void adicionarComponente(Componente c) {
        pilhaComponentes.push(c);
    }

    public Componente removerComponente() {
        Componente c = pilhaComponentes.pop();
        if (pilhaComponentes.isEmpty()) {
            this.consertado = true;
        }
        return c;
    }

    public int getId() { return id; }
    public String getModelo() { return modelo; }
    public String getPrioridade() { return prioridade; }
    public boolean isConsertado() { return consertado; }
    public CustomStack<Componente> getPilhaComponentes() { return pilhaComponentes; }
}
